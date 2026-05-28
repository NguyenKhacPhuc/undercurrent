package dev.weft.undercurrent.core

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.viewModelScope
import dev.weft.android.WeftRuntime
import dev.weft.android.credentials.StaticDeepSeekKeyProvider
import dev.weft.android.credentials.StaticKeyProvider
import dev.weft.android.credentials.StaticOpenAIKeyProvider
import dev.weft.android.credentials.StaticOpenRouterKeyProvider
import dev.weft.android.routing.defaultPoolFor
import dev.weft.android.routing.findModelInCatalog
import dev.weft.contracts.FileSaveSpec
import dev.weft.contracts.ShareContent
import dev.weft.contracts.ShareTarget
import dev.weft.contracts.UIUpdate
import dev.weft.contracts.WeftCredentialProvider
import dev.weft.harness.agents.WeftAgent
import dev.weft.harness.agents.routing.ModelPool
import dev.weft.harness.agents.streaming.StreamChunk
import dev.weft.harness.conversation.PersistedRole
import dev.weft.harness.observability.AgentTrace
import dev.weft.harness.skills.SkillRegistry
import dev.weft.harness.skills.SkillResult
import dev.weft.harness.skills.withHelp
import dev.weft.undercurrent.app.AgentSummary
import dev.weft.undercurrent.app.AppIntent
import dev.weft.undercurrent.app.AppState
import dev.weft.undercurrent.app.AppStore
import dev.weft.undercurrent.app.ChatStatus
import dev.weft.undercurrent.app.PermissionDialogState
import dev.weft.undercurrent.core.model.AppEffect
import dev.weft.undercurrent.core.model.ModelTier
import dev.weft.undercurrent.core.model.ProviderKind
import dev.weft.undercurrent.core.navigation.NavigationChannel
import dev.weft.undercurrent.core.navigation.Screen
import dev.weft.undercurrent.data.datastore.MiniAppsRepository
import dev.weft.undercurrent.data.datastore.ModelPrefsRepository
import dev.weft.undercurrent.data.datastore.OnboardingRepository
import dev.weft.undercurrent.data.datastore.ProviderPrefsRepository
import dev.weft.undercurrent.data.datastore.ThemeRepository
import dev.weft.undercurrent.feature.chat.DisplayMessage
import dev.weft.undercurrent.feature.chat.DisplayRole
import dev.weft.undercurrent.feature.chat.SkillSummary
import dev.weft.undercurrent.feature.creator.CreatorKind
import dev.weft.undercurrent.feature.creator.CreatorSession
import dev.weft.undercurrent.shared.gateway.UiBridgeGateway
import dev.weft.undercurrent.shared.mvi.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import dev.weft.contracts.ProviderKind as WeftProviderKind
import dev.weft.harness.agents.routing.ModelTier as WeftModelTier

/**
 * Android impl of [AppStore]. Closes over [WeftRuntime] directly — the
 * full agent loop, credential providers, model-pool resolution, and
 * trace export live here.
 *
 * Type policy: the interface in `:composeApp/commonMain` exposes mirror
 * types only; this class translates to Weft's enums at the runtime
 * boundary via `toWeft()` / `toMirror()`.
 *
 * `agent: WeftAgent?` is private — the App composable reads
 * `state.agentReady` and `state.currentConversationId`, never the
 * agent itself.
 */
