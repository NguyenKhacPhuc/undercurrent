package dev.weft.undercurrent.core.model

import dev.weft.undercurrent.core.navigation.Screen

/**
 * Root app state surface — the cross-cutting slots every layer reads.
 * Per-feature VMs project repository flows into the fields they own
 * (provider/onboarding/theme prefs); the agent host writes
 * [agentReady] + [currentConversationId] + [availableAgents] +
 * [activeAgentName] when the active [WeftAgent] is swapped.
 *
 * Lives in `:core:model` (not `:composeApp`) so the chat slice's
 * agent-host classes (`AgentSession` et al. in
 * `:feature:chat/androidMain`) can write to it without depending on
 * `:composeApp`.
 */
data class AppState(
    val screen: Screen,
    val previousScreen: Screen = Screen.Chat,
    val agentReady: Boolean = false,
    val currentConversationId: String? = null,
    val chat: ChatStatus = ChatStatus(),
    val themePrefs: ThemePrefs = ThemePrefs.Default,
    val onboardingCompleted: Boolean = false,
    val activeProvider: ProviderKind = ProviderKind.Anthropic,
    val defaultTier: ModelTier? = null,
    val activeAgentName: String = DEFAULT_AGENT_NAME,
    val availableAgents: List<AgentSummary> = emptyList(),
    val providerKeyStatus: Map<ProviderKind, String> = emptyMap(),
    val pendingPermissionDialog: PermissionDialogState? = null,
    val pendingMiniAppConsent: MiniAppConsentRequest? = null,
) {
    companion object {
        const val DEFAULT_AGENT_NAME: String = "default"

        fun initial(): AppState = AppState(
            screen = Screen.Loading,
            agentReady = false,
            currentConversationId = null,
            chat = ChatStatus(),
            themePrefs = ThemePrefs.Default,
            onboardingCompleted = false,
            activeProvider = ProviderKind.Anthropic,
            defaultTier = null,
        )
    }
}

data class PermissionDialogState(
    val toolName: String,
    val friendlyTitle: String,
    val friendlyBody: String,
)

/**
 * The first-run consent prompt for a mini-app: which mini-app is asking
 * and the plain-language [actions] it wants. Shown as a modal sheet; the
 * user approves or denies before the mini-app renders.
 */
data class MiniAppConsentRequest(
    val miniAppId: String,
    val miniAppName: String,
    val miniAppEmoji: String,
    val actions: List<ConsentAction>,
)

/** One action in a [MiniAppConsentRequest], with its user-facing label. */
data class ConsentAction(
    val name: String,
    val description: String,
)

data class AgentSummary(
    val name: String,
    val displayName: String,
    val description: String,
)

data class ChatStatus(
    val inFlight: Boolean = false,
    val lastError: String? = null,
)
