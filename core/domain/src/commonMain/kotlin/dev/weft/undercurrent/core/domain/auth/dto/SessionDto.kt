package dev.weft.undercurrent.core.domain.auth.dto

import kotlinx.serialization.Serializable

/** Session bearer + its server-side expiry (30 days per BE Inception D3). */
@Serializable
data class SessionDto(
    val token: String,
    val expiresAtMs: Long,
)
