package dev.weft.undercurrent.feature.auth

sealed interface SignInEffect {
    data object SignedIn : SignInEffect
}
