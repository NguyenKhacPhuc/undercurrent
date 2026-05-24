package dev.weft.undercurrent.features.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Thin Kotlin wrapper around Android's [SpeechRecognizer]. Reframes the
 * raw `RecognitionListener` callback API as a [StateFlow] of [VoiceState]
 * so Compose can observe it reactively.
 *
 * Lifecycle: created when the chat surface mounts (per-Composition via
 * `remember`), destroyed in a `DisposableEffect` when it leaves. The
 * underlying `SpeechRecognizer` is fairly cheap to create but holds an
 * AIDL binding to a system service, so we keep it scoped to where it's
 * actually used rather than promoting it to Application.
 *
 * **Threading**: callbacks fire on the main thread; safe to update Compose
 * state directly from them.
 *
 * **Permission**: creating the recognizer does not require RECORD_AUDIO,
 * but [start] will silently fail without it. The caller is responsible
 * for requesting the permission before calling start.
 */
internal class VoiceRecognizer(context: Context) {

    private val appContext = context.applicationContext
    private val recognizer: SpeechRecognizer? =
        if (SpeechRecognizer.isRecognitionAvailable(appContext)) {
            SpeechRecognizer.createSpeechRecognizer(appContext)
        } else {
            null
        }

    private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val state: StateFlow<VoiceState> = _state.asStateFlow()

    /**
     * Current audio level in dB as reported by [RecognitionListener.onRmsChanged].
     * Updated at ~30Hz while listening. Speech typically lands in the
     * `-2.0 .. +10.0` range. UI consumers should normalize and clamp.
     *
     * Reset to 0 on [start] / [cancel] so the previous session's tail
     * doesn't bleed into the next visualization.
     */
    private val _rmsdB = MutableStateFlow(0f)
    val rmsdB: StateFlow<Float> = _rmsdB.asStateFlow()

    /** True if the device has a speech recognition service available at all. */
    val isAvailable: Boolean = recognizer != null

    init {
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.value = VoiceState.Listening
            }

            override fun onBeginningOfSpeech() {
                // No-op — `onReadyForSpeech` already moved us to Listening.
            }

            override fun onRmsChanged(rmsdB: Float) {
                _rmsdB.value = rmsdB
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Audio frame callback — unused; we just want transcripts.
            }

            override fun onEndOfSpeech() {
                // Recognizer keeps working to finalize results — wait
                // for onResults / onError before moving state.
            }

            override fun onError(error: Int) {
                _state.value = VoiceState.Error(errorMessage(error))
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                _state.value = VoiceState.Final(text)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (text.isNotEmpty()) {
                    _state.value = VoiceState.Partial(text)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // No-op.
            }
        })
    }

    /**
     * Start listening. Requires RECORD_AUDIO permission — the caller must
     * have requested it before calling this. Safe to call when already
     * listening (no-op).
     */
    fun start(language: String? = null) {
        val r = recognizer ?: return
        if (_state.value is VoiceState.Listening || _state.value is VoiceState.Partial) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            language?.let { putExtra(RecognizerIntent.EXTRA_LANGUAGE, it) }
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
        }
        // Optimistically move to Listening so the UI updates immediately;
        // onReadyForSpeech will (re)affirm shortly after.
        _rmsdB.value = 0f
        _state.value = VoiceState.Listening
        r.startListening(intent)
    }

    /** Stop recording and finalize. The next state will be [VoiceState.Final] or [VoiceState.Error]. */
    fun stop() {
        recognizer?.stopListening()
    }

    /** Abort recording and reset state — partials discarded. */
    fun cancel() {
        recognizer?.cancel()
        _rmsdB.value = 0f
        _state.value = VoiceState.Idle
    }

    /**
     * Reset to [VoiceState.Idle] after the caller has consumed a Final or
     * Error result. Compose-state-style — explicit acknowledgment so the
     * UI knows when to drop visual feedback.
     */
    fun acknowledge() {
        _state.value = VoiceState.Idle
    }

    fun destroy() {
        recognizer?.destroy()
    }

    private fun errorMessage(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio error"
        SpeechRecognizer.ERROR_CLIENT -> "Client error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission denied"
        SpeechRecognizer.ERROR_NETWORK -> "Network error"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
        SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy — try again"
        SpeechRecognizer.ERROR_SERVER -> "Server error"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard"
        else -> "Speech error ($code)"
    }
}

internal sealed interface VoiceState {
    data object Idle : VoiceState
    /** Recognizer is open and capturing audio; no transcript yet. */
    data object Listening : VoiceState
    /** Live in-progress transcript. Replaces itself as the user keeps speaking. */
    data class Partial(val text: String) : VoiceState
    /** Recording stopped, final transcript ready. Caller should pipe to input + call [VoiceRecognizer.acknowledge]. */
    data class Final(val text: String) : VoiceState
    /** Recording failed. Caller should surface the message + call [VoiceRecognizer.acknowledge]. */
    data class Error(val message: String) : VoiceState
}
