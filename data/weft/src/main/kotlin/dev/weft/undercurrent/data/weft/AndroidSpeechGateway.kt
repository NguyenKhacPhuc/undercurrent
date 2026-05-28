package dev.weft.undercurrent.data.weft

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dev.weft.undercurrent.shared.gateway.SpeechGateway
import dev.weft.undercurrent.shared.gateway.VoiceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android impl of [SpeechGateway] backed by the platform
 * [SpeechRecognizer]. Reframes the raw `RecognitionListener` callback
 * API as observable state.
 *
 * Lifecycle: the recognizer holds an AIDL binding to a system service,
 * so this class is meant to be a singleton scoped to where it's used
 * (mic surface). [destroy] releases the binding.
 *
 * RECORD_AUDIO permission is the caller's responsibility — [start]
 * silently fails without it.
 */
public class AndroidSpeechGateway(context: Context) : SpeechGateway {

    private val appContext: Context = context.applicationContext
    private val recognizer: SpeechRecognizer? =
        if (SpeechRecognizer.isRecognitionAvailable(appContext)) {
            SpeechRecognizer.createSpeechRecognizer(appContext)
        } else {
            null
        }

    private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
    private val _rmsdB = MutableStateFlow(0f)

    override val state: StateFlow<VoiceState> = _state.asStateFlow()
    override val rmsdB: StateFlow<Float> = _rmsdB.asStateFlow()
    override val isAvailable: Boolean = recognizer != null

    init {
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.value = VoiceState.Listening
            }

            override fun onBeginningOfSpeech() = Unit

            override fun onRmsChanged(rmsdB: Float) {
                _rmsdB.value = rmsdB
            }

            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit

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
                if (!text.isNullOrEmpty()) {
                    _state.value = VoiceState.Partial(text)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
    }

    override fun start(language: String?) {
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
        _rmsdB.value = 0f
        _state.value = VoiceState.Listening
        r.startListening(intent)
    }

    override fun stop() {
        recognizer?.stopListening()
    }

    override fun cancel() {
        recognizer?.cancel()
        _rmsdB.value = 0f
        _state.value = VoiceState.Idle
    }

    override fun acknowledge() {
        _state.value = VoiceState.Idle
    }

    public fun destroy() {
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
