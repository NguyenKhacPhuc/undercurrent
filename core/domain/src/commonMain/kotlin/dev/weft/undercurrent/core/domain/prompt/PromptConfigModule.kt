package dev.weft.undercurrent.core.domain.prompt

import dev.weft.undercurrent.core.domain.auth.AUTH_HTTP_CLIENT_QUALIFIER
import dev.weft.undercurrent.core.domain.auth.BE_BASE_URL_QUALIFIER
import dev.weft.undercurrent.core.ext.ioDispatcher
import io.ktor.client.HttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Koin module wiring the [PromptConfigRepository] + its DataStore cache.
 *
 * Reuses the auth flavour's HTTP client + base URL (same BE, same
 * bearer-only defaults), so [authRepositoryModule]
 * (`dev.weft.undercurrent.core.domain.auth`) must be loaded first. The
 * named `DataStore<Preferences>` for [DataStorePromptConfigCache.FILE_NAME]
 * is supplied by the platform datastore modules.
 */
val promptConfigModule = module {
    single<PromptConfigCache> {
        DataStorePromptConfigCache(get(named(DataStorePromptConfigCache.FILE_NAME)))
    }
    single<PromptConfigRepository> {
        PromptConfigRepositoryImpl(
            httpClient = get<HttpClient>(named(AUTH_HTTP_CLIENT_QUALIFIER)),
            baseUrl = get(named(BE_BASE_URL_QUALIFIER)),
            sessionTokenStore = get(),
            cache = get(),
            ioDispatcher = ioDispatcher,
        )
    }
}
