package dev.weft.undercurrent.core.navigation

/**
 * Owns the app-wide nav back stack. Single source of truth for "what
 * screen is on top" — every screen's `onBack` callback and the
 * system back handler dispatch through [dispatch]; the App composable
 * renders [backStack]'s top entry via [NavDisplay].
 *
 * A plain class, not a `ViewModel`: it's registered as a Koin `single`,
 * so the one instance already lives for the whole process and is shared
 * across every consumer — composables, feature ViewModels that need to
 * navigate, and the Android system-back handler. There's no
 * `viewModelScope`, no `onCleared`, and the back stack is a
 * `SnapshotStateList`, so Compose tracks it directly and gives readers
 * an automatic recomposition signal on every push / pop — a
 * `StateFlow<NavigationState>` or MVI slot would just be a redundant
 * layer. Nothing the `ViewModel` base provides was being used.
 *
 * Concurrency: [dispatch] mutates [backStack] from any thread that
 * calls it. `SnapshotStateList`'s thread-safety guarantees apply —
 * mutations are atomic from Compose's perspective.
 */
class Navigator {

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
            is NavigationIntent.StartFlow -> {
                if (backStack.lastOrNull() != intent.graph.start) {
                    backStack.add(intent.graph.start)
                }
            }
            NavigationIntent.EndFlow -> {
                val flow = backStack.lastOrNull()?.let(::graphOf) ?: return
                while (backStack.size > 1 && backStack.last() in flow.screens) {
                    backStack.removeAt(backStack.lastIndex)
                }
            }
        }
    }


    val current: Screen?
        get() = backStack.lastOrNull()
}
