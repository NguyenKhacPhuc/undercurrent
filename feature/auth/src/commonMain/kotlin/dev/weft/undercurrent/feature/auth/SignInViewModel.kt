package dev.weft.undercurrent.feature.auth

import dev.weft.undercurrent.core.domain.AuthRepository
import dev.weft.undercurrent.core.domain.SessionTokenStore
import dev.weft.undercurrent.core.domain.auth.dto.AuthResponse
import dev.weft.undercurrent.core.ext.Result
import dev.weft.undercurrent.core.ui.toUserMessage
import dev.weft.undercurrent.data.network.common.ApiException
import dev.weft.undercurrent.data.network.common.ErrorCodes
import dev.weft.undercurrent.shared.mvi.MviViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

class SignInViewModel(
    private val authRepository: AuthRepository,
    private val sessionTokenStore: SessionTokenStore,
) : MviViewModel<SignInState, SignInIntent, SignInEffect>(
    initialState = SignInState(),
) {
    override fun dispatch(intent: SignInIntent) = launch {
        when (intent) {
            SignInIntent.SwitchMode -> update { state ->
                state.copy(
                    mode = when (state.mode) {
                        SignInState.Mode.SignIn -> SignInState.Mode.Register
                        SignInState.Mode.Register -> SignInState.Mode.SignIn
                    },
                    displayName = "",
                    topError = null,
                    fieldErrors = emptyMap(),
                    showSwitchToSignInShortcut = false,
                )
            }
            SignInIntent.SwitchToSignInWithEmail -> update { state ->
                state.copy(
                    mode = SignInState.Mode.SignIn,
                    topError = null,
                    fieldErrors = emptyMap(),
                    showSwitchToSignInShortcut = false,
                )
            }
            is SignInIntent.EmailChanged -> update { state -> state.copy(email = intent.value) }
            is SignInIntent.PasswordChanged -> update { state -> state.copy(password = intent.value) }
            is SignInIntent.DisplayNameChanged -> update { state -> state.copy(displayName = intent.value) }
            SignInIntent.ClearTopError -> update { state -> state.copy(topError = null) }
            SignInIntent.Continue -> handleContinue()
        }
    }

    private suspend fun handleContinue() {
        val snapshot = state.value
        if (!snapshot.canSubmit) return
        update { state ->
            state.copy(
                topError = null,
                fieldErrors = emptyMap(),
                showSwitchToSignInShortcut = false,
            )
        }
        withLoading {
            when (snapshot.mode) {
                SignInState.Mode.SignIn -> handleSignIn(snapshot.email, snapshot.password)
                SignInState.Mode.Register -> handleRegister(
                    displayName = snapshot.displayName,
                    email = snapshot.email,
                    password = snapshot.password,
                )
            }
        }
    }

    private suspend fun handleSignIn(email: String, password: String) {
        authRepository.signIn(email, password)
            .onEach { result ->
                when (result) {
                    Result.Loading -> Unit
                    is Result.Success -> onAuthSuccess(result.data)
                    is Result.Error -> update { state -> state.copy(topError = mapSignInError(result.exception)) }
                }
            }
            .collect()
    }

    private suspend fun handleRegister(displayName: String, email: String, password: String) {
        authRepository.signUp(displayName, email, password)
            .onEach { result ->
                when (result) {
                    Result.Loading -> Unit
                    is Result.Success -> onAuthSuccess(result.data)
                    is Result.Error -> applyRegisterError(result.exception)
                }
            }
            .collect()
    }

    private fun applyRegisterError(e: Throwable) {
        val api = e as? ApiException
        when {
            api != null && api.httpStatus == 400 && !api.details.isNullOrEmpty() ->
                update { state -> state.copy(fieldErrors = api.details.orEmpty(), topError = null) }
            api != null && api.code == ErrorCodes.EMAIL_ALREADY_REGISTERED ->
                update { state ->
                    state.copy(
                        topError = TopError.Message(api.apiMessage),
                        showSwitchToSignInShortcut = true,
                    )
                }
            else -> update { state -> state.copy(topError = e.toTopError()) }
        }
    }

    private suspend fun onAuthSuccess(response: AuthResponse) {
        sessionTokenStore.save(response.session.token)
        emit(SignInEffect.SignedIn)
    }

    private fun mapSignInError(e: Throwable): TopError {
        val api = e as? ApiException
        return when {
            api != null && api.httpStatus == 401 -> TopError.InvalidCredentials
            api != null && api.httpStatus == 429 -> TopError.RateLimited
            else -> e.toTopError()
        }
    }
}

private fun Throwable.toTopError(): TopError = when (this) {
    is dev.weft.undercurrent.data.network.common.NetworkException -> TopError.Network
    else -> TopError.Message(toUserMessage() ?: "Unknown error.")
}
