package dev.weft.undercurrent.core

import dev.weft.undercurrent.features.chat.NotificationsPermissionBanner
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.weft.android.WeftRuntime
import dev.weft.compose.ComposeUiBridge
import dev.weft.compose.PendingRequestRenderer
import dev.weft.compose.WeftOverlayHost
import dev.weft.compose.WeftUi
import dev.weft.compose.components.AgentRenderedTreeScreen
import dev.weft.contracts.ProviderKind
import dev.weft.oauth.OAuthCallbackChannel
import dev.weft.undercurrent.features.integrations.IntegrationsScreen
import dev.weft.undercurrent.features.personas.PersonaRepository
import dev.weft.undercurrent.features.providers.keyAlias
import dev.weft.undercurrent.theme.ThemeMode
import dev.weft.undercurrent.theme.UndercurrentTheme
import dev.weft.undercurrent.features.memories.AgentMemoriesScreen
import dev.weft.undercurrent.ui.AppDrawer
import dev.weft.undercurrent.features.settings.AppearanceScreen
import dev.weft.undercurrent.features.chat.ChatScreen
import dev.weft.undercurrent.features.conversations.ConversationsListScreen
import dev.weft.undercurrent.features.keypaste.KeyPasteScreen
import dev.weft.undercurrent.features.onboarding.OnboardingScreen
import dev.weft.undercurrent.features.personas.PersonasScreen
import dev.weft.undercurrent.features.providers.ProvidersScreen
import dev.weft.undercurrent.features.settings.SettingsScreen
import dev.weft.undercurrent.features.traces.TraceViewerScreen
import dev.weft.undercurrent.features.usage.UsageScreen
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import kotlinx.coroutines.withContext

// FragmentActivity (instead of ComponentActivity) is required so the
// substrate's AndroidBiometrics can attach its BiometricPrompt UI. The two
// classes share the same Compose / lifecycle / result-launcher surface area
// — FragmentActivity is a strict superset, no Compose changes needed.
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // targetSdk=35 forces edge-to-edge on Android 15+; on older targets
        // this call lifts the app into edge-to-edge mode explicitly. Inset
        // handling happens at the Compose root via [safeDrawingPadding].
        enableEdgeToEdge()
        // OAuth deep-link routing — cold-start case. If the OS launched us
        // via the `undercurrent://oauth/…` redirect, feed the URI into the
        // substrate's callback channel so the suspending `authorize()` call
        // resumes. Warm-start path is in `onNewIntent` below. See
        // [OAuthCallbackChannel] for the channel's role in the OAuth flow.
        intent?.data?.let { uri ->
            get<OAuthCallbackChannel>().submit(uri)
        }
        setContent { App() }
    }

    /**
     * Warm-start OAuth redirect. Manifest declares MainActivity with
     * `launchMode="singleTop"`, so a deep link delivered while the app is
     * already running fires onNewIntent instead of recreating us. The OS
     * also routes back here when the user finishes the Custom Tabs flow
     * — Custom Tabs preserves the source activity, so the redirect URI
     * lands on whatever activity opened the tab.
     */
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        intent.data?.let { uri ->
            get<OAuthCallbackChannel>().submit(uri)
        }
    }
}

