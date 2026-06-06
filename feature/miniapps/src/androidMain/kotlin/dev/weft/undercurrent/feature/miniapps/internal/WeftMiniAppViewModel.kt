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
import dev.weft.undercurrent.feature.miniapps.OfferableActions
import dev.weft.undercurrent.feature.miniapps.approvableScopes
import dev.weft.undercurrent.feature.miniapps.htmlMiniAppRenderTree
import dev.weft.undercurrent.feature.miniapps.needsConsent
import dev.weft.undercurrent.feature.miniapps.toConsentRequest
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
    private val offerable: OfferableActions,
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
            is MiniAppIntent.ApproveConsent -> context.scope.launch {
                resolveConsent(intent.miniAppId, approve = true)
            }
            is MiniAppIntent.DenyConsent -> context.scope.launch {
                resolveConsent(intent.miniAppId, approve = false)
            }
            is MiniAppIntent.SaveCurrentRenderAsMiniApp -> context.scope.launch {
                saveCurrentRender(intent)
            }
        }
    }

    /**
     * Persist the on-screen tree as a new mini-app: create the catalog
     * entry, then snapshot the rendered tree (if any) so the next open
     * paints instantly instead of re-running the agent.
     */
    private suspend fun saveCurrentRender(intent: MiniAppIntent.SaveCurrentRenderAsMiniApp) {
        val created = miniAppsRepo.add(intent.name, intent.emoji, intent.triggerPrompt)
        val tree = intent.renderedTree ?: return
        val json = Json.encodeToString(dev.weft.contracts.ComponentNode.serializer(), tree)
        miniAppsRepo.setCachedRender(created.id, json)
    }

    private suspend fun handleInvoke(intent: MiniAppIntent.InvokeMiniApp) {
        val miniApp = miniAppsRepo.miniApps.value.firstOrNull { it.id == intent.miniAppId }
        if (miniApp?.htmlDocument != null) {
            // First run with declared actions: ask before anything renders.
            if (miniApp.needsConsent()) {
                context.update { it.copy(pendingMiniAppConsent = miniApp.toConsentRequest(offerable)) }
                return
            }
            renderHtmlMiniApp(miniApp)
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

    /** Render a flexible HTML mini-app instantly via the bridged Html component. */
    private suspend fun renderHtmlMiniApp(miniApp: dev.weft.undercurrent.core.model.MiniApp) {
        runtime.uiBridge.emit(UIUpdate.RenderTree(htmlMiniAppRenderTree(miniApp)))
        if (context.current.screen !is Screen.RenderedTree) {
            navigationVm.dispatch(NavigationIntent.Navigate(Screen.RenderedTree))
        }
        miniAppsRepo.recordUsage(miniApp.id)
    }

    /**
     * Apply the user's consent decision: record the grant (the offerable
     * subset on approve, nothing on deny), dismiss the prompt, then render
     * the mini-app — which now runs scope-gated to exactly what they allowed.
     */
    private suspend fun resolveConsent(miniAppId: String, approve: Boolean) {
        val miniApp = miniAppsRepo.miniApps.value.firstOrNull { it.id == miniAppId }
        if (miniApp == null) {
            context.update { it.copy(pendingMiniAppConsent = null) }
            return
        }
        miniAppsRepo.recordConsent(
            miniAppId,
            if (approve) miniApp.approvableScopes(offerable) else emptySet(),
        )
        context.update { it.copy(pendingMiniAppConsent = null) }
        renderHtmlMiniApp(miniApp)
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
