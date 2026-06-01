package dev.weft.undercurrent.data.network.common

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.encodedPath
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Builds a default [HttpClient] configured with the project's standard
 * REST conventions, intended for any feature that talks to a BE
 * endpoint following the [BaseResponse] / [BaseErrorResponse] envelope.
 *
 * Out of the box:
 *  - **JSON content negotiation** — lenient, ignores unknown keys.
 *  - **Timeouts** — defaults to 10s request + 10s connect; override via
 *    [requestTimeoutMs] / [connectTimeoutMs].
 *  - **Error translation** via [HttpResponseValidator]:
 *    - Transport failure (connect/socket timeout, DNS unresolved) →
 *      [NetworkException] (cause preserved).
 *    - Non-2xx response with a parseable [BaseErrorResponse] body →
 *      [ApiException] carrying `code` / `message` / `details` /
 *      `endpoint` / `httpStatus`.
 *    - Non-2xx response with an unparseable body → bare [HttpException].
 *
 * Per-feature extras (logging, retry, defaultRequest baseUrl, bearer
 * interceptors, …) go in the [configure] block — composed AFTER the
 * defaults so features can install additional plugins without losing
 * the standard error translation.
 *
 * Example:
 * ```kotlin
 * val client = defaultHttpClient(engine, requestTimeoutMs = 30_000) {
 *     install(Logging) { level = LogLevel.HEADERS }
 *     defaultRequest { url(baseUrl) }
 * }
 * ```
 */
fun defaultHttpClient(
    engine: HttpClientEngine,
    requestTimeoutMs: Long = DEFAULT_REQUEST_TIMEOUT_MS,
    connectTimeoutMs: Long = DEFAULT_CONNECT_TIMEOUT_MS,
    configure: HttpClientConfig<*>.() -> Unit = {},
): HttpClient = HttpClient(engine) {
    install(ContentNegotiation) {
        json(defaultJson)
    }
    install(HttpTimeout) {
        requestTimeoutMillis = requestTimeoutMs
        connectTimeoutMillis = connectTimeoutMs
    }
    HttpResponseValidator {
        handleResponseExceptionWithRequest { cause, _ ->
            when (cause) {
                is ConnectTimeoutException,
                is SocketTimeoutException,
                is UnresolvedAddressException,
                -> throw NetworkException(cause)
            }
        }
        validateResponse { response ->
            if (response.status.isSuccess()) return@validateResponse
            val endpoint = response.request.url.encodedPath
            val statusCode = response.status.value
            val envelope = decodeBaseErrorResponse(response.bodyAsText())
            if (envelope != null) {
                throw ApiException(
                    code = envelope.code,
                    apiMessage = envelope.message,
                    details = envelope.details,
                    endpoint = endpoint,
                    httpStatus = statusCode,
                )
            } else {
                throw HttpException(endpoint, statusCode)
            }
        }
    }
    configure()
}

private fun decodeBaseErrorResponse(body: String): BaseErrorResponse? = try {
    if (body.isBlank()) null else defaultJson.decodeFromString(BaseErrorResponse.serializer(), body)
} catch (_: SerializationException) {
    null
}

private val defaultJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    isLenient = true
}

private const val DEFAULT_REQUEST_TIMEOUT_MS: Long = 10_000L
private const val DEFAULT_CONNECT_TIMEOUT_MS: Long = 10_000L
