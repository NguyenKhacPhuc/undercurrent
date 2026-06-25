package dev.weft.undercurrent.di

import dev.weft.android.WeftRuntime
import dev.weft.android.create
import dev.weft.compose.ComposeUiBridge
import dev.weft.compose.WeftUi
import dev.weft.compose.components.MiniAppAssistantHandler
import dev.weft.contracts.KeyVault
import dev.weft.harness.agents.AgentDeclaration
import dev.weft.mcp.McpServerConfig
import dev.weft.oauth.AndroidOAuthClient
import dev.weft.oauth.OAuthCallbackChannel
import dev.weft.oauth.OAuthClient
import dev.weft.oauth.OAuthTokenStore
import dev.weft.osbridge.keyvault.AndroidKeyVault
import dev.weft.security.NetworkPolicy
import dev.weft.security.whitelistingHttpClient
import dev.weft.undercurrent.app.AppViewModel
import dev.weft.undercurrent.core.ASSISTANT_APP_PREAMBLE
import dev.weft.undercurrent.core.WeftAppViewModel
import dev.weft.undercurrent.core.domain.IntegrationsRepository
import dev.weft.undercurrent.core.domain.MiniAppsRepository
import dev.weft.undercurrent.core.domain.PersonaRepository
import dev.weft.undercurrent.core.domain.ThemeRepository
import dev.weft.undercurrent.core.domain.auth.BE_BASE_URL_QUALIFIER
import dev.weft.undercurrent.core.domain.auth.authRepositoryModule
import dev.weft.undercurrent.core.domain.prompt.promptConfigModule
import dev.weft.undercurrent.core.domain.repositoryAndroidModule
import dev.weft.undercurrent.core.domain.repositoryModule
import dev.weft.undercurrent.core.domain.usecase.chat.chatUseCasesModule
import dev.weft.undercurrent.core.model.AppEffect
import dev.weft.undercurrent.core.model.AppState
import dev.weft.undercurrent.core.navigation.NavigationChannel
import dev.weft.undercurrent.core.navigation.navigationModule
import dev.weft.undercurrent.core.ui.components.undercurrentComponents
import dev.weft.undercurrent.data.datastore.datastoreAndroidModule
import dev.weft.undercurrent.data.network.PlatformHttpClientEngineFactory
import dev.weft.undercurrent.data.network.androidNetworkModule
import dev.weft.undercurrent.data.sqldelight.SqlDelightDataSource
import dev.weft.undercurrent.data.sqldelight.databaseAndroidModule
import dev.weft.undercurrent.data.weft.tools.CreateHtmlMiniAppTool
import dev.weft.undercurrent.data.weft.tools.CreateMiniAppTool
import dev.weft.undercurrent.data.weft.tools.CreatePersonaTool
import dev.weft.undercurrent.data.weft.tools.OpenConversationsTool
import dev.weft.undercurrent.data.weft.tools.OpenIntegrationsTool
import dev.weft.undercurrent.data.weft.tools.OpenMemoriesTool
import dev.weft.undercurrent.data.weft.tools.OpenPersonasTool
import dev.weft.undercurrent.data.weft.tools.OpenUsageTool
import dev.weft.undercurrent.data.weft.tools.ShowLocationOnMapTool
import dev.weft.undercurrent.data.weft.tools.WebSearchTool
import dev.weft.undercurrent.feature.auth.authModule
import dev.weft.undercurrent.feature.chat.ChatViewModel
import dev.weft.undercurrent.feature.chat.agent.AgentSession
import dev.weft.undercurrent.feature.chat.agent.askAssistant
import dev.weft.undercurrent.feature.chat.chatAndroidModule
import dev.weft.undercurrent.feature.chat.chatModule
import dev.weft.undercurrent.feature.conversations.conversationsModule
import dev.weft.undercurrent.feature.creator.CreatorKind
import dev.weft.undercurrent.feature.creator.creatorAndroidModule
import dev.weft.undercurrent.feature.settings.integrations.Integration
import dev.weft.undercurrent.feature.settings.integrations.Integrations
import dev.weft.undercurrent.feature.settings.integrations.IntegrationsViewModel
import dev.weft.undercurrent.feature.memories.memoriesModule
import dev.weft.undercurrent.feature.miniapps.MiniAppRepositoryStateStore
import dev.weft.undercurrent.feature.miniapps.MiniAppViewModel
import dev.weft.undercurrent.feature.miniapps.OfferableActions
import dev.weft.undercurrent.feature.miniapps.internal.WeftMiniAppViewModel
import dev.weft.undercurrent.feature.miniapps.miniAppActionInvoker
import dev.weft.undercurrent.feature.miniapps.miniAppScopeResolver
import dev.weft.undercurrent.feature.miniapps.miniAppsModule
import dev.weft.undercurrent.feature.onboarding.onboardingModule
import dev.weft.undercurrent.feature.personas.personasModule
import dev.weft.undercurrent.feature.settings.providers.providerAndroidModule
import dev.weft.undercurrent.feature.settings.settingsModule
import dev.weft.undercurrent.feature.theme.themeModule
import dev.weft.undercurrent.feature.traces.traceExportAndroidModule
import dev.weft.undercurrent.feature.traces.tracesModule
import dev.weft.undercurrent.feature.settings.usage.usageModule
import dev.weft.undercurrent.shared.mvi.MviContext
import dev.weft.undercurrent.tools.SetThemeModeTool
import dev.weft.undercurrent.tools.SetThemePaletteTool
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {

    single<String>(named(BE_BASE_URL_QUALIFIER)) { BE_BASE_URL }

    single {
        val imageLoader = coil3.ImageLoader.Builder(androidContext()).build()
        val miniAppsRepo = get<MiniAppsRepository>()
        val offerable = OfferableActions.readMostlyDefaults()
        val miniAppHttpClient = whitelistingHttpClient(
            engine = get<PlatformHttpClientEngineFactory>().create(),
            policy = NetworkPolicy.OPEN,
        )
        val miniAppStateStore = MiniAppRepositoryStateStore(miniAppsRepo)
        val agentSessionProvider = { get<AgentSession>() }
        WeftUi(
            context = androidContext(),
            extraComponents = undercurrentComponents(
                imageLoader = imageLoader,
                miniAppInvoker = miniAppActionInvoker(offerable, miniAppStateStore, miniAppHttpClient),
                miniAppScopeResolver = miniAppScopeResolver({ miniAppsRepo.miniApps.value }, offerable),
                miniAppStateStore = miniAppStateStore,
                miniAppAssistant = miniAppAssistantHandler(agentSessionProvider),
            ),
            includeDefaults = false,
        )
    }

    single { ComposeUiBridge(componentRegistry = get<WeftUi>().componentRegistry) }

    single { OAuthCallbackChannel() }
    single<KeyVault>(named(OAUTH_KEY_VAULT)) {
        AndroidKeyVault.create(androidContext(), fileName = "oauth_tokens")
    }
    single<OAuthClient> {
        AndroidOAuthClient(
            context = androidContext(),
            callbacks = get<OAuthCallbackChannel>(),
        )
    }
    single {
        OAuthTokenStore(
            keyVault = get(named(OAUTH_KEY_VAULT)),
            oauthClient = get<OAuthClient>(),
        )
    }

    single<WeftRuntime> {
        val personaRepo: PersonaRepository = get()
        val themeRepo: ThemeRepository = get()
        val integrationsRepo: IntegrationsRepository = get()
        val tokenStore: OAuthTokenStore = get()
        val navigation: NavigationChannel = get()
        val miniAppsRepo: MiniAppsRepository = get()
        val creatorSession = get<dev.weft.undercurrent.feature.creator.CreatorSession>()

        val mcpServers = mcpServersFor(integrationsRepo, tokenStore)

        WeftRuntime.create(
            context = androidContext(),
            uiBridge = get<ComposeUiBridge>(),
            appPromptPreamble = ASSISTANT_APP_PREAMBLE,
            mcpServers = mcpServers,
            dataSources = listOf(
                SqlDelightDataSource(
                    name = "notes",
                    database = get(),
                    description = "Free-form entries: water logs, mood notes, " +
                        "bookmarks, journal snippets, anything text-shaped. " +
                        "Add a `type` field on each record to discriminate categories.",
                ),
                SqlDelightDataSource(
                    name = "tasks",
                    database = get(),
                    description = "To-do items the user wants to track.",
                ),
            ),
            networkPolicy = NetworkPolicy.OPEN,
            // HTML mini-apps emit a full document as one tool-call argument;
            // 8192 truncates complex ones. Raise the budget (the runtime
            // clamps it to the active model's own ceiling, so this is safe).
            maxOutputTokens = 16_384,
            componentMetadata = get<WeftUi>().components,
            agents = listOf(
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
            ),
            extraToolsFactory = { ctx ->
                listOf(
                    SetThemePaletteTool(ctx, themeRepo),
                    SetThemeModeTool(ctx, themeRepo),
                    OpenPersonasTool(ctx, navigation),
                    OpenIntegrationsTool(ctx, navigation),
                    OpenMemoriesTool(ctx, navigation),
                    OpenConversationsTool(ctx, navigation),
                    OpenUsageTool(ctx, navigation),
                    ShowLocationOnMapTool(ctx),
                    WebSearchTool(ctx, dev.weft.undercurrent.BuildConfig.TAVILY_API_KEY),
                    CreatePersonaTool(
                        ctx = ctx,
                        personaRepo = personaRepo,
                        nav = navigation,
                        creatorSession = creatorSession,
                    ),
                    CreateMiniAppTool(
                        ctx = ctx,
                        miniAppsRepo = miniAppsRepo,
                        nav = navigation,
                        creatorSession = creatorSession,
                    ),
                    CreateHtmlMiniAppTool(
                        ctx = ctx,
                        miniAppsRepo = miniAppsRepo,
                        nav = navigation,
                    ),
                )
            },
            extraVolatilePrefix = {
                val voiceText = personaRepo.activeVoice.value.systemPromptText
                val roleText = personaRepo.activeRole.value?.systemPromptText
                val creatorKind = creatorSession.current()
                buildString {
                    if (voiceText.isNotBlank()) {
                        append("Voice instructions:\n")
                        append(voiceText)
                        append("\n")
                    }
                    if (!roleText.isNullOrBlank()) {
                        if (isNotEmpty()) append("\n")
                        append("Role instructions:\n")
                        append(roleText)
                        append("\n")
                    }
                    if (creatorKind != null) {
                        if (isNotEmpty()) append("\n")
                        append(creatorPreambleFor(creatorKind))
                        append("\n")
                    }
                }
            },
        )
    }

    single<AppViewModel> {
        WeftAppViewModel(
            runtime = get(),
            themeRepo = get(),
            onboardingRepo = get(),
            providerPrefsRepo = get(),
            navigationChannel = get(),
            navigationVm = get(),
            agentSlot = get(),
            agentFactory = get(),
            chatVm = get(),
            uiBridgeRepo = get(),
            sessionTokenStore = get(),
            promptConfig = get(),
        )
    }

    single<MviContext<AppState, AppEffect>> {
        (get<AppViewModel>() as WeftAppViewModel).mviContext
    }
    single<AgentSession> {
        (get<AppViewModel>() as WeftAppViewModel).agentSession
    }

    single<MiniAppViewModel> {
        val chatVm = get<ChatViewModel>()
        WeftMiniAppViewModel(
            context = get(),
            uiBridge = get<WeftRuntime>().uiBridge,
            miniAppsRepo = get(),
            navigationVm = get(),
            offerable = OfferableActions.readMostlyDefaults(),
            sendChat = { text -> chatVm.send(text) },
        )
    }

    viewModel {
        val integrationsRepo: IntegrationsRepository = get()
        IntegrationsViewModel(
            repo = integrationsRepo,
            oauth = get(),
            initialEnabled = runBlocking { integrationsRepo.enabledIdsNow() },
        )
    }
}


