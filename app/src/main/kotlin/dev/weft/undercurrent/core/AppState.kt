package dev.weft.undercurrent.core

import dev.weft.contracts.ProviderKind
import dev.weft.harness.agents.AgentDeclaration
import dev.weft.harness.agents.WeftAgent
import dev.weft.harness.agents.routing.ModelTier
import dev.weft.undercurrent.theme.ThemePrefs

/**
 * Root state for [App]. Held in [AppStore]; observed via
 * [androidx.lifecycle.compose.collectAsStateWithLifecycle] in the composable.
 *
 * `displayMessages` is intentionally *not* here — it lives on [AppStore] as
 * a [androidx.compose.runtime.snapshots.SnapshotStateList] so [ui.ChatScreen]
 * can keep appending streaming chunks the way it does today. Conversion to
 * `List<DisplayMessage>` in this data class would force a rewrite of the
 * streaming-append path inside ChatScreen, which is out of scope for the
 * MVI refactor.
 */
internal data class AppState(
    val screen: Screen,
    /**
     * The screen we navigated *from* on the most recent [AppIntent.Navigate].
     * Used by screens reachable from more than one entry point (e.g. the
     * Integrations screen, which is opened from both Settings and the
     * chat input's "Add to Chat" sheet) so their back buttons land on the
     * right place. Single-step only — not a full back stack. If we grow
     * past one level of dynamic back-routing, replace this with a
     * `List<Screen>` and push/pop in the reducer.
     */
    val previousScreen: Screen = Screen.Chat,
    val agent: WeftAgent?,
    val chat: ChatStatus = ChatStatus(),
    /**
     * Theme prefs. Mirrors what's persisted in DataStore via
     * [theme.ThemeRepository] — the store collects from the repo flow and
     * updates this slot. Direct mutations (Settings tap) dispatch
     * [AppIntent.SetPalette] / [AppIntent.SetThemeMode] which both persist
     * and propagate back through the same flow.
     */
    val themePrefs: ThemePrefs = ThemePrefs.Default,
    /**
     * Whether the user has finished the first-launch onboarding flow.
     * Mirrors [onboarding.OnboardingRepository.completedFlow]. The boot
     * path gates which screen to navigate to on resume.
     */
    val onboardingCompleted: Boolean = false,
    /**
     * Active LLM provider and default tier. Mirrors
     * [provider.ProviderPrefsRepository]. Drives which credential alias
     * the boot path reads, and what [ModelTier] (if any) the chat input
     * defaults to.
     */
    val activeProvider: ProviderKind = ProviderKind.Anthropic,
    val defaultTier: ModelTier? = null,
    /**
     * Name of the active [dev.weft.harness.agents.AgentDeclaration].
     * Defaults to [AgentDeclaration.DEFAULT_AGENT_NAME] for the
     * unswitched experience. Mutated via [AppIntent.SelectAgent] (the
     * chat-surface selector) or by parsing an `@mention` from user
     * input. Every [WeftAgent] rebuild routes through this — the
     * runtime's per-agent tool allowlist + system fragment + strategy
     * follow from the declaration this name resolves to.
     */
    val activeAgentName: String = AgentDeclaration.DEFAULT_AGENT_NAME,
    /**
     * User-addressable agents the host registered, in registration
     * order. Populated once at boot from `runtime.agentDeclarations`
     * and held here so the chat surface's [AgentSelector] can render
     * without reaching back into the runtime. Empty/single-element
     * lists hide the selector (see `AgentSelector` composable
     * behavior).
     */
    val availableAgents: List<AgentSummary> = emptyList(),
    /**
     * Set when a tool execution fails because Android denied a runtime
     * permission. The chat surface observes this and renders an
     * AlertDialog with an "Open Settings" deep-link button so the user
     * can grant the permission and try again — the system-prompt path
     * doesn't help once the user has hit "Don't allow" twice, because
     * Android stops showing the prompt entirely.
     *
     * Null when no dialog is pending. Cleared via
     * [AppIntent.DismissPermissionDialog].
     */
    val pendingPermissionDialog: PermissionDialogState? = null,
) {
    companion object {
        fun initial(): AppState = AppState(
            screen = Screen.Loading,
            agent = null,
            chat = ChatStatus(),
            themePrefs = ThemePrefs.Default,
            onboardingCompleted = false,
            activeProvider = ProviderKind.Anthropic,
            defaultTier = null,
        )
    }
}

