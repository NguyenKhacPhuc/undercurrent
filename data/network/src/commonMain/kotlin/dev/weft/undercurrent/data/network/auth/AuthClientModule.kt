package dev.weft.undercurrent.data.network.auth

import dev.weft.undercurrent.core.domain.AuthClient
import dev.weft.undercurrent.core.domain.SessionTokenStore
import dev.weft.undercurrent.data.network.PlatformHttpClientEngineFactory
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Koin module wiring the [AuthClient] used by mobile-auth-wiring.
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
val authClientModule = module {
    single<AuthClient>(named(AUTH_CLIENT_QUALIFIER)) {
        val baseUrl = get<String>(named(BE_BASE_URL_QUALIFIER))
        val client = HttpClient(get<PlatformHttpClientEngineFactory>().create()) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        explicitNulls = false
                        isLenient = true
                    },
                )
            }
            install(HttpTimeout) {
                requestTimeoutMillis = REQUEST_TIMEOUT_MS
                connectTimeoutMillis = CONNECT_TIMEOUT_MS
            }
        }
        HttpAuthClient(httpClient = client, baseUrl = baseUrl, sessionTokenStore = get())
    }
}

/** Koin qualifier for the BE base URL. */
const val BE_BASE_URL_QUALIFIER: String = "mobileAuthWiring.beBaseUrl"

/** Koin qualifier for the [AuthClient] binding. */
const val AUTH_CLIENT_QUALIFIER: String = "mobileAuthWiring.authClient"

private const val REQUEST_TIMEOUT_MS: Long = 10_000L
private const val CONNECT_TIMEOUT_MS: Long = 10_000L
