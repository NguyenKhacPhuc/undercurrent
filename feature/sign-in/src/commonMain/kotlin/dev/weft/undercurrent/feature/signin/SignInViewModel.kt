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
        when (intent) {
            SignInIntent.SwitchMode -> update { s ->
                s.copy(
                    mode = when (s.mode) {
                        SignInState.Mode.SignIn -> SignInState.Mode.Register
                        SignInState.Mode.Register -> SignInState.Mode.SignIn
                    },
                    // Toggling clears displayName + any stale errors so the
                    // new form starts in a clean state. Email + password are
                    // preserved per Q2's UX guess.
                    displayName = "",
                    topError = null,
                    fieldErrors = emptyMap(),
                    showSwitchToSignInShortcut = false,
                )
            }
            SignInIntent.SwitchToSignInWithEmail -> update { s ->
                s.copy(
                    mode = SignInState.Mode.SignIn,
                    topError = null,
                    fieldErrors = emptyMap(),
                    showSwitchToSignInShortcut = false,
                )
            }
            is SignInIntent.EmailChanged -> update { s -> s.copy(email = intent.value) }
            is SignInIntent.PasswordChanged -> update { s -> s.copy(password = intent.value) }
            is SignInIntent.DisplayNameChanged -> update { s -> s.copy(displayName = intent.value) }
            SignInIntent.ClearTopError -> update { s -> s.copy(topError = null) }
            // Continue dispatch lands in tasks 3 + 4.
            SignInIntent.Continue -> Unit
        }
    }
}
