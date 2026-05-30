package dev.weft.undercurrent.feature.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import dev.weft.undercurrent.core.domain.MiniAppsRepository
import dev.weft.undercurrent.core.domain.PersonaRepository
import dev.weft.undercurrent.core.domain.ProviderPrefsRepository
import dev.weft.undercurrent.core.model.ProviderKind
import dev.weft.undercurrent.core.navigation.NavigationIntent
import dev.weft.undercurrent.core.navigation.NavigationViewModel
import dev.weft.undercurrent.core.navigation.Screen
import dev.weft.undercurrent.feature.miniapps.MiniAppIntent
import dev.weft.undercurrent.feature.miniapps.MiniAppViewModel
import dev.weft.undercurrent.feature.theme.ThemeIntent
import dev.weft.undercurrent.feature.theme.ThemeViewModel
import dev.weft.undercurrent.core.domain.SpeechRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
public fun IosChatRoute(
    onOpenUrl: (String) -> Unit,
    onCopyText: (String) -> Unit,
) {
    val chatVm: ChatViewModel = koinInject()
    val themeVm: ThemeViewModel = koinInject()
    val navigationVm: NavigationViewModel = koinInject()
    val miniAppVm: MiniAppViewModel = koinInject()
    val speech: SpeechRepository = koinInject()
    val personaRepo: PersonaRepository = koinInject()
    val providerPrefs: ProviderPrefsRepository = koinInject()
    val miniAppsRepo: MiniAppsRepository = koinInject()

    val chatState by chatVm.state.collectAsState()
    val themeState by themeVm.state.collectAsState()
    val activeProvider by providerPrefs.activeProvider.collectAsState()
    val activeVoice by personaRepo.activeVoice.collectAsState()
    val activeRole by personaRepo.activeRole.collectAsState()
    val miniApps by miniAppsRepo.miniApps.collectAsState()
    val miniAppsScope = rememberCoroutineScope()

    val personaLabel = run {
        val voiceLabel = activeVoice.name.takeIf { it != "Default" }
        val roleLabel = activeRole?.name
        listOfNotNull(voiceLabel, roleLabel).joinToString(" + ").ifEmpty { "Default" }
    }
    val modelLabel = when (activeProvider) {
        ProviderKind.Anthropic -> "Claude Haiku 4.5"
        ProviderKind.OpenAI -> "GPT-5 Mini"
        ProviderKind.OpenRouter -> "Sonnet 4.5 via OpenRouter"
        ProviderKind.DeepSeek -> "DeepSeek Chat"
    }
    val lastModelId = when (activeProvider) {
        ProviderKind.Anthropic -> "claude-haiku-4-5"
        ProviderKind.OpenAI -> "gpt-5-mini"
        ProviderKind.OpenRouter -> "anthropic/claude-sonnet-4-5"
        ProviderKind.DeepSeek -> "deepseek-chat"
    }

    ChatScreen(
        header = ChatHeaderConfig(
            threadTitle = "Undercurrent",
            threadSubtitle = "$modelLabel · $personaLabel",
            onOpenDrawer = {
                navigationVm.dispatch(NavigationIntent.Navigate(Screen.Conversations))
            },
            onNewChat = { chatVm.dispatch(ChatIntent.NewChat) },
            onDeleteThread = { chatVm.dispatch(ChatIntent.NewChat) },
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
            speechGateway = speech,
            hasMicPermission = false,
            onRequestMicPermission = { },
            onSend = { text, _ -> chatVm.dispatch(ChatIntent.SendChat(text)) },
        ),
        agent = ChatAgentConfig(
            activeAgentName = chatState.activeAgentName,
            lastModelId = lastModelId,
        ),
        addToChatConfig = AddToChatConfig(
            activePalette = themeState.prefs.palette,
            activeMode = themeState.prefs.mode,
            connectedIntegrationsCount = 0,
            miniApps = miniApps,
            onSelectPalette = { p -> themeVm.dispatch(ThemeIntent.SetPalette(p)) },
            onSelectMode = { m -> themeVm.dispatch(ThemeIntent.SetThemeMode(m)) },
            onShowPersonas = {
                navigationVm.dispatch(NavigationIntent.Navigate(Screen.Personas))
            },
            onShowIntegrations = {
                navigationVm.dispatch(NavigationIntent.Navigate(Screen.Integrations))
            },
            onShowMiniApps = {
                navigationVm.dispatch(NavigationIntent.Navigate(Screen.MiniApps))
            },
            onInvokeMiniApp = { miniApp ->
                miniAppVm.dispatch(
                    MiniAppIntent.InvokeMiniApp(
                        miniAppId = miniApp.id,
                        triggerPrompt = miniApp.triggerPrompt,
                        cachedRenderTreeJson = null,
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
