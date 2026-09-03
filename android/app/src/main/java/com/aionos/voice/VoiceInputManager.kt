package com.aionos.voice

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.File

/**
 * On-device speech-to-text using Vosk.
 * Zero cloud dependency. Runs entirely offline.
 *
 * Requires: com.alphacephei:vosk-android:0.3.47
 * Model: Download from alphacephei.com/vosk/models to context.filesDir/vosk-model/
 */
class VoiceInputManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var speechService: SpeechService? = null
    private var recognizer: Recognizer? = null
    private var model: Model? = null

    private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val state: StateFlow<VoiceState> = _state

    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript

    fun initialize(modelPath: String = "${context.filesDir}/vosk-model"): Result<Unit> {
        return try {
            val modelDir = File(modelPath)
            if (!modelDir.exists()) {
                return Result.failure(IllegalStateException("Vosk model not found at $modelPath. Download from alphacephei.com/vosk/models"))
            }
            model = Model(modelPath)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun startListening() {
        val voskModel = model ?: run {
            _state.value = VoiceState.Error("Model not initialized")
            return
        }
        try {
            recognizer = Recognizer(voskModel, 16000.0f)
            speechService = SpeechService(recognizer, 16000.0f)
            speechService?.startListening(object : RecognitionListener {
                override fun onPartialResult(hypothesis: String?) {
                    hypothesis?.let { _transcript.value = parseResult(it) }
                }
                override fun onResult(hypothesis: String?) {
                    hypothesis?.let { _transcript.value = parseResult(it) }
                }
                override fun onFinalResult(hypothesis: String?) {
                    hypothesis?.let { _transcript.value = parseResult(it) }
                    _state.value = VoiceState.Idle
                }
                override fun onError(exception: Exception?) {
                    _state.value = VoiceState.Error(exception?.message ?: "Unknown error")
                }
                override fun onTimeout() {
                    _state.value = VoiceState.Idle
                }
            })
            _state.value = VoiceState.Listening
        } catch (e: Exception) {
            _state.value = VoiceState.Error(e.message ?: "Failed to start listening")
        }
    }

    fun stopListening() {
        speechService?.stop()
        speechService = null
        recognizer = null
        _state.value = VoiceState.Idle
    }

    fun destroy() {
        stopListening()
        model?.close()
        model = null
        scope.cancel()
    }

    private fun parseResult(json: String): String {
        return try {
            val regex = """"text"\s*:\s*"([^"]*)"""".toRegex()
            regex.find(json)?.groupValues?.get(1) ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    sealed class VoiceState {
        object Idle : VoiceState()
        object Listening : VoiceState()
        data class Error(val message: String) : VoiceState()
    }
}
