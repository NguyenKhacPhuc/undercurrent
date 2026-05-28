package dev.weft.undercurrent.core.navigation

/**
 * Every top-level destination the host can navigate to. Sealed so the
 * router's `when` is exhaustive — adding a new screen requires touching
 * the dispatcher.
 *
 * KMP — commonMain. Moved from `app/.../core/Screen.kt`.
 */
sealed interface Screen {
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
     * Visual presentation sub-screen. Combines palette picker + light/
     * dark mode. One drill-down from Settings → Appearance.
     */
    data object Appearance : Screen

    data object Usage : Screen
    data object Personas : Screen

    /**
     * Settings → Integrations sub-screen. Lists supported third-party
     * integrations; Connect kicks off the substrate's OAuth + MCP wiring.
     */
    data object Integrations : Screen

    /**
     * Drawer → Mini apps. Management view for user-saved mini-apps.
     */
    data object MiniApps : Screen

    /**
     * Guided creator wizard — driven entirely by the agent via
     * `ui_render`.
     */
    data object Creator : Screen
}
