package dev.weft.undercurrent.feature.signin

sealed interface SignInEffect {
    data object SignedIn : SignInEffect
}
