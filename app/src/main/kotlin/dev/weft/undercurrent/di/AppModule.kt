package dev.weft.undercurrent.di

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
import dev.weft.undercurrent.core.ASSISTANT_APP_PREAMBLE
import dev.weft.undercurrent.core.AppStore
import dev.weft.undercurrent.data.AppDatabaseFactory
import dev.weft.undercurrent.data.SqlDelightDataSource
import dev.weft.undercurrent.db.AppDatabase
import dev.weft.undercurrent.features.integrations.Integration
import dev.weft.undercurrent.features.integrations.Integrations
import dev.weft.undercurrent.features.integrations.IntegrationsRepository
import dev.weft.undercurrent.features.integrations.IntegrationsViewModel
import dev.weft.undercurrent.features.maps.ShowLocationOnMapTool
import dev.weft.undercurrent.features.navigation.NavigationChannel
import dev.weft.undercurrent.features.navigation.OpenConversationsTool
import dev.weft.undercurrent.features.navigation.OpenIntegrationsTool
import dev.weft.undercurrent.features.navigation.OpenMemoriesTool
import dev.weft.undercurrent.features.navigation.OpenPersonasTool
import dev.weft.undercurrent.features.navigation.OpenUsageTool
import dev.weft.undercurrent.features.onboarding.OnboardingRepository
import dev.weft.undercurrent.features.personas.PersonaRepository
import dev.weft.undercurrent.features.savedfeatures.SavedFeaturesRepository
import dev.weft.undercurrent.features.savedfeatures.SavedFeaturesViewModel
import dev.weft.undercurrent.features.providers.ModelPrefsRepository
import dev.weft.undercurrent.features.providers.ProviderPrefsRepository
import dev.weft.undercurrent.features.theme.SetThemeModeTool
import dev.weft.undercurrent.features.theme.SetThemePaletteTool
import dev.weft.undercurrent.theme.ThemeRepository
import dev.weft.undercurrent.features.conversations.ConversationsViewModel
import dev.weft.undercurrent.features.memories.MemoriesViewModel
import dev.weft.undercurrent.features.personas.PersonasViewModel
import dev.weft.undercurrent.features.traces.TracesViewModel
import dev.weft.undercurrent.features.usage.UsageViewModel
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Koin module wiring every long-lived dependency in the app.
 *
 * Layering (bottom → top in dependency direction):
 *  1. **Repos** — DataStore-backed, no upstream deps. Pure Android-context
 *     consumers. Cheap to construct.
 *  2. **UI singletons** — [WeftUi] (Compose-side palette + component
 *     registry) and [ComposeUiBridge] (Compose state holder). The bridge
 *     holds Compose `mutableStateOf` fields — they survive process-internal
 *     Activity recreation because Koin keeps the singleton alive.
 *  3. **Runtime** — [WeftRuntime.create] depends on bridge + weftUi +
 *     personaRepo (the `extraVolatilePrefix` lambda reads persona text per
 *     turn). Koin resolves the DAG lazily on first `get()`.
 *  4. **ViewModel** — [AppStore]. Constructor injection via Koin's
 *     `viewModel { }` DSL; `koinViewModel<AppStore>()` in @Composable
 *     handles scoping to the calling ViewModelStoreOwner.
 */
