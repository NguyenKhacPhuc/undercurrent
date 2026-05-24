package dev.weft.undercurrent.core

/**
 * One-shot side effects emitted by [AppStore]. Consumed in [App] via a
 * `LaunchedEffect` collecting [AppStore.effects].
 *
 * Kept minimal — most state is reduced into [AppState] instead. Effects are
 * reserved for things that genuinely shouldn't replay on configuration change
 * (e.g. error toasts).
 */
internal sealed interface AppEffect {
    /** Surface a transient error message (e.g. via Snackbar / Toast). */
    data class Error(val message: String) : AppEffect
}
