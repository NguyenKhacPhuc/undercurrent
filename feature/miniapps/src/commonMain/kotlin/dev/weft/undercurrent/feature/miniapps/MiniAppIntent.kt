package dev.weft.undercurrent.feature.miniapps

import dev.weft.undercurrent.core.domain.UiRenderEvent

sealed interface MiniAppIntent {

    data class InvokeMiniApp(
        val miniAppId: String,
        val triggerPrompt: String,
        val cachedRenderTreeJson: String? = null,
    ) : MiniAppIntent

    data class UiBridgeUpdate(val event: UiRenderEvent?) : MiniAppIntent

    /** The user approved a mini-app's first-run consent prompt. */
    data class ApproveConsent(val miniAppId: String) : MiniAppIntent

    /** The user denied a mini-app's first-run consent prompt. */
    data class DenyConsent(val miniAppId: String) : MiniAppIntent
}
