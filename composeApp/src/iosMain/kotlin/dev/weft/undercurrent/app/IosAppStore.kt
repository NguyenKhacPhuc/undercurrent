package dev.weft.undercurrent.app

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import dev.weft.undercurrent.app.llm.AnthropicLlmClient
import dev.weft.undercurrent.app.llm.IOS_SYSTEM_PROMPT
import dev.weft.undercurrent.app.llm.LlmChunk
import dev.weft.undercurrent.app.llm.LlmClient
import dev.weft.undercurrent.app.llm.LlmMessage
import dev.weft.undercurrent.app.llm.deepSeekClient
import dev.weft.undercurrent.app.llm.openAIClient
import dev.weft.undercurrent.app.llm.openRouterClient
import dev.weft.undercurrent.core.model.AppEffect
import dev.weft.undercurrent.core.model.ProviderKind
import dev.weft.undercurrent.core.navigation.Screen
import dev.weft.undercurrent.data.datastore.OnboardingRepository
import dev.weft.undercurrent.data.datastore.PersonaRepository
import dev.weft.undercurrent.data.datastore.ProviderPrefsRepository
import dev.weft.undercurrent.data.datastore.ThemeRepository
import dev.weft.undercurrent.db.UndercurrentDatabase
import dev.weft.undercurrent.feature.chat.DisplayMessage
import dev.weft.undercurrent.feature.chat.DisplayRole
import dev.weft.undercurrent.feature.chat.SkillSummary
import dev.weft.undercurrent.shared.gateway.KeyVaultGateway
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * iOS [AppStore] impl. Persistence layers:
 *  - DataStore-Preferences for theme / onboarding / provider prefs
 *  - iOS Keychain for API keys
 *  - SQLDelight (`UndercurrentDatabase`) for conversations + messages
 *
 * Streaming LLM clients per provider (Anthropic / OpenAI / OpenRouter /
 * DeepSeek). Multi-conversation: chat threads persist across restarts;
 * the user can switch between threads via the Conversations screen.
 *
 * Still no-op (deferred): tools, OAuth, mini-apps, traces, memory,
 * voice (waiting on cinterop fix).
 */
