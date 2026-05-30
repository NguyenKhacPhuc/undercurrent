package dev.weft.undercurrent.app

import dev.weft.undercurrent.core.model.ModelTier
import dev.weft.undercurrent.core.model.ProviderKind
import dev.weft.undercurrent.core.model.ThemePrefs
import dev.weft.undercurrent.core.navigation.Screen

/**
 * Root state for the App composable. Held in [AppViewModel]; observed via
 * [androidx.lifecycle.compose.collectAsStateWithLifecycle] in the
 * composable.
 *
 * `displayMessages` is intentionally *not* here — it lives on AppViewModel
 * as a [androidx.compose.runtime.snapshots.SnapshotStateList] so the
 * chat surface can keep appending streaming chunks directly.
 *
 * KMP — commonMain. Uses mirror types only ([ProviderKind] / [ModelTier]
 * from `:core:model`); no Weft type leaks into commonMain so iOS can
 * compile the same surface.
 */
data class AppState(
    val screen: Screen,
    /**
     * Previous screen captured on every [AppIntent.Navigate]. Lets
     * screens reachable from more than one entry point (Integrations,
     * opened from both Settings and the chat input) route back to the
     * right place. Single-step only — replace with a `List<Screen>` if
     * the back-target depth grows past one level.
     */
    val previousScreen: Screen = Screen.Chat,
    /**
     * Whether AppViewModel has a built agent ready. The full `WeftAgent`
     * stays inside the Android-only AppViewModel impl; the UI only needs
     * to know "can we send / are we still booting".
     */
    val agentReady: Boolean = false,
    /**
     * Active conversation id, mirrored from the agent. Drives the
     * drawer's "currently active" highlight and the
     * ConversationsListScreen's selection. Null while the agent
     * isn't built yet.
     */
    val currentConversationId: String? = null,
    val chat: ChatStatus = ChatStatus(),
    /**
     * Theme prefs. Mirrors what's persisted in DataStore — AppViewModel
     * collects from the repo flow and updates this slot.
     */
    val themePrefs: ThemePrefs = ThemePrefs.Default,
    /**
     * Whether the user has finished first-launch onboarding.
     */
    val onboardingCompleted: Boolean = false,
    /**
     * Active LLM provider + default tier. Mirrors the provider-prefs
     * repo.
     */
    val activeProvider: ProviderKind = ProviderKind.Anthropic,
    val defaultTier: ModelTier? = null,
    /**
     * Name of the active agent declaration. The full per-agent tool
     * allowlist + system fragment + strategy live inside AppViewModel's
     * runtime; the UI only renders the name in the selector.
     */
    val activeAgentName: String = DEFAULT_AGENT_NAME,
    /**
     * User-addressable agents the host registered. Empty / one-element
     * lists hide the chat selector.
     */
    val availableAgents: List<AgentSummary> = emptyList(),
    /**
     * Per-provider last-4 of the stored key — `Map<ProviderKind, last4>`.
     * Empty entries (no key stored) are absent from the map. Drives
     * the providers screen subtitle without exposing the secret.
     */
    val providerKeyStatus: Map<ProviderKind, String> = emptyMap(),
    /**
     * Surface a "permission needed" dialog when a tool fails because
     * Android denied a runtime permission. Cleared via
     * [AppIntent.DismissPermissionDialog].
     */
    val pendingPermissionDialog: PermissionDialogState? = null,
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

/**
 * Payload for the "permission needed" dialog. Surfaced when a tool
 * call returns "Permission denied for <tool>" — the host parses that
 * message into this state instead of leaving a cryptic tool-fail
 * bubble in chat.
 */
data class PermissionDialogState(
    val toolName: String,
    val friendlyTitle: String,
    val friendlyBody: String,
)

/**
 * Mirror for `dev.weft.harness.agents.AgentDeclaration` — the lossy
 * subset the chat selector needs. Hosts adapt
 * `runtime.agentDeclarations.values` into this list at boot.
 */
data class AgentSummary(
    val name: String,
    val displayName: String,
    val description: String,
)

data class ChatStatus(
    val inFlight: Boolean = false,
    val lastError: String? = null,
)

// Re-export the default-agent constant via AppState so consumers don't
// need to dig into the companion. Mirrors what
// `dev.weft.harness.agents.AgentDeclaration.DEFAULT_AGENT_NAME` exposes.
internal const val DEFAULT_AGENT_NAME: String = "default"
