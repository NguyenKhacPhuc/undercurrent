package dev.weft.undercurrent.app

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import dev.weft.undercurrent.app.AnthropicClient.Message.Companion.ROLE_ASSISTANT
import dev.weft.undercurrent.app.AnthropicClient.Message.Companion.ROLE_USER
import dev.weft.undercurrent.core.model.AppEffect
import dev.weft.undercurrent.core.model.ProviderKind
import dev.weft.undercurrent.core.navigation.Screen
import dev.weft.undercurrent.data.datastore.OnboardingRepository
import dev.weft.undercurrent.data.datastore.ProviderPrefsRepository
import dev.weft.undercurrent.data.datastore.ThemeRepository
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

/**
 * iOS [AppStore] impl. Backed by:
 *  - Real DataStore-Preferences for theme / persona / onboarding /
 *    provider prefs (via the KMP repos in `:data:datastore`).
 *  - Real iOS Keychain for API key storage (via
 *    [dev.weft.undercurrent.shared.gateway.KeychainKeyVaultGateway]).
 *  - No agent runtime yet — `SendChat` and friends still no-op until
 *    the Ktor-based Anthropic client lands (Phase 1, next step).
 *
 * What works on iOS after this:
 *  - Onboarding completion persists across restarts.
 *  - API key paste persists (Keychain) and survives uninstall by default.
 *  - Theme palette + mode persist (DataStore).
 *  - Active provider + default tier persist.
 *  - Chat surface still shows the "Chat — coming to iOS" placeholder
 *    until the Anthropic client lands.
 */
