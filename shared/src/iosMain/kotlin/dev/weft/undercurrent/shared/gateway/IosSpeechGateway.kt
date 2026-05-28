package dev.weft.undercurrent.shared.gateway

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * iOS [SpeechGateway] — stub-but-named for now.
 *
 * The Kotlin/Native binding for `AVAudioSession.setActive(_:error:)`
 * doesn't resolve cleanly through `platform.AVFAudio.*` in this
 * project's K/N toolchain (2.0.21) — three setActive overloads exist
 * in the klib (`setActive:error:`, `setActive:withOptions:error:`,
 * `setActive:withFlags:error:`) but the Kotlin compiler reports
 * "Unresolved reference 'setActive'" at the call site. Suspected
 * cause: K/N's selector-to-Kotlin-method mapping collides on the
 * three overloads or our project's source set doesn't fully expose
 * the AVFAudio Objective-C category. Worth a clean repro + a Kotlin
 * issue when we revisit.
 *
 * Until that's sorted out, the iOS mic CTA is hidden by setting
 * [isAvailable] to false in [IosPlatformAdapter] (`hasMicPermission`
 * remains true, but the chat surface gates the mic button on
 * `speechGateway.isAvailable` too). All four [SpeechGateway] entry
 * points no-op so the recognizer never gets exercised.
 *
 * Sketch of the real impl lives in git history (commit before this
 * revert) — `SFSpeechRecognizer.requestAuthorization` +
 * `SFSpeechAudioBufferRecognitionRequest` +
 * `AVAudioEngine.inputNode.installTapOnBus` — pick it up when the
 * audio-session binding is resolved.
 */
public class IosSpeechGateway : SpeechGateway {
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