@Composable
private fun App() {
    // Koin-resolved singletons. `koinInject<T>()` returns the same instance
    // the AppStore + everyone else got, since they're all `single { }` in
    // appModule. `koinViewModel<AppStore>()` scopes the VM to the calling
    // ViewModelStoreOwner (here, MainActivity) — survives recreation.
    val runtime: WeftRuntime = koinInject()
    val uiBridge: ComposeUiBridge = koinInject()
    val weftUi: WeftUi = koinInject()
    val store: AppStore = koinViewModel()
    val state by store.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    // Captured here so the Integrations screen's `onRestart` can call the
    // process-restart helper without prop-drilling the Activity reference
    // through the navigation tree.
    val context = LocalContext.current

    // Save-as-feature dialog state for the RenderedTree screen's Save
    // button. Lives at the App level (not inside any specific screen
    // branch) so the dialog stays mounted while the user is on
    // RenderedTree — opening it from a screen-scoped state would
    // dismount on the navigate-back.
    val appSavedFeaturesRepo: dev.weft.undercurrent.features.savedfeatures.SavedFeaturesRepository =
        koinInject()
    val appSavedFeaturesScope = rememberCoroutineScope()
    var saveFromRenderDraft by remember { mutableStateOf<String?>(null) }
    val systemDark = isSystemInDarkTheme()
    val darkMode = when (state.themePrefs.mode) {
        ThemeMode.Auto -> systemDark
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    // Boot path — read stored key, build agent, hydrate the most recent
    // conversation. Runs once because the store's state initializes to
    // Screen.Loading.
    LaunchedEffect(store) {
        store.dispatch(AppIntent.Resume)
    }

    // Auto-navigate to the rendered-tree screen whenever the agent emits a
    // UIUpdate.RenderTree. Forwarded into the store so navigation lives
    // in one place.
    LaunchedEffect(store, uiBridge) {
        snapshotFlow { uiBridge.lastUpdate }.collect { update ->
            store.dispatch(AppIntent.UiBridgeUpdate(update))
        }
    }

    // Drain one-shot effects. Errors surface via the Snackbar overlay
    // below; add new effect arms here as they appear.
    LaunchedEffect(store, snackbarHostState) {
        store.effects.collect { effect ->
            when (effect) {
                is AppEffect.Error -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    // System back routing. Without this, the Android back gesture /
    // button does nothing on sub-screens (Compose doesn't pop a back
    // stack we never built), and the user can get stranded — most
    // common report: opens a tracker mini-app via a saved feature,
    // can't find their way back to chat. Behavior:
    //
    //   - On Chat: don't intercept. System back exits the app as usual.
    //   - On RenderedTree: clear the bridge update + go to chat.
    //   - On any other screen: go to chat. Drawer-reachable screens
    //     don't have a sub-hierarchy worth preserving.
    androidx.activity.compose.BackHandler(enabled = state.screen != Screen.Chat) {
        if (state.screen is Screen.RenderedTree) {
            uiBridge.clearLastUpdate()
        }
        store.dispatch(AppIntent.Navigate(Screen.Chat))
    }

    UndercurrentTheme(palette = state.themePrefs.palette, darkMode = darkMode) {
        // Weft DevTools overlay — wraps the whole app surface in debug builds
        // (provides a floating action button → inspection sheet). In release
        // builds the src/release stub of DevToolsHost renders just `content()`,
        // so the dep + UI both drop out of the shipped APK.
        DevToolsHost(runtime = runtime) {
            // Surface fills the entire window (its background draws under the
            // system bars, giving the seamless edge-to-edge look), then
            // `safeDrawingPadding` offsets the content area away from the
            // status bar, navigation bar, display cutout, and IME. For a
            // chat app this means: messages list + input row both stay clear
            // of the system bars, and the input row floats above the keyboard
            // when it opens (because IME insets are part of safeDrawing).
            Surface(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                // Explicit Box so the snackbar host can use `Modifier.align`.
                // Material3 Surface's content lambda isn't BoxScope-extended,
                // even though it stacks children Box-like internally.
                Box(modifier = Modifier.fillMaxSize()) {
                // Default substrate dialog renderer — apps that want custom styling
                // skip this and render against uiBridge.pending themselves.
                PendingRequestRenderer(uiBridge)
                // Default substrate overlay host — renders toasts (auto-dismiss)
                // and banners (persistent) emitted via ui_notify.
                WeftOverlayHost(uiBridge)

                when (state.screen) {
                    Screen.Loading -> LoadingPlaceholder()
                    Screen.Onboarding -> {
                        val onboardingPersonaRepo: PersonaRepository = koinInject()
                        val onboardingScope = rememberCoroutineScope()
                        OnboardingScreen(
                            onComplete = { picked, voiceId ->
                                // Persist the picked voice first so the
                                // runtime's per-turn lambda already reads
                                // the right persona on the first send.
                                // Idempotent — Default is the initial
                                // state so a Skip just re-asserts it.
                                onboardingScope.launch {
                                    onboardingPersonaRepo.setActiveVoice(voiceId)
                                }
                                // Persist provider choice BEFORE marking
                                // onboarding complete, so the KeyPaste screen
                                // already sees the right activeProvider when
                                // it composes next.
                                store.dispatch(AppIntent.SetProvider(picked))
                                store.dispatch(AppIntent.CompleteOnboarding)
                            },
                        )
                    }
                    Screen.KeyPaste -> KeyPasteScreen(
                        provider = state.activeProvider,
                        onKeyAccepted = { key -> store.dispatch(AppIntent.SubmitKey(key)) },
                        saveKey = { key -> store.saveKey(key) },
                    )
                    Screen.Chat -> {
                        val a = state.agent
                        if (a == null) {
                            // Defensive fall-through — should be unreachable
                            // since the boot path navigates to KeyPaste when
                            // the key vault is empty.
                            LaunchedEffect(Unit) {
                                store.dispatch(AppIntent.Navigate(Screen.KeyPaste))
                            }
                        } else {
                            ChatRouteWithDrawer(
                                store = store,
                                runtime = runtime,
                                state = state,
                                agentCurrentConvId = a.currentConversationId.collectAsState().value,
                                circuitBreaker = a.circuitBreaker,
                            )
                        }
                    }
                    Screen.RenderedTree -> {
                        AgentRenderedTreeScreen(
                            uiBridge = uiBridge,
                            registry = weftUi.componentRegistry,
                            onAction = { action, label, fields ->
                                store.sendUiEvent(action, label, fields)
                            },
                            onBack = {
                                uiBridge.clearLastUpdate()
                                store.dispatch(AppIntent.Navigate(Screen.Chat))
                            },
                            // Pass the runtime's data-source registry so the
                            // screen's BindingAwareRenderer can resolve
                            // `$binding` sentinels in agent-emitted props
                            // against live data — taps that fire $exec
                            // mutations update the displayed numbers
                            // without an LLM round-trip.
                            dataSources = runtime.dataSources,
                            // In-place save. Without this affordance, users
                            // who build a tracker via "make me a water
                            // tracker" have no obvious way to keep it
                            // around — they'd have to back out to chat
                            // and find the assistant reply's "Save as
                            // feature" link. This puts the save right
                            // where they're looking at the mini-app.
                            onSaveAsFeature = onSaveAsFeature@{
                                val lastUserText = store.displayMessages
                                    .lastOrNull { it.role == dev.weft.undercurrent.features.chat.DisplayRole.USER }
                                    ?.text
                                    ?.takeIf { it.isNotBlank() }
                                    ?: return@onSaveAsFeature
                                saveFromRenderDraft = lastUserText
                            },
                        )
                    }
                    Screen.Traces -> TraceViewerScreen(
                        onBack = { store.dispatch(AppIntent.Navigate(Screen.Chat)) },
                        onExportTrace = { trace -> store.dispatch(AppIntent.ExportTrace(trace)) },
                    )
                    Screen.Memories -> AgentMemoriesScreen(
                        onBack = { store.dispatch(AppIntent.Navigate(Screen.Chat)) },
                    )
                    Screen.Settings -> SettingsScreen(
                        activeProvider = state.activeProvider,
                        onShowProvider = { store.dispatch(AppIntent.Navigate(Screen.Providers)) },
                        onShowUsage = { store.dispatch(AppIntent.Navigate(Screen.Usage)) },
                        onShowIntegrations = { store.dispatch(AppIntent.Navigate(Screen.Integrations)) },
                        onBack = { store.dispatch(AppIntent.Navigate(Screen.Chat)) },
                    )
                    Screen.Integrations -> IntegrationsScreen(
                        // Integrations is reachable from two places:
                        // Settings (drill-down) and the Chat input's
                        // "Add to Chat" sheet (Connectors row). Route
                        // back to wherever we came from instead of a
                        // fixed target. `state.previousScreen` is set
                        // by `AppIntent.Navigate` in the reducer.
                        onBack = {
                            store.dispatch(AppIntent.Navigate(state.previousScreen))
                        },
                        // Process-level restart so the WeftRuntime rebuilds
                        // with the new MCP server list. The substrate
                        // doesn't support hot-swapping MCP servers — see
                        // the README of the integrations feature for the
                        // restart-to-apply rationale.
                        onRestart = { restartProcess(context) },
                    )
                    Screen.Providers -> {
                        // Observe the model-overrides StateFlow so the
                        // dropdowns recompose when the user picks a new
                        // model. We don't read the snapshot directly —
                        // the closure below reads .value via the repo
                        // — but observing here drives the recomposition.
                        @Suppress("UNUSED_VARIABLE")
                        val modelOverridesTick by store.modelPrefsRepo.overrides.collectAsState()
                        // Key-status fetch: re-runs when the agent identity
                        // changes (rebuild on save) or the active provider
                        // changes. Map value = the last 4 chars of the
                        // stored key. ProvidersScreen formats the subtitle
                        // line locally (it needs different prefixes
                        // depending on active state); we never expose more
                        // than last4 from the vault to the UI.
                        val providerKeyStatus by produceState(
                            initialValue = emptyMap<dev.weft.contracts.ProviderKind, String>(),
                            key1 = state.agent,
                            key2 = state.activeProvider,
                        ) {
                            value = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                buildMap {
                                    dev.weft.contracts.ProviderKind.entries.forEach { provider ->
                                        val k = runtime.keyVault.get(provider.keyAlias())
                                        if (!k.isNullOrBlank()) {
                                            put(provider, k.takeLast(4))
                                        }
                                    }
                                }
                            }
                        }
                        ProvidersScreen(
                            activeProvider = state.activeProvider,
                            defaultTier = state.defaultTier,
                            providerKeyStatus = providerKeyStatus,
                            onProviderSelected = { p -> store.dispatch(AppIntent.SetProvider(p)) },
                            onProviderKeySaved = { p, k ->
                                store.dispatch(AppIntent.SaveProviderKey(p, k))
                            },
                            onProviderKeyRemoved = { p ->
                                store.dispatch(AppIntent.RemoveProviderKey(p))
                            },
                            onDefaultTierSelected = { t -> store.dispatch(AppIntent.SetDefaultTier(t)) },
                            getModelOverride = { p, t -> store.modelPrefsRepo.overrideFor(p, t) },
                            onModelOverrideSelected = { p, t, id ->
                                store.dispatch(AppIntent.SetModelForTier(p, t, id))
                            },
                            onBack = { store.dispatch(AppIntent.Navigate(Screen.Settings)) },
                        )
                    }
                    Screen.Appearance -> AppearanceScreen(
                        selectedPalette = state.themePrefs.palette,
                        selectedMode = state.themePrefs.mode,
                        onPaletteSelected = { p -> store.dispatch(AppIntent.SetPalette(p)) },
                        onModeSelected = { m -> store.dispatch(AppIntent.SetThemeMode(m)) },
                        onBack = { store.dispatch(AppIntent.Navigate(Screen.Settings)) },
                    )
                    Screen.Usage -> UsageScreen(
                        onBack = { store.dispatch(AppIntent.Navigate(Screen.Settings)) },
                    )
                    Screen.Personas -> PersonasScreen(
                        onBack = { store.dispatch(AppIntent.Navigate(Screen.Chat)) },
                    )
                    Screen.SavedFeatures ->
                        dev.weft.undercurrent.features.savedfeatures.SavedFeaturesScreen(
                            onBack = { store.dispatch(AppIntent.Navigate(Screen.Chat)) },
                        )
                    Screen.Conversations -> {
                        val a = state.agent
                        if (a == null) {
                            LaunchedEffect(Unit) {
                                store.dispatch(AppIntent.Navigate(Screen.KeyPaste))
                            }
                        } else {
                            ConversationsListScreen(
                                activeConversationId = a.currentConversationId.value,
                                onSelect = { id -> store.dispatch(AppIntent.SelectConversation(id)) },
                                onNewChat = { store.dispatch(AppIntent.NewChat) },
                                onBack = { store.dispatch(AppIntent.Navigate(Screen.Chat)) },
                            )
                        }
                    }
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )

                // "Permission needed" dialog — surfaces when an agent
                // tool call fails because Android denied a runtime
                // permission (typically the user hit "Don't allow"
                // twice, so the system stops showing its own prompt).
                // Rendered at the App root so it appears over any
                // screen the user is on when the tool fired.
                state.pendingPermissionDialog?.let { pending ->
                    PermissionNeededDialog(
                        state = pending,
                        onOpenSettings = {
                            openAppDetailsSettings(context)
                            store.dispatch(AppIntent.DismissPermissionDialog)
                        },
                        onDismiss = {
                            store.dispatch(AppIntent.DismissPermissionDialog)
                        },
                    )
                }

                // Save-as-feature dialog for the RenderedTree screen.
                // Opens when the user taps "Save" on a mini-app surface.
                // Captures both the trigger prompt (the user's latest
                // message, from chat history) AND the current render
                // tree (so a future tap on the chip shows the cached
                // UI instantly while the agent re-runs).
                saveFromRenderDraft?.let { draft ->
                    dev.weft.undercurrent.features.savedfeatures.SaveAsFeatureDialog(
                        initial = null,
                        suggestedPrompt = draft,
                        onDismiss = { saveFromRenderDraft = null },
                        onSave = { name, emoji, triggerPrompt ->
                            saveFromRenderDraft = null
                            appSavedFeaturesScope.launch {
                                val created = appSavedFeaturesRepo.add(name, emoji, triggerPrompt)
                                // Capture the current rendered tree so
                                // the next invocation can show it
                                // instantly. The bridge always has a
                                // tree when this dialog is open (the
                                // Save button only renders on
                                // RenderedTree), so the cast is safe.
                                val tree = (uiBridge.lastUpdate as?
                                    dev.weft.contracts.UIUpdate.RenderTree)?.tree
                                if (tree != null) {
                                    val json = kotlinx.serialization.json.Json.encodeToString(
                                        dev.weft.contracts.ComponentNode.serializer(),
                                        tree,
                                    )
                                    appSavedFeaturesRepo.setCachedRender(created.id, json)
                                }
                                snackbarHostState.showSnackbar(
                                    "Saved $emoji $name. Find it in My features.",
                                )
                            }
                        },
                    )
                }
                } // close Box
            }
        }
    }
}

/**
 * Chat surface wrapped in the side navigation drawer. Pulled into its own
 * composable so the drawer state and conversation-flow collection don't
 * pollute [App]'s top-level scope (and so it doesn't run when we're on
 * any other screen — keeps the conversation store flow subscription tied
 * to chat being on-screen).
 *
 * Thread title is derived from the active conversation's summary; falls
 * back to "Undercurrent" when nothing's persisted yet (brand-new chat).
 */
@Composable
private fun ChatRouteWithDrawer(
    store: AppStore,
    runtime: dev.weft.android.WeftRuntime,
    state: AppState,
    agentCurrentConvId: String,
    circuitBreaker: dev.weft.harness.reliability.CircuitBreaker,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    // The bridge is the canonical Compose-side surface where agent
    // ui_render payloads land. Pulled in here so the saved-feature
    // invoke path can seed `lastUpdate` with the cached tree for the
    // "instant on tap" UX — without waiting for the agent's refresh.
    val uiBridge: ComposeUiBridge = koinInject()
    val conversations by remember { runtime.conversationStore.search("") }
        .collectAsState(initial = emptyList())

    val threadTitle = conversations
        .firstOrNull { it.id == agentCurrentConvId }
        ?.title
        ?.takeIf { it.isNotBlank() }
        ?: "Undercurrent"

    // Header subtitle — "<Provider> · <ModelFamily> · <Persona-label>".
    //
    // Voice + role pulled from the same singleton PersonaRepository the
    // runtime closes over for `extraVolatilePrefix`, so the badge in the
    // header matches what the agent actually sees per turn.
    //
    // Persona-label composition:
    //  - Both voice (non-default) + role → "Editor + Doctor"
    //  - Voice only (non-default), no role → "Editor"
    //  - Role only, voice is Default → "Doctor"
    //  - Default voice, no role → "Default"
    val personaRepo: PersonaRepository = koinInject()
    val activeVoice by personaRepo.activeVoice.collectAsState()
    val activeRole by personaRepo.activeRole.collectAsState()
    val personaLabel = run {
        val voiceLabel = activeVoice.name.takeIf { it != "Default" }
        val roleLabel = activeRole?.name
        listOfNotNull(voiceLabel, roleLabel).joinToString(" + ")
            .ifEmpty { "Default" }
    }
    val threadSubtitle = listOf(
        state.activeProvider.displayName(),
        state.activeProvider.modelFamily(),
        personaLabel,
    ).joinToString(" · ")

    fun closeAnd(action: () -> Unit) {
        coroutineScope.launch { drawerState.close() }
        action()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                conversations = conversations,
                activeConversationId = agentCurrentConvId,
                onSelect = { id -> closeAnd { store.dispatch(AppIntent.SelectConversation(id)) } },
                onNewChat = { closeAnd { store.dispatch(AppIntent.NewChat) } },
                // Drawer keeps itself open after the delete dispatches
                // (no closeAnd) — the confirmation dialog already
                // captured the user's intent; closing the drawer right
                // afterward would feel like extra motion.
                onDeleteConversation = { id ->
                    store.dispatch(AppIntent.DeleteConversation(id))
                },
                onShowAllConversations = {
                    closeAnd { store.dispatch(AppIntent.Navigate(Screen.Conversations)) }
                },
                onShowPersonas = { closeAnd { store.dispatch(AppIntent.Navigate(Screen.Personas)) } },
                onShowSavedFeatures = {
                    closeAnd { store.dispatch(AppIntent.Navigate(Screen.SavedFeatures)) }
                },
                onShowMemories = { closeAnd { store.dispatch(AppIntent.Navigate(Screen.Memories)) } },
                onShowTraces = { closeAnd { store.dispatch(AppIntent.Navigate(Screen.Traces)) } },
                onShowSettings = { closeAnd { store.dispatch(AppIntent.Navigate(Screen.Settings)) } },
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            NotificationsPermissionBanner()
            // Connected-integrations count for the "Add to Chat" sheet's
            // Connectors row. Pulled from IntegrationsRepository so the
            // trailing label is always live ("1 connected" / "None").
            // No restart-on-change concerns here — the count is a UI hint,
            // not something the agent reads.
            val integrationsRepo: dev.weft.undercurrent.features.integrations.IntegrationsRepository =
                koinInject()
            val enabledIntegrationIds by integrationsRepo.enabledIdsFlow
                .collectAsState(initial = emptySet())

            // Saved features for the "Add to Chat" chip row +
            // post-reply "Save as feature" affordance. Process-scoped
            // repo collected as state here so the sheet always reflects
            // the latest list (newly-saved features show up immediately
            // on the next open). Invocation goes through the existing
            // SendChat intent — feels indistinguishable from typing.
            val savedFeaturesRepo: dev.weft.undercurrent.features.savedfeatures.SavedFeaturesRepository =
                koinInject()
            val savedFeatures by savedFeaturesRepo.features.collectAsState()
            val savedFeaturesScope = rememberCoroutineScope()
            ChatScreen(
                displayMessages = store.displayMessages,
                inFlight = state.chat.inFlight,
                lastError = state.chat.lastError,
                onSend = { text, tier ->
                    store.dispatch(AppIntent.SendChat(text, tier))
                },
                defaultTier = state.defaultTier,
                threadTitle = threadTitle,
                threadSubtitle = threadSubtitle,
                activePersonaName = personaLabel,
                onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                onNewChat = { store.dispatch(AppIntent.NewChat) },
                onDeleteThread = { store.dispatch(AppIntent.DeleteCurrentConversation) },
                onRegenerate = { store.dispatch(AppIntent.RegenerateLast) },
                skills = store.skills,
                usageStore = runtime.usageStore,
                circuitBreaker = circuitBreaker,
                // Multi-agent selector wiring. The state's AgentSummary
                // list maps 1:1 to AgentSelector's AgentOption DTO; the
                // composable hides itself when fewer than two agents are
                // registered, so single-agent hosts pay nothing.
                agents = state.availableAgents.map {
                    dev.weft.compose.components.AgentOption(
                        name = it.name,
                        displayName = it.displayName,
                        description = it.description,
                    )
                },
                activeAgentName = state.activeAgentName,
                onSelectAgent = { name -> store.dispatch(AppIntent.SelectAgent(name)) },
                // Wire the "Add to Chat" sheet. Theme controls live inline
                // (palette + mode) so we route the picker callbacks through
                // the existing intents — same path Settings used before
                // Appearance was removed from there. Style + Connectors
                // navigate to their dedicated screens.
                addToChatConfig = dev.weft.undercurrent.features.chat.addToChatConfig(
                    activePalette = state.themePrefs.palette,
                    activeMode = state.themePrefs.mode,
                    connectedIntegrationsCount = enabledIntegrationIds.size,
                    savedFeatures = savedFeatures,
                    onSelectPalette = { p -> store.dispatch(AppIntent.SetPalette(p)) },
                    onSelectMode = { m -> store.dispatch(AppIntent.SetThemeMode(m)) },
                    onShowPersonas = {
                        store.dispatch(AppIntent.Navigate(Screen.Personas))
                    },
                    onShowIntegrations = {
                        store.dispatch(AppIntent.Navigate(Screen.Integrations))
                    },
                    onShowSavedFeatures = {
                        store.dispatch(AppIntent.Navigate(Screen.SavedFeatures))
                    },
                    // Invocation: if the feature has a cached UI tree
                    // from a previous run, seed the Compose UI bridge
                    // synchronously + navigate to the rendered-tree
                    // screen so the user sees the mini-app instantly.
                    // THEN dispatch InvokeSavedFeature so the agent
                    // re-runs the trigger prompt to refresh whatever
                    // data the UI reads — capturing the new tree onto
                    // the feature happens in AppStore.
                    //
                    // Features without a cached tree fall through to
                    // the normal chat flow: prompt visible in history,
                    // ui_render (if the agent emits one) auto-navigates
                    // via the existing handleUiBridgeUpdate path.
                    onInvokeFeature = { feature ->
                        val cached = feature.lastRenderTreeJson
                        if (cached != null) {
                            // `lastUpdate` has `private set` on the bridge,
                            // so we seed via the public suspend `emit(...)`
                            // — same path the substrate's `ui_render` tool
                            // uses internally. Launch on the activity scope
                            // because the lambda itself isn't suspend.
                            coroutineScope.launch {
                                runCatching {
                                    val tree = kotlinx.serialization.json.Json
                                        .decodeFromString(
                                            dev.weft.contracts.ComponentNode.serializer(),
                                            cached,
                                        )
                                    uiBridge.emit(
                                        dev.weft.contracts.UIUpdate.RenderTree(tree),
                                    )
                                }
                            }
                            store.dispatch(AppIntent.Navigate(Screen.RenderedTree))
                        }
                        store.dispatch(
                            AppIntent.InvokeSavedFeature(
                                featureId = feature.id,
                                triggerPrompt = feature.triggerPrompt,
                            ),
                        )
                    },
                    onAddSavedFeature = { name, emoji, prompt ->
                        savedFeaturesScope.launch {
                            savedFeaturesRepo.add(name, emoji, prompt)
                        }
                    },
                ),
            )
        }
    }
}

/**
 * AlertDialog surfaced when a tool fails because of a denied runtime
 * permission. Two outcomes: tap **Open Settings** to deep-link into the
 * system app-details page (where the user can flip the permission
 * toggle), or **Cancel** to dismiss. Either path clears the pending
 * dialog state via [AppIntent.DismissPermissionDialog].
 */
@Composable
private fun PermissionNeededDialog(
    state: PermissionDialogState,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { androidx.compose.material3.Text(state.friendlyTitle) },
        text = { androidx.compose.material3.Text(state.friendlyBody) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onOpenSettings) {
                androidx.compose.material3.Text("Open Settings")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                androidx.compose.material3.Text("Not now")
            }
        },
    )
}

