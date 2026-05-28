package dev.weft.undercurrent.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val state by store.state.collectAsState()

    ChatScreen(
        displayMessages = store.displayMessages,
        inFlight = state.chat.inFlight,
        lastError = state.chat.lastError,
        onSend = { text, _ -> store.dispatch(AppIntent.SendChat(text)) },
        defaultTier = null,
        threadTitle = "Undercurrent",
        threadSubtitle = "iOS · Claude Haiku 4.5",
        activePersonaName = "Default",
        lastModelId = "claude-haiku-4-5",
        degradedMode = null,
        speechGateway = speech,
        // Voice input not wired on iOS yet — StubSpeechGateway no-ops
        // so granting the permission wouldn't help. Hide the mic CTA.
        hasMicPermission = false,
        onRequestMicPermission = { /* no-op */ },
        onCopyText = { text -> UIPasteboard.generalPasteboard.string = text },
        onOpenUrl = { url -> openUrl(url) },
        // No drawer on iOS yet — multi-thread conversation history is
        // Phase 2. The onOpenDrawer callback fires when the user taps
        // the hamburger; tapping does nothing visible.
        onOpenDrawer = { /* no-op */ },
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
