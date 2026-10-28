package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat

data class SettingsNavigationResult(
    val success: Boolean,
    val destination: String,
    val actionAttempted: String,
    val errorMessage: String? = null,
    val manualGuidance: String? = null
)

object AndroidSettingsNavigator {
    private const val TAG = "SettingsNavigator"

    fun openAccessibilitySettings(context: Context): SettingsNavigationResult {
        val primary = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        val fallback = Intent(Settings.ACTION_SETTINGS)
        return safeLaunch(
            context,
            "Accessibility Settings",
            primary,
            fallback,
            "Open Settings -> Accessibility -> Installed Services -> Turn ON 'JARVIS'."
        )
    }

    fun openNotificationListenerSettings(context: Context): SettingsNavigationResult {
        val primary = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        val fallback = Intent(Settings.ACTION_SETTINGS)
        return safeLaunch(
            context,
            "Notification Access",
            primary,
            fallback,
            "Open Settings -> Apps -> Special app access -> Notification access -> Enable 'JARVIS'."
        )
    }

    fun openBatteryOptimizationSettings(context: Context): SettingsNavigationResult {
        val primary = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        return safeLaunch(
            context,
            "Battery Optimization",
            primary,
            fallback,
            "Open Settings -> Apps -> JARVIS -> Battery -> Set to 'Unrestricted'."
        )
    }

    fun openOverlaySettings(context: Context): SettingsNavigationResult {
        val primary = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        return safeLaunch(
            context,
            "Display Over Other Apps",
            primary,
            fallback,
            "Open Settings -> Apps -> Special app access -> Display over other apps -> Enable 'JARVIS'."
        )
    }

    fun openAppDetailsSettings(context: Context): SettingsNavigationResult {
        val primary = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        val fallback = Intent(Settings.ACTION_SETTINGS)
        return safeLaunch(
            context,
            "App Permissions",
            primary,
            fallback,
            "Open Settings -> Apps -> JARVIS -> Permissions."
        )
    }

    fun openVoiceInputSettings(context: Context): SettingsNavigationResult {
        val primary = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
        val fallback = Intent(Settings.ACTION_SETTINGS)
        return safeLaunch(
            context,
            "Voice & Assistant Settings",
            primary,
            fallback,
            "Open Settings -> Apps -> Default apps -> Digital assistant app -> Select JARVIS."
        )
    }

    fun openNotificationSettings(context: Context): SettingsNavigationResult {
        val primary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
        val fallback = Intent(Settings.ACTION_SETTINGS)
        return safeLaunch(
            context,
            "Notifications",
            primary,
            fallback,
            "Open Settings -> Apps -> JARVIS -> Notifications -> Turn all notifications ON."
        )
    }

    fun openDefaultAssistantSettings(context: Context): SettingsNavigationResult {
        val primary = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        val fallback = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
        return safeLaunch(
            context,
            "Default Assistant",
            primary,
            fallback,
            "Open Settings -> Apps -> Default apps -> Digital assistant app -> Select 'JARVIS'."
        )
    }

    fun openBluetoothSettings(context: Context): SettingsNavigationResult {
        val primary = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        val fallback = Intent(Settings.ACTION_SETTINGS)
        return safeLaunch(
            context,
            "Bluetooth Settings",
            primary,
            fallback,
            "Open Settings -> Connected devices / Bluetooth -> Turn ON Bluetooth."
        )
    }

    fun openLocationSettings(context: Context): SettingsNavigationResult {
        val primary = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        val fallback = Intent(Settings.ACTION_SETTINGS)
        return safeLaunch(
            context,
            "Location Settings",
            primary,
            fallback,
            "Open Settings -> Location -> Turn ON Location access."
        )
    }

    private fun safeLaunch(
        context: Context,
        destination: String,
        primaryIntent: Intent,
        fallbackIntent: Intent? = null,
        manualGuidance: String? = null
    ): SettingsNavigationResult {
        primaryIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(primaryIntent)
            Log.d(TAG, "Successfully opened $destination via primary intent: ${primaryIntent.action}")
            SettingsNavigationResult(
                success = true,
                destination = destination,
                actionAttempted = primaryIntent.action ?: "Primary Route",
                manualGuidance = manualGuidance
            )
        } catch (e: Exception) {
            Log.w(TAG, "Primary intent failed for $destination (${e.message}), attempting fallback")
            if (fallbackIntent != null) {
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(fallbackIntent)
                    SettingsNavigationResult(
                        success = true,
                        destination = destination,
                        actionAttempted = "${fallbackIntent.action} (Fallback)",
                        manualGuidance = manualGuidance
                    )
                } catch (e2: Exception) {
                    Log.e(TAG, "Fallback intent failed for $destination: ${e2.message}")
                    SettingsNavigationResult(
                        success = false,
                        destination = destination,
                        actionAttempted = "${primaryIntent.action} & ${fallbackIntent.action}",
                        errorMessage = e2.message,
                        manualGuidance = manualGuidance
                    )
                }
            } else {
                SettingsNavigationResult(
                    success = false,
                    destination = destination,
                    actionAttempted = primaryIntent.action ?: "Primary Route",
                    errorMessage = e.message,
                    manualGuidance = manualGuidance
                )
            }
        }
    }

    fun isDefaultAssistant(context: Context): Boolean {
        return try {
            val assistant = Settings.Secure.getString(context.contentResolver, "assistant")
            val voiceService = Settings.Secure.getString(context.contentResolver, "voice_interaction_service")
            val pkg = context.packageName
            (assistant != null && assistant.contains(pkg)) || (voiceService != null && voiceService.contains(pkg))
        } catch (_: Exception) {
            false
        }
    }

    fun isMediaPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, "android.permission.READ_MEDIA_IMAGES") == 0
        } else {
            ContextCompat.checkSelfPermission(context, "android.permission.READ_EXTERNAL_STORAGE") == 0
        }
    }
}