/**
 * Payload for the "permission needed" dialog surfaced when a tool fails
 * because the user has denied (often "forever") a runtime permission.
 *
 * The substrate throws [dev.weft.tools.PermissionDeniedException] from
 * the tool's permission gate, which the agent loop converts to a
 * [dev.weft.harness.agents.streaming.StreamChunk.ToolFailed] with the
 * stock message format `"Permission denied for {tool}: {PERM_NAME(s)}."`.
 * [AppStore] parses that message into this state instead of letting
 * the cryptic stack-trace-shaped text land in the chat as a tool-fail
 * bubble.
 *
 * @property toolName the substrate tool that failed (e.g. `location_current`).
 * @property friendlyTitle short human-readable title for the dialog —
 *   e.g. "Location access needed". Mapped from [toolName] in AppStore.
 * @property friendlyBody one or two sentences explaining what the
 *   permission unlocks and inviting the user to open Settings.
 */
internal data class PermissionDialogState(
    val toolName: String,
    val friendlyTitle: String,
    val friendlyBody: String,
)

/**
 * App-layer projection of [dev.weft.harness.agents.AgentDeclaration]
 * for use by the chat-surface [AgentSelector] composable. We don't
 * pass the SDK's declaration into the Compose layer because the
 * `:android-compose-defaults` module deliberately doesn't depend on
 * `:harness:agents`; this small DTO is the contract between the two.
 */
internal data class AgentSummary(
    val name: String,
    val displayName: String,
    val description: String,
)

/**
 * Chat-surface UI state. Drives the "Thinking…" indicator, the disabled
 * input/button states, and the inline error text in [ui.ChatScreen]. Lives
 * inside [AppState] so a config change (rotation) preserves an in-flight
 * send's spinner.
 */
internal data class ChatStatus(
    val inFlight: Boolean = false,
    val lastError: String? = null,
)

internal sealed interface Screen {
    data object Loading : Screen
    data object Onboarding : Screen
    data object KeyPaste : Screen
    data object Chat : Screen
    data object Traces : Screen
    data object Memories : Screen
    data object Conversations : Screen
    data object RenderedTree : Screen
    data object Settings : Screen
    /**
     * Provider & default-model sub-screen. Combines provider selection +
     * API key management + per-tier model overrides + the default-tier
     * picker — a single drill-down from Settings → Provider.
     */
    data object Providers : Screen
    /**
     * Visual presentation sub-screen. Combines palette picker + light/dark
     * mode. One drill-down from Settings → Appearance.
     */
    data object Appearance : Screen
    data object Usage : Screen
    data object Personas : Screen
    /**
     * Settings → Integrations sub-screen. Lists supported third-party
     * integrations (Linear, Notion, …); Connect kicks off the
     * substrate's OAuth + MCP wiring.
     */
    data object Integrations : Screen
    /**
     * Drawer → Mini apps. Management view for user-saved mini-apps
     * (a UI tree + the prompt that produced it). Each card renders
     * its cached `ui_render` tree non-interactively; tap opens the
     * full mini-app via the seed-bridge-then-refresh path. Creation
     * lives in the chat input's Add-to-Chat sheet's "Save as
     * mini-app" affordance, not here.
     */
    data object MiniApps : Screen

    /**
     * Guided creator wizard — driven entirely by the agent via
     * `ui_render`. The user enters from a "+ New" tap on Personas or
     * Mini Apps; they see only the agent's rendered question widgets
     * (no chat input bar). The agent finalizes via `create_persona`
     * or `create_mini_app`, which navigates back automatically.
     *
     * One variant for both flavors of creation (persona-voice,
     * persona-role, mini-app) — the [dev.weft.undercurrent.features.creator.CreatorSession]
     * singleton tracks which one is active.
     */
    data object Creator : Screen
}
