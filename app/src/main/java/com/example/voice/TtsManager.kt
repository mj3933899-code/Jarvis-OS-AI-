package com.example.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

class TtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<TtsVoiceModel>>(emptyList())
    val availableVoices: StateFlow<List<TtsVoiceModel>> = _availableVoices.asStateFlow()

    private val _selectedVoice = MutableStateFlow<TtsVoiceModel?>(null)
    val selectedVoice: StateFlow<TtsVoiceModel?> = _selectedVoice.asStateFlow()

    private var currentRate: Float = 1.0f
    private var currentPitch: Float = 1.0f
    private var onCompleteCallback: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            _isInitialized.value = true
            setupProgressListener()
            loadAvailableVoices()
            setDefaultBengaliOrEnglishVoice()
        } else {
            Log.e("TtsManager", "TextToSpeech init failed with status: $status")
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
                onCompleteCallback?.invoke()
                onCompleteCallback = null
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                onCompleteCallback?.invoke()
                onCompleteCallback = null
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _isSpeaking.value = false
                onCompleteCallback?.invoke()
                onCompleteCallback = null
            }
        })
    }

    private fun loadAvailableVoices() {
        val ttsInstance = tts ?: return
        try {
            val voices = ttsInstance.voices ?: return
            val models = voices.map { voice ->
                val isBn = voice.locale.language == "bn"
                TtsVoiceModel(
                    id = voice.name,
                    name = "${voice.locale.displayLanguage} (${voice.locale.country}) - ${voice.name.takeLast(6)}",
                    locale = voice.locale,
                    isBengali = isBn,
                    requiresNetwork = voice.isNetworkConnectionRequired
                )
            }.sortedWith(compareByDescending<TtsVoiceModel> { it.isBengali }.thenBy { it.name })
            _availableVoices.value = models
        } catch (_: Exception) {}
    }

    private fun setDefaultBengaliOrEnglishVoice() {
        val ttsInstance = tts ?: return
        val bnLocale = Locale("bn", "BD")
        val result = ttsInstance.setLanguage(bnLocale)
        if (result == TextToSpeech.LANG_AVAILABLE || result == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
            val bnVoice = _availableVoices.value.firstOrNull { it.isBengali }
            if (bnVoice != null) {
                setVoice(bnVoice.id)
            }
        } else {
            ttsInstance.language = Locale.US
            val enVoice = _availableVoices.value.firstOrNull { it.locale.language == "en" }
            if (enVoice != null) {
                setVoice(enVoice.id)
            }
        }
    }

    fun setVoice(voiceId: String) {
        val ttsInstance = tts ?: return
        try {
            val matchingVoice = ttsInstance.voices?.firstOrNull { it.name == voiceId }
            if (matchingVoice != null) {
                ttsInstance.voice = matchingVoice
                _selectedVoice.value = _availableVoices.value.firstOrNull { it.id == voiceId }
            }
        } catch (_: Exception) {}
    }

    fun setSpeechRate(rate: Float) {
        currentRate = rate
        tts?.setSpeechRate(rate)
    }

    fun setPitch(pitch: Float) {
        currentPitch = pitch
        tts?.setPitch(pitch)
    }

    fun speak(text: String, isBengali: Boolean = true, onComplete: (() -> Unit)? = null) {
        val ttsInstance = tts ?: return
        if (text.isBlank()) {
            onComplete?.invoke()
            return
        }
        this.onCompleteCallback = onComplete

        val targetLocale = if (isBengali) Locale("bn", "BD") else Locale.US
        ttsInstance.language = targetLocale
        ttsInstance.setSpeechRate(currentRate)
        ttsInstance.setPitch(currentPitch)

        val utteranceId = UUID.randomUUID().toString()
        val params = Bundle()
        ttsInstance.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
        onCompleteCallback = null
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        _isInitialized.value = false
        _isSpeaking.value = false
    }
}
