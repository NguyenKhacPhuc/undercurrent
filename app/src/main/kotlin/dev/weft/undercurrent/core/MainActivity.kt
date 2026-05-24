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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.weft.android.WeftRuntime
import dev.weft.compose.ComposeUiBridge
import dev.weft.compose.PendingRequestRenderer
import dev.weft.compose.WeftOverlayHost
import dev.weft.compose.WeftUi
import dev.weft.compose.components.AgentRenderedTreeScreen
import dev.weft.contracts.ProviderKind
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
        setContent { App() }
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
                        paletteDisplayName = state.themePrefs.palette.displayName,
                        modeDisplayName = state.themePrefs.mode.displayName,
                        onShowProvider = { store.dispatch(AppIntent.Navigate(Screen.Providers)) },
                        onShowAppearance = { store.dispatch(AppIntent.Navigate(Screen.Appearance)) },
                        onShowUsage = { store.dispatch(AppIntent.Navigate(Screen.Usage)) },
                        onBack = { store.dispatch(AppIntent.Navigate(Screen.Chat)) },
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
                onShowMemories = { closeAnd { store.dispatch(AppIntent.Navigate(Screen.Memories)) } },
                onShowTraces = { closeAnd { store.dispatch(AppIntent.Navigate(Screen.Traces)) } },
                onShowSettings = { closeAnd { store.dispatch(AppIntent.Navigate(Screen.Settings)) } },
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            NotificationsPermissionBanner()
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
            )
        }
    }
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
