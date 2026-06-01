package dev.weft.undercurrent.feature.signin

import dev.weft.undercurrent.core.ext.MIN_PASSWORD_LENGTH
import dev.weft.undercurrent.core.ext.displayNameValid
import dev.weft.undercurrent.core.ext.emailLooksValid

data class SignInState(
    val mode: Mode = Mode.SignIn,
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val topError: TopError? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    val showSwitchToSignInShortcut: Boolean = false,
) {
    enum class Mode { SignIn, Register }

    val canSubmit: Boolean
        get() = when (mode) {
            Mode.SignIn -> emailLooksValid(email) && password.isNotEmpty()
            Mode.Register ->
                displayNameValid(displayName) &&
                    emailLooksValid(email) &&
                    password.length >= MIN_PASSWORD_LENGTH
        }
}

sealed interface TopError {
    data class Message(val message: String) : TopError
    data object RateLimited : TopError
    data object InvalidCredentials : TopError
    data object Network : TopError
}
