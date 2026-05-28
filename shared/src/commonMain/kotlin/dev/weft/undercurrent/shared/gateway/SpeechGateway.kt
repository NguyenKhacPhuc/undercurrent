package dev.weft.undercurrent.shared.gateway

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
public interface SpeechGateway {

    /** Whether the platform has a recognition service. False on iOS for v1. */
    public val isAvailable: Boolean

    /** Reactive recognizer state — drives the mic UI. */
    public val state: StateFlow<VoiceState>

    /**
     * Live audio-level RMS in dB while listening; 0 when idle. Compose
     * waveform components read off this. Updated at ~30Hz on Android.
     */
    public val rmsdB: StateFlow<Float>

    /** Begin listening. Requires the platform mic permission. */
    public fun start(language: String? = null)

    /** Stop recording and finalize. Next state: [VoiceState.Final] or [VoiceState.Error]. */
    public fun stop()

    /** Abort recording — partials discarded. */
    public fun cancel()

    /** Acknowledge a terminal result and reset to [VoiceState.Idle]. */
    public fun acknowledge()
}

public sealed interface VoiceState {
    public data object Idle : VoiceState
    public data object Listening : VoiceState
    public data class Partial(val text: String) : VoiceState
    public data class Final(val text: String) : VoiceState
    public data class Error(val message: String) : VoiceState
}