internal class WeftAppStore(
    private val runtime: WeftRuntime,
    private val themeRepo: ThemeRepository,
    private val onboardingRepo: OnboardingRepository,
    private val providerPrefsRepo: ProviderPrefsRepository,
    private val modelPrefsRepo: ModelPrefsRepository,
    private val navigationChannel: NavigationChannel,
    private val miniAppsRepo: MiniAppsRepository,
    private val creatorSession: CreatorSession,
    private val uiBridge: UiBridgeGateway,
) : Store<AppState, AppIntent, AppEffect>(
    initialState = AppState.initial(),
), AppStore {

    override val displayMessages: SnapshotStateList<DisplayMessage> = mutableStateListOf()

    private val skillRegistry: SkillRegistry = SkillRegistry(skills = emptyList()).withHelp()
    override val skills: List<SkillSummary> =
        skillRegistry.all().map { SkillSummary(it.name, it.description) }

    /** Internal — never exposed via the AppStore interface. */
    private var agent: WeftAgent? = null

    @Volatile
    private var activeMiniAppInvocationId: String? = null

    init {
        viewModelScope.launch {
            themeRepo.prefsFlow.collect { prefs ->
                update { it.copy(themePrefs = prefs) }
            }
        }
        viewModelScope.launch {
            onboardingRepo.completedFlow.collect { done ->
                update { it.copy(onboardingCompleted = done) }
            }
        }
        viewModelScope.launch {
            providerPrefsRepo.activeProvider.collect { provider ->
                update { it.copy(activeProvider = provider) }
                refreshProviderKeyStatus()
            }
        }
        viewModelScope.launch {
            navigationChannel.requests.collect { screen ->
                dispatch(AppIntent.Navigate(screen))
            }
        }
        viewModelScope.launch {
            providerPrefsRepo.defaultTier.collect { tier ->
                update { it.copy(defaultTier = tier) }
            }
        }
        // Forward agent `ui_render` events into the intent stream so
        // navigation + cached-tree capture happen in one place.
        viewModelScope.launch {
            uiBridge.renderEvents.collect { event ->
                dispatch(AppIntent.UiBridgeUpdate(event))
            }
        }
        // Initial key-status snapshot. The activeProvider collector
        // above also triggers a refresh, but it runs asynchronously —
        // do an eager pass so the providers screen has data on first
        // mount.
        viewModelScope.launch { refreshProviderKeyStatus() }
    }

    override fun dispatch(intent: AppIntent) {
        when (intent) {
            AppIntent.Resume -> viewModelScope.launch { handleResume() }
            is AppIntent.SubmitKey -> viewModelScope.launch { handleSubmitKey(intent.key) }
            is AppIntent.Navigate -> update { current ->
                if (current.screen == intent.screen) current
                else current.copy(
                    screen = intent.screen,
                    previousScreen = current.screen,
                )
            }
            is AppIntent.SelectConversation -> viewModelScope.launch {
                handleSelectConversation(intent.id)
            }
            AppIntent.NewChat -> viewModelScope.launch { handleNewChat() }
            AppIntent.DeleteCurrentConversation -> viewModelScope.launch {
                val a = agent ?: return@launch
                handleDeleteConversation(a.currentConversationId.value)
            }
            is AppIntent.DeleteConversation -> viewModelScope.launch {
                handleDeleteConversation(intent.id)
            }
            is AppIntent.SendChat -> viewModelScope.launch {
                handleSendChat(intent.text, intent.modelTier)
            }
            AppIntent.RegenerateLast -> viewModelScope.launch { handleRegenerateLast() }
            is AppIntent.ExportTrace -> viewModelScope.launch { handleExportTrace(intent.traceId) }
            is AppIntent.UiBridgeUpdate -> handleUiBridgeUpdate(intent)
            is AppIntent.SetPalette -> viewModelScope.launch { themeRepo.setPalette(intent.palette) }
            is AppIntent.SetThemeMode -> viewModelScope.launch { themeRepo.setMode(intent.mode) }
            AppIntent.CompleteOnboarding -> viewModelScope.launch {
                onboardingRepo.markCompleted()
                update { it.copy(screen = Screen.KeyPaste) }
            }
            AppIntent.DismissPermissionDialog -> update {
                it.copy(pendingPermissionDialog = null)
            }
            is AppIntent.InvokeMiniApp -> viewModelScope.launch {
                handleInvokeMiniApp(intent)
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
            is AppIntent.SelectAgent -> viewModelScope.launch {
                handleSelectAgent(intent.name)
            }
            is AppIntent.StartCreator -> viewModelScope.launch {
                handleStartCreator(intent.kind)
            }
            AppIntent.CancelCreator -> viewModelScope.launch {
                handleCancelCreator()
            }
        }
    }

    override suspend fun sendUiEvent(
        action: String,
        sourceLabel: String?,
        fieldValues: Map<String, String>,
    ) {
        displayMessages += DisplayMessage.event(action, sourceLabel, fieldValues)
        val a = agent ?: return
        val result = runCatching {
            withContext(Dispatchers.IO) { a.sendEvent(action, sourceLabel, fieldValues) }
        }
        result.fold(
            onSuccess = { reply ->
                displayMessages += DisplayMessage.assistant(
                    text = reply,
                    agentName = current.activeAgentName,
                )
            },
            onFailure = { t ->
                displayMessages += DisplayMessage.toolFail(
                    "ui_event",
                    t.message ?: t::class.simpleName.orEmpty(),
                )
            },
        )
    }

    override suspend fun saveKey(key: String) {
        val provider = providerPrefsRepo.activeProviderNow()
        runtime.keyVault.put(provider.keyAlias(), key)
        refreshProviderKeyStatus()
    }

    /**
     * Internal accessor for the chat surface (Android-side
     * ChatRoute / drawer) that genuinely needs the agent — e.g.
     * `agent.currentConversationId.collectAsState()` for the drawer
     * highlight. Returns null while the agent isn't built yet.
     *
     * App composable consumers should read `state.agentReady` and
     * `state.currentConversationId` instead.
     */
    internal fun agentOrNull(): WeftAgent? = agent

    // ─── Boot & agent build ───────────────────────────────────────────

    private suspend fun handleStartCreator(kind: CreatorKind) {
        val a = agent ?: return
        creatorSession.start(kind)
        a.newChat()
        displayMessages.clear()
        update { current ->
            current.copy(
                screen = Screen.Creator,
                previousScreen = current.screen,
                chat = ChatStatus(),
                currentConversationId = a.currentConversationId.value,
            )
        }
        val kickoff = when (kind) {
            CreatorKind.PersonaVoice ->
                "I want to create a new voice persona. Ask me what I need."
            CreatorKind.PersonaRole ->
                "I want to create a new role persona. Ask me what I need."
            CreatorKind.MiniApp ->
                "I want to create a new mini-app. Ask me what I need."
        }
        handleSendChat(kickoff, modelTier = null)
    }

    private suspend fun handleCancelCreator() {
        val kind = creatorSession.current()
        creatorSession.clear()
        val back = when (kind) {
            CreatorKind.PersonaVoice, CreatorKind.PersonaRole -> Screen.Personas
            CreatorKind.MiniApp -> Screen.MiniApps
            null -> Screen.Settings
        }
        update { it.copy(screen = back, previousScreen = Screen.Creator) }
    }

    private suspend fun handleResume() {
        val onboardingDone = onboardingRepo.completedFlow.first()
        if (!onboardingDone) {
            update { it.copy(screen = Screen.Onboarding) }
            return
        }
        val activeProvider = providerPrefsRepo.activeProviderNow()
        val storedKey = withContext(Dispatchers.IO) {
            runtime.keyVault.get(activeProvider.keyAlias())
        }
        if (storedKey == null) {
            update { it.copy(screen = Screen.KeyPaste) }
            return
        }
        val agentSummaries = runtime.agentDeclarations.values
            .filter { it.userAddressable }
            .map { AgentSummary(it.name, it.displayName, it.description) }
        val a = buildAgentFor(
            agentName = current.activeAgentName,
            provider = activeProvider,
            apiKey = storedKey,
        )
        a.resume()
        hydrateMessages(a.currentConversationId.value)
        setAgent(a, screen = Screen.Chat, availableAgents = agentSummaries)
    }

    private suspend fun buildAgentFor(
        agentName: String,
        provider: ProviderKind,
        apiKey: String,
    ): WeftAgent = runtime.buildAgent(
        agentName = agentName,
        provider = credentialProviderFor(provider, apiKey),
        modelPoolOverride = modelPoolOverrideFor(provider),
    )

    private suspend fun handleSelectAgent(name: String) {
        val current = current
        if (name == current.activeAgentName) return
        if (current.availableAgents.none { it.name == name }) return
        val provider = providerPrefsRepo.activeProviderNow()
        val key = withContext(Dispatchers.IO) {
            runtime.keyVault.get(provider.keyAlias())
        } ?: return
        val a = buildAgentFor(agentName = name, provider = provider, apiKey = key)
        a.resume()
        hydrateMessages(a.currentConversationId.value)
        setAgent(a, activeAgentName = name)
    }

    private fun credentialProviderFor(provider: ProviderKind, apiKey: String): WeftCredentialProvider =
        when (provider) {
            ProviderKind.Anthropic -> StaticKeyProvider(apiKey)
            ProviderKind.OpenAI -> StaticOpenAIKeyProvider(apiKey)
            ProviderKind.OpenRouter -> StaticOpenRouterKeyProvider(apiKey)
            ProviderKind.DeepSeek -> StaticDeepSeekKeyProvider(apiKey)
        }

    private fun modelPoolOverrideFor(provider: ProviderKind): ModelPool? {
        val anyOverride = ModelTier.entries.any { tier ->
            modelPrefsRepo.overrideFor(provider, tier) != null
        }
        if (!anyOverride) return null

        val weftProvider = provider.toWeft()
        val defaultPool = defaultPoolFor(weftProvider)
        fun resolve(tier: ModelTier, default: ai.koog.prompt.llm.LLModel) =
            modelPrefsRepo.overrideFor(provider, tier)
                ?.let { findModelInCatalog(weftProvider, it) }
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
        val activeProvider = providerPrefsRepo.activeProviderNow()
        if (provider == activeProvider) {
            val key = withContext(Dispatchers.IO) {
                runtime.keyVault.get(provider.keyAlias())
            } ?: return
            val a = buildAgentFor(
                agentName = current.activeAgentName,
                provider = provider,
                apiKey = key,
            )
            a.resume()
            hydrateMessages(a.currentConversationId.value)
            setAgent(a)
        }
    }

    private suspend fun handleSubmitKey(key: String) {
        val activeProvider = providerPrefsRepo.activeProviderNow()
        val a = buildAgentFor(
            agentName = current.activeAgentName,
            provider = activeProvider,
            apiKey = key,
        )
        a.resume()
        refreshProviderKeyStatus()
        setAgent(a, screen = Screen.Chat)
    }

    private suspend fun handleSetProvider(provider: ProviderKind) {
        providerPrefsRepo.setProvider(provider)
        val key = withContext(Dispatchers.IO) {
            runtime.keyVault.get(provider.keyAlias())
        }
        if (key != null) {
            val a = buildAgentFor(
                agentName = current.activeAgentName,
                provider = provider,
                apiKey = key,
            )
            a.resume()
            hydrateMessages(a.currentConversationId.value)
            setAgent(a)
        } else {
            agent = null
            update {
                it.copy(agentReady = false, currentConversationId = null)
            }
        }
    }

    private suspend fun handleSaveProviderKey(provider: ProviderKind, key: String) {
        withContext(Dispatchers.IO) {
            runtime.keyVault.put(provider.keyAlias(), key)
        }
        refreshProviderKeyStatus()
        val activeProvider = providerPrefsRepo.activeProviderNow()
        if (provider == activeProvider) {
            val a = buildAgentFor(
                agentName = current.activeAgentName,
                provider = provider,
                apiKey = key,
            )
            a.resume()
            hydrateMessages(a.currentConversationId.value)
            setAgent(a)
        }
    }

    private suspend fun handleRemoveProviderKey(provider: ProviderKind) {
        withContext(Dispatchers.IO) {
            runtime.keyVault.remove(provider.keyAlias())
        }
        refreshProviderKeyStatus()
        val activeProvider = providerPrefsRepo.activeProviderNow()
        if (provider == activeProvider) {
            agent = null
            update {
                it.copy(
                    agentReady = false,
                    currentConversationId = null,
                    screen = Screen.KeyPaste,
                )
            }
        }
    }

    private suspend fun handleSelectConversation(id: String) {
        val a = agent ?: return
        a.resume(id)
        hydrateMessages(id)
        update {
            it.copy(
                screen = Screen.Chat,
                currentConversationId = a.currentConversationId.value,
            )
        }
    }

    private suspend fun handleNewChat() {
        val a = agent ?: return
        a.newChat()
        displayMessages.clear()
        update {
            it.copy(
                screen = Screen.Chat,
                chat = ChatStatus(),
                currentConversationId = a.currentConversationId.value,
            )
        }
    }

    private suspend fun handleDeleteConversation(id: String) {
        val a = agent ?: return
        val wasActive = a.currentConversationId.value == id
        withContext(Dispatchers.IO) {
            runtime.conversationStore.deleteConversation(id)
        }
        if (wasActive) {
            a.newChat()
            displayMessages.clear()
            update {
                it.copy(
                    screen = Screen.Chat,
                    chat = ChatStatus(),
                    currentConversationId = a.currentConversationId.value,
                )
            }
        }
    }

    private suspend fun handleRegenerateLast() {
        if (current.chat.inFlight) return
        val a = agent ?: return
        val lastUserIdx = displayMessages.indexOfLast { it.role == DisplayRole.USER }
        if (lastUserIdx == -1) return

        while (displayMessages.size > lastUserIdx + 1) {
            displayMessages.removeAt(displayMessages.size - 1)
        }

        update { it.copy(chat = ChatStatus(inFlight = true, lastError = null)) }
        consumeAgentStream(a.regenerateStreaming())
        update { it.copy(chat = it.chat.copy(inFlight = false)) }
    }

    private suspend fun handleSendChat(
        text: String,
        modelTier: ModelTier? = null,
    ) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        val a = agent ?: return
        if (current.chat.inFlight) return

        update { it.copy(chat = ChatStatus(inFlight = true, lastError = null)) }
        displayMessages += DisplayMessage.user(trimmed)

        val match = skillRegistry.resolve(trimmed)
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
            update { it.copy(chat = it.chat.copy(inFlight = false)) }
            return
        }

        val effectiveTier = modelTier ?: current.defaultTier
        consumeAgentStream(a.sendStreaming(trimmed, modelTier = effectiveTier?.toWeft()))
        update { it.copy(chat = it.chat.copy(inFlight = false)) }
    }

    private suspend fun consumeAgentStream(stream: Flow<StreamChunk>) {
        var streamingMessageId: Long? = null
        runCatching {
            stream.flowOn(Dispatchers.IO).collect { chunk ->
                when (chunk) {
                    is StreamChunk.TextDelta -> {
                        val existingId = streamingMessageId
                        if (existingId == null) {
                            val msg = DisplayMessage.assistant(
                                text = chunk.text,
                                agentName = current.activeAgentName,
                            )
                            streamingMessageId = msg.id
                            displayMessages += msg
                        } else {
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
                    is StreamChunk.ToolFailed -> {
                        val dialog = parsePermissionFailure(chunk.toolName, chunk.message)
                        if (dialog != null) {
                            displayMessages += DisplayMessage.toolFail(
                                chunk.toolName,
                                "Needs permission — see dialog.",
                            )
                            update { it.copy(pendingPermissionDialog = dialog) }
                        } else {
                            displayMessages += DisplayMessage.toolFail(chunk.toolName, chunk.message)
                        }
                    }
                    is StreamChunk.Done -> {
                        if (streamingMessageId == null && chunk.finalReply.isNotBlank()) {
                            displayMessages += DisplayMessage.assistant(
                                text = chunk.finalReply,
                                agentName = current.activeAgentName,
                            )
                        }
                    }
                    is StreamChunk.Failed ->
                        update { it.copy(chat = it.chat.copy(lastError = chunk.message)) }
                }
            }
        }.onFailure { t ->
            update {
                val existing = it.chat.lastError
                it.copy(
                    chat = it.chat.copy(
                        lastError = existing ?: (t.message ?: t::class.simpleName.orEmpty()),
                    ),
                )
            }
        }
    }

    private suspend fun handleExportTrace(traceId: String) {
        val trace: AgentTrace? = runtime.traceStore.traces.value.firstOrNull { it.id == traceId }
        if (trace == null) {
            emit(
                AppEffect.Error("Trace export failed: $traceId not found (evicted?)."),
            )
            return
        }
        try {
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
            emit(
                AppEffect.Error("Trace export failed: ${t.message ?: t::class.simpleName.orEmpty()}"),
            )
        }
    }

    private fun handleUiBridgeUpdate(intent: AppIntent.UiBridgeUpdate) {
        val event = intent.event ?: return
        if (current.screen !is Screen.RenderedTree && current.screen !is Screen.Creator) {
            update { it.copy(screen = Screen.RenderedTree) }
        }
        val miniAppId = activeMiniAppInvocationId
        if (miniAppId != null) {
            val treeJson = TRACE_JSON.encodeToString(
                dev.weft.undercurrent.shared.gateway.ComponentNode.serializer(),
                event.tree,
            )
            viewModelScope.launch {
                runCatching { miniAppsRepo.setCachedRender(miniAppId, treeJson) }
            }
        }
    }

    private suspend fun handleInvokeMiniApp(intent: AppIntent.InvokeMiniApp) {
        // Seed the cached UI tree into the substrate's bridge so the
        // user sees the tracker instantly while the agent's reply
        // streams in. Wire-format compatible — the mirror JSON
        // round-trips into the Weft ComponentNode used by the bridge.
        val cached = intent.cachedRenderTreeJson
        if (cached != null) {
            runCatching {
                val tree = Json.decodeFromString(
                    dev.weft.contracts.ComponentNode.serializer(),
                    cached,
                )
                runtime.uiBridge.emit(UIUpdate.RenderTree(tree))
            }
            update {
                if (it.screen is Screen.RenderedTree) it
                else it.copy(screen = Screen.RenderedTree, previousScreen = it.screen)
            }
        }
        activeMiniAppInvocationId = intent.miniAppId
        try {
            miniAppsRepo.recordUsage(intent.miniAppId)
            handleSendChat(intent.triggerPrompt, modelTier = null)
        } finally {
            activeMiniAppInvocationId = null
        }
    }

    private fun parsePermissionFailure(toolName: String, message: String): PermissionDialogState? {
        val marker = "Permission denied"
        if (!message.contains(marker)) return null
        val tail = message.substringAfter(marker)
        val perms = tail.substringAfter(": ", missingDelimiterValue = "")
            .trimEnd('.', '!', ' ', '"', '\'')
        return PermissionDialogState(
            toolName = toolName,
            friendlyTitle = friendlyTitleForTool(toolName),
            friendlyBody = friendlyBodyForTool(toolName, perms),
        )
    }

    private fun friendlyTitleForTool(toolName: String): String = when {
        toolName.startsWith("location_") -> "Location access needed"
        toolName.startsWith("calendar_") -> "Calendar access needed"
        toolName.startsWith("contacts_") -> "Contacts access needed"
        toolName.startsWith("camera_") -> "Camera access needed"
        toolName == "notify_show" || toolName.startsWith("schedule_") -> "Notification permission needed"
        toolName.startsWith("bluetooth_") -> "Bluetooth permission needed"
        toolName.startsWith("audio_") -> "Microphone access needed"
        else -> "Permission needed"
    }

    private fun friendlyBodyForTool(toolName: String, perms: String): String {
        val action = when {
            toolName.startsWith("location_") -> "find your location"
            toolName.startsWith("calendar_") -> "read or update your calendar"
            toolName.startsWith("contacts_") -> "look up your contacts"
            toolName.startsWith("camera_") -> "take a photo"
            toolName == "notify_show" -> "post notifications"
            toolName.startsWith("schedule_") -> "schedule a notification"
            toolName.startsWith("bluetooth_") -> "see your paired Bluetooth devices"
            toolName.startsWith("audio_") -> "record audio"
            else -> "use this capability ($perms)"
        }
        return "Undercurrent needs permission to $action. Android won't show the system prompt again — " +
            "open Settings to grant the permission, then try once more."
    }

    private suspend fun hydrateMessages(convId: String) {
        val msgs = withContext(Dispatchers.IO) {
            runtime.conversationStore.loadMessages(convId)
        }
        displayMessages.clear()
        for (m in msgs) {
            displayMessages += when (m.role) {
                PersistedRole.USER -> DisplayMessage.user(m.content)
                PersistedRole.ASSISTANT -> DisplayMessage.assistant(
                    text = m.content,
                    agentName = m.agentName,
                )
            }
        }
    }

    /**
     * Set the active agent + push the derived state slots
     * (`agentReady` + `currentConversationId`). Other AppState fields
     * (`screen`, `availableAgents`, `activeAgentName`) can be updated
     * in the same atomic copy via the optional parameters.
     */
    private fun setAgent(
        a: WeftAgent,
        screen: Screen = current.screen,
        availableAgents: List<AgentSummary> = current.availableAgents,
        activeAgentName: String = current.activeAgentName,
    ) {
        agent = a
        update {
            it.copy(
                agentReady = true,
                currentConversationId = a.currentConversationId.value,
                screen = screen,
                availableAgents = availableAgents,
                activeAgentName = activeAgentName,
            )
        }
        // Re-subscribe to the agent's conv-id flow so subsequent
        // newChat() / resume() calls propagate to state. The previous
        // agent's collection cancels naturally when `agent` is
        // reassigned (viewModelScope.launch keeps running, but the
        // next emission goes nowhere visible — kept simple by
        // launching a fresh collector each time and relying on
        // SharedFlow buffering being benign).
        viewModelScope.launch {
            a.currentConversationId.collect { convId ->
                if (agent === a) {
                    update { current ->
                        if (current.currentConversationId == convId) current
                        else current.copy(currentConversationId = convId)
                    }
                }
            }
        }
    }

    private suspend fun refreshProviderKeyStatus() {
        val status = withContext(Dispatchers.IO) {
            buildMap {
                ProviderKind.entries.forEach { provider ->
                    val k = runtime.keyVault.get(provider.keyAlias())
                    if (!k.isNullOrBlank()) put(provider, k.takeLast(4))
                }
            }
        }
        update { it.copy(providerKeyStatus = status) }
    }

    companion object {
        private val TRACE_JSON = Json {
            prettyPrint = true
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    }
}

// ─── Mirror ↔ Weft conversions ────────────────────────────────────────────

internal fun ProviderKind.keyAlias(): String = when (this) {
    ProviderKind.Anthropic -> WeftRuntime.ANTHROPIC_KEY_ALIAS
    ProviderKind.OpenAI -> WeftRuntime.OPENAI_KEY_ALIAS
    ProviderKind.OpenRouter -> WeftRuntime.OPENROUTER_KEY_ALIAS
    ProviderKind.DeepSeek -> WeftRuntime.DEEPSEEK_KEY_ALIAS
}

internal fun ProviderKind.toWeft(): WeftProviderKind = when (this) {
    ProviderKind.Anthropic -> WeftProviderKind.Anthropic
    ProviderKind.OpenAI -> WeftProviderKind.OpenAI
    ProviderKind.OpenRouter -> WeftProviderKind.OpenRouter
    ProviderKind.DeepSeek -> WeftProviderKind.DeepSeek
}

internal fun ModelTier.toWeft(): WeftModelTier = when (this) {
    ModelTier.Cheap -> WeftModelTier.Cheap
    ModelTier.Standard -> WeftModelTier.Standard
    ModelTier.Vision -> WeftModelTier.Vision
    ModelTier.Heavy -> WeftModelTier.Heavy
}