public class IosAppStore(
    private val keyVault: KeyVaultGateway,
    private val onboardingRepo: OnboardingRepository,
    private val themeRepo: ThemeRepository,
    private val providerPrefsRepo: ProviderPrefsRepository,
) : AppStore {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(AppState.initial())
    override val state: StateFlow<AppState> = _state.asStateFlow()

    private val _effects = Channel<AppEffect>(Channel.BUFFERED)
    override val effects: Flow<AppEffect> = _effects.receiveAsFlow()

    override val displayMessages: SnapshotStateList<DisplayMessage> = mutableStateListOf()
    override val skills: List<SkillSummary> = emptyList()

    /**
     * In-memory conversation history fed to the Anthropic Messages API.
     * Mirrors [displayMessages] but stripped to alternating user/
     * assistant roles (no tool / event entries). Reset on `NewChat`
     * and survives only the session — persistent history is Phase 2.
     */
    private val anthropicHistory: MutableList<AnthropicClient.Message> = mutableListOf()

    private val anthropic = AnthropicClient(
        getApiKey = { keyVault.getApiKey(ProviderKind.Anthropic) },
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
                            AppEffect.Error(
                                "Couldn't save key: ${t.message ?: t::class.simpleName.orEmpty()}",
                            ),
                        )
                        return@launch
                    }
                refreshProviderKeyStatus()
                _state.value = _state.value.copy(
                    agentReady = true,
                    screen = Screen.Chat,
                )
            }

            // ── Plain navigation + persistent state mutations ────
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
                // Mirror Android: clearing the active provider's key
                // drops the agent + sends the user back to KeyPaste.
                if (intent.provider == providerPrefsRepo.activeProviderNow()) {
                    _state.value = _state.value.copy(
                        agentReady = false,
                        screen = Screen.KeyPaste,
                    )
                }
            }
            is AppIntent.SelectAgent -> _state.value = _state.value.copy(
                activeAgentName = intent.name,
            )
            AppIntent.DismissPermissionDialog -> _state.value = _state.value.copy(
                pendingPermissionDialog = null,
            )

            // ── Real chat (Anthropic only, no tools, no streaming) ──
            is AppIntent.SendChat -> scope.launch { handleSendChat(intent.text) }
            AppIntent.NewChat -> {
                anthropicHistory.clear()
                displayMessages.clear()
                _state.value = _state.value.copy(chat = ChatStatus())
            }
            AppIntent.RegenerateLast -> scope.launch { handleRegenerate() }

            // ── Still no-op on iOS ──────────────────────────────────
            // Need persistent conversations / agent declarations /
            // ui_render / etc. Either feature isn't wired (mini-apps,
            // creator) or needs Phase 2+ work (multi-conversation
            // history, traces, agent selector, tools).
            AppIntent.DeleteCurrentConversation,
            AppIntent.CancelCreator,
            is AppIntent.SelectConversation,
            is AppIntent.DeleteConversation,
            is AppIntent.ExportTrace,
            is AppIntent.UiBridgeUpdate,
            is AppIntent.InvokeMiniApp,
            is AppIntent.SetModelForTier,
            is AppIntent.StartCreator -> Unit
        }
    }

    /** One round-trip against Anthropic. No streaming yet — Phase 2. */
    private suspend fun handleSendChat(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        if (_state.value.chat.inFlight) return

        _state.value = _state.value.copy(
            chat = ChatStatus(inFlight = true, lastError = null),
        )
        displayMessages += DisplayMessage.user(trimmed)
        anthropicHistory += AnthropicClient.Message(ROLE_USER, trimmed)

        val result = withContext(Dispatchers.Default) {
            runCatching { anthropic.send(anthropicHistory) }
        }
        result.fold(
            onSuccess = { reply ->
                anthropicHistory += AnthropicClient.Message(ROLE_ASSISTANT, reply)
                displayMessages += DisplayMessage.assistant(
                    text = reply,
                    agentName = _state.value.activeAgentName,
                )
                _state.value = _state.value.copy(chat = ChatStatus(inFlight = false))
            },
            onFailure = { t ->
                // Roll the user message back out of the model-facing
                // history so retry doesn't double up. The display
                // message stays so the user sees what they sent.
                anthropicHistory.removeLastOrNull()
                _state.value = _state.value.copy(
                    chat = ChatStatus(
                        inFlight = false,
                        lastError = t.message ?: t::class.simpleName.orEmpty(),
                    ),
                )
            },
        )
    }

    /** "Ask again" — replay the last user message after popping the previous reply. */
    private suspend fun handleRegenerate() {
        if (_state.value.chat.inFlight) return
        val lastUserIdx = displayMessages.indexOfLast { it.role == DisplayRole.USER }
        if (lastUserIdx == -1) return
        val lastUserText = displayMessages[lastUserIdx].text

        // Trim display + history to just-through-last-user.
        while (displayMessages.size > lastUserIdx + 1) {
            displayMessages.removeAt(displayMessages.size - 1)
        }
        while (anthropicHistory.isNotEmpty() && anthropicHistory.last().role != ROLE_USER) {
            anthropicHistory.removeAt(anthropicHistory.size - 1)
        }
        // Pop the trailing user too — handleSendChat re-appends it.
        if (anthropicHistory.lastOrNull()?.role == ROLE_USER) {
            anthropicHistory.removeAt(anthropicHistory.size - 1)
        }
        // Same pop for displayMessages so handleSendChat re-appends.
        if (displayMessages.lastOrNull()?.role == DisplayRole.USER) {
            displayMessages.removeAt(displayMessages.size - 1)
        }
        handleSendChat(lastUserText)
    }

    override suspend fun sendUiEvent(
        action: String,
        sourceLabel: String?,
        fieldValues: Map<String, String>,
    ) {
        // No-op until a real iOS agent lands.
    }

    override suspend fun saveKey(key: String) {
        // Suspend variant used by the KeyPaste screen's `saveKey`
        // callback. The screen also dispatches SubmitKey afterwards,
        // which is where we persist + advance — this method writes
        // the key without changing screen state so the dispatch path
        // owns transitions.
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
        // Key present + onboarding done → land on Chat. The Chat surface
        // renders the "Chat — coming to iOS" placeholder; `agentReady`
        // is true so the screen actually mounts (vs. defensive
        // redirect-to-KeyPaste in ScreenRouter).
        _state.value = _state.value.copy(
            agentReady = true,
            screen = Screen.Chat,
        )
    }

    // ─── Provider key status snapshot ─────────────────────────────
    // Iterates KeyVaultGateway.hasApiKey for each provider. The map
    // value is a placeholder string ("•••") since the gateway doesn't
    // expose last-4 — the Providers screen renders "configured" when
    // an entry exists, otherwise "Add key".

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
}
