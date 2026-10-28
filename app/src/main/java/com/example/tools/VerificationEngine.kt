package com.example.tools

import android.content.Context
import android.media.AudioManager
import com.example.model.VerificationStatus
import com.example.service.JarvisAccessibilityService
import kotlinx.coroutines.delay

class VerificationEngine(private val context: Context) {

    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    suspend fun verifyAppOpen(expectedPackage: String): Pair<VerificationStatus, String> {
        delay(600)
        val current = JarvisAccessibilityService.currentPackage.value
        return if (current.contains(expectedPackage, ignoreCase = true) || expectedPackage.contains(current, ignoreCase = true)) {
            Pair(VerificationStatus.CONFIRMED, "App package '$current' confirmed active in foreground")
        } else if (current != "Unknown") {
            Pair(VerificationStatus.LIKELY, "Foreground app observed: $current")
        } else {
            Pair(VerificationStatus.UNCERTAIN, "Could not confirm foreground window via accessibility")
        }
    }

    suspend fun verifyVolumeChange(initialVolume: Int): Pair<VerificationStatus, String> {
        delay(150)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return if (current != initialVolume) {
            Pair(VerificationStatus.CONFIRMED, "Volume changed from $initialVolume to $current")
        } else {
            Pair(VerificationStatus.UNCERTAIN, "Volume remained at $current")
        }
    }

    suspend fun verifyAccessibilityAction(description: String): Pair<VerificationStatus, String> {
        delay(300)
        val isConnected = JarvisAccessibilityService.isAccessibilityConnected.value
        return if (isConnected) {
            Pair(VerificationStatus.CONFIRMED, "Executed '$description' through Accessibility Service")
        } else {
            Pair(VerificationStatus.UNCERTAIN, "Accessibility service disconnected during verification")
        }
    }
}
