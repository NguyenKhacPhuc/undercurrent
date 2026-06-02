package dev.weft.undercurrent.app

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory
import dev.weft.android.WeftRuntime
import dev.weft.android.create
import dev.weft.android.persistence.WeftPlatform
import dev.weft.compose.ComposeUiBridge
import dev.weft.compose.components.WeftComponentRegistry
import dev.weft.harness.agents.AgentDeclaration
import dev.weft.harness.prompt.WeftSystemPromptDefaults
import dev.weft.security.NetworkPolicy
import dev.weft.undercurrent.core.domain.auth.BE_BASE_URL_QUALIFIER
import dev.weft.undercurrent.core.domain.auth.authRepositoryModule
import dev.weft.undercurrent.core.domain.repositoryModule
import dev.weft.undercurrent.core.domain.usecase.chat.chatUseCasesModule
import dev.weft.undercurrent.core.navigation.navigationModule
import dev.weft.undercurrent.core.ui.components.undercurrentComponents
import dev.weft.undercurrent.data.datastore.datastoreIosModule
import dev.weft.undercurrent.data.network.iosNetworkModule
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
import dev.weft.undercurrent.feature.auth.authModule
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
import dev.weft.undercurrent.core.domain.KeychainSessionTokenStore
import dev.weft.undercurrent.core.domain.MemoryStoreRepository
import dev.weft.undercurrent.core.domain.SessionTokenStore
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
import org.koin.core.qualifier.named
import org.koin.dsl.module

val iosAppModule = module {

    // BE base URL consumed by authRepositoryModule. Hardcoded for v1
    // per BE Inception D5; swap to a build-time constant when we add
    // a staging environment.
    single<String>(named(BE_BASE_URL_QUALIFIER)) { BE_BASE_URL }

    single<ImageLoader> {
        ImageLoader.Builder(PlatformContext.INSTANCE)
            .components { add(KtorNetworkFetcherFactory()) }
            .build()
    }
    single<WeftComponentRegistry> {
        WeftComponentRegistry(undercurrentComponents(get()))
    }

    single<ComposeUiBridge> { ComposeUiBridge(componentRegistry = get<WeftComponentRegistry>()) }

    // The real Weft runtime, stood up via the turnkey iOS factory
    // (defaults `os` to IosOsCapabilities). Foundation for the iOS agent
    // bring-up; consumers (chat, history, usage) are wired in follow-up
    // stories. mcpServers / dataSources / componentMetadata and the full
    // tuned app preamble are deferred — see ios-agent-bringup 06 / 08.
    single<WeftRuntime> {
        WeftRuntime.create(
            platform = WeftPlatform(),
            uiBridge = get<ComposeUiBridge>(),
            appPromptPreamble = IOS_APP_PREAMBLE,
            networkPolicy = NetworkPolicy.OPEN,
            agents = iosAgentDeclarations(),
        )
    }

    single<KeyVaultRepository> { KeychainKeyVaultRepository() }
    single<SessionTokenStore> { KeychainSessionTokenStore() }
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
            sessionTokenStore = get(),
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
    iosNetworkModule,
    authRepositoryModule,
    authModule,
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

/**
 * Minimal iOS app preamble. Mirrors the *shape* of the Android
 * `ASSISTANT_APP_PREAMBLE` (app intro + [WeftSystemPromptDefaults.STANDARD])
 * but with a short intro for now — unifying with the full tuned Android
 * preamble is deferred to a follow-up (the Android `AppPreamble.kt` is
 * mid-refactor; see ios-agent-bringup [[open-questions]] Q1).
 */
private val IOS_APP_PREAMBLE: String = IOS_APP_INTRO + WeftSystemPromptDefaults.STANDARD

private const val IOS_APP_INTRO: String = """
You are Undercurrent's assistant — a capable, general-purpose AI running
on the user's own iPhone. You call device tools, remember durable facts
across conversations, and search the live web. Your training has a
knowledge cutoff, so for the current date, time, locale, and device
state, read system_user_context rather than assuming.
"""

/**
 * The addressable agents, mirroring the Android registration (default +
 * writer). Sharing these declarations across platforms is a follow-up.
 */
private fun iosAgentDeclarations(): List<AgentDeclaration> = listOf(
    AgentDeclaration(
        name = AgentDeclaration.DEFAULT_AGENT_NAME,
        displayName = "Assistant",
        description = "Default agent. Handles every turn unless the user " +
            "addresses another agent via the selector.",
    ),
    AgentDeclaration(
        name = "writer",
        displayName = "Writer",
        description = "Long-form prose: drafts, edits, rewrites. Same tools as " +
            "the default agent; system fragment biases toward clarity, voice, " +
            "and structural revision.",
        systemFragment = """
            You are now in writing mode. Bias toward long-form prose:
            drafts, edits, rewrites. When the user asks for a draft,
            produce a complete piece — don't stop at an outline unless
            they asked for one. When asked to edit, return the revised
            text inline; reserve commentary for a brief "Changes" section
            at the end. Voice and structure matter: prefer concrete nouns
            over abstract ones, keep paragraphs purposeful, vary sentence
            rhythm. Tools are still available, but reach for them only
            when factual lookup is required — most writing turns are
            pure-text round-trips.
        """.trimIndent(),
    ),
)

/**
 * Production BE — pinned 2026-05-31 after the BE Story 01 deploy
 * (`inception/260531-1733-backend-bootstrap-auth/api-contract.md#Conventions`).
 * Swap to a build-time constant when we add a staging environment.
 */
private const val BE_BASE_URL: String = "https://undercurrent-backend-production.up.railway.app"
