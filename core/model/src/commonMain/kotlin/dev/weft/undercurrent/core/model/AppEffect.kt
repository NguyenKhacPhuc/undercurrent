package dev.weft.undercurrent.core.model

/**
 * One-shot side effects emitted by AppStore. Consumed in `App` via a
 * `LaunchedEffect` collecting `AppStore.effects`.
 *
 * Kept minimal — most state is reduced into AppState instead. Effects
 * are reserved for things that genuinely shouldn't replay on
 * configuration change (e.g. error toasts).
 *
 * KMP — commonMain. Moved from `app/.../core/AppEffect.kt`.
 */
public sealed interface AppEffect {
    /** Surface a transient error message (e.g. via Snackbar / Toast). */
    public data class Error(public val message: String) : AppEffect
}
