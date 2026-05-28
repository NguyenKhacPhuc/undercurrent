package dev.weft.undercurrent.core

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dev.weft.android.WeftRuntime
import dev.weft.compose.ComposeUiBridge
import dev.weft.compose.PendingRequestRenderer
import dev.weft.compose.WeftOverlayHost
import dev.weft.compose.WeftUi
import dev.weft.compose.components.AgentRenderedTreeScreen
import dev.weft.compose.components.TreeRenderer
import dev.weft.contracts.ComponentNode
import dev.weft.contracts.UIUpdate
import dev.weft.oauth.OAuthCallbackChannel
import dev.weft.undercurrent.app.App
import dev.weft.undercurrent.app.AppIntent
import dev.weft.undercurrent.app.AppStore
import dev.weft.undercurrent.app.PlatformAdapter
import dev.weft.undercurrent.core.designsystem.colors
import dev.weft.undercurrent.core.model.ThemeMode
import dev.weft.undercurrent.core.navigation.Screen
import dev.weft.undercurrent.data.datastore.IntegrationsRepository
import dev.weft.undercurrent.data.datastore.MiniAppsRepository
import dev.weft.undercurrent.data.datastore.PersonaRepository
import dev.weft.undercurrent.feature.chat.AddToChatConfig
import dev.weft.undercurrent.feature.chat.AgentOption
import dev.weft.undercurrent.feature.chat.ChatScreen
import dev.weft.undercurrent.feature.chat.DisplayRole
import dev.weft.undercurrent.feature.chat.NotificationsPermissionBanner
import dev.weft.undercurrent.feature.creator.CreatorScreen
import dev.weft.undercurrent.feature.miniapps.MiniAppsScreen
import dev.weft.undercurrent.feature.miniapps.SaveAsMiniAppDialog
import dev.weft.undercurrent.shared.gateway.SpeechGateway
import dev.weft.undercurrent.core.ext.openInBrowser
import dev.weft.undercurrent.core.ui.components.AppDrawer
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.android.ext.android.get
import org.koin.compose.koinInject

/**
 * Single Android activity. FragmentActivity (not ComponentActivity) so
 * the substrate's biometric prompt can attach. All actual UI lives in
 * the commonMain [App] composable; this class just boots Koin, plumbs
 * OAuth deep links, and wires the [PlatformAdapter] with Android-only
 * screens.
 */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        intent?.data?.let { uri -> get<OAuthCallbackChannel>().submit(uri) }
        setContent { AndroidApp() }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        intent.data?.let { uri -> get<OAuthCallbackChannel>().submit(uri) }
    }
}

/**
 * Android-side wrapper around the commonMain [App] composable. Owns:
 *  - Save-as-mini-app dialog (at the App root so it persists across
 *    screen transitions).
 *  - System back routing.
 *  - The [PlatformAdapter] holding all substrate-coupled screen
 *    composables.
 */
