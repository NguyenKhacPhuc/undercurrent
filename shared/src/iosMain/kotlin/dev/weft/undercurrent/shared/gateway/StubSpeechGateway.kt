package dev.weft.undercurrent.shared.gateway

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * iOS stub. A proper impl bridges to `SFSpeechRecognizer` — for v1 the
 * mic button stays disabled because [isAvailable] is false.
 */
public class StubSpeechGateway : SpeechGateway {
    override val isAvailable: Boolean = false
    override val state: StateFlow<VoiceState> =
        MutableStateFlow<VoiceState>(VoiceState.Idle).asStateFlow()
    override val rmsdB: StateFlow<Float> =
        MutableStateFlow(0f).asStateFlow()

    override fun start(language: String?) = Unit
    override fun stop() = Unit
    override fun cancel() = Unit
    override fun acknowledge() = Unit
}
