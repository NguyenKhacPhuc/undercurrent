package dev.weft.undercurrent.core.domain

import dev.weft.android.WeftRuntime
import dev.weft.compose.ComposeUiBridge
import dev.weft.oauth.OAuthClient
import dev.weft.oauth.OAuthTokenStore
import dev.weft.undercurrent.core.domain.ConversationStoreRepository
import dev.weft.undercurrent.core.domain.KeyValidationRepository
import dev.weft.undercurrent.core.domain.KeyVaultRepository
import dev.weft.undercurrent.core.domain.MemoryStoreRepository
import dev.weft.undercurrent.core.domain.ModelCatalogRepository
import dev.weft.undercurrent.core.domain.OAuthRepository
import dev.weft.undercurrent.core.domain.SpeechRepository
import dev.weft.undercurrent.core.domain.TraceStoreRepository
import dev.weft.undercurrent.core.domain.UiBridgeRepository
import dev.weft.undercurrent.core.domain.UsageRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val repositoryAndroidModule = module {
    single<KeyVaultRepository> { WeftKeyVaultRepository(get<WeftRuntime>().keyVault) }
    single<KeyValidationRepository> { WeftKeyValidationRepository() }
    single<OAuthRepository> { WeftOAuthRepository(get<OAuthClient>(), get<OAuthTokenStore>()) }
    single<ConversationStoreRepository> {
        WeftConversationStoreRepository(get<WeftRuntime>().conversationStore)
    }
    single<MemoryStoreRepository> { WeftMemoryStoreRepository(get<WeftRuntime>().memoryStore) }
    single<TraceStoreRepository> { WeftTraceStoreRepository(get<WeftRuntime>().traceStore) }
    single<UsageRepository> { WeftUsageRepository(get<WeftRuntime>().usageStore) }
    single<ModelCatalogRepository> { WeftModelCatalogRepository() }
    single<SpeechRepository> { AndroidSpeechRepository(androidContext()) }
    single<UiBridgeRepository> { WeftUiBridgeRepository(get<ComposeUiBridge>()) }
}
