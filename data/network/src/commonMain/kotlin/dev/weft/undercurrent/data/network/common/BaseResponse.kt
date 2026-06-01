package dev.weft.undercurrent.data.network.common

import kotlinx.serialization.Serializable

/**
 * Generic JSON envelope for REST responses that wrap their payload in
 * `{ success, data, message, code }`. Services that return raw
 * unwrapped JSON should not use this — model the response directly.
 */
@Serializable
data class BaseResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String? = null,
    val code: String? = null,
)

/**
 * Server-emitted error body. The HTTP-response validator deserializes
 * the body of any non-2xx response into this when possible, then
 * throws [ApiException] carrying the parsed fields. If parsing fails,
 * the validator throws a plain [HttpException] with status + endpoint
 * only.
 *
 * `code` is a stable machine-readable string (`"invalid_request"`,
 * `"unauthenticated"`, …) — clients branch on it. `details` is an
 * optional per-field-error map used by 400 validation responses
 * (`{ "email": "must be a valid email address" }`).
 */
@Serializable
data class BaseErrorResponse(
    val code: String,
    val message: String,
    val details: Map<String, String>? = null,
)
