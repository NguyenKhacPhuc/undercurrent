package dev.weft.undercurrent.feature.signin

import dev.weft.undercurrent.core.domain.AuthRepository
import dev.weft.undercurrent.core.domain.SessionTokenStore
import dev.weft.undercurrent.core.domain.auth.dto.AuthResponse
import dev.weft.undercurrent.core.ext.Result
import dev.weft.undercurrent.data.network.common.ApiException
import dev.weft.undercurrent.data.network.common.HttpException
import dev.weft.undercurrent.data.network.common.NetworkException
import dev.weft.undercurrent.shared.mvi.MviViewModel
import kotlinx.coroutines.flow.collect

/**
 * MVI ViewModel for the first-launch sign-in / register screen.
 *
 * Wraps the two write endpoints (`signUp`, `signIn`) on [AuthRepository];
 * on success, persists the bearer via [sessionTokenStore] and emits
 * [SignInEffect.SignedIn] so the Route can re-trigger
 * `AppViewModel.resume()` and let the boot cascade route the user
 * forward to the existing provider/key onboarding step (per `decisions#D7`).
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
            SignInIntent.Continue -> handleContinue()
        }
    }

    private suspend fun handleContinue() {
        val snapshot = state.value
        if (!snapshot.canSubmit) return
        update { s ->
            s.copy(
                submitting = true,
                topError = null,
                fieldErrors = emptyMap(),
                showSwitchToSignInShortcut = false,
            )
        }
        when (snapshot.mode) {
            SignInState.Mode.SignIn -> handleSignIn(snapshot.email, snapshot.password)
            SignInState.Mode.Register -> handleRegister(
                displayName = snapshot.displayName,
                email = snapshot.email,
                password = snapshot.password,
            )
        }
    }

    private suspend fun handleSignIn(email: String, password: String) {
        authRepository.signIn(email, password).collect { result ->
            when (result) {
                Result.Loading -> Unit
                is Result.Success -> onAuthSuccess(result.data)
                is Result.Error -> update { s ->
                    s.copy(submitting = false, topError = mapSignInError(result.exception))
                }
            }
        }
    }

    private suspend fun handleRegister(displayName: String, email: String, password: String) {
        // Behavior lands in task 4.
    }

    private suspend fun onAuthSuccess(response: AuthResponse) {
        sessionTokenStore.save(response.session.token)
        update { s -> s.copy(submitting = false) }
        emit(SignInEffect.SignedIn)
    }

    /**
     * Maps an exception thrown by `authRepository.signIn()` to the form's
     * top-error slot. Codes mirror the BE error envelope documented in
     * `api-contract.md`; everything unrecognized falls through to a
     * generic message so the user is never left with a silent failure.
     */
    private fun mapSignInError(e: Throwable): TopError = when {
        e is ApiException && e.httpStatus == 401 -> TopError.InvalidCredentials
        e is ApiException && e.httpStatus == 429 -> TopError.RateLimited
        e is ApiException -> TopError.Message(e.apiMessage)
        e is NetworkException -> TopError.Network
        e is HttpException -> TopError.Message("Server error (${e.httpStatus}).")
        else -> TopError.Message(e.message ?: "Unknown error.")
    }
}
