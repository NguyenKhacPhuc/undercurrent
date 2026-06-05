package dev.weft.undercurrent.feature.miniapps.internal

import dev.weft.android.WeftRuntime
import dev.weft.contracts.UIUpdate
import dev.weft.undercurrent.core.domain.MiniAppsRepository
import dev.weft.undercurrent.core.model.AppEffect
import dev.weft.undercurrent.core.model.AppState
import dev.weft.undercurrent.core.navigation.NavigationIntent
import dev.weft.undercurrent.core.navigation.NavigationViewModel
import dev.weft.undercurrent.core.navigation.Screen
import dev.weft.undercurrent.feature.miniapps.MiniAppIntent
import dev.weft.undercurrent.feature.miniapps.MiniAppViewModel
import dev.weft.undercurrent.feature.miniapps.htmlMiniAppRenderTree
import dev.weft.undercurrent.shared.mvi.MviContext
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Mini-app invocation + UI-bridge forwarding (Android impl).
 *
 * Takes [sendChat] as a callback rather than a [ChatViewModel] ref so
 * `:feature:miniapps/androidMain` doesn't depend on `:feature:chat`
 * (which would cycle — `:feature:chat` already depends on
 * `:feature:miniapps` for the intent + dialog types). The host's DI
 * module wires the lambda to `chatVm.send(...)` at construction time.
 */
public class WeftMiniAppViewModel(
    private val context: MviContext<AppState, AppEffect>,
    private val runtime: WeftRuntime,
    private val miniAppsRepo: MiniAppsRepository,
    private val navigationVm: NavigationViewModel,
    private val sendChat: suspend (String) -> Unit,
) : MiniAppViewModel {

    @Volatile
    private var activeInvocationId: String? = null

    override fun dispatch(intent: MiniAppIntent) {
        when (intent) {
            is MiniAppIntent.InvokeMiniApp -> context.scope.launch {
                handleInvoke(intent)
            }
            is MiniAppIntent.UiBridgeUpdate -> handleUiBridgeUpdate(intent)
        }
    }

    private suspend fun handleInvoke(intent: MiniAppIntent.InvokeMiniApp) {
        val miniApp = miniAppsRepo.miniApps.value.firstOrNull { it.id == intent.miniAppId }
        if (miniApp?.htmlDocument != null) {
            // Flexible (HTML) mini-app: render its saved document instantly
            // through the bridged Html component — no agent turn. The id in
            // the tree resolves the bridge's scope gate + state store.
            runtime.uiBridge.emit(UIUpdate.RenderTree(htmlMiniAppRenderTree(miniApp)))
            if (context.current.screen !is Screen.RenderedTree) {
                navigationVm.dispatch(NavigationIntent.Navigate(Screen.RenderedTree))
            }
            miniAppsRepo.recordUsage(intent.miniAppId)
            return
        }
        val cached = intent.cachedRenderTreeJson
        if (cached != null) {
            runCatching {
                val tree = Json.decodeFromString(
                    dev.weft.contracts.ComponentNode.serializer(),
                    cached,
                )
                runtime.uiBridge.emit(UIUpdate.RenderTree(tree))
            }
            if (context.current.screen !is Screen.RenderedTree) {
                navigationVm.dispatch(NavigationIntent.Navigate(Screen.RenderedTree))
            }
        }
        activeInvocationId = intent.miniAppId
        try {
            miniAppsRepo.recordUsage(intent.miniAppId)
            sendChat(intent.triggerPrompt)
        } finally {
            activeInvocationId = null
        }
    }

    private fun handleUiBridgeUpdate(intent: MiniAppIntent.UiBridgeUpdate) {
        val event = intent.event ?: return
        if (context.current.screen !is Screen.RenderedTree &&
            context.current.screen !is Screen.Creator
        ) {
            navigationVm.dispatch(NavigationIntent.Navigate(Screen.RenderedTree))
        }
        val miniAppId = activeInvocationId
        if (miniAppId != null) {
            val treeJson = TRACE_JSON.encodeToString(
                dev.weft.undercurrent.core.domain.ComponentNode.serializer(),
                event.tree,
            )
            context.scope.launch {
                runCatching { miniAppsRepo.setCachedRender(miniAppId, treeJson) }
            }
        }
    }

    private companion object {
        val TRACE_JSON = Json {
            prettyPrint = true
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    }
}
