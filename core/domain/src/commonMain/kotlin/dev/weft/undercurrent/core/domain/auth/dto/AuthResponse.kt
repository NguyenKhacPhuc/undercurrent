package dev.weft.undercurrent.core.domain.auth.dto

import kotlinx.serialization.Serializable

/**
 * Success payload of `POST /v1/auth/sign-up` and `POST /v1/auth/sign-in`
 * (carried inside `BaseResponse<T>.data`). Bundles the newly-issued
 * session with the account it authenticates.
 */
@Serializable
data class AuthResponse(
    val account: AccountDto,
    val session: SessionDto,
)