@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
public class IosAppStore(
    private val keyVault: KeyVaultGateway,
    private val onboardingRepo: OnboardingRepository,
    private val themeRepo: ThemeRepository,
    private val providerPrefsRepo: ProviderPrefsRepository,
    private val personaRepo: PersonaRepository,
    private val db: UndercurrentDatabase,
) : AppStore {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(AppState.initial())
    override val state: StateFlow<AppState> = _state.asStateFlow()

    private val _effects = Channel<AppEffect>(Channel.BUFFERED)
    override val effects: Flow<AppEffect> = _effects.receiveAsFlow()

    override val displayMessages: SnapshotStateList<DisplayMessage> = mutableStateListOf()
    override val skills: List<SkillSummary> = emptyList()

    /** In-memory history kept in sync with the persisted messages of the current conversation. */
    private val history: MutableList<LlmMessage> = mutableListOf()

    private val clients: Map<ProviderKind, LlmClient> = mapOf(
        ProviderKind.Anthropic to AnthropicLlmClient(
            getApiKey = { keyVault.getApiKey(ProviderKind.Anthropic) },
        ),
        ProviderKind.OpenAI to openAIClient(
            getApiKey = { keyVault.getApiKey(ProviderKind.OpenAI) },
        ),
        ProviderKind.OpenRouter to openRouterClient(
            getApiKey = { keyVault.getApiKey(ProviderKind.OpenRouter) },
        ),
        ProviderKind.DeepSeek to deepSeekClient(
            getApiKey = { keyVault.getApiKey(ProviderKind.DeepSeek) },
        ),
    )

    init {
        scope.launch {
            themeRepo.prefsFlow.collect { prefs ->
                _state.value = _state.value.copy(themePrefs = prefs)
            }
        }
        scope.launch {
            onboardingRepo.completedFlow.collect { done ->
                _state.value = _state.value.copy(onboardingCompleted = done)
            }
        }
        scope.launch {
            providerPrefsRepo.activeProvider.collect { provider ->
                _state.value = _state.value.copy(activeProvider = provider)
                refreshProviderKeyStatus()
            }
        }
        scope.launch {
            providerPrefsRepo.defaultTier.collect { tier ->
                _state.value = _state.value.copy(defaultTier = tier)
            }
        }
    }

    override fun dispatch(intent: AppIntent) {
        when (intent) {
            AppIntent.Resume -> scope.launch { handleResume() }

            // ── Boot path ─────────────────────────────────────────
            is AppIntent.SetProvider -> scope.launch {
                providerPrefsRepo.setProvider(intent.provider)
                // Mirror Android: if the new provider has no key, drop
                // the user on KeyPaste so they can paste one instead of
                // silently failing the next SendChat.
                val hasKey = withContext(Dispatchers.Default) {
                    runCatching { keyVault.hasApiKey(intent.provider) }.getOrDefault(false)
                }
                if (!hasKey) {
                    _state.value = _state.value.copy(
                        agentReady = false,
                        screen = Screen.KeyPaste,
                    )
                } else if (!_state.value.agentReady) {
                    // New provider has a key → ready to send.
                    _state.value = _state.value.copy(agentReady = true)
                }
            }
            AppIntent.CompleteOnboarding -> scope.launch {
                onboardingRepo.markCompleted()
                _state.value = _state.value.copy(screen = Screen.KeyPaste)
            }
            is AppIntent.SubmitKey -> scope.launch {
                val provider = providerPrefsRepo.activeProviderNow()
                runCatching { keyVault.putApiKey(provider, intent.key) }
                    .onFailure { t ->
                        _effects.trySend(
                            AppEffect.Error("Couldn't save key: ${t.message ?: t::class.simpleName.orEmpty()}"),
                        )
                        return@launch
                    }
                refreshProviderKeyStatus()
                _state.value = _state.value.copy(agentReady = true, screen = Screen.Chat)
            }

            // ── Plain navigation + state mutations ────────────────
            is AppIntent.Navigate -> _state.value = _state.value.let {
                if (it.screen == intent.screen) it
                else it.copy(screen = intent.screen, previousScreen = it.screen)
            }
            is AppIntent.SetPalette -> scope.launch { themeRepo.setPalette(intent.palette) }
            is AppIntent.SetThemeMode -> scope.launch { themeRepo.setMode(intent.mode) }
            is AppIntent.SetDefaultTier -> scope.launch {
                providerPrefsRepo.setDefaultTier(intent.tier)
            }
            is AppIntent.SaveProviderKey -> scope.launch {
                runCatching { keyVault.putApiKey(intent.provider, intent.apiKey) }
                    .onFailure { t ->
                        _effects.trySend(
                            AppEffect.Error("Couldn't save key: ${t.message ?: t::class.simpleName.orEmpty()}"),
                        )
                    }
                refreshProviderKeyStatus()
            }
            is AppIntent.RemoveProviderKey -> scope.launch {
                runCatching { keyVault.clearApiKey(intent.provider) }
                refreshProviderKeyStatus()
                if (intent.provider == providerPrefsRepo.activeProviderNow()) {
                    _state.value = _state.value.copy(agentReady = false, screen = Screen.KeyPaste)
                }
            }
            is AppIntent.SelectAgent -> _state.value = _state.value.copy(
                activeAgentName = intent.name,
            )
            AppIntent.DismissPermissionDialog -> _state.value = _state.value.copy(
                pendingPermissionDialog = null,
            )

            // ── Chat + conversation lifecycle ──────────────────────
            is AppIntent.SendChat -> scope.launch { handleSendChat(intent.text) }
            AppIntent.NewChat -> scope.launch { handleNewChat() }
            AppIntent.RegenerateLast -> scope.launch { handleRegenerate() }
            is AppIntent.SelectConversation -> scope.launch {
                handleSelectConversation(intent.id)
            }
            is AppIntent.DeleteConversation -> scope.launch {
                handleDeleteConversation(intent.id)
            }
            AppIntent.DeleteCurrentConversation -> scope.launch {
                val currentId = _state.value.currentConversationId ?: return@launch
                handleDeleteConversation(currentId)
            }

            // ── Mini-app invocation (text-only on iOS) ──────────────
            // No ui_render on iOS yet → no cached-tree seeding, no
            // RenderedTree screen navigation. Just dispatch the trigger
            // prompt as if the user typed it. The Phase-3+ work to bring
            // ui_render to iOS will replace this with the full flow.
            is AppIntent.InvokeMiniApp -> scope.launch {
                handleSendChat(intent.triggerPrompt)
            }

            // ── Still no-op (Phase 3+) ──────────────────────────────
            AppIntent.CancelCreator,
            is AppIntent.ExportTrace,
            is AppIntent.UiBridgeUpdate,
            is AppIntent.SetModelForTier,
            is AppIntent.StartCreator -> Unit
        }
    }

    override suspend fun sendUiEvent(
        action: String,
        sourceLabel: String?,
        fieldValues: Map<String, String>,
    ) = Unit

    override suspend fun saveKey(key: String) {
        val provider = providerPrefsRepo.activeProviderNow()
        withContext(Dispatchers.Default) {
            runCatching { keyVault.putApiKey(provider, key) }
        }
        refreshProviderKeyStatus()
    }

    // ─── Boot ─────────────────────────────────────────────────────

    private suspend fun handleResume() {
        val onboardingDone = onboardingRepo.completedFlow.first()
        if (!onboardingDone) {
            _state.value = _state.value.copy(screen = Screen.Onboarding)
            return
        }
        val provider = providerPrefsRepo.activeProviderNow()
        val hasKey = withContext(Dispatchers.Default) {
            runCatching { keyVault.hasApiKey(provider) }.getOrDefault(false)
        }
        if (!hasKey) {
            _state.value = _state.value.copy(screen = Screen.KeyPaste)
            return
        }
        // Land on the most recent conversation if any exists; the
        // ConversationsListScreen lets the user switch.
        val mostRecent = withContext(Dispatchers.Default) {
            db.conversationsQueries.listConversations().executeAsList().firstOrNull()
        }
        if (mostRecent != null) {
            hydrateFromConversation(mostRecent.id)
        } else {
            history.clear()
            displayMessages.clear()
        }
        _state.value = _state.value.copy(
            agentReady = true,
            screen = Screen.Chat,
            currentConversationId = mostRecent?.id,
        )
    }

    // ─── Streaming chat ───────────────────────────────────────────

    private suspend fun handleSendChat(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        if (_state.value.chat.inFlight) return

        val provider = _state.value.activeProvider
        val client = clients[provider] ?: run {
            _state.value = _state.value.copy(
                chat = ChatStatus(inFlight = false, lastError = "No client for $provider"),
            )
            return
        }

        val now = Clock.System.now().toEpochMilliseconds()
        val conversationId = ensureConversation(now)
        val isFirstUserTurn = history.none { it.role == LlmMessage.ROLE_USER }

        _state.value = _state.value.copy(
            chat = ChatStatus(inFlight = true, lastError = null),
            currentConversationId = conversationId,
        )

        displayMessages += DisplayMessage.user(trimmed)
        history += LlmMessage(LlmMessage.ROLE_USER, trimmed)

        // Persist the user turn synchronously so a process kill mid-
        // stream doesn't lose it.
        withContext(Dispatchers.Default) {
            db.conversationsQueries.insertMessage(
                id = newId("msg"),
                conversation_id = conversationId,
                role = LlmMessage.ROLE_USER,
                content = trimmed,
                created_at_ms = now,
            )
            db.conversationsQueries.touchConversation(now, conversationId)
            if (isFirstUserTurn) {
                db.conversationsQueries.updateConversationTitle(
                    title = trimmed.take(40),
                    ts = now,
                    id = conversationId,
                )
            }
        }

        var assistantMessageId: Long? = null
        val replyBuilder = StringBuilder()
        var sawError = false

        client.send(history, composeSystemPrompt()).collect { chunk ->
            when (chunk) {
                is LlmChunk.TextDelta -> {
                    replyBuilder.append(chunk.text)
                    val existingId = assistantMessageId
                    if (existingId == null) {
                        val msg = DisplayMessage.assistant(
                            text = chunk.text,
                            agentName = _state.value.activeAgentName,
                        )
                        assistantMessageId = msg.id
                        displayMessages += msg
                    } else {
                        val idx = displayMessages.indexOfLast { it.id == existingId }
                        if (idx >= 0) {
                            val current = displayMessages[idx]
                            displayMessages[idx] = current.copy(
                                text = current.text + chunk.text,
                            )
                        }
                    }
                }
                is LlmChunk.Error -> {
                    sawError = true
                    _state.value = _state.value.copy(
                        chat = _state.value.chat.copy(lastError = chunk.message),
                    )
                }
                LlmChunk.Done -> Unit
            }
        }

        if (sawError) {
            // Roll the user turn off model-facing history (display
            // keeps it so the user can see what they sent). Don't
            // touch the DB — the user message stays persisted; on
            // re-load the failed turn is visible without a reply.
            if (history.isNotEmpty()) history.removeAt(history.size - 1)
        } else {
            val reply = replyBuilder.toString()
            if (reply.isNotEmpty()) {
                history += LlmMessage(LlmMessage.ROLE_ASSISTANT, reply)
                val replyTs = Clock.System.now().toEpochMilliseconds()
                withContext(Dispatchers.Default) {
                    db.conversationsQueries.insertMessage(
                        id = newId("msg"),
                        conversation_id = conversationId,
                        role = LlmMessage.ROLE_ASSISTANT,
                        content = reply,
                        created_at_ms = replyTs,
                    )
                    db.conversationsQueries.touchConversation(replyTs, conversationId)
                }
            }
        }
        _state.value = _state.value.copy(
            chat = _state.value.chat.copy(inFlight = false),
        )
    }

    private suspend fun handleNewChat() {
        history.clear()
        displayMessages.clear()
        _state.value = _state.value.copy(
            screen = Screen.Chat,
            chat = ChatStatus(),
            currentConversationId = null, // Lazy — created on next SendChat.
        )
    }

    private suspend fun handleRegenerate() {
        if (_state.value.chat.inFlight) return
        val lastUserIdx = displayMessages.indexOfLast { it.role == DisplayRole.USER }
        if (lastUserIdx == -1) return
        val lastUserText = displayMessages[lastUserIdx].text

        // Trim display + history + DB back to just-before-the-last-user.
        // We don't fully reset the DB row for the user message — keeping
        // it lets the user see what they sent. We just drop the trailing
        // assistant reply (if any) so handleSendChat replays cleanly.
        while (displayMessages.size > lastUserIdx + 1) {
            displayMessages.removeAt(displayMessages.size - 1)
        }
        while (history.isNotEmpty() && history.last().role != LlmMessage.ROLE_USER) {
            history.removeAt(history.size - 1)
        }
        if (history.lastOrNull()?.role == LlmMessage.ROLE_USER) {
            history.removeAt(history.size - 1)
        }
        if (displayMessages.lastOrNull()?.role == DisplayRole.USER) {
            displayMessages.removeAt(displayMessages.size - 1)
        }
        handleSendChat(lastUserText)
    }

    private suspend fun handleSelectConversation(id: String) {
        if (_state.value.currentConversationId == id) {
            _state.value = _state.value.copy(screen = Screen.Chat)
            return
        }
        hydrateFromConversation(id)
        _state.value = _state.value.copy(
            screen = Screen.Chat,
            currentConversationId = id,
            chat = ChatStatus(),
        )
    }

    private suspend fun handleDeleteConversation(id: String) {
        val wasActive = _state.value.currentConversationId == id
        withContext(Dispatchers.Default) {
            db.transaction {
                db.conversationsQueries.deleteMessagesForConversation(id)
                db.conversationsQueries.deleteConversation(id)
            }
        }
        if (wasActive) {
            history.clear()
            displayMessages.clear()
            _state.value = _state.value.copy(
                screen = Screen.Chat,
                chat = ChatStatus(),
                currentConversationId = null,
            )
        }
    }

    // ─── DB helpers ───────────────────────────────────────────────

    /** Ensure a conversation row exists; create one if [currentConversationId] is null. */
    private suspend fun ensureConversation(nowMs: Long): String {
        val existing = _state.value.currentConversationId
        if (existing != null) return existing
        val id = newId("conv")
        withContext(Dispatchers.Default) {
            db.conversationsQueries.insertConversation(
                id = id,
                title = "New chat",
                created_at_ms = nowMs,
                last_message_at_ms = nowMs,
            )
        }
        return id
    }

    /** Load every message for [id] into history + displayMessages. */
    private suspend fun hydrateFromConversation(id: String) {
        val rows = withContext(Dispatchers.Default) {
            db.conversationsQueries.listMessagesByConversation(id).executeAsList()
        }
        history.clear()
        displayMessages.clear()
        for (row in rows) {
            history += LlmMessage(role = row.role, content = row.content)
            when (row.role) {
                LlmMessage.ROLE_USER ->
                    displayMessages += DisplayMessage.user(row.content)
                LlmMessage.ROLE_ASSISTANT ->
                    displayMessages += DisplayMessage.assistant(
                        text = row.content,
                        agentName = _state.value.activeAgentName,
                    )
            }
        }
    }

    // ─── Provider key status snapshot ─────────────────────────────

    private suspend fun refreshProviderKeyStatus() {
        val status = withContext(Dispatchers.Default) {
            buildMap {
                ProviderKind.entries.forEach { provider ->
                    if (runCatching { keyVault.hasApiKey(provider) }.getOrDefault(false)) {
                        put(provider, "•••")
                    }
                }
            }
        }
        _state.value = _state.value.copy(providerKeyStatus = status)
    }

    private fun newId(prefix: String): String = "$prefix.${Uuid.random().toString().take(12)}"

    /**
     * Compose the per-turn system prompt = base + voice instructions +
     * role instructions. Matches Weft's `extraVolatilePrefix` shape on
     * Android so behaviour is consistent across platforms: the user's
     * picked voice (Editor / Field Notes / …) and role (Doctor /
     * Lawyer / …) shape the model's reply.
     *
     * Empty voice (built-in `Default`) contributes nothing — the base
     * prompt stands alone. Same for unset role.
     */
    private fun composeSystemPrompt(): String {
        val voiceText = personaRepo.activeVoice.value.systemPromptText
        val roleText = personaRepo.activeRole.value?.systemPromptText
        return buildString {
            append(IOS_SYSTEM_PROMPT.trim())
            if (voiceText.isNotBlank()) {
                append("\n\nVoice instructions:\n")
                append(voiceText)
            }
            if (!roleText.isNullOrBlank()) {
                append("\n\nRole instructions:\n")
                append(roleText)
            }
        }
    }
}
