package dev.weft.undercurrent.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dev.weft.android.WeftRuntime
import dev.weft.compose.ComposeUiBridge
import dev.weft.compose.WeftUi
import dev.weft.contracts.KeyVault
import dev.weft.harness.agents.AgentDeclaration
import dev.weft.mcp.McpServerConfig
import dev.weft.oauth.AndroidOAuthClient
import dev.weft.oauth.OAuthCallbackChannel
import dev.weft.oauth.OAuthClient
import dev.weft.oauth.OAuthTokenStore
import dev.weft.osbridge.keyvault.AndroidKeyVault
import dev.weft.security.NetworkPolicy
import dev.weft.undercurrent.app.AppStore
import dev.weft.undercurrent.core.ASSISTANT_APP_PREAMBLE
import dev.weft.undercurrent.core.WeftAppStore
import dev.weft.undercurrent.core.navigation.NavigationChannel
import dev.weft.undercurrent.data.SqlDelightDataSource
import dev.weft.undercurrent.data.datastore.IntegrationsRepository
import dev.weft.undercurrent.data.datastore.MiniAppsRepository
import dev.weft.undercurrent.data.datastore.ModelPrefsRepository
import dev.weft.undercurrent.data.datastore.OnboardingRepository
import dev.weft.undercurrent.data.datastore.PersonaRepository
import dev.weft.undercurrent.data.datastore.ProviderPrefsRepository
import dev.weft.undercurrent.data.datastore.ThemeRepository
import dev.weft.undercurrent.data.datastore.createPreferencesDataStore
import dev.weft.undercurrent.data.sqldelight.DatabaseDriverFactory
import dev.weft.undercurrent.data.sqldelight.createUndercurrentDatabase
import dev.weft.undercurrent.data.weft.AndroidSpeechGateway
import dev.weft.undercurrent.data.weft.WeftConversationStoreGateway
import dev.weft.undercurrent.data.weft.WeftKeyValidationGateway
import dev.weft.undercurrent.data.weft.WeftKeyVaultGateway
import dev.weft.undercurrent.data.weft.WeftMemoryStoreGateway
import dev.weft.undercurrent.data.weft.WeftModelCatalog
import dev.weft.undercurrent.data.weft.WeftOAuthGateway
import dev.weft.undercurrent.data.weft.WeftTraceStoreGateway
import dev.weft.undercurrent.data.weft.WeftUiBridgeGateway
import dev.weft.undercurrent.data.weft.WeftUsageGateway
import dev.weft.undercurrent.data.weft.tools.CreateMiniAppTool
import dev.weft.undercurrent.data.weft.tools.CreatePersonaTool
import dev.weft.undercurrent.data.weft.tools.OpenConversationsTool
import dev.weft.undercurrent.data.weft.tools.OpenIntegrationsTool
import dev.weft.undercurrent.data.weft.tools.OpenMemoriesTool
import dev.weft.undercurrent.data.weft.tools.OpenPersonasTool
import dev.weft.undercurrent.data.weft.tools.OpenUsageTool
import dev.weft.undercurrent.data.weft.tools.ShowLocationOnMapTool
import dev.weft.undercurrent.db.UndercurrentDatabase
import dev.weft.undercurrent.feature.creator.CreatorKind
import dev.weft.undercurrent.feature.creator.CreatorSession
import dev.weft.undercurrent.feature.integrations.Integration
import dev.weft.undercurrent.feature.integrations.Integrations
import dev.weft.undercurrent.feature.integrations.IntegrationsStore
import dev.weft.undercurrent.feature.conversations.ConversationsStore
import dev.weft.undercurrent.feature.memories.MemoriesStore
import dev.weft.undercurrent.feature.miniapps.MiniAppsStore
import dev.weft.undercurrent.feature.personas.PersonasStore
import dev.weft.undercurrent.feature.traces.TracesStore
import dev.weft.undercurrent.feature.usage.UsageStore
import dev.weft.undercurrent.shared.gateway.ConversationStoreGateway
import dev.weft.undercurrent.shared.gateway.KeyValidationGateway
import dev.weft.undercurrent.shared.gateway.KeyVaultGateway
import dev.weft.undercurrent.shared.gateway.MemoryStoreGateway
import dev.weft.undercurrent.shared.gateway.ModelCatalog
import dev.weft.undercurrent.shared.gateway.OAuthGateway
import dev.weft.undercurrent.shared.gateway.SpeechGateway
import dev.weft.undercurrent.shared.gateway.TraceStoreGateway
import dev.weft.undercurrent.shared.gateway.UiBridgeGateway
import dev.weft.undercurrent.shared.gateway.UsageGateway
import dev.weft.undercurrent.tools.SetThemeModeTool
import dev.weft.undercurrent.tools.SetThemePaletteTool
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Root Koin module wiring every long-lived dependency.
 *
 * Layering:
 *  1. **DataStores** — one per repo, named by file. Cheap to construct.
 *  2. **Repos** — DataStore-backed; constructor takes `DataStore<Preferences>`.
 *  3. **Gateways** — KMP interfaces in `:shared`; Android impls in `:data:weft`.
 *  4. **Database** — SQLDelight via `:data:sqldelight`.
 *  5. **UI singletons** — [WeftUi], [ComposeUiBridge].
 *  6. **Runtime** — [WeftRuntime.create] depends on bridge + weftUi + persona repo.
 *  7. **ViewModels** — root [AppStore] + per-screen VMs.
 */
