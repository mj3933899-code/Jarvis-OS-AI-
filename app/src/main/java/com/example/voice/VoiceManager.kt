package com.example.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.example.model.AssistantState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceManager(
    private val context: Context,
    private val voiceMatchManager: VoiceMatchManager
) {
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListeningState = MutableStateFlow(false)
    val isListeningState: StateFlow<Boolean> = _isListeningState.asStateFlow()

    private val _isMicrophoneAvailable = MutableStateFlow(false)
    val isMicrophoneAvailable: StateFlow<Boolean> = _isMicrophoneAvailable.asStateFlow()

    private val _rmsDb = MutableStateFlow(0.0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private val _lastRecognizedText = MutableStateFlow("")
    val lastRecognizedText: StateFlow<String> = _lastRecognizedText.asStateFlow()

    private val _voiceError = MutableStateFlow<String?>(null)
    val voiceError: StateFlow<String?> = _voiceError.asStateFlow()

    private val _wakeWordModeStatus = MutableStateFlow("Tap to Talk (No native DSP wake-word engine bundled)")
    val wakeWordModeStatus: StateFlow<String> = _wakeWordModeStatus.asStateFlow()

    private val audioBuffer = mutableListOf<Float>()

    var isTtsSpeakingProvider: (() -> Boolean)? = null
    var onInterruptTtsRequested: (() -> Unit)? = null

    var onSpeechRecognized: ((String) -> Unit)? = null
    var onWakeWordDetected: (() -> Unit)? = null
    var onEmergencyStop: (() -> Unit)? = null
    var onJarvisOnCommand: (() -> Unit)? = null
    var onJarvisOffCommand: (() -> Unit)? = null
    var onPauseCommand: (() -> Unit)? = null
    var onResumeCommand: (() -> Unit)? = null
    var onCancelCommand: (() -> Unit)? = null
    var onStateChanged: ((AssistantState) -> Unit)? = null

    init {
        checkAvailability()
    }

    fun checkAvailability() {
        val available = try {
            SpeechRecognizer.isRecognitionAvailable(context)
        } catch (_: Exception) {
            false
        }
        _isMicrophoneAvailable.value = available
    }

    fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getRecentAudioFeatures(): FloatArray {
        return if (audioBuffer.isNotEmpty()) audioBuffer.toFloatArray() else floatArrayOf(0.5f, 0.6f, 0.4f, 0.7f)
    }

    fun startListening(language: String = "bn", userInitiatedTap: Boolean = true) {
        if (_isListeningState.value) return

        if (!hasRecordAudioPermission()) {
            _voiceError.value = "Microphone permission (RECORD_AUDIO) is not granted."
            onStateChanged?.invoke(AssistantState.ERROR)
            return
        }

        // Prevent TTS feedback loop where JARVIS hears its own speech
        if (isTtsSpeakingProvider?.invoke() == true) {
            if (userInitiatedTap) {
                onInterruptTtsRequested?.invoke()
            } else {
                return
            }
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _voiceError.value = "Android SpeechRecognizer service is not available on this device."
            onStateChanged?.invoke(AssistantState.ERROR)
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                if (language == "bn") {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
                } else {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
                }
            }

            speechRecognizer?.startListening(intent)
            _isListeningState.value = true
            _wakeWordModeStatus.value = "Session Recognizer Listening (bn-BD / en-US)"
            audioBuffer.clear()
            _voiceError.value = null
            onStateChanged?.invoke(AssistantState.LISTENING)
        } catch (e: Exception) {
            _voiceError.value = e.message ?: "Failed to initialize SpeechRecognizer"
            _isListeningState.value = false
            _wakeWordModeStatus.value = "Tap to Talk (Recognizer Error)"
            onStateChanged?.invoke(AssistantState.ERROR)
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (_: Exception) {}
        _isListeningState.value = false
        _rmsDb.value = 0.0f
        _wakeWordModeStatus.value = "Tap to Talk Ready (Session STT)"
    }

    private fun handleRecognizedText(text: String) {
        val lower = text.lowercase(Locale.getDefault()).trim()

        if (lower.contains("জরুরী থামো") || lower.contains("emergency stop") || lower.contains("stop now") || lower == "stop" || lower == "থামো") {
            onEmergencyStop?.invoke()
            return
        }

        if (lower.contains("জার্ভিস অন") || lower.contains("jarvis on") || lower.contains("start jarvis")) {
            onJarvisOnCommand?.invoke()
            return
        }

        if (lower.contains("জার্ভিস অফ") || lower.contains("jarvis off") || lower.contains("sleep jarvis") || lower.contains("জার্ভিস বন্ধ")) {
            onJarvisOffCommand?.invoke()
            return
        }

        if (lower.contains("বিরতি") || lower.contains("পজ") || lower.contains("pause")) {
            onPauseCommand?.invoke()
            return
        }

        if (lower.contains("আবার শুরু করো") || lower.contains("চালিয়ে যাও") || lower.contains("রিসিউম") || lower.contains("resume")) {
            onResumeCommand?.invoke()
            return
        }

        if (lower.contains("বাতিল") || lower.contains("ক্যানসেল") || lower.contains("cancel task")) {
            onCancelCommand?.invoke()
            return
        }

        if (lower.startsWith("জার্ভিস") || lower.startsWith("jarvis") || lower.contains("jarvis") || lower.contains("জার্ভিস")) {
            onWakeWordDetected?.invoke()
        }

        onSpeechRecognized?.invoke(text)
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _voiceError.value = null
                _isListeningState.value = true
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {
                _rmsDb.value = rmsdB
                audioBuffer.add(rmsdB)
                if (audioBuffer.size > 50) {
                    audioBuffer.removeAt(0)
                }
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _isListeningState.value = false
                _wakeWordModeStatus.value = "Tap to Talk Ready (Session STT)"
            }

            override fun onError(error: Int) {
                _isListeningState.value = false
                _rmsDb.value = 0.0f
                _voiceError.value = getSpeechErrorMessage(error)
                _wakeWordModeStatus.value = "Tap to Talk Ready (Last: ${getSpeechErrorMessage(error)})"
                onStateChanged?.invoke(AssistantState.ACTIVE)
            }

            override fun onResults(results: Bundle?) {
                _isListeningState.value = false
                _rmsDb.value = 0.0f
                _wakeWordModeStatus.value = "Tap to Talk Ready (Session STT)"
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognized = matches?.firstOrNull() ?: ""
                _lastRecognizedText.value = recognized
                if (recognized.isNotBlank()) {
                    if (audioBuffer.isNotEmpty()) {
                        voiceMatchManager.verifyVoice(audioBuffer.toFloatArray())
                    }
                    handleRecognizedText(recognized)
                } else {
                    onStateChanged?.invoke(AssistantState.ACTIVE)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull() ?: ""
                if (partial.isNotBlank()) {
                    _lastRecognizedText.value = partial
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private fun getSpeechErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Recognizer client idle/stopped"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
            SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Speech server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected"
            else -> "Speech recognition error ($errorCode)"
        }
    }
}
