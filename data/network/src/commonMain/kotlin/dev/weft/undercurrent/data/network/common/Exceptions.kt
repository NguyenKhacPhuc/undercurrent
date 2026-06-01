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
    val code: String,
    val apiMessage: String,
    val details: Map<String, String>? = null,
    override val endpoint: String = "",
    override val httpStatus: Int = 0,
) : HttpException(endpoint, httpStatus, apiMessage)

/**
 * Thrown when a request fails at the transport layer — DNS lookup,
 * connect timeout, socket reset, no internet. The error-validator in
 * [NetworkModule] maps Ktor's transport exceptions to this so callers
 * have a single exception type to render "no connection" UI on.
 *
 * [cause] retains the underlying Ktor / OkHttp / NSURLSession exception
 * for logging; UI code branches on `is NetworkException` and shows the
 * same "no connection" copy regardless.
 */
class NetworkException(cause: Throwable? = null) : Exception(
    "Network error occurred. Please check your connection and try again.",
    cause,
)
