package dev.weft.undercurrent.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import dev.weft.undercurrent.app.AppViewModel
import dev.weft.undercurrent.app.PlatformAdapter
import dev.weft.undercurrent.core.designsystem.colors
import dev.weft.undercurrent.core.ext.openInBrowser
import dev.weft.undercurrent.core.model.ThemeMode
import dev.weft.undercurrent.core.navigation.NavigationIntent
import dev.weft.undercurrent.core.navigation.NavigationViewModel
import dev.weft.undercurrent.core.navigation.Screen
import dev.weft.undercurrent.core.resources.Res
import dev.weft.undercurrent.core.resources.mini_app_preview_placeholder
import dev.weft.undercurrent.feature.chat.ChatRoute
import dev.weft.undercurrent.feature.chat.components.DisplayRole
import dev.weft.undercurrent.feature.creator.CreatorIntent
import dev.weft.undercurrent.feature.creator.CreatorScreen
import dev.weft.undercurrent.feature.creator.CreatorViewModel
import dev.weft.undercurrent.feature.miniapps.MiniAppIntent
import dev.weft.undercurrent.feature.miniapps.MiniAppViewModel
import dev.weft.undercurrent.feature.miniapps.MiniAppsScreen
import dev.weft.undercurrent.feature.miniapps.SaveAsMiniAppDialog
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.stringResource
import org.koin.android.ext.android.get
import org.koin.compose.koinInject

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        intent?.data?.let { uri -> get<OAuthCallbackChannel>().submit(uri) }
        setContent { AndroidApp() }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        intent.data?.let { uri -> get<OAuthCallbackChannel>().submit(uri) }
    }
}

@Composable
private fun AndroidApp() {
    val store: AppViewModel = koinInject()
    val navigationVm: NavigationViewModel = koinInject()
    val miniAppVm: MiniAppViewModel = koinInject()
    val creatorVm: CreatorViewModel = koinInject()
    val runtime: WeftRuntime = koinInject()
    val uiBridge: ComposeUiBridge = koinInject()
    val weftUi: WeftUi = koinInject()
    // Scope for forwarding rendered-surface UI actions to the AppViewModel.
    val uiEventScope = rememberCoroutineScope()
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
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("message", text))
    }

    var saveFromRenderDraft by remember { mutableStateOf<String?>(null) }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { /* nothing to do — banner removed; silent denial is fine */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val platform = remember {
        PlatformAdapter(
            chatRoute = {
                ChatRoute(
                    onOpenUrl = onOpenUrl,
                    onCopyText = onCopyText,
                )
            },
            renderedTreeRoute = {
                AgentRenderedTreeScreen(
                    uiBridge = uiBridge,
                    registry = weftUi.componentRegistry,
                    onAction = { action, label, fields ->
                        uiEventScope.launch {
                            store.sendUiEvent(action, label, fields)
                        }
                    },
                    onBack = {
                        uiBridge.clearLastUpdate()
                        navigationVm.dispatch(NavigationIntent.Navigate(Screen.Chat))
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
                                Text(stringResource(Res.string.mini_app_preview_placeholder))
                            }
                        }
                    },
                    onBack = { navigationVm.dispatch(NavigationIntent.Navigate(Screen.Chat)) },
                    onStartCreator = {
                        creatorVm.dispatch(
                            CreatorIntent.StartCreator(
                                dev.weft.undercurrent.feature.creator.CreatorKind.MiniApp,
                            ),
                        )
                    },
                    onOpenMiniApp = { miniApp ->
                        miniAppVm.dispatch(
                            MiniAppIntent.InvokeMiniApp(
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
                    onCancel = { creatorVm.dispatch(CreatorIntent.CancelCreator) },
                    body = {
                        AgentRenderedTreeScreen(
                            uiBridge = uiBridge,
                            registry = weftUi.componentRegistry,
                            onAction = { action, label, fields ->
                                uiEventScope.launch {
                                    store.sendUiEvent(action, label, fields)
                                }
                            },
                            onBack = { creatorVm.dispatch(CreatorIntent.CancelCreator) },
                            dataSources = runtime.dataSources,
                        )
                    },
                )
            },
            onOpenUrl = onOpenUrl,
            onRestartProcess = { restartProcess(context) },
            onOpenAppDetailsSettings = { openAppDetailsSettings(context) },
        )
    }

    BackHandler(enabled = state.screen != Screen.Chat) {
        if (state.screen is Screen.RenderedTree) {
            uiBridge.clearLastUpdate()
        }
        navigationVm.dispatch(NavigationIntent.Back)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        App(store = store, platform = platform) {
            PendingRequestRenderer(uiBridge)
            WeftOverlayHost(uiBridge)
            saveFromRenderDraft?.let { draft ->
                SaveAsMiniAppDialog(
                    initial = null,
                    suggestedPrompt = draft,
                    onDismiss = { saveFromRenderDraft = null },
                    onSave = { name, emoji, triggerPrompt ->
                        saveFromRenderDraft = null
                        miniAppVm.dispatch(
                            MiniAppIntent.SaveCurrentRenderAsMiniApp(
                                name = name,
                                emoji = emoji,
                                triggerPrompt = triggerPrompt,
                                renderedTree = (uiBridge.lastUpdate as? UIUpdate.RenderTree)?.tree,
                            ),
                        )
                    },
                )
            }
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
