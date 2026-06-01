package dev.weft.undercurrent.feature.signin

sealed interface SignInIntent {
    data object SwitchMode : SignInIntent
    data object SwitchToSignInWithEmail : SignInIntent
    data class EmailChanged(val value: String) : SignInIntent
    data class PasswordChanged(val value: String) : SignInIntent
    data class DisplayNameChanged(val value: String) : SignInIntent
    data object Continue : SignInIntent
    data object ClearTopError : SignInIntent
}