val appModule = module {
    // ─── Repos ─────────────────────────────────────────────────────────
    single { ThemeRepository(androidContext()) }
    single { OnboardingRepository(androidContext()) }
    single { PersonaRepository(androidContext()) }
    single { ProviderPrefsRepository(androidContext()) }
    single { ModelPrefsRepository(androidContext()) }
    single { IntegrationsRepository(androidContext()) }
    single { SavedFeaturesRepository(androidContext()) }
    // SQLDelight-backed records database. Shared across every
    // SqlDelightDataSource instance (one per source name).
    single<AppDatabase> { AppDatabaseFactory.create(androidContext()) }
    // Process-scoped pipe for agent-initiated navigation. AppStore
    // collects from it; navigation tools emit into it. One singleton
    // is enough because all tools share the same destination space.
    single { NavigationChannel() }

    // ─── UI singletons ─────────────────────────────────────────────────
    single { WeftUi(androidContext()) }
    single { ComposeUiBridge(componentRegistry = get<WeftUi>().componentRegistry) }

    // ─── OAuth ──────────────────────────────────────────────────────────
    // Standalone (not derived from WeftRuntime.keyVault) so the
    // dependency graph stays acyclic: the runtime's MCP token-provider
    // lambda reads from OAuthTokenStore, so OAuthTokenStore must exist
    // before the runtime is built. Tokens live in their own encrypted
    // file (`oauth_tokens.xml`) — separate from the runtime's main
    // KeyVault file so an OAuth corruption can't tank app credentials.
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

    // ─── Runtime ───────────────────────────────────────────────────────
    // Heavy construction (database, OS capabilities, network client, all
    // the tool registries). Single { } caches the result for the process
    // lifetime — matches the previous Application-scoped lateinit.
    single<WeftRuntime> {
        val personaRepo: PersonaRepository = get()
        val themeRepo: ThemeRepository = get()
        val integrationsRepo: IntegrationsRepository = get()
        val tokenStore: OAuthTokenStore = get()
        val navigation: NavigationChannel = get()

        // Materialize MCP server configs for every currently-enabled
        // integration. Each server's tokenProvider closes over the
        // OAuthTokenStore so the substrate fetches a live (refreshed if
        // needed) access token per request. onAuthFailed wipes the
        // stored bundle, which lets the agent surface a "reconnect"
        // hint on the next 401.
        val mcpServers = mcpServersFor(integrationsRepo, tokenStore)

        // Sync factory — MCP discovery runs in the background on
        // Dispatchers.IO inside the runtime. The first buildAgent call
        // (in viewModelScope, from AppStore) awaits discovery before
        // returning, so the user sees the same "first turn might take
        // a moment" UX but the DI graph isn't blocked at app startup.
        // Empty integration set = ~instant. One connected integration
        // adds ~500-1500ms to first-turn latency instead of cold start.
        WeftRuntime.create(
            context = androidContext(),
            uiBridge = get<ComposeUiBridge>(),
            appPromptPreamble = ASSISTANT_APP_PREAMBLE,
            mcpServers = mcpServers,
            dataSources = listOf(
                // SQLDelight-backed — survives app restarts. Both
                // sources share the same `records` table; the
                // `source` column separates them. Descriptions
                // here are auto-rendered into the system prompt
                // by the SDK's `assembleSystemPrompt`, so we no
                // longer need to hand-document the data layer in
                // AppPreamble. Update these strings (not the
                // preamble) when adding new collection categories.
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
            // Multi-agent registry. "default" is auto-synthesized if we
            // omitted it; declaring it explicitly here makes the
            // registered set self-documenting. "writer" is a worked
            // example of a specialized agent — system fragment focuses
            // the model on long-form prose without changing tools.
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
            // App-defined agent tools layered on top of the substrate's
            // built-in catalog. Closures over the Koin-resolved
            // ThemeRepository so the LLM can change palette / mode via
            // chat ("make it darker", "try newsprint") and the UI picks
            // the new values up through the same DataStore flow that
            // backs Settings.
            extraToolsFactory = { ctx ->
                listOf(
                    // Theme — direct mutation. Parameter space is small
                    // and well-typed, no user choice required.
                    SetThemePaletteTool(ctx, themeRepo),
                    SetThemeModeTool(ctx, themeRepo),
                    // Navigation — when the user needs to make a choice
                    // (which persona, which integration), the right move
                    // is to put them in front of the picker rather than
                    // guess. Each tool emits into NavigationChannel;
                    // AppStore collects + dispatches Navigate.
                    OpenPersonasTool(ctx, navigation),
                    OpenIntegrationsTool(ctx, navigation),
                    OpenMemoriesTool(ctx, navigation),
                    OpenConversationsTool(ctx, navigation),
                    OpenUsageTool(ctx, navigation),
                    // Maps — fills the gap left by the substrate's
                    // `maps_open_directions` tool (which requires a
                    // destination). "Show me my location on a map" /
                    // "where is that on a map" route through this.
                    ShowLocationOnMapTool(ctx),
                )
            },
            // Per-turn persona injection. The picker has two independent
            // slots — voice (always set; Default = empty prompt) and
            // role (optional). We compose them as separate labeled
            // sections so the model can tell which axis each block is
            // shaping. Empty when both are no-op.
            extraVolatilePrefix = {
                val voiceText = personaRepo.activeVoice.value.systemPromptText
                val roleText = personaRepo.activeRole.value?.systemPromptText
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
                }
            },
        )
    }

    // ─── ViewModels ────────────────────────────────────────────────────
    // Root MVI store — global app state (navigation, agent, chat).
    viewModel {
        AppStore(
            runtime = get(),
            themeRepo = get(),
            onboardingRepo = get(),
            providerPrefsRepo = get(),
            modelPrefsRepo = get(),
            navigationChannel = get(),
            savedFeaturesRepo = get(),
        )
    }
    // Per-screen VMs — own the dependency for one surface so the screen
    // resolves via `koinViewModel()` instead of receiving stores via
    // MainActivity prop-drilling. Scoped to whichever ViewModelStoreOwner
    // the screen composes under (in practice: MainActivity), so they
    // share lifetime with the activity.
    viewModel { PersonasViewModel(repo = get()) }
    viewModel { SavedFeaturesViewModel(repo = get()) }
    viewModel { UsageViewModel(runtime = get()) }
    viewModel { MemoriesViewModel(runtime = get()) }
    viewModel { TracesViewModel(runtime = get()) }
    viewModel { ConversationsViewModel(runtime = get()) }
    viewModel {
        // initialEnabled is captured at VM construction — it represents
        // the set of integrations the *currently running* WeftRuntime
        // was built with. Comparing this against the live enabled set
        // (which changes immediately on Connect/Disconnect) is how the
        // restart banner decides whether to show. We resolve the
        // WeftRuntime *first* via `get()` here so the captured snapshot
        // matches what the runtime saw at build time.
        val runtime: WeftRuntime = get()
        val integrationsRepo: IntegrationsRepository = get()
        IntegrationsViewModel(
            repo = integrationsRepo,
            oauthClient = get(),
            tokenStore = get(),
            // runBlocking is safe here — DataStore's first read is fast
            // and the VM is created inside MainActivity's ViewModelStore
            // setup. The runtime resolution above already paid the
            // first-read cost.
            initialEnabled = runBlocking { integrationsRepo.enabledIdsNow() },
        ).also {
            // Touch the runtime so unused warnings don't flag it. The
            // dependency is intentional even though we don't read it:
            // we want the VM to be constructed *after* the runtime so
            // initialEnabled and the runtime's MCP list are in sync.
            @Suppress("UNUSED_EXPRESSION") runtime
        }
    }
}

/**
 * Compose [McpServerConfig]s for every currently-enabled integration.
 * Reads the enabled set synchronously (Koin's `single { }` factory is
 * synchronous; the DataStore first-read is cheap). Each server's
 * `tokenProvider` closes over [tokenStore] so the substrate fetches a
 * live, refresh-if-needed access token per MCP request.
 */
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
            tokenProvider = { tokenStore.activeAccessToken(integration.id, integration.oauth) },
            onAuthFailed = {
                // 401 from the MCP server → drop the persisted token so
                // the next call returns null instead of looping on the
                // expired bundle. The user has to reconnect via Settings.
                tokenStore.remove(integration.id)
                false
            },
        )
    }
}

/** Qualifier for the OAuth-only KeyVault — see the `OAuth` block in `appModule`. */
private const val OAUTH_KEY_VAULT: String = "oauth_key_vault"
