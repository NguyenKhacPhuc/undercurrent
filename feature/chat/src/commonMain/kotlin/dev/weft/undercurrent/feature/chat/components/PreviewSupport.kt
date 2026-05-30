package dev.weft.undercurrent.feature.chat.components

import dev.weft.undercurrent.core.domain.SpeechRepository
import dev.weft.undercurrent.core.domain.VoiceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal object PreviewSpeechGateway : SpeechRepository {
    override val isAvailable: Boolean = true
    override val state: StateFlow<VoiceState> = MutableStateFlow(VoiceState.Idle)
    override val rmsdB: StateFlow<Float> = MutableStateFlow(0f)
    override fun start(language: String?) = Unit
    override fun stop() = Unit
    override fun cancel() = Unit
    override fun acknowledge() = Unit
}
