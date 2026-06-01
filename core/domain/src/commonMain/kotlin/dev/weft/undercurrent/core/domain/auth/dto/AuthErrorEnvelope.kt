package dev.weft.undercurrent.core.domain.auth.dto

import kotlinx.serialization.Serializable

/**
 * `{ "error": { "code", "message", "details": { … }? } }` envelope the
 * BE returns on any non-2xx response (per api-contract.md). The
 * [io.ktor.client.HttpClient]'s [io.ktor.client.plugins.HttpResponseValidator]
 * decodes this and rewraps it as [dev.weft.undercurrent.core.domain.AuthException.Http].
 */
@Serializable
internal data class AuthErrorEnvelope(val error: AuthErrorBody)

@Serializable
internal data class AuthErrorBody(
    val code: String,
    val message: String,
    val details: Map<String, String>? = null,
)