val allModules = listOf(
    appModule,
    navigationModule,
    repositoryModule,
    chatUseCasesModule,
    datastoreAndroidModule,
    databaseAndroidModule,
    androidNetworkModule,
    repositoryAndroidModule,
    authRepositoryModule,
    promptConfigModule,
    authModule,
    chatModule,
    chatAndroidModule,
    themeModule,
    onboardingModule,
    personasModule,
    miniAppsModule,
    conversationsModule,
    memoriesModule,
    tracesModule,
    traceExportAndroidModule,
    creatorAndroidModule,
    providerAndroidModule,
    usageModule,
    settingsModule,
)

private fun mcpServersFor(
    integrationsRepo: IntegrationsRepository,
    tokenStore: OAuthTokenStore,
): List<McpServerConfig> {
    val enabled: Set<String> = runBlocking { integrationsRepo.enabledIdsNow() }
    return enabled.mapNotNull { id ->
        val integration: Integration = Integrations.byId(id) ?: return@mapNotNull null
        McpServerConfig(
            name = integration.id,
            url = integration.mcpUrl,
            tokenProvider = { tokenStore.activeAccessToken(integration.id, integration.oauth.toWeft()) },
            onAuthFailed = {
                tokenStore.remove(integration.id)
                false
            },
        )
    }
}

