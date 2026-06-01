package dev.weft.undercurrent.core.domain.auth

import dev.weft.undercurrent.core.domain.AuthRepository
import dev.weft.undercurrent.core.domain.SessionTokenStore
import dev.weft.undercurrent.core.ext.ioDispatcher
import dev.weft.undercurrent.data.network.PlatformHttpClientEngineFactory
import dev.weft.undercurrent.data.network.common.defaultHttpClient
import io.ktor.client.HttpClient
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
 * flow, so reusing the integrations machinery (which assumes the
 * `TokenManager` + refresh flow) would mismatch. Both clients share
 * the same defaults via [defaultHttpClient].
 */
val authRepositoryModule = module {
    single<HttpClient>(named(AUTH_HTTP_CLIENT_QUALIFIER)) {
        defaultHttpClient(get<PlatformHttpClientEngineFactory>().create())
    }
    single<AuthRepository>(named(AUTH_REPOSITORY_QUALIFIER)) {
        AuthRepositoryImpl(
            httpClient = get(named(AUTH_HTTP_CLIENT_QUALIFIER)),
            baseUrl = get(named(BE_BASE_URL_QUALIFIER)),
            sessionTokenStore = get(),
            ioDispatcher = ioDispatcher,
        )
    }
}

/** Koin qualifier for the BE base URL. */
const val BE_BASE_URL_QUALIFIER: String = "mobileAuthWiring.beBaseUrl"

/** Koin qualifier for the auth-flavored [HttpClient]. */
const val AUTH_HTTP_CLIENT_QUALIFIER: String = "mobileAuthWiring.authHttpClient"

/** Koin qualifier for the [AuthRepository] binding. */
const val AUTH_REPOSITORY_QUALIFIER: String = "mobileAuthWiring.authRepository"
