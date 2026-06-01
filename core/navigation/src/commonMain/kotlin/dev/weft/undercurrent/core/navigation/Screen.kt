package dev.weft.undercurrent.core.navigation

/**
 * Every top-level destination the host can navigate to. Each variant
 * is a [NavKey] so it can sit on the [NavBackStack] that backs the
 * host's router.
 *
 * Sealed so the router's `when` is exhaustive — adding a new screen
 * requires touching the dispatcher.
 *
 * Convention so far: every destination is a `data object` with no
 * params (e.g. the active conversation id lives in `AppState`, not on
 * the Screen entry). When a screen genuinely benefits from typed
 * params (e.g. `TraceDetail(traceId)`), convert that one to a data
 * class — the rest stay objects.
 */
sealed interface Screen : NavKey {
    data object Loading : Screen

    /**
     * First-launch register-or-sign-in flow against the BE
     * (`mobile-auth-wiring/05`). Rendered when no session token is
     * stored on-device; after a successful sign-in or register,
     * the host resumes the normal onboarding cascade
     * ([Onboarding] → [KeyPaste] → [Chat]).
     */
    data object SignIn : Screen

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
