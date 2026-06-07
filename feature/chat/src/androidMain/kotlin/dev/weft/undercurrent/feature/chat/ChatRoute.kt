package dev.weft.undercurrent.feature.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import dev.weft.undercurrent.core.navigation.NavigationIntent
import dev.weft.undercurrent.core.navigation.NavigationViewModel
import dev.weft.undercurrent.core.navigation.Screen
import dev.weft.undercurrent.core.resources.Res
import dev.weft.undercurrent.core.resources.app_name
import dev.weft.undercurrent.core.ui.components.AppDrawer
import dev.weft.undercurrent.feature.miniapps.MiniAppIntent
import dev.weft.undercurrent.feature.miniapps.MiniAppViewModel
import dev.weft.undercurrent.feature.theme.ThemeIntent
import dev.weft.undercurrent.feature.theme.ThemeViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun ChatRoute(
    onOpenUrl: (String) -> Unit,
    onCopyText: (String) -> Unit,
) {
    val chatVm: ChatViewModel = koinInject()
    val shellVm: ChatShellViewModel = koinInject()
    val themeVm: ThemeViewModel = koinInject()
    val navigationVm: NavigationViewModel = koinInject()
    val miniAppVm: MiniAppViewModel = koinInject()

    val chatState by chatVm.state.collectAsState()
    val themeState by themeVm.state.collectAsState()
    val shellState by shellVm.state.collectAsState()

    val agentCurrentConvId = chatState.currentConversationId.orEmpty()
    val conversations = shellState.conversations

    val threadTitle = shellState.conversationTitle(chatState.currentConversationId)
        ?: stringResource(Res.string.app_name)
    val personaLabel = shellState.personaLabel
    val threadSubtitle = shellState.threadSubtitle

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED,
        )
    }
    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> hasMicPermission = granted }

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
                onSelect = { id -> closeAnd { chatVm.dispatch(ChatIntent.SelectConversation(id)) } },
                onNewChat = { closeAnd { chatVm.dispatch(ChatIntent.NewChat) } },
                onDeleteConversation = { id -> chatVm.dispatch(ChatIntent.DeleteConversation(id)) },
                onShowAllConversations = {
                    closeAnd { navigationVm.dispatch(NavigationIntent.Navigate(Screen.Conversations)) }
                },
                onShowPersonas = {
                    closeAnd { navigationVm.dispatch(NavigationIntent.Navigate(Screen.Personas)) }
                },
                onShowMiniApps = {
                    closeAnd { navigationVm.dispatch(NavigationIntent.Navigate(Screen.MiniApps)) }
                },
                onShowMemories = {
                    closeAnd { navigationVm.dispatch(NavigationIntent.Navigate(Screen.Memories)) }
                },
                onShowTraces = {
                    closeAnd { navigationVm.dispatch(NavigationIntent.Navigate(Screen.Traces)) }
                },
                onShowSettings = {
                    closeAnd { navigationVm.dispatch(NavigationIntent.Navigate(Screen.Settings)) }
                },
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ChatScreen(
                header = ChatHeaderConfig(
                    threadTitle = threadTitle,
                    threadSubtitle = threadSubtitle,
                    onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                    onNewChat = { chatVm.dispatch(ChatIntent.NewChat) },
                    onDeleteThread = { chatVm.dispatch(ChatIntent.DeleteCurrentConversation) },
                ),
                messages = ChatMessagesConfig(
                    displayMessages = chatVm.displayMessages,
                    inFlight = chatState.inFlight,
                    lastError = chatState.lastError,
                    activePersonaName = personaLabel,
                    onCopyText = onCopyText,
                    onOpenUrl = onOpenUrl,
                    onRegenerate = { chatVm.dispatch(ChatIntent.RegenerateLast) },
                ),
                input = ChatInputConfig(
                    voiceState = shellState.voiceState,
                    voiceRms = shellVm.voiceRms,
                    voiceAvailable = shellVm.voiceAvailable,
                    hasMicPermission = hasMicPermission,
                    onRequestMicPermission = {
                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onSend = { text, tier -> chatVm.dispatch(ChatIntent.SendChat(text, tier)) },
                    onStartListening = { shellVm.dispatch(ChatShellIntent.StartListening) },
                    onStopListening = { shellVm.dispatch(ChatShellIntent.StopListening) },
                    onCancelListening = { shellVm.dispatch(ChatShellIntent.CancelListening) },
                    onAcknowledgeVoice = { shellVm.dispatch(ChatShellIntent.AcknowledgeVoice) },
                    onStop = { chatVm.dispatch(ChatIntent.StopResponse) },
                ),
                agent = ChatAgentConfig(
                    agents = chatState.availableAgents.map {
                        AgentOption(it.name, it.displayName, it.description)
                    },
                    activeAgentName = chatState.activeAgentName,
                    onSelectAgent = { name -> chatVm.dispatch(ChatIntent.SelectAgent(name)) },
                    defaultTier = shellState.defaultTier,
                ),
                addToChatConfig = AddToChatConfig(
                    activePalette = themeState.prefs.palette,
                    activeMode = themeState.prefs.mode,
                    connectedIntegrationsCount = shellState.connectedIntegrationsCount,
                    miniApps = shellState.miniApps,
                    onSelectPalette = { p -> themeVm.dispatch(ThemeIntent.SetPalette(p)) },
                    onSelectMode = { m -> themeVm.dispatch(ThemeIntent.SetThemeMode(m)) },
                    onShowPersonas = { navigationVm.dispatch(NavigationIntent.Navigate(Screen.Personas)) },
                    onShowIntegrations = {
                        navigationVm.dispatch(NavigationIntent.Navigate(Screen.Integrations))
                    },
                    onShowMiniApps = { navigationVm.dispatch(NavigationIntent.Navigate(Screen.MiniApps)) },
                    onInvokeMiniApp = { miniApp ->
                        miniAppVm.dispatch(
                            MiniAppIntent.InvokeMiniApp(
                                miniAppId = miniApp.id,
                                triggerPrompt = miniApp.triggerPrompt,
                                cachedRenderTreeJson = miniApp.lastRenderTreeJson,
                            ),
                        )
                    },
                    onAddMiniApp = { name, emoji, prompt ->
                        shellVm.dispatch(ChatShellIntent.AddMiniApp(name, emoji, prompt))
                    },
                ),
            )
        }
    }
}