@Composable
private fun AndroidApp() {
    val store: AppStore = koinInject()
    val runtime: WeftRuntime = koinInject()
    val uiBridge: ComposeUiBridge = koinInject()
    val weftUi: WeftUi = koinInject()
    val appMiniAppsRepo: MiniAppsRepository = koinInject()
    val appMiniAppsScope = rememberCoroutineScope()
    val context = LocalContext.current
    val state by store.state.collectAsState()

    val systemDark = isSystemInDarkTheme()
    val darkMode = when (state.themePrefs.mode) {
        ThemeMode.Auto -> systemDark
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val accentArgb = state.themePrefs.palette.colors(darkMode).accent.toArgb()
    val onOpenUrl: (String) -> Unit = { url -> openInBrowser(context, url, accentArgb) }
    val onCopyText: (String) -> Unit = { text ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("message", text))
    }

    var saveFromRenderDraft by remember { mutableStateOf<String?>(null) }

    val platform = remember {
        PlatformAdapter(
            chatRoute = {
                ChatRoute(
                    store = store,
                    runtime = runtime,
                    onOpenUrl = onOpenUrl,
                    onCopyText = onCopyText
                )
            },
            renderedTreeRoute = {
                AgentRenderedTreeScreen(
                    uiBridge = uiBridge,
                    registry = weftUi.componentRegistry,
                    onAction = { action, label, fields ->
                        appMiniAppsScope.launch {
                            store.sendUiEvent(action, label, fields)
                        }
                    },
                    onBack = {
                        uiBridge.clearLastUpdate()
                        store.dispatch(AppIntent.Navigate(Screen.Chat))
                    },
                    dataSources = runtime.dataSources,
                    onSaveAsFeature = onSaveAsFeature@{
                        val lastUserText = store.displayMessages
                            .lastOrNull { it.role == DisplayRole.USER }
                            ?.text
                            ?.takeIf { it.isNotBlank() }
                            ?: return@onSaveAsFeature
                        saveFromRenderDraft = lastUserText
                    },
                )
            },
            miniAppsRoute = {
                MiniAppsScreen(
                    treePreview = { treeJson, onTap ->
                        val tree = remember(treeJson) {
                            runCatching {
                                Json.decodeFromString(ComponentNode.serializer(), treeJson)
                            }.getOrNull()
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(onClick = onTap),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (tree != null) {
                                TreeRenderer(
                                    tree = tree,
                                    registry = weftUi.componentRegistry,
                                    onEvent = { /* swallow — taps go to onTap */ },
                                )
                            } else {
                                Text("(preview)")
                            }
                        }
                    },
                    onBack = { store.dispatch(AppIntent.Navigate(Screen.Chat)) },
                    onStartCreator = {
                        store.dispatch(
                            AppIntent.StartCreator(
                                dev.weft.undercurrent.feature.creator.CreatorKind.MiniApp,
                            ),
                        )
                    },
                    onOpenMiniApp = { miniApp ->
                        store.dispatch(
                            AppIntent.InvokeMiniApp(
                                miniAppId = miniApp.id,
                                triggerPrompt = miniApp.triggerPrompt,
                                cachedRenderTreeJson = miniApp.lastRenderTreeJson,
                            ),
                        )
                    },
                )
            },
            creatorRoute = {
                CreatorScreen(
                    creatorSession = koinInject(),
                    isThinking = state.chat.inFlight,
                    inFlight = state.chat.inFlight,
                    hasTree = (uiBridge.lastUpdate is UIUpdate.RenderTree),
                    lastError = state.chat.lastError,
                    onCancel = { store.dispatch(AppIntent.CancelCreator) },
                    body = {
                        AgentRenderedTreeScreen(
                            uiBridge = uiBridge,
                            registry = weftUi.componentRegistry,
                            onAction = { action, label, fields ->
                                appMiniAppsScope.launch {
                                    store.sendUiEvent(action, label, fields)
                                }
                            },
                            onBack = { store.dispatch(AppIntent.CancelCreator) },
                            dataSources = runtime.dataSources,
                        )
                    },
                )
            },
            onOpenUrl = onOpenUrl,
            onCopyText = onCopyText,
            onRestartProcess = { restartProcess(context) },
            onOpenAppDetailsSettings = { openAppDetailsSettings(context) },
            onOpenSaveDialog = { suggestedPrompt -> saveFromRenderDraft = suggestedPrompt },
        )
    }

    BackHandler(enabled = state.screen != Screen.Chat) {
        if (state.screen is Screen.RenderedTree) {
            uiBridge.clearLastUpdate()
        }
        store.dispatch(AppIntent.Navigate(Screen.Chat))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            App(store = store, platform = platform)
            PendingRequestRenderer(uiBridge)
            WeftOverlayHost(uiBridge)
        }

        saveFromRenderDraft?.let { draft ->
            SaveAsMiniAppDialog(
                initial = null,
                suggestedPrompt = draft,
                onDismiss = { saveFromRenderDraft = null },
                onSave = { name, emoji, triggerPrompt ->
                    saveFromRenderDraft = null
                    appMiniAppsScope.launch {
                        val created = appMiniAppsRepo.add(name, emoji, triggerPrompt)
                        val tree = (uiBridge.lastUpdate as? UIUpdate.RenderTree)?.tree
                        if (tree != null) {
                            val json = Json.encodeToString(ComponentNode.serializer(), tree)
                            appMiniAppsRepo.setCachedRender(created.id, json)
                        }
                    }
                },
            )
        }
    }
}

/**
 * Chat surface wrapped in the side navigation drawer. The drawer reads
 * conversations directly from the runtime — KMP-clean alternative
 * would be to route through `ConversationStoreGateway`, but the drawer
 * is Android-only anyway (uses the substrate Conversation type for
 * the `id` highlight + delete), so calling runtime directly is fine.
 */