/**
 * Deep-link into the system's "App info" page for this app. From there
 * the user can tap "Permissions" → flip the relevant switch.
 *
 * This is the only path back to GRANTED once Android has marked a
 * permission as "denied forever" (the user picked "Don't allow" twice
 * on Android 11+, or "Never ask again" on older releases) — the
 * standard runtime-permission API silently returns DENIED without
 * showing the system dialog.
 */
private fun openAppDetailsSettings(context: android.content.Context) {
    val intent = android.content.Intent(
        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
    ).apply {
        data = android.net.Uri.fromParts("package", context.packageName, null)
        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}

/**
 * Process-level restart. Called from Integrations after a Connect or
 * Disconnect: the WeftRuntime's tool registry is fixed at construction
 * time, so the only way to pick up new MCP tools is to rebuild the
 * runtime — which we do by killing the process and letting the OS
 * resurrect us via the launcher intent.
 *
 * The launch intent uses `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`
 * so the back stack starts clean (no chance of resuming into the old,
 * stale runtime). `Runtime.getRuntime().exit(0)` terminates the process
 * after the new task is queued; the OS then starts a fresh process to
 * service the queued launch.
 */
private fun restartProcess(context: android.content.Context) {
    val pm = context.packageManager
    val launchIntent = pm.getLaunchIntentForPackage(context.packageName)
        ?: return
    launchIntent.addFlags(
        android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
            android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK,
    )
    context.startActivity(launchIntent)
    Runtime.getRuntime().exit(0)
}

/**
 * Provider display name for the chat header subtitle. Kept here (vs. in
 * ProvidersScreen) because the header subtitle is core/MainActivity's
 * concern — duplicating four lines beats coupling the chat header to
 * the providers feature package.
 */
private fun ProviderKind.displayName(): String = when (this) {
    ProviderKind.Anthropic -> "Anthropic"
    ProviderKind.OpenAI -> "OpenAI"
    ProviderKind.OpenRouter -> "OpenRouter"
    ProviderKind.DeepSeek -> "DeepSeek"
}

/**
 * Brand-family name for each provider — the middle slot of the chat
 * header subtitle ("<Provider> · <ModelFamily> · <Persona>"). For
 * single-family providers (Anthropic → Claude, OpenAI → GPT) this gives
 * the user the warm brand name they recognize; for multi-model
 * gateways (OpenRouter) and reasoning families (DeepSeek) it falls
 * back to the provider name.
 */
private fun ProviderKind.modelFamily(): String = when (this) {
    ProviderKind.Anthropic -> "Claude"
    ProviderKind.OpenAI -> "GPT"
    ProviderKind.OpenRouter -> "OpenRouter"
    ProviderKind.DeepSeek -> "DeepSeek"
}
