package dev.weft.undercurrent.core.domain.auth.dto

import kotlinx.serialization.Serializable

/** Wire shape for `POST /v1/auth/sign-up`. */
@Serializable
internal data class SignUpRequest(
    val displayName: String,
    val email: String,
    val password: String,
)

/** Wire shape for `POST /v1/auth/sign-in`. */
@Serializable
internal data class SignInRequest(
    val email: String,
    val password: String,
)
