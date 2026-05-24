package dev.weft.undercurrent.core

import dev.weft.contracts.ProviderKind
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
}
