package dev.weft.undercurrent.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.weft.undercurrent.core.model.ProviderKind
import dev.weft.undercurrent.core.domain.MiniAppsRepository
import dev.weft.undercurrent.core.domain.PersonaRepository
import dev.weft.undercurrent.feature.chat.AddToChatConfig
import dev.weft.undercurrent.feature.chat.ChatScreen
import dev.weft.undercurrent.shared.gateway.SpeechGateway
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
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
fun iosPlatformAdapter(): PlatformAdapter = PlatformAdapter(
    chatRoute = { IosChatRoute() },
    renderedTreeRoute = { IosPlaceholder(label = "Rendered tree") },
    miniAppsRoute = { IosMiniAppsRoute() },
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
    val miniAppsRepo: MiniAppsRepository = koinInject()
    val miniAppsScope = rememberCoroutineScope()
    val state by store.state.collectAsState()
    val activeVoice by personaRepo.activeVoice.collectAsState()
    val activeRole by personaRepo.activeRole.collectAsState()
    val miniApps by miniAppsRepo.miniApps.collectAsState()

    // Persona label: voice + role joined. Default voice + no role → "Default".
    val personaLabel = run {
        val voiceLabel = activeVoice.name.takeIf { it != "Default" }
        val roleLabel = activeRole?.name
        listOfNotNull(voiceLabel, roleLabel).joinToString(" + ").ifEmpty { "Default" }
    }
    // Pinned model per provider — mirrors the IDs hardcoded in the
    // LlmClient factories (composeApp/iosMain/.../app/llm/*). When iOS
    // gets a per-provider model picker (Phase 3+), source this from
    // ModelPrefsRepository instead.
    val modelLabel = when (state.activeProvider) {
        ProviderKind.Anthropic -> "Claude Haiku 4.5"
        ProviderKind.OpenAI -> "GPT-5 Mini"
        ProviderKind.OpenRouter -> "Sonnet 4.5 via OpenRouter"
        ProviderKind.DeepSeek -> "DeepSeek Chat"
    }

    ChatScreen(
        displayMessages = store.displayMessages,
        inFlight = state.chat.inFlight,
        lastError = state.chat.lastError,
        onSend = { text, _ -> store.dispatch(AppIntent.SendChat(text)) },
        defaultTier = null,
        threadTitle = "Undercurrent",
        threadSubtitle = "$modelLabel · $personaLabel",
        activePersonaName = personaLabel,
        lastModelId = when (state.activeProvider) {
            ProviderKind.Anthropic -> "claude-haiku-4-5"
            ProviderKind.OpenAI -> "gpt-5-mini"
            ProviderKind.OpenRouter -> "anthropic/claude-sonnet-4-5"
            ProviderKind.DeepSeek -> "deepseek-chat"
        },
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
        // The Add-to-Chat sheet ("+") gives iOS users access to:
        //  - Theme palette + mode toggles
        //  - Persona picker shortcut
        //  - Mini-apps row + invocation (text-only — no ui_render on iOS yet)
        //  - "Add a mini-app" affordance
        // Integrations row hidden because StubOAuthGateway can't actually
        // connect anything; connectedIntegrationsCount stays 0.
        addToChatConfig = AddToChatConfig(
            activePalette = state.themePrefs.palette,
            activeMode = state.themePrefs.mode,
            connectedIntegrationsCount = 0,
            miniApps = miniApps,
            onSelectPalette = { p -> store.dispatch(AppIntent.SetPalette(p)) },
            onSelectMode = { m -> store.dispatch(AppIntent.SetThemeMode(m)) },
            onShowPersonas = {
                store.dispatch(AppIntent.Navigate(dev.weft.undercurrent.core.navigation.Screen.Personas))
            },
            onShowIntegrations = {
                store.dispatch(AppIntent.Navigate(dev.weft.undercurrent.core.navigation.Screen.Integrations))
            },
            onShowMiniApps = {
                store.dispatch(AppIntent.Navigate(dev.weft.undercurrent.core.navigation.Screen.MiniApps))
            },
            onInvokeMiniApp = { miniApp ->
                store.dispatch(
                    AppIntent.InvokeMiniApp(
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
        agents = emptyList(),
        activeAgentName = state.activeAgentName,
        onSelectAgent = { },
    )
}

@Composable
private fun IosMiniAppsRoute() {
    val store: AppStore = koinInject()
    dev.weft.undercurrent.feature.miniapps.MiniAppsScreen(
        // No ui_render preview on iOS — show a "(no preview)" label.
        // The user still sees the mini-app card (name + emoji +
        // trigger prompt) and tapping invokes via SendChat.
        treePreview = { _, _ ->
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { Text("(no preview)") }
        },
        onBack = {
            store.dispatch(
                AppIntent.Navigate(dev.weft.undercurrent.core.navigation.Screen.Chat),
            )
        },
        onOpenMiniApp = { miniApp ->
            store.dispatch(
                AppIntent.InvokeMiniApp(
                    miniAppId = miniApp.id,
                    triggerPrompt = miniApp.triggerPrompt,
                    cachedRenderTreeJson = null,
                ),
            )
        },
        onStartCreator = {
            // Creator wizard requires the agent's `ui_render` tool to
            // pose QnA — not available on iOS yet. The "+ New" button
            // in MiniAppsScreen calls this; we surface an effect-style
            // message instead of letting the user wander into a
            // broken flow.
            //
            // TODO when iOS ui_render lands: dispatch StartCreator(MiniApp).
        },
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
