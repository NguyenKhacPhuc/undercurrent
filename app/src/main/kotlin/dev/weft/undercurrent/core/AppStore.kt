package dev.weft.undercurrent.core

import dev.weft.undercurrent.features.chat.buildSkillRegistry
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.weft.android.WeftRuntime
import dev.weft.contracts.FileSaveSpec
import dev.weft.contracts.ShareContent
import dev.weft.contracts.ShareTarget
import dev.weft.contracts.UIUpdate
import dev.weft.harness.agents.streaming.StreamChunk
import dev.weft.harness.conversation.PersistedRole
import dev.weft.harness.observability.AgentTrace
import dev.weft.android.credentials.StaticDeepSeekKeyProvider
import dev.weft.android.credentials.StaticKeyProvider
import dev.weft.android.credentials.StaticOpenAIKeyProvider
import dev.weft.android.credentials.StaticOpenRouterKeyProvider
import dev.weft.android.routing.defaultPoolFor
import dev.weft.android.routing.findModelInCatalog
import dev.weft.contracts.ProviderKind
import dev.weft.contracts.WeftCredentialProvider
import dev.weft.harness.agents.routing.ModelTier
import dev.weft.harness.agents.routing.ModelPool
import dev.weft.harness.skills.SkillRegistry
import dev.weft.harness.skills.SkillResult
import dev.weft.undercurrent.features.onboarding.OnboardingRepository
import dev.weft.undercurrent.features.providers.ModelPrefsRepository
import dev.weft.undercurrent.features.providers.ProviderPrefsRepository
import dev.weft.undercurrent.features.providers.keyAlias
import dev.weft.undercurrent.theme.ThemeRepository
import dev.weft.undercurrent.features.chat.DisplayMessage
import dev.weft.undercurrent.features.chat.DisplayRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Root MVI store for [App]. Owns the screen state machine, the agent
 * reference, and the streaming message list. Holds no Compose-side UI
 * objects directly — dependencies arrive via constructor injection from
 * Koin (see `dev.weft.undercurrent.di.appModule`).
 *
 * Surfaces:
 *  - [state] — observable [AppState].
 *  - [effects] — one-shot side-effects (mostly errors).
 *  - [displayMessages] — kept as [SnapshotStateList] for interop with
 *    [ui.ChatScreen], which appends streaming chunks directly. See the
 *    note in [AppState] for why this isn't part of the reduced state.
 *  - [dispatch] — fire-and-forget intent submission.
 *  - [sendUiEvent], [saveKey] — suspend helpers for paths that await a reply.
 */
