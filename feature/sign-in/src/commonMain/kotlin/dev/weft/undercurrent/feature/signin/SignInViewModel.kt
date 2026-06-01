package dev.weft.undercurrent.feature.signin

import dev.weft.undercurrent.core.domain.AuthRepository
import dev.weft.undercurrent.core.domain.SessionTokenStore
import dev.weft.undercurrent.shared.mvi.MviViewModel

/**
 * MVI ViewModel for the first-launch sign-in / register screen.
 *
 * Wraps the four BE auth endpoints (`signUp`, `signIn`) via
 * [AuthRepository]; on success, persists the bearer via
 * [sessionTokenStore] and emits [SignInEffect.SignedIn] so the Route
 * can resume the boot cascade.
 *
 * Behavior is filled in across subsequent commits — this class only
 * carries the skeleton MVI plumbing for the first task.
 */
class SignInViewModel(
    private val authRepository: AuthRepository,
    private val sessionTokenStore: SessionTokenStore,
) : MviViewModel<SignInState, SignInIntent, SignInEffect>(
    initialState = SignInState(),
) {
    override fun dispatch(intent: SignInIntent) = launch {
        // Intent handling lands in tasks 2–4.
    }
}
