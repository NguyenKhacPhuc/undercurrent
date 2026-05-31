package dev.weft.undercurrent.app

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory
import dev.weft.compose.components.WeftComponentRegistry
import dev.weft.undercurrent.core.domain.repositoryModule
import dev.weft.undercurrent.core.domain.usecase.chat.chatUseCasesModule
import dev.weft.undercurrent.core.navigation.navigationModule
import dev.weft.undercurrent.core.ui.components.undercurrentComponents
import dev.weft.undercurrent.data.datastore.datastoreIosModule
import dev.weft.undercurrent.data.sqldelight.databaseIosModule
import dev.weft.undercurrent.feature.chat.chatModule
import dev.weft.undercurrent.feature.conversations.conversationsModule
import dev.weft.undercurrent.feature.creator.CreatorViewModel
import dev.weft.undercurrent.feature.memories.memoriesModule
import dev.weft.undercurrent.feature.miniapps.MiniAppViewModel
import dev.weft.undercurrent.feature.miniapps.miniAppsModule
import dev.weft.undercurrent.feature.onboarding.onboardingModule
import dev.weft.undercurrent.feature.personas.personasModule
import dev.weft.undercurrent.feature.providers.ProviderViewModel
import dev.weft.undercurrent.feature.theme.themeModule
import dev.weft.undercurrent.feature.traces.TraceExportViewModel
import dev.weft.undercurrent.feature.traces.tracesModule
import dev.weft.undercurrent.feature.usage.usageModule
import dev.weft.undercurrent.core.domain.ChatRepository
import dev.weft.undercurrent.core.domain.ConversationStoreRepository
import dev.weft.undercurrent.core.domain.IosSpeechRepository
import dev.weft.undercurrent.core.domain.KeyValidationRepository
import dev.weft.undercurrent.core.domain.KeyVaultRepository
import dev.weft.undercurrent.core.domain.KeychainKeyVaultRepository
import dev.weft.undercurrent.core.domain.MemoryStoreRepository
import dev.weft.undercurrent.core.domain.ModelCatalogRepository
import dev.weft.undercurrent.core.domain.OAuthRepository
import dev.weft.undercurrent.core.domain.SpeechRepository
import dev.weft.undercurrent.core.domain.StubChatRepository
import dev.weft.undercurrent.core.domain.StubKeyValidationRepository
import dev.weft.undercurrent.core.domain.StubMemoryStoreRepository
import dev.weft.undercurrent.core.domain.StubModelCatalogRepository
import dev.weft.undercurrent.core.domain.StubOAuthRepository
import dev.weft.undercurrent.core.domain.StubTraceStoreRepository
import dev.weft.undercurrent.core.domain.StubUiBridgeRepository
import dev.weft.undercurrent.core.domain.StubUsageRepository
import dev.weft.undercurrent.core.domain.TraceStoreRepository
import dev.weft.undercurrent.core.domain.UiBridgeRepository
import dev.weft.undercurrent.core.domain.UsageRepository
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val iosAppModule = module {

    single<ImageLoader> {
        ImageLoader.Builder(PlatformContext.INSTANCE)
            .components { add(KtorNetworkFetcherFactory()) }
            .build()
    }
    single<WeftComponentRegistry> {
        WeftComponentRegistry(undercurrentComponents(get()))
    }

    single<KeyVaultRepository> { KeychainKeyVaultRepository() }
    single<KeyValidationRepository> { StubKeyValidationRepository() }
    single<OAuthRepository> { StubOAuthRepository() }
    single<ConversationStoreRepository> { IosConversationStoreRepository(get()) }
    single<MemoryStoreRepository> { StubMemoryStoreRepository() }
    single<TraceStoreRepository> { StubTraceStoreRepository() }
    single<UsageRepository> { StubUsageRepository() }
    single<ModelCatalogRepository> { StubModelCatalogRepository() }
    single<SpeechRepository> { IosSpeechRepository() }
    single<UiBridgeRepository> { StubUiBridgeRepository() }
    single<ChatRepository> { StubChatRepository() }

    single<AppViewModel> {
        IosAppViewModel(
            keyVault = get(),
            onboardingRepo = get(),
            themeRepo = get(),
            providerPrefsRepo = get(),
            navigationVm = get(),
            chatVm = get(),
        )
    }

    single<ProviderViewModel> { IosProviderViewModel(get<AppViewModel>() as IosAppViewModel) }
    single<MiniAppViewModel> { IosMiniAppViewModel(get<AppViewModel>() as IosAppViewModel) }
    single<CreatorViewModel> { NoOpCreatorViewModel() }
    single<TraceExportViewModel> { NoOpTraceExportViewModel() }

    viewModel {
        dev.weft.undercurrent.feature.integrations.IntegrationsViewModel(
            repo = get(),
            oauth = get(),
            initialEnabled = emptySet(),
        )
    }
}

val iosAllModules = listOf(
    iosAppModule,
    navigationModule,
    repositoryModule,
    chatUseCasesModule,
    datastoreIosModule,
    databaseIosModule,
    chatModule,
    themeModule,
    onboardingModule,
    personasModule,
    miniAppsModule,
    conversationsModule,
    memoriesModule,
    tracesModule,
    usageModule,
)