val appModule = module {

    // ─── DataStores ───────────────────────────────────────────────────
    single<DataStore<Preferences>>(named(ThemeRepository.FILE_NAME)) {
        createPreferencesDataStore(androidContext(), ThemeRepository.FILE_NAME)
    }
    single<DataStore<Preferences>>(named(OnboardingRepository.FILE_NAME)) {
        createPreferencesDataStore(androidContext(), OnboardingRepository.FILE_NAME)
    }
    single<DataStore<Preferences>>(named(PersonaRepository.FILE_NAME)) {
        createPreferencesDataStore(androidContext(), PersonaRepository.FILE_NAME)
    }
    single<DataStore<Preferences>>(named(ProviderPrefsRepository.FILE_NAME)) {
        createPreferencesDataStore(androidContext(), ProviderPrefsRepository.FILE_NAME)
    }
    single<DataStore<Preferences>>(named(ModelPrefsRepository.FILE_NAME)) {
        createPreferencesDataStore(androidContext(), ModelPrefsRepository.FILE_NAME)
    }
    single<DataStore<Preferences>>(named(IntegrationsRepository.FILE_NAME)) {
        createPreferencesDataStore(androidContext(), IntegrationsRepository.FILE_NAME)
    }
    single<DataStore<Preferences>>(named(MiniAppsRepository.FILE_NAME)) {
        createPreferencesDataStore(androidContext(), MiniAppsRepository.FILE_NAME)
    }

    // ─── Repos ────────────────────────────────────────────────────────
    single { ThemeRepository(get(named(ThemeRepository.FILE_NAME))) }
    single { OnboardingRepository(get(named(OnboardingRepository.FILE_NAME))) }
    single { PersonaRepository(get(named(PersonaRepository.FILE_NAME))) }
    single { ProviderPrefsRepository(get(named(ProviderPrefsRepository.FILE_NAME))) }
    single { ModelPrefsRepository(get(named(ModelPrefsRepository.FILE_NAME))) }
    single { IntegrationsRepository(get(named(IntegrationsRepository.FILE_NAME))) }
    single { MiniAppsRepository(get(named(MiniAppsRepository.FILE_NAME))) }

    // ─── Database ─────────────────────────────────────────────────────
    single { DatabaseDriverFactory(androidContext()) }
    single<UndercurrentDatabase> { createUndercurrentDatabase(get<DatabaseDriverFactory>().create()) }

    // Navigation pipe — agent tools emit Screen values; AppStore collects.
    single { NavigationChannel() }

    // Creator-session tracker — set while a guided QnA flow is active.
    single { CreatorSession() }

    // ─── UI singletons ────────────────────────────────────────────────
    single {
        val imageLoader = dev.weft.compose.components.buildWeftImageLoader(
            androidContext().applicationContext,
        )
        WeftUi(
            context = androidContext(),
            extraComponents = dev.weft.undercurrent.core.ui.components.undercurrentComponents(imageLoader),
            includeDefaults = false,
        )
    }
    single { ComposeUiBridge(componentRegistry = get<WeftUi>().componentRegistry) }

    // ─── OAuth ────────────────────────────────────────────────────────
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

    // ─── Runtime ──────────────────────────────────────────────────────
    single<WeftRuntime> {
        val personaRepo: PersonaRepository = get()
        val themeRepo: ThemeRepository = get()
        val integrationsRepo: IntegrationsRepository = get()
        val tokenStore: OAuthTokenStore = get()
        val navigation: NavigationChannel = get()
        val miniAppsRepo: MiniAppsRepository = get()
        val creatorSession: CreatorSession = get()

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

    // ─── Gateways ─────────────────────────────────────────────────────
    // KMP gateway impls. Feature screens consume the interface from
    // :shared/commonMain; here we bind the Android impls from :data:weft.
    single<KeyVaultGateway> { WeftKeyVaultGateway(get<WeftRuntime>().keyVault) }
    single<KeyValidationGateway> { WeftKeyValidationGateway() }
    single<OAuthGateway> { WeftOAuthGateway(get<OAuthClient>(), get<OAuthTokenStore>()) }
    single<ConversationStoreGateway> { WeftConversationStoreGateway(get<WeftRuntime>().conversationStore) }
    single<MemoryStoreGateway> { WeftMemoryStoreGateway(get<WeftRuntime>().memoryStore) }
    single<TraceStoreGateway> { WeftTraceStoreGateway(get<WeftRuntime>().traceStore) }
    single<UsageGateway> { WeftUsageGateway(get<WeftRuntime>().usageStore) }
    single<ModelCatalog> { WeftModelCatalog() }
    single<SpeechGateway> { AndroidSpeechGateway(androidContext()) }
    single<UiBridgeGateway> { WeftUiBridgeGateway(get<ComposeUiBridge>()) }

    // ─── ViewModels ───────────────────────────────────────────────────
    // AppStore as a single, not viewModel — the commonMain interface
    // doesn't extend androidx.lifecycle.ViewModel (it can't, iOS
    // wouldn't compile). WeftAppStore still extends ViewModel
    // internally for viewModelScope; Koin holds the singleton across
    // configuration changes, so we don't lose state on rotation.
    single<AppStore> {
        WeftAppStore(
            runtime = get(),
            themeRepo = get(),
            onboardingRepo = get(),
            providerPrefsRepo = get(),
            modelPrefsRepo = get(),
            navigationChannel = get(),
            miniAppsRepo = get(),
            creatorSession = get(),
            uiBridge = get(),
        )
    }
    viewModel { PersonasStore(repo = get()) }
    viewModel { MiniAppsStore(repo = get()) }
    viewModel { UsageStore(gateway = get()) }
    viewModel { MemoriesStore(store = get()) }
    viewModel { TracesStore(store = get()) }
    viewModel { ConversationsStore(store = get()) }
    viewModel {
        val integrationsRepo: IntegrationsRepository = get()
        IntegrationsStore(
            repo = integrationsRepo,
            oauth = get(),
            initialEnabled = runBlocking { integrationsRepo.enabledIdsNow() },
        )
    }
}

/** Compose [McpServerConfig]s for every currently-enabled integration. */
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

/** Qualifier for the OAuth-only KeyVault. */
private const val OAUTH_KEY_VAULT: String = "oauth_key_vault"

/**
 * Convert the commonMain OAuthConfig mirror back to Weft's OAuthConfig.
 * The mirror was introduced so :feature:integrations could compile
 * against iOS without dragging the substrate in; here at the runtime
 * boundary we feed Weft's type to the OAuthTokenStore.
 */
private fun dev.weft.undercurrent.shared.gateway.OAuthConfig.toWeft(): dev.weft.oauth.OAuthConfig =
    dev.weft.oauth.OAuthConfig(
        clientId = clientId,
        authorizationEndpoint = authorizationEndpoint,
        tokenEndpoint = tokenEndpoint,
        redirectUri = redirectUri,
        scopes = scopes,
        extraAuthParams = extraAuthParams,
    )

/**
 * Creator-mode preamble injected via `extraVolatilePrefix` while a
 * [CreatorSession] is active.
 */
private fun creatorPreambleFor(kind: CreatorKind): String {
    val sharedRules = """
        |[Creator mode is active — ${kind.humanLabel}.]
        |
        |You are guiding the user through a guided QnA flow on a
        |dedicated creator screen. There is NO free-form chat input —
        |the user can ONLY interact with widgets you render via the
        |`ui_render` tool.
        |
        |Behaviour:
        | 1. Ask exactly ONE focused question per turn. Render it as a
        |    `ui_render` payload using Stack + Heading + (Field /
        |    Choice / SegmentedToggle / MoodScale / Composer / Toggle)
        |    + a Button (label "Next" or "Save", onTap "next").
        | 2. Keep prompts short and concrete. Show progress with the
        |    `Steps` component when helpful.
        | 3. After each user answer (delivered to you as a synthetic
        |    [UI event] message with field values), ask the NEXT
        |    question — or finalize when you have enough.
        | 4. Do NOT call any write tool other than the finalize tool
        |    below. Do not narrate; render.
        | 5. Aim for 3-6 questions total. Quality over quantity.
    """.trimMargin()

    val specifics = when (kind) {
        CreatorKind.PersonaVoice -> """
            |
            |Cover, roughly in this order:
            | - Name (short, e.g. "Editor", "Botanist")
            | - What kind of writing should this voice produce?
            | - Tone preferences
            | - Any specific constraints
            | - One-line tagline for the picker
            |
            |When ready, call `create_persona` with:
            |  name, tagline, systemPrompt (3-6 sentences), kind: "voice".
        """.trimMargin()
        CreatorKind.PersonaRole -> """
            |
            |Cover, roughly in this order:
            | - Name (short, e.g. "Pediatrician", "Tax attorney")
            | - Domain / expertise area
            | - Constraints / disclaimers
            | - Communication style
            | - One-line tagline for the picker
            |
            |When ready, call `create_persona` with:
            |  name, tagline, systemPrompt (3-6 sentences), kind: "role".
        """.trimMargin()
        CreatorKind.MiniApp -> """
            |
            |A mini-app is a saved shortcut — name + emoji + trigger
            |prompt that re-runs a task on tap. Cover:
            | - What does the mini-app do?
            | - Name (short, e.g. "Log water", "Daily review")
            | - Emoji (single grapheme)
            | - Trigger prompt
            |
            |When ready, call `create_mini_app` with name, emoji, triggerPrompt.
        """.trimMargin()
    }
    return sharedRules + specifics
}
