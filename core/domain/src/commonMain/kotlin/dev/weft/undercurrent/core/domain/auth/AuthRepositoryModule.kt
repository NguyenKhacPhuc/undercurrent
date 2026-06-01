package dev.weft.undercurrent.core.domain.auth

import dev.weft.undercurrent.core.domain.AuthException
import dev.weft.undercurrent.core.domain.AuthRepository
import dev.weft.undercurrent.core.domain.SessionTokenStore
import dev.weft.undercurrent.core.domain.auth.dto.AuthErrorEnvelope
import dev.weft.undercurrent.data.network.PlatformHttpClientEngineFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Koin module wiring the [AuthRepository] used by mobile-auth-wiring.
 *
 * Consumers must register, before loading this module:
 *  - `single<String>(named(BE_BASE_URL_QUALIFIER)) { … }` — BE base URL
 *    (e.g. `https://undercurrent-backend-production.up.railway.app`).
 *  - `single<PlatformHttpClientEngineFactory> { … }` — supplied by the
 *    platform module (already wired by `data/network`'s
 *    `androidNetworkModule` / `iosNetworkModule`).
 *  - `single<SessionTokenStore> { … }` — supplied by the platform
 *    module (`mobile-auth-wiring/02` and `/03`).
 *
 * The HTTP client built here is intentionally separate from the
 * `networkModule` integrations client — the BE has no refresh-token
 * flow and a different error envelope, so reusing the integrations
 * machinery would mismatch.
 */
val authRepositoryModule = module {
    single<HttpClient>(named(AUTH_HTTP_CLIENT_QUALIFIER)) {
        defaultAuthHttpClient(get<PlatformHttpClientEngineFactory>().create())
    }
    single<AuthRepository>(named(AUTH_REPOSITORY_QUALIFIER)) {
        AuthRepositoryImpl(
            httpClient = get(named(AUTH_HTTP_CLIENT_QUALIFIER)),
            baseUrl = get(named(BE_BASE_URL_QUALIFIER)),
            sessionTokenStore = get(),
            ioDispatcher = authIoDispatcher,
        )
    }
}

/**
 * Platform-supplied I/O dispatcher used by [AuthRepositoryImpl].
 *
 * `Dispatchers.IO` is not public from `commonMain` on Apple native
 * targets in `kotlinx-coroutines` 1.9.x, so the actual lives in the
 * platform source sets. Android → `Dispatchers.IO`. iOS → `Dispatchers.Default`
 * (Apple's pool is shared and there's no separate I/O thread pool —
 * Ktor's Darwin engine drives I/O on its own NSURLSession queue
 * regardless of the dispatcher passed to `flowOn`).
 */
internal expect val authIoDispatcher: CoroutineDispatcher

/**
 * Builds the default [HttpClient] used by [AuthRepositoryImpl].
 * Centralizes ContentNegotiation, timeouts, and the response validator
 * that translates BE error envelopes → [AuthException.Http] and
 * transport-level failures → [AuthException.Network]. Exposed as a
 * function so tests can build an equivalently-configured client off
 * a `MockEngine` without going through Koin.
 */
fun defaultAuthHttpClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
    install(ContentNegotiation) {
        json(authJson)
    }
    install(HttpTimeout) {
        requestTimeoutMillis = REQUEST_TIMEOUT_MS
        connectTimeoutMillis = CONNECT_TIMEOUT_MS
    }
    HttpResponseValidator {
        handleResponseExceptionWithRequest { cause, _ ->
            when (cause) {
                is ConnectTimeoutException,
                is SocketTimeoutException,
                is UnresolvedAddressException,
                -> throw AuthException.Network(cause)
            }
        }
        validateResponse { response ->
            if (response.status.isSuccess()) return@validateResponse
            val envelope = decodeAuthErrorEnvelope(response.bodyAsText())
            throw AuthException.Http(
                status = response.status.value,
                errorCode = envelope?.error?.code,
                errorMessage = envelope?.error?.message,
                fieldErrors = envelope?.error?.details,
            )
        }
    }
}

private fun decodeAuthErrorEnvelope(body: String): AuthErrorEnvelope? = try {
    if (body.isBlank()) null else authJson.decodeFromString(AuthErrorEnvelope.serializer(), body)
} catch (_: SerializationException) {
    null
}

private val authJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    isLenient = true
}

/** Koin qualifier for the BE base URL. */
const val BE_BASE_URL_QUALIFIER: String = "mobileAuthWiring.beBaseUrl"

/** Koin qualifier for the auth-flavored [HttpClient]. */
const val AUTH_HTTP_CLIENT_QUALIFIER: String = "mobileAuthWiring.authHttpClient"

/** Koin qualifier for the [AuthRepository] binding. */
const val AUTH_REPOSITORY_QUALIFIER: String = "mobileAuthWiring.authRepository"

private const val REQUEST_TIMEOUT_MS: Long = 10_000L
private const val CONNECT_TIMEOUT_MS: Long = 10_000L
