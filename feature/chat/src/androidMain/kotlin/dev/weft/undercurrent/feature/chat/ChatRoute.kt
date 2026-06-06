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
import dev.weft.undercurrent.core.domain.IntegrationsRepository
import dev.weft.undercurrent.core.domain.MiniAppsRepository
import dev.weft.undercurrent.core.domain.PersonaRepository
import dev.weft.undercurrent.core.domain.ProviderPrefsRepository
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
import dev.weft.undercurrent.core.domain.ConversationStoreRepository
import dev.weft.undercurrent.core.domain.SpeechRepository
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun ChatRoute(
    onOpenUrl: (String) -> Unit,
    onCopyText: (String) -> Unit,
) {
    val chatVm: ChatViewModel = koinInject()
    val themeVm: ThemeViewModel = koinInject()
    val navigationVm: NavigationViewModel = koinInject()
    val miniAppVm: MiniAppViewModel = koinInject()
    val speechGateway: SpeechRepository = koinInject()
    val conversationStore: ConversationStoreRepository = koinInject()
    val providerPrefs: ProviderPrefsRepository = koinInject()
    val personaRepo: PersonaRepository = koinInject()
    val integrationsRepo: IntegrationsRepository = koinInject()
    val miniAppsRepo: MiniAppsRepository = koinInject()

    val chatState by chatVm.state.collectAsState()
    val themeState by themeVm.state.collectAsState()
    val activeProvider by providerPrefs.activeProvider.collectAsState()
    val defaultTier by providerPrefs.defaultTier.collectAsState()
    val activeVoice by personaRepo.activeVoice.collectAsState()
    val activeRole by personaRepo.activeRole.collectAsState()
    val miniApps by miniAppsRepo.miniApps.collectAsState()
    val enabledIntegrationIds by integrationsRepo.enabledIdsFlow.collectAsState(initial = emptySet())

    val agentCurrentConvId = chatState.currentConversationId.orEmpty()
    val conversations by remember { conversationStore.search("") }
        .collectAsState(initial = emptyList())

    val threadTitle = conversations
        .firstOrNull { it.id == agentCurrentConvId }
        ?.title
        ?.takeIf { it.isNotBlank() }
        ?: stringResource(Res.string.app_name)

    val personaLabel = run {
        val voiceLabel = activeVoice.name.takeIf { it != "Default" }
        val roleLabel = activeRole?.name
        listOfNotNull(voiceLabel, roleLabel).joinToString(" + ").ifEmpty { "Default" }
    }
    val threadSubtitle = listOf(activeProvider.displayName, personaLabel).joinToString(" · ")

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val miniAppsScope = rememberCoroutineScope()

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
                    speechGateway = speechGateway,
                    hasMicPermission = hasMicPermission,
                    onRequestMicPermission = {
                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onSend = { text, tier -> chatVm.dispatch(ChatIntent.SendChat(text, tier)) },
                    onStop = { chatVm.dispatch(ChatIntent.StopResponse) },
                ),
                agent = ChatAgentConfig(
                    agents = chatState.availableAgents.map {
                        AgentOption(it.name, it.displayName, it.description)
                    },
                    activeAgentName = chatState.activeAgentName,
                    onSelectAgent = { name -> chatVm.dispatch(ChatIntent.SelectAgent(name)) },
                    defaultTier = defaultTier,
                ),
                addToChatConfig = AddToChatConfig(
                    activePalette = themeState.prefs.palette,
                    activeMode = themeState.prefs.mode,
                    connectedIntegrationsCount = enabledIntegrationIds.size,
                    miniApps = miniApps,
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
                        miniAppsScope.launch {
                            miniAppsRepo.add(name, emoji, prompt)
                        }
                    },
                ),
            )
        }
    }
}
