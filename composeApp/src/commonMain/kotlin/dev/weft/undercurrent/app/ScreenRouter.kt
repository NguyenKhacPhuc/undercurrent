package dev.weft.undercurrent.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.weft.undercurrent.core.navigation.NavDisplay
import dev.weft.undercurrent.core.navigation.NavigationIntent
import dev.weft.undercurrent.core.navigation.NavigationViewModel
import dev.weft.undercurrent.core.navigation.Screen
import dev.weft.undercurrent.core.ui.LoadingPlaceholder
import dev.weft.undercurrent.feature.chat.ChatViewModel
import dev.weft.undercurrent.feature.conversations.ConversationsRoute
import dev.weft.undercurrent.feature.integrations.IntegrationsRoute
import dev.weft.undercurrent.feature.keypaste.KeyPasteRoute
import dev.weft.undercurrent.feature.memories.MemoriesRoute
import dev.weft.undercurrent.feature.onboarding.OnboardingRoute
import dev.weft.undercurrent.feature.personas.PersonasRoute
import dev.weft.undercurrent.feature.providers.ProvidersRoute
import dev.weft.undercurrent.feature.settings.SettingsRoute
import dev.weft.undercurrent.feature.auth.SignInRoute
import dev.weft.undercurrent.feature.theme.AppearanceRoute
import dev.weft.undercurrent.feature.traces.TracesRoute
import dev.weft.undercurrent.feature.usage.UsageRoute
import org.koin.compose.koinInject

@Composable
internal fun ScreenRouter(platform: PlatformAdapter) {
    val navigationVm: NavigationViewModel = koinInject()
    val appVm: AppViewModel = koinInject()
    NavDisplay(backStack = navigationVm.backStack) { entry ->
        when (entry) {
            Screen.Loading -> LoadingPlaceholder()
            Screen.SignIn -> SignInRoute(onSignedIn = { appVm.resume() })
            Screen.Onboarding -> OnboardingRoute()
            Screen.KeyPaste -> KeyPasteRoute(onOpenConsole = platform.onOpenUrl)
            Screen.Chat -> ChatGate { platform.chatRoute() }
            Screen.RenderedTree -> platform.renderedTreeRoute()
            Screen.Creator -> platform.creatorRoute()
            Screen.MiniApps -> platform.miniAppsRoute()
            Screen.Conversations -> ConversationsRoute()
            Screen.Traces -> TracesRoute()
            Screen.Memories -> MemoriesRoute()
            Screen.Settings -> SettingsRoute()
            Screen.Integrations -> IntegrationsRoute(onRestart = platform.onRestartProcess)
            Screen.Providers -> ProvidersRoute(onOpenConsole = platform.onOpenUrl)
            Screen.Appearance -> AppearanceRoute()
            Screen.Usage -> UsageRoute()
            Screen.Personas -> PersonasRoute()
        }
    }
}

@Composable
private fun ChatGate(content: @Composable () -> Unit) {
    val nav: NavigationViewModel = koinInject()
    val chat: ChatViewModel = koinInject()
    val chatState by chat.state.collectAsState()
    if (!chatState.agentReady) {
        LaunchedEffect(Unit) {
            nav.dispatch(NavigationIntent.Navigate(Screen.KeyPaste))
        }
    } else {
        content()
    }
}
