package dev.weft.undercurrent.core.domain

import kotlinx.coroutines.flow.StateFlow

/**
 * Speech-to-text capture. The Android impl in `:data:weft` wraps the
 * platform `SpeechRecognizer`; iOS stub holds `Idle` indefinitely (a
 * proper iOS impl bridges to `SFSpeechRecognizer` later).
 *
 * The contract is "single in-flight recognition session":
 *   - [start] is a no-op while already listening.
 *   - [acknowledge] is the explicit handshake the UI uses to clear a
 *     terminal [VoiceState.Final] / [VoiceState.Error] back to
 *     [VoiceState.Idle] after consuming the result.
 *
 * [isAvailable] is false on devices without a recognition service (and
 * always false on the iOS stub).
 */
interface SpeechRepository {

    /** Whether the platform has a recognition service. False on iOS for v1. */
    val isAvailable: Boolean

    /** Reactive recognizer state — drives the mic UI. */
    val state: StateFlow<VoiceState>

    /**
     * Live audio-level RMS in dB while listening; 0 when idle. Compose
     * waveform components read off this. Updated at ~30Hz on Android.
     */
    val rmsdB: StateFlow<Float>

    /** Begin listening. Requires the platform mic permission. */
    fun start(language: String? = null)

    /** Stop recording and finalize. Next state: [VoiceState.Final] or [VoiceState.Error]. */
    fun stop()

    /** Abort recording — partials discarded. */
    fun cancel()

    /** Acknowledge a terminal result and reset to [VoiceState.Idle]. */
    fun acknowledge()
}

sealed interface VoiceState {
    data object Idle : VoiceState
    data object Listening : VoiceState
    data class Partial(val text: String) : VoiceState
    data class Final(val text: String) : VoiceState
    data class Error(val message: String) : VoiceState
}
