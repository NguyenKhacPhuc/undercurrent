package dev.weft.undercurrent.core.domain.auth.dto

import kotlinx.serialization.Serializable

/**
 * Success payload of `GET /v1/me` (carried inside `BaseResponse<T>.data`).
 * Returns only the authed account — the session token is already in
 * the caller's hand, so it isn't echoed.
 */
@Serializable
data class MeResponse(val account: AccountDto)
