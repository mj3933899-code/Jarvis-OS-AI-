package com.example.diagnostics

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.ai.AiProviderManager
import com.example.service.JarvisAccessibilityService
import com.example.service.JarvisNotificationListenerService
import com.example.util.AndroidSettingsNavigator
import com.example.voice.TtsManager
import com.example.voice.VoiceManager
import com.example.voice.VoiceMatchManager

class DiagnosticsManager(
    private val context: Context,
    private val voiceManager: VoiceManager,
    private val ttsManager: TtsManager,
    private val voiceMatchManager: VoiceMatchManager,
    private val aiProviderManager: AiProviderManager
) {
    suspend fun runFullDiagnostics(): List<DiagnosticItem> {
        val list = mutableListOf<DiagnosticItem>()

        // 1. Microphone
        val hasMicPermission = ContextCompat.checkSelfPermission(context, "android.permission.RECORD_AUDIO") == 0
        voiceManager.checkAvailability()
        val isMicAvailable = voiceManager.isMicrophoneAvailable.value
        list.add(
            DiagnosticItem(
                title = "Microphone & STT",
                status = if (hasMicPermission && isMicAvailable) "READY" else "PERMISSION_OR_ENGINE_MISSING",
                isHealthy = hasMicPermission && isMicAvailable,
                details = "Permission: ${if (hasMicPermission) "Granted" else "Denied"}, STT Engine: ${if (isMicAvailable) "Available" else "Unavailable"}",
                fixHint = if (!hasMicPermission) "Grant Microphone permission in app settings" else null
            )
        )

        // 2. TTS
        val isTtsInit = ttsManager.isInitialized.value
        val hasVoices = ttsManager.availableVoices.value.isNotEmpty()
        list.add(
            DiagnosticItem(
                title = "Text-to-Speech Engine",
                status = if (isTtsInit) "INITIALIZED" else "INITIALIZING",
                isHealthy = isTtsInit,
                details = "Voices installed: ${ttsManager.availableVoices.value.size}",
                fixHint = if (!isTtsInit) "Install or update Google Speech Services" else null
            )
        )

        // 3. Bengali Voice
        val hasBnVoice = ttsManager.availableVoices.value.any { it.isBengali }
        list.add(
            DiagnosticItem(
                title = "Bengali Voice Support",
                status = if (hasBnVoice) "INSTALLED" else "NOT_FOUND",
                isHealthy = hasBnVoice,
                details = if (hasBnVoice) "Bengali (Bangla) TTS voice package is available" else "Bangla voice package not installed in system TTS",
                fixHint = if (!hasBnVoice) "Download Bangla voice in Google TTS settings" else null
            )
        )

        // 4. Accessibility Service
        val isAccessConnected = JarvisAccessibilityService.isAccessibilityConnected.value
        list.add(
            DiagnosticItem(
                title = "Accessibility Automation Service",
                status = if (isAccessConnected) "CONNECTED" else "DISCONNECTED",
                isHealthy = isAccessConnected,
                details = "Allows JARVIS to click, scroll, type and navigate within apps",
                fixHint = if (!isAccessConnected) "Enable JARVIS in System Accessibility settings" else null
            )
        )

        // 5. Notification Listener
        val isNotifListenerGranted = JarvisNotificationListenerService.isPermissionGranted(context)
        list.add(
            DiagnosticItem(
                title = "Notification Listener",
                status = if (isNotifListenerGranted) "ACTIVE" else "INACTIVE",
                isHealthy = isNotifListenerGranted,
                details = "Allows JARVIS to detect caller IDs, messages and alerts",
                fixHint = if (!isNotifListenerGranted) "Allow Notification Access for JARVIS" else null
            )
        )

        // 6. Battery Optimization
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isBatteryExempt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && powerManager != null) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else true
        list.add(
            DiagnosticItem(
                title = "Background Execution & Battery",
                status = if (isBatteryExempt) "UNRESTRICTED" else "OPTIMIZED",
                isHealthy = isBatteryExempt,
                details = if (isBatteryExempt) "App can run continuous background monitoring safely" else "Background tasks may be killed by OS power saving",
                fixHint = if (!isBatteryExempt) "Set Battery to Unrestricted in App Info" else null
            )
        )

        // 7. Overlay Permission
        val canOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
        list.add(
            DiagnosticItem(
                title = "Display Over Other Apps",
                status = if (canOverlay) "GRANTED" else "RESTRICTED",
                isHealthy = canOverlay,
                details = "Required for Dynamic Island floating HUD and status overlays",
                fixHint = if (!canOverlay) "Grant 'Display over other apps' permission" else null
            )
        )

        // 8. AI Provider Connection
        val lastDiag = aiProviderManager.lastDiagnostics.value
        val aiOnline = lastDiag?.generationSucceeded == true
        val aiSummary = when {
            lastDiag == null -> "Ready for Test Connection in Settings (${com.example.model.AppConfig.DEFAULT_GEMINI_MODEL})"
            lastDiag.generationSucceeded -> "Verified (${lastDiag.provider}/${lastDiag.model} • ${lastDiag.outcome.summaryLabel})"
            else -> "${lastDiag.outcome.summaryLabel}: ${lastDiag.sampleOutput}"
        }
        list.add(
            DiagnosticItem(
                title = "AI Model Engine",
                status = if (aiOnline) "VERIFIED" else if (lastDiag == null) "NOT_TESTED_YET" else "ATTENTION",
                isHealthy = aiOnline || lastDiag == null,
                details = aiSummary,
                fixHint = if (lastDiag != null && !aiOnline) "Verify API key & model (${lastDiag.model}) in Settings" else "Use 'Test Connection' in Settings -> AI Provider"
            )
        )

        // 9. Internet Connectivity
        val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = connManager?.activeNetwork
        val caps = connManager?.getNetworkCapabilities(network)
        val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        list.add(
            DiagnosticItem(
                title = "Network Connectivity",
                status = if (hasInternet) "CONNECTED" else "OFFLINE",
                isHealthy = hasInternet,
                details = if (hasInternet) "Active internet connection detected" else "No internet connection",
                fixHint = if (!hasInternet) "Connect to Wi-Fi or mobile data" else null
            )
        )

        // 10. Default Assistant
        val isDefault = AndroidSettingsNavigator.isDefaultAssistant(context)
        list.add(
            DiagnosticItem(
                title = "Default Digital Assistant",
                status = if (isDefault) "JARVIS_SET" else "OPTIONAL",
                isHealthy = true,
                details = if (isDefault) "JARVIS is selected as device digital assistant" else "Google Assistant or other is currently default",
                fixHint = if (!isDefault) "Set JARVIS as Default Assistant for long-press power/home invoke" else null
            )
        )

        return list
    }
}
