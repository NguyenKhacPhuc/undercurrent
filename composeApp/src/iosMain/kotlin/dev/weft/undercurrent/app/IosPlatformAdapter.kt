package dev.weft.undercurrent.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.weft.undercurrent.data.datastore.PersonaRepository
import dev.weft.undercurrent.feature.chat.ChatScreen
import dev.weft.undercurrent.shared.gateway.SpeechGateway
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.compose.koinInject
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIPasteboard

/**
 * iOS [PlatformAdapter]. Wires:
 *  - **chatRoute** to the real commonMain `ChatScreen` against
 *    `IosAppStore.displayMessages` + the Anthropic client. Single-
 *    conversation, no drawer (drawer + multi-thread is Phase 2).
 *  - **renderedTreeRoute / miniAppsRoute / creatorRoute** to
 *    "coming to iOS" placeholders — these need `ui_render` payloads
 *    from an agent that emits them, which iOS won't have until tools
 *    land.
 *  - **OS bridges** to real UIApplication / UIPasteboard calls.
 */
public fun iosPlatformAdapter(): PlatformAdapter = PlatformAdapter(
    chatRoute = { IosChatRoute() },
    renderedTreeRoute = { IosPlaceholder(label = "Rendered tree") },
    miniAppsRoute = { IosPlaceholder(label = "Mini apps") },
    creatorRoute = { IosPlaceholder(label = "Creator") },
    onOpenUrl = { url -> openUrl(url) },
    onCopyText = { text -> UIPasteboard.generalPasteboard.string = text },
    // iOS doesn't have a "restart the process" affordance — the
    // Integrations screen's onRestart is unused there since
    // StubOAuthGateway never connects anything anyway.
    onRestartProcess = { /* no-op on iOS */ },
    onOpenAppDetailsSettings = { openUrl(UIApplicationOpenSettingsURLString) },
    onOpenSaveDialog = { /* mini-app save dialog lands with iOS mini-apps */ },
)

@Composable
private fun IosChatRoute() {
    val store: AppStore = koinInject()
    val speech: SpeechGateway = koinInject()
    val personaRepo: PersonaRepository = koinInject()
    val state by store.state.collectAsState()
    val activeVoice by personaRepo.activeVoice.collectAsState()
    val activeRole by personaRepo.activeRole.collectAsState()

    // Persona label: voice + role joined. Default voice + no role → "Default".
    val personaLabel = run {
        val voiceLabel = activeVoice.name.takeIf { it != "Default" }
        val roleLabel = activeRole?.name
        listOfNotNull(voiceLabel, roleLabel).joinToString(" + ").ifEmpty { "Default" }
    }
    val providerLabel = state.activeProvider.displayName

    ChatScreen(
        displayMessages = store.displayMessages,
        inFlight = state.chat.inFlight,
        lastError = state.chat.lastError,
        onSend = { text, _ -> store.dispatch(AppIntent.SendChat(text)) },
        defaultTier = null,
        threadTitle = "Undercurrent",
        threadSubtitle = "$providerLabel · $personaLabel",
        activePersonaName = personaLabel,
        lastModelId = "claude-haiku-4-5",
        degradedMode = null,
        speechGateway = speech,
        // iOS voice deferred — IosSpeechGateway reports
        // isAvailable=false until the AVAudioSession.setActive
        // cinterop resolution is sorted out. Mic CTA hidden via the
        // gateway's availability flag; this flag is informational.
        hasMicPermission = false,
        onRequestMicPermission = { /* iOS voice deferred */ },
        onCopyText = { text -> UIPasteboard.generalPasteboard.string = text },
        onOpenUrl = { url -> openUrl(url) },
        // No native drawer composable on iOS — the hamburger tap
        // routes to the Conversations list screen (commonMain) which
        // shows all persisted threads and supports switching /
        // deleting. The user taps a row → ConversationsListScreen
        // dispatches SelectConversation, which hydrates from SQLDelight.
        onOpenDrawer = {
            store.dispatch(AppIntent.Navigate(dev.weft.undercurrent.core.navigation.Screen.Conversations))
        },
        onNewChat = { store.dispatch(AppIntent.NewChat) },
        onDeleteThread = { store.dispatch(AppIntent.NewChat) },
        onRegenerate = { store.dispatch(AppIntent.RegenerateLast) },
        skills = null,
        addToChatConfig = null,
        agents = emptyList(),
        activeAgentName = state.activeAgentName,
        onSelectAgent = { },
    )
}

@Composable
private fun IosPlaceholder(label: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("$label — coming to iOS")
    }
}

/**
 * Open a URL via UIApplication. Async by nature; we don't await the
 * completion handler because the caller doesn't care about the result.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun openUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    if (UIApplication.sharedApplication.canOpenURL(nsUrl)) {
        UIApplication.sharedApplication.openURL(
            url = nsUrl,
            options = emptyMap<Any?, Any>(),
            completionHandler = null,
        )
    }
}
