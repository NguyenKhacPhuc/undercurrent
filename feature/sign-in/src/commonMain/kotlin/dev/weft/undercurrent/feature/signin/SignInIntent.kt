package dev.weft.undercurrent.feature.signin

/**
 * User-driven mutations on the sign-in / register form. The UI never
 * touches [SignInState] directly — every change goes through one of
 * these.
 */
sealed interface SignInIntent {
    /** Toggle between Sign-In and Register. Email + password preserved; errors cleared. */
    data object SwitchMode : SignInIntent

    /**
     * Shortcut from the `email_already_registered` 409 path: flip to
     * Sign-In mode while keeping the email pre-filled. Clears the
     * "Switch to Sign In" affordance + any top error.
     */
    data object SwitchToSignInWithEmail : SignInIntent

    data class EmailChanged(val value: String) : SignInIntent
    data class PasswordChanged(val value: String) : SignInIntent
    data class DisplayNameChanged(val value: String) : SignInIntent

    /** Submit the active mode's form to the BE. No-op if `state.canSubmit` is false. */
    data object Continue : SignInIntent

    /** Dismiss the current top error — used by the Retry action on network failure. */
    data object ClearTopError : SignInIntent
}
