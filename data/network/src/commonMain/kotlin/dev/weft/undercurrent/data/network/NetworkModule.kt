package dev.weft.undercurrent.data.network

import dev.weft.undercurrent.data.network.common.ApiException
import dev.weft.undercurrent.data.network.common.BaseErrorResponse
import dev.weft.undercurrent.data.network.common.HttpException
import dev.weft.undercurrent.data.network.common.NetworkException
import dev.weft.undercurrent.data.network.interceptor.RefreshTokenCall
import dev.weft.undercurrent.data.network.interceptor.TokenManager
import dev.weft.undercurrent.data.network.interceptor.addTokenInterceptor
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodedPath
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Koin module that builds a configured [HttpClient] for REST traffic.
 *
 * Consumers must register, before loading this module:
 *
 *   - `single<String>(named(BASE_URL_QUALIFIER)) { … }` — base URL.
 *   - `single<PlatformHttpClientEngineFactory> { … }` — supplied by
 *     [androidNetworkModule] / [iosNetworkModule].
 *   - `single<TokenStore> { … }` — backing store for [UserToken].
 *   - `single<AppRestarter> { … }` — supplied by the platform module.
 *   - `single<RefreshTokenCall> { … }` — feature-specific endpoint.
 *
 * Optional:
 *   - `single<Set<String>>(named(SKIP_AUTH_PATHS_QUALIFIER)) { … }` —
 *     paths to bypass the token interceptor for. Defaults to empty.
 */
val networkModule = module {

    singleOf(::TokenManager)

    single<HttpClient> {
        val tokenManager = get<TokenManager>()
        val refreshTokenCall = get<RefreshTokenCall>()
        val skipAuthPaths = getOrNull<Set<String>>(named(SKIP_AUTH_PATHS_QUALIFIER)).orEmpty()
        val baseUrl = get<String>(named(BASE_URL_QUALIFIER))

        HttpClient(get<PlatformHttpClientEngineFactory>().create()) {
            install(Resources)
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Napier.d(message, null, "HTTP Client")
                    }
                }
                level = LogLevel.NONE
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        explicitNulls = false
                        isLenient = true
                    },
                )
            }
            install(HttpRequestRetry) {
                // Network-level retries only — server 5xx (except 503,
                // which we treat as "maintenance mode" and surface
                // unchanged) retry with exponential backoff.
                maxRetries = 3
                retryIf { _, response ->
                    val code = response.status.value
                    code >= 500 && code != HttpStatusCode.ServiceUnavailable.value
                }
                retryOnExceptionIf { _, cause ->
                    // DNS failure → real "no network" — don't retry,
                    // surface as NetworkException via the validator.
                    cause !is UnresolvedAddressException
                }
                exponentialDelay()
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 30_000
            }
            HttpResponseValidator {
                handleResponseExceptionWithRequest { cause, _ ->
                    Napier.d("Http exception", cause, "HTTP Client")
                    when (cause) {
                        is ConnectTimeoutException,
                        is UnresolvedAddressException,
                        is SocketTimeoutException,
                        -> throw NetworkException()
                    }
                }
                validateResponse { response ->
                    if (response.status.isSuccess()) return@validateResponse

                    val statusCode = response.status.value
                    val endpoint = response.request.url.encodedPath
                    val errorBody = runCatching { response.bodyAsText() }.getOrNull()
                    val errorResponse = decodeBaseErrorResponse(errorBody)

                    when (statusCode) {
                        HttpStatusCode.Forbidden.value -> tokenManager.clearSession()
                        else -> {
                            if (errorResponse != null) {
                                throw ApiException(
                                    code = errorResponse.code,
                                    apiMessage = errorResponse.message,
                                    endpoint = endpoint,
                                    httpStatus = statusCode,
                                )
                            } else {
                                throw HttpException(endpoint, statusCode)
                            }
                        }
                    }
                }
            }
            defaultRequest {
                url(baseUrl)
            }
        }.apply {
            addTokenInterceptor(
                tokenManager = tokenManager,
                refreshTokenCall = refreshTokenCall,
                skipAuthForPaths = skipAuthPaths,
            )
        }
    }
}

/**
 * Koin qualifier for the base URL string. Consumers register:
 * `single<String>(named(BASE_URL_QUALIFIER)) { "https://api.example.com" }`.
 */
const val BASE_URL_QUALIFIER: String = "network.baseUrl"

/**
 * Koin qualifier for the optional `Set<String>` of `encodedPath`s the
 * token interceptor should skip — typically the refresh-token endpoint
 * plus any public endpoints (login, register, terms, …).
 */
const val SKIP_AUTH_PATHS_QUALIFIER: String = "network.skipAuthPaths"

private val errorResponseJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    isLenient = true
}

private fun decodeBaseErrorResponse(body: String?): BaseErrorResponse? {
    if (body.isNullOrBlank()) return null
    return runCatching {
        errorResponseJson.decodeFromString(BaseErrorResponse.serializer(), body)
    }.getOrNull()
}
