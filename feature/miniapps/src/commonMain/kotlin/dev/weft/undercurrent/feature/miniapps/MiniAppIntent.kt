package dev.weft.undercurrent.feature.miniapps

import dev.weft.contracts.ComponentNode
import dev.weft.undercurrent.core.domain.UiRenderEvent

sealed interface MiniAppIntent {

    data class InvokeMiniApp(
        val miniAppId: String,
        val triggerPrompt: String,
        val cachedRenderTreeJson: String? = null,
    ) : MiniAppIntent

    data class UiBridgeUpdate(val event: UiRenderEvent?) : MiniAppIntent

    /**
     * Persist the currently-rendered tree as a new, re-invocable mini-app.
     * [renderedTree] is whatever is on screen (read from the UI bridge by
     * the caller); null skips the cached-render snapshot and the mini-app
     * re-runs the agent on first open instead.
     */
    data class SaveCurrentRenderAsMiniApp(
        val name: String,
        val emoji: String,
        val triggerPrompt: String,
        val renderedTree: ComponentNode? = null,
    ) : MiniAppIntent

    /** The user approved a mini-app's first-run consent prompt. */
    data class ApproveConsent(val miniAppId: String) : MiniAppIntent

    /** The user denied a mini-app's first-run consent prompt. */
    data class DenyConsent(val miniAppId: String) : MiniAppIntent
}
