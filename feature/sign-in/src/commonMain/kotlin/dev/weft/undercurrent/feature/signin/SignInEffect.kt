package dev.weft.undercurrent.feature.signin

/**
 * One-shot signals fired by [SignInViewModel] that the Route observes
 * and converts into navigation / lifecycle actions.
 */
sealed interface SignInEffect {
    /**
     * The session is established (token persisted). The Route triggers
     * `AppViewModel.resume()` which re-evaluates the boot cascade —
     * onboarding → KeyPaste → Chat.
     */
    data object SignedIn : SignInEffect
}
