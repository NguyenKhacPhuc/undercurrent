package dev.weft.undercurrent.data.network.common

/**
 * Thrown for any non-2xx response that did not parse into a
 * [BaseErrorResponse]. Carries the endpoint path + status code so the
 * UI / logger has enough context to attribute the failure.
 */
open class HttpException(
    open val endpoint: String,
    open val httpStatus: Int,
    message: String? = null,
) : Exception(message)

/**
 * Thrown for any non-2xx response that did parse into a
 * [BaseErrorResponse] — carries the server-emitted error `code` +
 * `message`. Subclasses [HttpException] so a single catch can handle
 * both shapes.
 */
data class ApiException(
    val code: Int,
    val apiMessage: String,
    override val endpoint: String = "",
    override val httpStatus: Int = 0,
) : HttpException(endpoint, httpStatus, apiMessage)

/**
 * Thrown when a request fails at the transport layer — DNS lookup,
 * connect timeout, socket reset, no internet. The error-validator in
 * [NetworkModule] maps Ktor's transport exceptions to this so callers
 * have a single exception type to render "no connection" UI on.
 */
class NetworkException : Exception(
    "Network error occurred. Please check your connection and try again.",
)
