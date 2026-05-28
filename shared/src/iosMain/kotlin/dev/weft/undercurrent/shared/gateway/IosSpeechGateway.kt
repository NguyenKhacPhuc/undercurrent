package dev.weft.undercurrent.shared.gateway

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * iOS [SpeechGateway] — stub-but-named for now.
 *
 * Root cause for the stub: Kotlin/Native 2.0.21's `platform.AVFAudio`
 * bindings against the iOS 26.4 SDK (Xcode 26.4 beta) don't expose
 * `AVAudioSession.setActive(_:error:)` to Kotlin. The selectors
 * appear in the .knm strings dump but the compiler reports
 * "Unresolved reference 'setActive'" for every call shape tried —
 * positional, named, single-arg, three-arg, memScoped with
 * `ObjCObjectVar<NSError?>` pointers. Same for `setCategory` /
 * `setMode`. Suspect an SDK-API change in iOS 26.4 that K/N's
 * commonized bindings don't yet handle.
 *
 * Two reasonable fixes (whichever comes first):
 *  1. Upgrade Kotlin to a version whose K/N has refreshed
 *     iOS 26.4 SDK bindings.
 *  2. Add a custom cinterop .def to explicitly bind the
 *     SFSpeechRecognizer + AVAudioSession surface we need,
 *     sidestepping the project-wide bindings.
 *
 * Scaffolding (SFSpeechRecognizer.requestAuthorization +
 * recognitionTaskWithRequest + audio tap) lives in commit
 * `7607d2d`'s git history — pick up there once the binding issue
 * resolves.
 *
 * Until then: `isAvailable` is false, the chat surface hides the
 * mic CTA, and every method no-ops.
 *
 * Info.plist already has `NSSpeechRecognitionUsageDescription` +
 * `NSMicrophoneUsageDescription` so finishing later doesn't need an
 * Xcode round-trip.
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
