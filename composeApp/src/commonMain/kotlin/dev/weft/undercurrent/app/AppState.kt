package dev.weft.undercurrent.app

import dev.weft.undercurrent.core.model.ModelTier
import dev.weft.undercurrent.core.model.ProviderKind
import dev.weft.undercurrent.core.model.ThemePrefs
import dev.weft.undercurrent.core.navigation.Screen

/**
 * Root state for the App composable. Held in [AppStore]; observed via
 * [androidx.lifecycle.compose.collectAsStateWithLifecycle] in the
 * composable.
 *
 * `displayMessages` is intentionally *not* here — it lives on AppStore
 * as a [androidx.compose.runtime.snapshots.SnapshotStateList] so the
 * chat surface can keep appending streaming chunks directly.
 *
 * KMP — commonMain. Uses mirror types only ([ProviderKind] / [ModelTier]
 * from `:core:model`); no Weft type leaks into commonMain so iOS can
 * compile the same surface.
 */
public data class AppState(
    public val screen: Screen,
    /**
     * Previous screen captured on every [AppIntent.Navigate]. Lets
     * screens reachable from more than one entry point (Integrations,
     * opened from both Settings and the chat input) route back to the
     * right place. Single-step only — replace with a `List<Screen>` if
     * the back-target depth grows past one level.
     */
    public val previousScreen: Screen = Screen.Chat,
    /**
     * Whether AppStore has a built agent ready. The full `WeftAgent`
     * stays inside the Android-only AppStore impl; the UI only needs
     * to know "can we send / are we still booting".
     */
    public val agentReady: Boolean = false,
    /**
     * Active conversation id, mirrored from the agent. Drives the
     * drawer's "currently active" highlight and the
     * ConversationsListScreen's selection. Null while the agent
     * isn't built yet.
     */
    public val currentConversationId: String? = null,
    public val chat: ChatStatus = ChatStatus(),
    /**
     * Theme prefs. Mirrors what's persisted in DataStore — AppStore
     * collects from the repo flow and updates this slot.
     */
    public val themePrefs: ThemePrefs = ThemePrefs.Default,
    /**
     * Whether the user has finished first-launch onboarding.
     */
    public val onboardingCompleted: Boolean = false,
    /**
     * Active LLM provider + default tier. Mirrors the provider-prefs
     * repo.
     */
    public val activeProvider: ProviderKind = ProviderKind.Anthropic,
    public val defaultTier: ModelTier? = null,
    /**
     * Name of the active agent declaration. The full per-agent tool
     * allowlist + system fragment + strategy live inside AppStore's
     * runtime; the UI only renders the name in the selector.
     */
    public val activeAgentName: String = DEFAULT_AGENT_NAME,
    /**
     * User-addressable agents the host registered. Empty / one-element
     * lists hide the chat selector.
     */
    public val availableAgents: List<AgentSummary> = emptyList(),
    /**
     * Per-provider last-4 of the stored key — `Map<ProviderKind, last4>`.
     * Empty entries (no key stored) are absent from the map. Drives
     * the providers screen subtitle without exposing the secret.
     */
    public val providerKeyStatus: Map<ProviderKind, String> = emptyMap(),
    /**
     * Surface a "permission needed" dialog when a tool fails because
     * Android denied a runtime permission. Cleared via
     * [AppIntent.DismissPermissionDialog].
     */
    public val pendingPermissionDialog: PermissionDialogState? = null,
) {
    public companion object {
        public const val DEFAULT_AGENT_NAME: String = "default"

        public fun initial(): AppState = AppState(
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
public data class PermissionDialogState(
    public val toolName: String,
    public val friendlyTitle: String,
    public val friendlyBody: String,
)

/**
 * Mirror for `dev.weft.harness.agents.AgentDeclaration` — the lossy
 * subset the chat selector needs. Hosts adapt
 * `runtime.agentDeclarations.values` into this list at boot.
 */
public data class AgentSummary(
    public val name: String,
    public val displayName: String,
    public val description: String,
)

public data class ChatStatus(
    public val inFlight: Boolean = false,
    public val lastError: String? = null,
)

// Re-export the default-agent constant via AppState so consumers don't
// need to dig into the companion. Mirrors what
// `dev.weft.harness.agents.AgentDeclaration.DEFAULT_AGENT_NAME` exposes.
internal const val DEFAULT_AGENT_NAME: String = "default"