internal class AppStore(
    private val runtime: WeftRuntime,
    private val themeRepo: ThemeRepository,
    private val onboardingRepo: OnboardingRepository,
    // PersonaRepository is no longer a constructor dep here — the Personas
    // screen owns it via PersonasViewModel, and the runtime's
    // `extraVolatilePrefix` lambda (configured in di/AppModule.kt) closes
    // over the same Koin singleton, so persona changes propagate on the
    // next agent turn without AppStore needing the reference.
    private val providerPrefsRepo: ProviderPrefsRepository,
    val modelPrefsRepo: ModelPrefsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AppState.initial())
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        // Mirror persisted theme prefs into state. DataStore is the source
        // of truth — SetPalette/SetThemeMode intents write to the repo,
        // and this collector picks the new values back up.
        viewModelScope.launch {
            themeRepo.prefsFlow.collect { prefs ->
                _state.update { it.copy(themePrefs = prefs) }
            }
        }
        // Same pattern for onboarding completion.
        viewModelScope.launch {
            onboardingRepo.completedFlow.collect { done ->
                _state.update { it.copy(onboardingCompleted = done) }
            }
        }
        // Provider + default tier — also DataStore-backed.
        viewModelScope.launch {
            providerPrefsRepo.activeProvider.collect { provider ->
                _state.update { it.copy(activeProvider = provider) }
            }
        }
        viewModelScope.launch {
            providerPrefsRepo.defaultTier.collect { tier ->
                _state.update { it.copy(defaultTier = tier) }
            }
        }
    }

    private val _effects = Channel<AppEffect>(Channel.BUFFERED)
    val effects: Flow<AppEffect> = _effects.receiveAsFlow()

    val displayMessages: SnapshotStateList<DisplayMessage> = mutableStateListOf()

    /**
     * Skill registry, derived from the runtime. Exposed read-only so
     * [ui.ChatScreen] can render the `[+]` quick-actions menu. Resolution
     * (the fast-path that bypasses the LLM) happens inside [handleSendChat].
     */
    val skills: SkillRegistry = buildSkillRegistry(runtime)

    fun dispatch(intent: AppIntent) {
        when (intent) {
            AppIntent.Resume -> viewModelScope.launch { handleResume() }
            is AppIntent.SubmitKey -> viewModelScope.launch { handleSubmitKey(intent.key) }
            is AppIntent.Navigate -> _state.update { it.copy(screen = intent.screen) }
            is AppIntent.SelectConversation -> viewModelScope.launch { handleSelectConversation(intent.id) }
            AppIntent.NewChat -> viewModelScope.launch { handleNewChat() }
            AppIntent.DeleteCurrentConversation -> viewModelScope.launch {
                val a = _state.value.agent ?: return@launch
                handleDeleteConversation(a.currentConversationId.value)
            }
            is AppIntent.DeleteConversation -> viewModelScope.launch {
                handleDeleteConversation(intent.id)
            }
            is AppIntent.SendChat -> viewModelScope.launch { handleSendChat(intent.text, intent.modelTier) }
            AppIntent.RegenerateLast -> viewModelScope.launch { handleRegenerateLast() }
            is AppIntent.ExportTrace -> viewModelScope.launch { handleExportTrace(intent.trace) }
            is AppIntent.UiBridgeUpdate -> handleUiBridgeUpdate(intent.update)
            is AppIntent.SetPalette -> viewModelScope.launch { themeRepo.setPalette(intent.palette) }
            is AppIntent.SetThemeMode -> viewModelScope.launch { themeRepo.setMode(intent.mode) }
            AppIntent.CompleteOnboarding -> viewModelScope.launch {
                onboardingRepo.markCompleted()
                _state.update { it.copy(screen = Screen.KeyPaste) }
            }
            is AppIntent.SetProvider -> viewModelScope.launch { handleSetProvider(intent.provider) }
            is AppIntent.SaveProviderKey -> viewModelScope.launch {
                handleSaveProviderKey(intent.provider, intent.apiKey)
            }
            is AppIntent.RemoveProviderKey -> viewModelScope.launch {
                handleRemoveProviderKey(intent.provider)
            }
            is AppIntent.SetDefaultTier -> viewModelScope.launch {
                providerPrefsRepo.setDefaultTier(intent.tier)
            }
            is AppIntent.SetModelForTier -> viewModelScope.launch {
                handleSetModelForTier(intent.provider, intent.tier, intent.modelId)
            }
        }
    }

    /**
     * Suspend-shaped because [dev.weft.compose.components.AgentRenderedTreeScreen]'s
     * `onAction` is suspend and the screen awaits completion (for in-flight UI).
     * Mutations to [displayMessages] flow through here so the store stays the
     * single source of truth for that list.
     */
    suspend fun sendUiEvent(action: String, sourceLabel: String?, fieldValues: Map<String, String>) {
        displayMessages += DisplayMessage.event(action, sourceLabel, fieldValues)
        val a = _state.value.agent ?: return
        val result = runCatching {
            withContext(Dispatchers.IO) { a.sendEvent(action, sourceLabel, fieldValues) }
        }
        result.fold(
            onSuccess = { reply -> displayMessages += DisplayMessage.assistant(reply) },
            onFailure = { t ->
                displayMessages += DisplayMessage.toolFail(
                    "ui_event",
                    t.message ?: t::class.simpleName.orEmpty(),
                )
            },
        )
    }

    /**
     * Persists a freshly-pasted API key to the secure key vault under
     * the currently-active provider's alias. Suspend because
     * [ui.KeyPasteScreen] declares its `saveKey` callback suspend so the
     * paste flow can serialize "save → build agent → navigate."
     */
    suspend fun saveKey(key: String) {
        val provider = providerPrefsRepo.activeProviderNow()
        runtime.keyVault.put(provider.keyAlias(), key)
    }

    private suspend fun handleResume() {
        // Gate 1: has the user finished first-launch onboarding?
        // Read directly from the repo's flow rather than `_state.value` —
        // the init collector might not have written the first emission
        // yet when Resume is dispatched, which would race onboarding back
        // on every cold start.
        val onboardingDone = onboardingRepo.completedFlow.first()
        if (!onboardingDone) {
            _state.update { it.copy(screen = Screen.Onboarding) }
            return
        }
        // Gate 2: is there a stored API key for the active provider?
        val activeProvider = providerPrefsRepo.activeProviderNow()
        val storedKey = withContext(Dispatchers.IO) {
            runtime.keyVault.get(activeProvider.keyAlias())
        }
        if (storedKey == null) {
            _state.update { it.copy(screen = Screen.KeyPaste) }
            return
        }
        val a = runtime.buildAgent(
            credentialProviderFor(activeProvider, storedKey),
            modelPoolOverride = modelPoolOverrideFor(activeProvider),
        )
        a.resume()
        hydrateMessages(a.currentConversationId.value)
        _state.update { it.copy(agent = a, screen = Screen.Chat) }
    }

    /**
     * Build the right [WeftCredentialProvider] for the active backend.
     * Anthropic uses the default x-api-key flow; OpenAI bears a
     * `Authorization: Bearer …` against api.openai.com.
     */
    private fun credentialProviderFor(provider: ProviderKind, apiKey: String): WeftCredentialProvider =
        when (provider) {
            ProviderKind.Anthropic -> StaticKeyProvider(apiKey)
            ProviderKind.OpenAI -> StaticOpenAIKeyProvider(apiKey)
            ProviderKind.OpenRouter -> StaticOpenRouterKeyProvider(apiKey)
            ProviderKind.DeepSeek -> StaticDeepSeekKeyProvider(apiKey)
        }

    /**
     * Resolve the current model-override pool for [provider] from the
     * model-prefs repo. Returns null when the user has no overrides set
     * for this provider — caller should let the runtime use its default
     * pool. Returns a sparse-merged [ModelPool] when at least one slot
     * is overridden; unset slots fall back to defaults from the runtime.
     *
     * Slot resolution: look up the override id in the repo, then resolve
     * to a typed [ai.koog.prompt.llm.LLModel] via
     * [findModelInCatalog]. Stale ids (model removed from catalog after
     * a Koog upgrade) skip gracefully — the default fills in.
     */
    /**
     * Compose the [ModelPool] for [provider] by merging user overrides
     * from [modelPrefsRepo] with the SDK's defaults. Returns null when
     * the user has no overrides for any slot — caller passes null to
     * [WeftRuntime.buildAgent] and the runtime uses its own defaults.
     */
    private fun modelPoolOverrideFor(provider: ProviderKind): ModelPool? {
        val anyOverride = ModelTier.entries.any { tier ->
            modelPrefsRepo.overrideFor(provider, tier) != null
        }
        if (!anyOverride) return null

        val defaultPool = defaultPoolFor(provider)
        fun resolve(tier: ModelTier, default: ai.koog.prompt.llm.LLModel) =
            modelPrefsRepo.overrideFor(provider, tier)
                ?.let { findModelInCatalog(provider, it) }
                ?: default

        return ModelPool(
            cheap = resolve(ModelTier.Cheap, defaultPool.cheap),
            standard = resolve(ModelTier.Standard, defaultPool.standard),
            vision = resolve(ModelTier.Vision, defaultPool.vision),
            heavy = resolve(ModelTier.Heavy, defaultPool.heavy),
        )
    }

    private suspend fun handleSetModelForTier(
        provider: ProviderKind,
        tier: ModelTier,
        modelId: String?,
    ) {
        modelPrefsRepo.setOverride(provider, tier, modelId)
        // If the changed provider is active, rebuild the agent so the
        // next send uses the new model pool. Otherwise the change is
        // just persistence — Settings UI will reflect it on next read.
        val activeProvider = providerPrefsRepo.activeProviderNow()
        if (provider == activeProvider) {
            val key = withContext(Dispatchers.IO) {
                runtime.keyVault.get(provider.keyAlias())
            } ?: return  // no key yet → nothing to rebuild
            val override = modelPoolOverrideFor(provider)
            val a = runtime.buildAgent(
                credentialProviderFor(provider, key),
                modelPoolOverride = override,
            )
            a.resume()
            hydrateMessages(a.currentConversationId.value)
            _state.update { it.copy(agent = a) }
        }
    }

    private suspend fun handleSubmitKey(key: String) {
        val activeProvider = providerPrefsRepo.activeProviderNow()
        val a = runtime.buildAgent(
            credentialProviderFor(activeProvider, key),
            modelPoolOverride = modelPoolOverrideFor(activeProvider),
        )
        a.resume()
        _state.update { it.copy(agent = a, screen = Screen.Chat) }
    }

    /**
     * Switch the active provider. If a key is already stored for the new
     * provider, rebuild the agent and stay on the current screen. If not,
     * we leave [state.agent] null — Settings shows the inline paste prompt
     * for whatever provider is missing.
     */
    private suspend fun handleSetProvider(provider: ProviderKind) {
        providerPrefsRepo.setProvider(provider)
        val key = withContext(Dispatchers.IO) {
            runtime.keyVault.get(provider.keyAlias())
        }
        if (key != null) {
            val a = runtime.buildAgent(
                credentialProviderFor(provider, key),
                modelPoolOverride = modelPoolOverrideFor(provider),
            )
            a.resume()
            hydrateMessages(a.currentConversationId.value)
            _state.update { it.copy(agent = a) }
        } else {
            // No key for the new provider — drop the current agent so the
            // chat surface doesn't keep sending to the *old* backend.
            // Settings is where the user will paste the new key.
            _state.update { it.copy(agent = null) }
        }
    }

    /**
     * Persist a provider's API key. If it matches the currently active
     * provider, also (re)build the agent so the next send goes through.
     */
    private suspend fun handleSaveProviderKey(provider: ProviderKind, key: String) {
        withContext(Dispatchers.IO) {
            runtime.keyVault.put(provider.keyAlias(), key)
        }
        val activeProvider = providerPrefsRepo.activeProviderNow()
        if (provider == activeProvider) {
            val a = runtime.buildAgent(
                credentialProviderFor(provider, key),
                modelPoolOverride = modelPoolOverrideFor(provider),
            )
            a.resume()
            hydrateMessages(a.currentConversationId.value)
            _state.update { it.copy(agent = a) }
        }
    }

    /**
     * Delete a provider's stored key. If we just removed the *active*
     * provider's key, drop the agent reference and navigate the user back
     * to [Screen.KeyPaste] — they now have no working credential and the
     * chat surface would silently fail next send otherwise.
     *
     * For an inactive provider the only effect is the next Settings read
     * showing "No key set" — no agent rebuild needed.
     */
    private suspend fun handleRemoveProviderKey(provider: ProviderKind) {
        withContext(Dispatchers.IO) {
            runtime.keyVault.remove(provider.keyAlias())
        }
        val activeProvider = providerPrefsRepo.activeProviderNow()
        if (provider == activeProvider) {
            _state.update { it.copy(agent = null, screen = Screen.KeyPaste) }
        }
    }

    private suspend fun handleSelectConversation(id: String) {
        val a = _state.value.agent ?: return
        a.resume(id)
        hydrateMessages(id)
        _state.update { it.copy(screen = Screen.Chat) }
    }

    private suspend fun handleNewChat() {
        val a = _state.value.agent ?: return
        a.newChat()
        displayMessages.clear()
        _state.update { it.copy(screen = Screen.Chat, chat = ChatStatus()) }
    }

    /**
     * Delete a conversation by id. If [id] matches the currently-active
     * conversation, the chat surface resets — agent rolls to a fresh
     * thread, displayMessages cleared, in-flight status reset — so the
     * user isn't left looking at a thread that no longer exists. If
     * [id] is some other conversation (e.g. user deleted from the
     * drawer's recent list), the active thread is untouched and only
     * the row disappears from the conversation-store flow.
     *
     * Order matters: delete first, then `newChat()`. If we did newChat
     * first, the agent would auto-create + persist a new (empty)
     * conversation with a new id, and we'd then be deleting the *old*
     * one — fine, same outcome. Deleting first keeps the intent
     * obvious in traces.
     *
     * Both [AppIntent.DeleteCurrentConversation] (chat header overflow)
     * and [AppIntent.DeleteConversation] (drawer long-press) route here.
     */
    private suspend fun handleDeleteConversation(id: String) {
        val a = _state.value.agent ?: return
        val wasActive = a.currentConversationId.value == id
        withContext(Dispatchers.IO) {
            runtime.conversationStore.deleteConversation(id)
        }
        if (wasActive) {
            a.newChat()
            displayMessages.clear()
            _state.update { it.copy(screen = Screen.Chat, chat = ChatStatus()) }
        }
    }

    /**
     * Re-run the last user message. Removes everything in [displayMessages]
     * after the last user bubble (so the rollback matches what the SDK is
     * about to do to history + persistence), flips to in-flight, then
     * collects from [dev.weft.harness.agents.WeftAgent.regenerateStreaming].
     *
     * The SDK takes care of (1) popping the last user+assistant from
     * in-memory history, (2) deleting the same pair from the conversation
     * store, and (3) re-running the user text through `sendStreaming`.
     * From the app's perspective this looks like a normal stream that
     * doesn't need a leading user-bubble append.
     */
    private suspend fun handleRegenerateLast() {
        if (_state.value.chat.inFlight) return
        val a = _state.value.agent ?: return
        val lastUserIdx = displayMessages.indexOfLast { it.role == DisplayRole.USER }
        if (lastUserIdx == -1) return

        // Roll back display to keep just-through-the-last-user message.
        while (displayMessages.size > lastUserIdx + 1) {
            displayMessages.removeAt(displayMessages.size - 1)
        }

        _state.update { it.copy(chat = ChatStatus(inFlight = true, lastError = null)) }
        consumeAgentStream(a.regenerateStreaming())
        _state.update { it.copy(chat = it.chat.copy(inFlight = false)) }
    }

    private suspend fun handleSendChat(
        text: String,
        modelTier: dev.weft.harness.agents.routing.ModelTier? = null,
    ) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        val a = _state.value.agent ?: return
        if (_state.value.chat.inFlight) return

        _state.update { it.copy(chat = ChatStatus(inFlight = true, lastError = null)) }
        displayMessages += DisplayMessage.user(trimmed)

        // Fast-path: if the input matches a registered skill, run it
        // locally and render its result as a chat bubble. No agent call
        // → saves a round-trip, saves tokens, keeps the payload on-device.
        val match = skills.resolve(trimmed)
        if (match != null) {
            val result = runCatching { match.skill.execute(match.payload) }
                .getOrElse { SkillResult.Fail(it.message ?: it::class.simpleName.orEmpty()) }
            when (result) {
                is SkillResult.Ok ->
                    displayMessages += DisplayMessage.toolDone(
                        "/${match.skill.name}: ${result.message}",
                    )
                is SkillResult.Fail ->
                    displayMessages += DisplayMessage.toolFail(
                        "/${match.skill.name}",
                        result.message,
                    )
            }
            _state.update { it.copy(chat = it.chat.copy(inFlight = false)) }
            return
        }

        // Tier resolution: per-message override > Settings default > null
        // (router decides). `Auto` from the UI maps to null here.
        val effectiveTier = modelTier ?: _state.value.defaultTier
        consumeAgentStream(a.sendStreaming(trimmed, modelTier = effectiveTier))
        _state.update { it.copy(chat = it.chat.copy(inFlight = false)) }
    }

    /**
     * Shared consumer for the agent's streaming output. Reduces
     * [StreamChunk]s into [displayMessages] and writes any in-flight
     * errors to [ChatStatus.lastError]. Used by both [handleSendChat]
     * (after the leading user bubble is appended) and [handleRegenerateLast]
     * (after the previous reply is rolled back) — both paths share the
     * same delta/tool/done/failed handling so the streaming UX stays
     * identical regardless of how the turn was initiated.
     */
    private suspend fun consumeAgentStream(stream: kotlinx.coroutines.flow.Flow<StreamChunk>) {
        // Sentinel for the assistant bubble we're streaming into. Lazily
        // created on the first TextDelta so we don't show an empty bubble
        // while the model is "thinking" (the LazyColumn's "Thinking…"
        // indicator covers that gap).
        var streamingMessageId: Long? = null
        runCatching {
            stream.flowOn(Dispatchers.IO).collect { chunk ->
                when (chunk) {
                    is StreamChunk.TextDelta -> {
                        val existingId = streamingMessageId
                        if (existingId == null) {
                            val msg = DisplayMessage.assistant(chunk.text)
                            streamingMessageId = msg.id
                            displayMessages += msg
                        } else {
                            // SnapshotStateList doesn't expose in-place mutation,
                            // so rebuild the index and replace the entry.
                            val idx = displayMessages.indexOfLast { it.id == existingId }
                            if (idx >= 0) {
                                val current = displayMessages[idx]
                                displayMessages[idx] = current.copy(text = current.text + chunk.text)
                            }
                        }
                    }
                    is StreamChunk.ToolStarting ->
                        displayMessages += DisplayMessage.toolStart(chunk.toolName)
                    is StreamChunk.ToolCompleted ->
                        displayMessages += DisplayMessage.toolDone(chunk.toolName)
                    is StreamChunk.ToolFailed ->
                        displayMessages += DisplayMessage.toolFail(chunk.toolName, chunk.message)
                    is StreamChunk.Done -> {
                        // If no TextDelta arrived (rare — the response ended with
                        // a tool call only, no follow-up text) append the final
                        // reply now.
                        if (streamingMessageId == null && chunk.finalReply.isNotBlank()) {
                            displayMessages += DisplayMessage.assistant(chunk.finalReply)
                        }
                    }
                    is StreamChunk.Failed ->
                        _state.update { it.copy(chat = it.chat.copy(lastError = chunk.message)) }
                }
            }
        }.onFailure { t ->
            _state.update {
                val existing = it.chat.lastError
                it.copy(
                    chat = it.chat.copy(
                        lastError = existing ?: (t.message ?: t::class.simpleName.orEmpty()),
                    ),
                )
            }
        }
    }

    private suspend fun handleExportTrace(trace: AgentTrace) {
        try {
            // Run the substrate redactor over the JSON before the file even
            // hits disk. Defense-in-depth: most fields were already redacted
            // at write-time inside WeftAgent, but userMessage is the user's
            // original input (never touched on the way in). One more pass
            // here catches anything that slipped, and protects against future
            // rules being added without re-running over old traces.
            val rawJson = TRACE_JSON.encodeToString(AgentTrace.serializer(), trace)
            val json = runtime.redactor.redact(rawJson)
            val ref = withContext(Dispatchers.IO) {
                runtime.os.files.save(
                    FileSaveSpec(
                        name = "trace-${trace.id}.json",
                        text = json,
                        mimeType = "application/json",
                    ),
                )
            }
            runtime.os.sharing.share(
                ShareContent(fileUri = ref.uri),
                ShareTarget.SystemSheet,
            )
        } catch (t: Throwable) {
            _effects.trySend(
                AppEffect.Error("Trace export failed: ${t.message ?: t::class.simpleName.orEmpty()}"),
            )
        }
    }

    private fun handleUiBridgeUpdate(update: UIUpdate?) {
        if (update is UIUpdate.RenderTree && _state.value.screen !is Screen.RenderedTree) {
            _state.update { it.copy(screen = Screen.RenderedTree) }
        }
    }

    private suspend fun hydrateMessages(convId: String) {
        val msgs = withContext(Dispatchers.IO) {
            runtime.conversationStore.loadMessages(convId)
        }
        displayMessages.clear()
        for (m in msgs) {
            displayMessages += when (m.role) {
                PersistedRole.USER -> DisplayMessage.user(m.content)
                PersistedRole.ASSISTANT -> DisplayMessage.assistant(m.content)
            }
        }
    }

    companion object {
        private val TRACE_JSON = Json {
            prettyPrint = true
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    }
}