@Composable
private fun ChatRoute(
    store: AppStore,
    runtime: WeftRuntime,
    onOpenUrl: (String) -> Unit,
    onCopyText: (String) -> Unit,
) {
    val state by store.state.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val speechGateway: SpeechGateway = koinInject()
    val uiBridge: ComposeUiBridge = koinInject()
    val agentCurrentConvId = state.currentConversationId.orEmpty()

    val conversations by remember { runtime.conversationStore.search("") }
        .collectAsState(initial = emptyList())

    val threadTitle = conversations
        .firstOrNull { it.id == agentCurrentConvId }
        ?.title
        ?.takeIf { it.isNotBlank() }
        ?: "Undercurrent"

    val personaRepo: PersonaRepository = koinInject()
    val activeVoice by personaRepo.activeVoice.collectAsState()
    val activeRole by personaRepo.activeRole.collectAsState()
    val personaLabel = run {
        val voiceLabel = activeVoice.name.takeIf { it != "Default" }
        val roleLabel = activeRole?.name
        listOfNotNull(voiceLabel, roleLabel).joinToString(" + ").ifEmpty { "Default" }
    }
    val threadSubtitle = listOf(
        state.activeProvider.displayName,
        personaLabel,
    ).joinToString(" · ")

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED,
        )
    }
    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> hasMicPermission = granted }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { /* banner re-checks on recompose */ }

    fun closeAnd(action: () -> Unit) {
        coroutineScope.launch { drawerState.close() }
        action()
    }

    val drawerConversations = conversations.map {
        dev.weft.undercurrent.shared.gateway.ConversationSummary(
            id = it.id,
            title = it.title,
            createdAtMs = it.createdAtMs,
            lastMessageAtMs = it.lastMessageAtMs,
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                conversations = drawerConversations,
                activeConversationId = agentCurrentConvId,
                onSelect = { id -> closeAnd { store.dispatch(AppIntent.SelectConversation(id)) } },
                onNewChat = { closeAnd { store.dispatch(AppIntent.NewChat) } },
                onDeleteConversation = { id -> store.dispatch(AppIntent.DeleteConversation(id)) },
                onShowAllConversations = {
                    closeAnd { store.dispatch(AppIntent.Navigate(Screen.Conversations)) }
                },
                onShowPersonas = { closeAnd { store.dispatch(AppIntent.Navigate(Screen.Personas)) } },
                onShowMiniApps = { closeAnd { store.dispatch(AppIntent.Navigate(Screen.MiniApps)) } },
                onShowMemories = { closeAnd { store.dispatch(AppIntent.Navigate(Screen.Memories)) } },
                onShowTraces = { closeAnd { store.dispatch(AppIntent.Navigate(Screen.Traces)) } },
                onShowSettings = { closeAnd { store.dispatch(AppIntent.Navigate(Screen.Settings)) } },
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            NotificationsPermissionBanner(
                onGrant = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
            )

            val integrationsRepo: IntegrationsRepository = koinInject()
            val enabledIntegrationIds by integrationsRepo.enabledIdsFlow
                .collectAsState(initial = emptySet())

            val miniAppsRepo: MiniAppsRepository = koinInject()
            val miniApps by miniAppsRepo.miniApps.collectAsState()
            val miniAppsScope = rememberCoroutineScope()

            ChatScreen(
                displayMessages = store.displayMessages,
                inFlight = state.chat.inFlight,
                lastError = state.chat.lastError,
                onSend = { text, tier -> store.dispatch(AppIntent.SendChat(text, tier)) },
                defaultTier = state.defaultTier,
                threadTitle = threadTitle,
                threadSubtitle = threadSubtitle,
                activePersonaName = personaLabel,
                lastModelId = null,
                degradedMode = null,
                speechGateway = speechGateway,
                hasMicPermission = hasMicPermission,
                onRequestMicPermission = {
                    micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                onCopyText = onCopyText,
                onOpenUrl = onOpenUrl,
                onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                onNewChat = { store.dispatch(AppIntent.NewChat) },
                onDeleteThread = { store.dispatch(AppIntent.DeleteCurrentConversation) },
                onRegenerate = { store.dispatch(AppIntent.RegenerateLast) },
                skills = store.skills,
                agents = state.availableAgents.map {
                    AgentOption(it.name, it.displayName, it.description)
                },
                activeAgentName = state.activeAgentName,
                onSelectAgent = { name -> store.dispatch(AppIntent.SelectAgent(name)) },
                addToChatConfig = AddToChatConfig(
                    activePalette = state.themePrefs.palette,
                    activeMode = state.themePrefs.mode,
                    connectedIntegrationsCount = enabledIntegrationIds.size,
                    miniApps = miniApps,
                    onSelectPalette = { p -> store.dispatch(AppIntent.SetPalette(p)) },
                    onSelectMode = { m -> store.dispatch(AppIntent.SetThemeMode(m)) },
                    onShowPersonas = { store.dispatch(AppIntent.Navigate(Screen.Personas)) },
                    onShowIntegrations = { store.dispatch(AppIntent.Navigate(Screen.Integrations)) },
                    onShowMiniApps = { store.dispatch(AppIntent.Navigate(Screen.MiniApps)) },
                    onInvokeMiniApp = { miniApp ->
                        store.dispatch(
                            AppIntent.InvokeMiniApp(
                                miniAppId = miniApp.id,
                                triggerPrompt = miniApp.triggerPrompt,
                                cachedRenderTreeJson = miniApp.lastRenderTreeJson,
                            ),
                        )
                    },
                    onAddMiniApp = { name, emoji, prompt ->
                        miniAppsScope.launch {
                            miniAppsRepo.add(name, emoji, prompt)
                        }
                    },
                ),
            )
        }
    }
}

private fun openAppDetailsSettings(context: Context) {
    val intent = android.content.Intent(
        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
    ).apply {
        data = android.net.Uri.fromParts("package", context.packageName, null)
        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}

private fun restartProcess(context: Context) {
    val pm = context.packageManager
    val launchIntent = pm.getLaunchIntentForPackage(context.packageName) ?: return
    launchIntent.addFlags(
        android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK,
    )
    context.startActivity(launchIntent)
    Runtime.getRuntime().exit(0)
}
