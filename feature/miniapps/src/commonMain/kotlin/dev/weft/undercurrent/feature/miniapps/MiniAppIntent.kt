package dev.weft.undercurrent.feature.miniapps

import dev.weft.undercurrent.core.domain.UiRenderEvent

sealed interface MiniAppIntent {

    data class InvokeMiniApp(
        val miniAppId: String,
        val triggerPrompt: String,
        val cachedRenderTreeJson: String? = null,
    ) : MiniAppIntent

    data class UiBridgeUpdate(val event: UiRenderEvent?) : MiniAppIntent
}
