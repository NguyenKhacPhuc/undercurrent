package dev.weft.undercurrent.core.navigation

/**
 * Every top-level destination the host can navigate to. Sealed so the
 * router's `when` is exhaustive — adding a new screen requires touching
 * the dispatcher.
 *
 * KMP — commonMain. Moved from `app/.../core/Screen.kt`.
 */
public sealed interface Screen {
    public data object Loading : Screen
    public data object Onboarding : Screen
    public data object KeyPaste : Screen
    public data object Chat : Screen
    public data object Traces : Screen
    public data object Memories : Screen
    public data object Conversations : Screen
    public data object RenderedTree : Screen
    public data object Settings : Screen

    /**
     * Provider & default-model sub-screen. Combines provider selection +
     * API key management + per-tier model overrides + the default-tier
     * picker — a single drill-down from Settings → Provider.
     */
    public data object Providers : Screen

    /**
     * Visual presentation sub-screen. Combines palette picker + light/
     * dark mode. One drill-down from Settings → Appearance.
     */
    public data object Appearance : Screen

    public data object Usage : Screen
    public data object Personas : Screen

    /**
     * Settings → Integrations sub-screen. Lists supported third-party
     * integrations; Connect kicks off the substrate's OAuth + MCP wiring.
     */
    public data object Integrations : Screen

    /**
     * Drawer → Mini apps. Management view for user-saved mini-apps.
     */
    public data object MiniApps : Screen

    /**
     * Guided creator wizard — driven entirely by the agent via
     * `ui_render`.
     */
    public data object Creator : Screen
}
