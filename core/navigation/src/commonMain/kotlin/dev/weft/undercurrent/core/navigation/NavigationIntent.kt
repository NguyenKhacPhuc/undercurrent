package dev.weft.undercurrent.core.navigation

/**
 * Intents that mutate the nav back stack. Handled by the root VM's
 * navigation slice — pushes / pops / resets the [NavBackStack].
 *
 * Every screen's "back" callback dispatches [Back] (the system back
 * handler too) — call sites don't need to know which screen they were
 * navigated from. Boot transitions use [NavigateAndClear] to seed a
 * fresh root.
 */
sealed interface NavigationIntent {

    /**
     * Push [screen] onto the back stack. The router renders the top
     * entry; system back / explicit [Back] pops it.
     */
    data class Navigate(val screen: Screen) : NavigationIntent

    /**
     * Pop the top entry off the back stack. No-op when the stack is
     * at its root (one entry left).
     */
    data object Back : NavigationIntent

    /**
     * Reset the back stack so [screen] is the only entry. Used by
     * boot transitions (Loading → Onboarding / KeyPaste / Chat) where
     * there's no meaningful "back" target — and by deep links that
     * should replace any historical navigation.
     */
    data class NavigateAndClear(val screen: Screen) : NavigationIntent
}
