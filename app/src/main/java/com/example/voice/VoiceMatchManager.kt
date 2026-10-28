package com.example.voice

import android.content.Context
import android.content.SharedPreferences
import com.example.model.VoiceMatchStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

class VoiceMatchManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("jarvis_voice_match", Context.MODE_PRIVATE)

    private val _status = MutableStateFlow(VoiceMatchStatus.OFF)
    val status: StateFlow<VoiceMatchStatus> = _status.asStateFlow()

    private val _enrollmentStep = MutableStateFlow(0)
    val enrollmentStep: StateFlow<Int> = _enrollmentStep.asStateFlow()

    private val _lastScore = MutableStateFlow(0.0f)
    val lastScore: StateFlow<Float> = _lastScore.asStateFlow()

    private val _enrollmentError = MutableStateFlow<String?>(null)
    val enrollmentError: StateFlow<String?> = _enrollmentError.asStateFlow()

    private var lastVerifiedTimestamp: Long = 0L
    private val enrollmentSamples = mutableListOf<FloatArray>()

    init {
        checkCurrentStatus()
    }

    private fun checkCurrentStatus() {
        val enrolled = prefs.getBoolean("is_enrolled", false)
        val enabled = prefs.getBoolean("is_enabled", false)
        if (enrolled && enabled) {
            _status.value = VoiceMatchStatus.READY
        } else if (enrolled) {
            _status.value = VoiceMatchStatus.OFF
        } else {
            _status.value = VoiceMatchStatus.OFF
        }
    }

    fun startEnrollment() {
        enrollmentSamples.clear()
        _enrollmentStep.value = 1
        _enrollmentError.value = null
        _status.value = VoiceMatchStatus.ENROLLING
    }

    fun addEnrollmentSample(energyFeatures: FloatArray): Boolean {
        if (_status.value != VoiceMatchStatus.ENROLLING) return false
        if (energyFeatures.isEmpty()) {
            _enrollmentError.value = "Sample too short or silent"
            return false
        }
        enrollmentSamples.add(energyFeatures)
        val step = enrollmentSamples.size
        _enrollmentStep.value = step + 1
        if (step >= 3) {
            val avg = computeAverageProfile(enrollmentSamples)
            saveProfile(avg)
            prefs.edit().putBoolean("is_enrolled", true).putBoolean("is_enabled", true).apply()
            _status.value = VoiceMatchStatus.READY
            _enrollmentStep.value = 0
            return true
        }
        return false
    }

    fun cancelEnrollment() {
        enrollmentSamples.clear()
        _enrollmentStep.value = 0
        _enrollmentError.value = null
        checkCurrentStatus()
    }

    fun verifyVoice(sampleFeatures: FloatArray): Boolean {
        if (_status.value == VoiceMatchStatus.OFF) return true
        val profile = loadProfile() ?: return true
        _status.value = VoiceMatchStatus.VERIFYING
        val score = computeSimilarity(profile, sampleFeatures)
        _lastScore.value = score

        val isMatch = score >= 0.70f
        if (isMatch) {
            _status.value = VoiceMatchStatus.VERIFIED
            lastVerifiedTimestamp = System.currentTimeMillis()
        } else {
            _status.value = VoiceMatchStatus.REJECTED
        }
        return isMatch
    }

    fun isSpeakerAuthorized(strictMode: Boolean): Boolean {
        val enrolled = prefs.getBoolean("is_enrolled", false)
        val enabled = prefs.getBoolean("is_enabled", false)
        if (!enrolled || !enabled) return true

        val now = System.currentTimeMillis()
        val timeSinceVerified = now - lastVerifiedTimestamp
        val timeoutMs = if (strictMode) 30_000L else 120_000L

        return timeSinceVerified <= timeoutMs && _status.value == VoiceMatchStatus.VERIFIED
    }

    fun invalidateVerification() {
        lastVerifiedTimestamp = 0L
        if (_status.value == VoiceMatchStatus.VERIFIED) {
            _status.value = VoiceMatchStatus.READY
        }
    }

    fun resetProfile() {
        enrollmentSamples.clear()
        prefs.edit().clear().apply()
        _status.value = VoiceMatchStatus.OFF
        _enrollmentStep.value = 0
        _lastScore.value = 0.0f
    }

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("is_enabled", enabled).apply()
        checkCurrentStatus()
    }

    private fun computeAverageProfile(samples: List<FloatArray>): FloatArray {
        val size = samples.first().size
        val avg = FloatArray(size)
        for (sample in samples) {
            for (i in 0 until minOf(size, sample.size)) {
                avg[i] += sample[i]
            }
        }
        for (i in 0 until size) {
            avg[i] = avg[i] / samples.size
        }
        return avg
    }

    private fun computeSimilarity(a: FloatArray, b: FloatArray): Float {
        val len = minOf(a.size, b.size)
        if (len == 0) return 0.0f
        var dot = 0.0f
        var normA = 0.0f
        var normB = 0.0f
        for (i in 0 until len) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = sqrt(normA.toDouble()) * sqrt(normB.toDouble())
        return if (denom > 0.0) (dot / denom).toFloat() else 0.0f
    }

    private fun saveProfile(profile: FloatArray) {
        val str = profile.joinToString(",") { it.toString() }
        prefs.edit().putString("voice_profile", str).apply()
    }

    private fun loadProfile(): FloatArray? {
        val str = prefs.getString("voice_profile", null) ?: return null
        return try {
            val parts = str.split(",")
            parts.map { it.toFloat() }.toFloatArray()
        } catch (_: Exception) {
            null
        }
    }
}