private const val OAUTH_KEY_VAULT: String = "oauth_key_vault"

private const val BE_BASE_URL: String = "https://undercurrent-backend-production.up.railway.app"

private fun dev.weft.undercurrent.core.domain.OAuthConfig.toWeft(): dev.weft.oauth.OAuthConfig =
    dev.weft.oauth.OAuthConfig(
        clientId = clientId,
        authorizationEndpoint = authorizationEndpoint,
        tokenEndpoint = tokenEndpoint,
        redirectUri = redirectUri,
        scopes = scopes,
        extraAuthParams = extraAuthParams,
    )

private fun creatorPreambleFor(kind: CreatorKind): String = when (kind) {
    CreatorKind.PersonaVoice ->
        "You are guiding the user through creating a persona (voice). " +
            "Use the ui_render tool to ask one question at a time. " +
            "Keep it short. End with the create_persona tool when done."
    CreatorKind.PersonaRole ->
        "You are guiding the user through creating a persona (role). " +
            "Use the ui_render tool to ask one question at a time. " +
            "Keep it short. End with the create_persona tool when done."
    CreatorKind.MiniApp ->
        "You are guiding the user through creating a mini-app. " +
            "Use the ui_render tool to ask one question at a time. " +
            "Keep it short. End with the create_mini_app tool when done."
}

/**
 * The host's [MiniAppAssistantHandler] for `window.weft.sendMessage`.
 * [agentSession] is resolved lazily (per call) to avoid a DI cycle at
 * WeftUi construction. The turn logic is shared with iOS via [askAssistant].
 */
private fun miniAppAssistantHandler(
    agentSession: () -> AgentSession,
): MiniAppAssistantHandler = MiniAppAssistantHandler { _, text ->
    askAssistant(agentSession().currentAgent, text)
}
