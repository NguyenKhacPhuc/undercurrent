package dev.weft.undercurrent.core.navigation

import androidx.lifecycle.ViewModel

/**
 * Owns the app-wide nav back stack. Single source of truth for "what
 * screen is on top" — every screen's `onBack` callback and the
 * system back handler dispatch through [dispatch]; the App composable
 * renders [backStack]'s top entry via [NavDisplay].
 *
 * Lifecycle: registered as a Koin `single` (not a per-screen
 * `viewModel`) so the same instance is shared across every consumer
 * — composables, feature ViewModels that need to navigate, and the
 * Android system-back handler. Extends [ViewModel] for the
 * configuration-change retention semantics; doesn't need an MVI
 * state slot because Compose already tracks [backStack] directly
 * (it's a `SnapshotStateList`).
 *
 * Why this isn't an [dev.weft.undercurrent.shared.mvi.MviViewModel]:
 * the "state" here IS the back stack — Compose's snapshot system
 * gives readers an automatic recomposition signal on every push /
 * pop, so wrapping it in a `StateFlow<NavigationState>` would add
 * a redundant layer. Effects aren't meaningful for navigation.
 *
 * Concurrency: [dispatch] mutates [backStack] from any thread that
 * calls it. `SnapshotStateList`'s thread-safety guarantees apply —
 * mutations are atomic from Compose's perspective.
 */
class NavigationViewModel : ViewModel() {

    val backStack: NavBackStack<Screen> = NavBackStack(Screen.Loading)

    fun dispatch(intent: NavigationIntent) {
        when (intent) {
            is NavigationIntent.Navigate -> {
                if (backStack.lastOrNull() != intent.screen) {
                    backStack.add(intent.screen)
                }
            }
            NavigationIntent.Back -> {
                if (backStack.size > 1) {
                    backStack.removeAt(backStack.lastIndex)
                }
            }
            is NavigationIntent.NavigateAndClear -> {
                backStack.clear()
                backStack.add(intent.screen)
            }
        }
    }


    val current: Screen?
        get() = backStack.lastOrNull()
}
