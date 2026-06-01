package dev.weft.undercurrent.core.domain.auth.dto

import kotlinx.serialization.Serializable

/** The BE's representation of an account — id + profile basics. */
@Serializable
data class AccountDto(
    val id: String,
    val displayName: String,
    val email: String,
    val createdAtMs: Long,
)
