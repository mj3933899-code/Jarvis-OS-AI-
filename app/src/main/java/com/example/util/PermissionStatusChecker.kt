package com.example.util

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.service.JarvisAccessibilityService
import com.example.service.JarvisNotificationListenerService

data class PermissionCardModel(
    val id: String,
    val index: Int,
    val titleEn: String,
    val titleBn: String,
    val descriptionEn: String,
    val descriptionBn: String,
    val isGranted: Boolean,
    val statusBadge: String,
    val isSpecialAccess: Boolean,
    val runtimePermissions: List<String> = emptyList()
)

object PermissionStatusChecker {

    fun getTenRuntimePermissionCards(context: Context): List<PermissionCardModel> {
        val micGranted = hasPerm(context, Manifest.permission.RECORD_AUDIO)

        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPerm(context, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

        val cameraGranted = hasPerm(context, Manifest.permission.CAMERA)
        val phoneCallGranted = hasPerm(context, Manifest.permission.CALL_PHONE)
        val contactsGranted = hasPerm(context, Manifest.permission.READ_CONTACTS)

        val smsGranted = hasPerm(context, Manifest.permission.SEND_SMS) &&
            hasPerm(context, Manifest.permission.READ_SMS)

        val locationGranted = hasPerm(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
            hasPerm(context, Manifest.permission.ACCESS_COARSE_LOCATION)

        val galleryGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPerm(context, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            hasPerm(context, Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val answerCallsGranted = hasPerm(context, Manifest.permission.READ_PHONE_STATE) &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || hasPerm(context, Manifest.permission.ANSWER_PHONE_CALLS))

        val bluetoothGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPerm(context, Manifest.permission.BLUETOOTH_CONNECT) &&
                hasPerm(context, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            true
        }

        return listOf(
            PermissionCardModel(
                id = "perm_mic",
                index = 1,
                titleEn = "1. Microphone",
                titleBn = "১. মাইক্রোফোন (Microphone)",
                descriptionEn = "Required for speech recognition (STT) and voice commands.",
                descriptionBn = "ভয়েস কমান্ড এবং কথা শোনার জন্য প্রয়োজন।",
                isGranted = micGranted,
                statusBadge = if (micGranted) "GRANTED" else "REQUIRED",
                isSpecialAccess = false,
                runtimePermissions = listOf(Manifest.permission.RECORD_AUDIO)
            ),
            PermissionCardModel(
                id = "perm_notif",
                index = 2,
                titleEn = "2. Notifications",
                titleBn = "২. নোটিফিকেশন (Notifications)",
                descriptionEn = "Required to display foreground service status and task alerts.",
                descriptionBn = "ব্যাকগ্রাউন্ড সার্ভিস স্ট্যাটাস ও অ্যালার্ট দেখানোর জন্য প্রয়োজন।",
                isGranted = notifGranted,
                statusBadge = if (notifGranted) "GRANTED" else "REQUIRED",
                isSpecialAccess = false,
                runtimePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    listOf(Manifest.permission.POST_NOTIFICATIONS)
                } else emptyList()
            ),
            PermissionCardModel(
                id = "perm_camera",
                index = 3,
                titleEn = "3. Camera",
                titleBn = "৩. ক্যামেরা (Camera)",
                descriptionEn = "Allows JARVIS to launch camera and visual assistance tools.",
                descriptionBn = "ক্যামেরা ও ভিজ্যুয়াল অ্যাসিস্ট্যান্স টুলের জন্য।",
                isGranted = cameraGranted,
                statusBadge = if (cameraGranted) "GRANTED" else "ALLOW",
                isSpecialAccess = false,
                runtimePermissions = listOf(Manifest.permission.CAMERA)
            ),
            PermissionCardModel(
                id = "perm_call",
                index = 4,
                titleEn = "4. Phone Calls",
                titleBn = "৪. ফোন কল (Phone Calls)",
                descriptionEn = "Enables placing phone calls when explicitly requested.",
                descriptionBn = "আপনার নির্দেশে ফোন কল করার জন্য।",
                isGranted = phoneCallGranted,
                statusBadge = if (phoneCallGranted) "GRANTED" else "ALLOW",
                isSpecialAccess = false,
                runtimePermissions = listOf(Manifest.permission.CALL_PHONE)
            ),
            PermissionCardModel(
                id = "perm_contacts",
                index = 5,
                titleEn = "5. Contacts",
                titleBn = "৫. কন্ট্যাক্টস (Contacts)",
                descriptionEn = "Lookup contact names for calling and messaging.",
                descriptionBn = "নাম ধরে কল বা মেসেজ করার জন্য।",
                isGranted = contactsGranted,
                statusBadge = if (contactsGranted) "GRANTED" else "ALLOW",
                isSpecialAccess = false,
                runtimePermissions = listOf(Manifest.permission.READ_CONTACTS)
            ),
            PermissionCardModel(
                id = "perm_sms",
                index = 6,
                titleEn = "6. SMS",
                titleBn = "৬. এসএমএস (SMS)",
                descriptionEn = "Read or send SMS messages with user confirmation.",
                descriptionBn = "আপনার সম্মতিতে এসএমএস পড়া ও পাঠানোর জন্য।",
                isGranted = smsGranted,
                statusBadge = if (smsGranted) "GRANTED" else "ALLOW",
                isSpecialAccess = false,
                runtimePermissions = listOf(
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.RECEIVE_SMS
                )
            ),
            PermissionCardModel(
                id = "perm_location",
                index = 7,
                titleEn = "7. Location",
                titleBn = "৭. লোকেশন (Location)",
                descriptionEn = "Used for local weather updates and navigation context.",
                descriptionBn = "স্থানীয় আবহাওয়া ও নেভিগেশন তথ্যের জন্য।",
                isGranted = locationGranted,
                statusBadge = if (locationGranted) "GRANTED" else "ALLOW",
                isSpecialAccess = false,
                runtimePermissions = listOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            ),
            PermissionCardModel(
                id = "perm_gallery",
                index = 8,
                titleEn = "8. Gallery & Files",
                titleBn = "৮. গ্যালারি ও ফাইল (Gallery & Files)",
                descriptionEn = "Access media images and user-selected files.",
                descriptionBn = "ছবি ও মিডিয়া ফাইল ব্যবহারের জন্য।",
                isGranted = galleryGranted,
                statusBadge = if (galleryGranted) "GRANTED" else "ALLOW",
                isSpecialAccess = false,
                runtimePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    listOf(Manifest.permission.READ_MEDIA_IMAGES)
                } else {
                    listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            ),
            PermissionCardModel(
                id = "perm_manage_calls",
                index = 9,
                titleEn = "9. Answer & Manage Calls",
                titleBn = "৯. কল রিসিভ ও নিয়ন্ত্রণ (Manage Calls)",
                descriptionEn = "Detect incoming call interruptions and pause active tasks safely.",
                descriptionBn = "ইনকামিং কলের সময় কাজ অটো-পজ ও রিজিউম করার জন্য।",
                isGranted = answerCallsGranted,
                statusBadge = if (answerCallsGranted) "GRANTED" else "ALLOW",
                isSpecialAccess = false,
                runtimePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    listOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.ANSWER_PHONE_CALLS)
                } else {
                    listOf(Manifest.permission.READ_PHONE_STATE)
                }
            ),
            PermissionCardModel(
                id = "perm_bluetooth",
                index = 10,
                titleEn = "10. Bluetooth",
                titleBn = "১০. ব্লুটুথ ও হেডসেট (Bluetooth)",
                descriptionEn = "Connect and route audio to Bluetooth headsets.",
                descriptionBn = "ব্লুটুথ অডিও ও হেডসেট কানেকশনের জন্য।",
                isGranted = bluetoothGranted,
                statusBadge = if (bluetoothGranted) "GRANTED" else "ALLOW",
                isSpecialAccess = false,
                runtimePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
                } else emptyList()
            )
        )
    }

    fun getFiveAdvancedBackgroundCards(context: Context): List<PermissionCardModel> {
        val batteryExempt = isBatteryOptimizationIgnored(context)
        val overlayGranted = isOverlayPermissionGranted(context)
        val notifListenerGranted = JarvisNotificationListenerService.isPermissionGranted(context)
        val accessibilityEnabled = isAccessibilityServiceEnabled(context)
        val defaultAssistantSet = isDefaultDigitalAssistant(context)

        return listOf(
            PermissionCardModel(
                id = "adv_battery",
                index = 1,
                titleEn = "1. Battery — No Optimisation",
                titleBn = "১. ব্যাটারি — আনরেস্ট্রিক্টেড (No Optimisation)",
                descriptionEn = "Prevents OS from killing JARVIS background service unexpectedly.",
                descriptionBn = "ব্যাকগ্রাউন্ডে জার্ভিসকে সচল রাখতে ব্যাটারি অপটিমাইজেশন বন্ধ করুন।",
                isGranted = batteryExempt,
                statusBadge = if (batteryExempt) "UNRESTRICTED" else "OPTIMIZED",
                isSpecialAccess = true
            ),
            PermissionCardModel(
                id = "adv_overlay",
                index = 2,
                titleEn = "2. Display Over Other Apps",
                titleBn = "২. অন্য অ্যাপের ওপর প্রদর্শন (Overlay)",
                descriptionEn = "Allows floating HUD status while using other apps.",
                descriptionBn = "অন্য অ্যাপ ব্যবহারের সময় ফ্লোটিং স্ট্যাটাস দেখানোর জন্য।",
                isGranted = overlayGranted,
                statusBadge = if (overlayGranted) "GRANTED" else "ENABLE",
                isSpecialAccess = true
            ),
            PermissionCardModel(
                id = "adv_notif_access",
                index = 3,
                titleEn = "3. Notification Access",
                titleBn = "৩. নোটিফিকেশন অ্যাক্সেস (Notification Listener)",
                descriptionEn = "Read incoming alerts and WhatsApp/message notifications.",
                descriptionBn = "মেসেজ ও নোটিফিকেশন পড়ার জন্য।",
                isGranted = notifListenerGranted,
                statusBadge = if (notifListenerGranted) "ACTIVE" else "ENABLE",
                isSpecialAccess = true
            ),
            PermissionCardModel(
                id = "adv_accessibility",
                index = 4,
                titleEn = "4. Accessibility Service",
                titleBn = "৪. অ্যাক্সেসিবিলিটি সার্ভিস (Accessibility)",
                descriptionEn = "Enables screen reading, clicking, scrolling and UI automation.",
                descriptionBn = "স্ক্রিন পড়া, ক্লিক, স্ক্রোল এবং অটোমেশনের জন্য।",
                isGranted = accessibilityEnabled,
                statusBadge = if (accessibilityEnabled) "CONNECTED" else "ENABLE",
                isSpecialAccess = true
            ),
            PermissionCardModel(
                id = "adv_assistant",
                index = 5,
                titleEn = "5. Default Digital Assistant",
                titleBn = "৫. ডিফল্ট অ্যাসিস্ট্যান্ট (Default Assistant)",
                descriptionEn = "Optional: Set JARVIS as system default digital assistant app.",
                descriptionBn = "ঐচ্ছিক: পাওয়ার বা হোম বাটন লং-প্রেসে জার্ভিস চালু করতে।",
                isGranted = defaultAssistantSet,
                statusBadge = if (defaultAssistantSet) "DEFAULT" else "OPTIONAL",
                isSpecialAccess = true
            )
        )
    }

    fun getAllMissingRuntimePermissions(context: Context): List<String> {
        return getTenRuntimePermissionCards(context)
            .filter { !it.isGranted }
            .flatMap { it.runtimePermissions }
            .distinct()
    }

    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } else true
    }

    fun isOverlayPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        if (JarvisAccessibilityService.isAccessibilityConnected.value) return true
        return try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            enabledServices.contains(context.packageName, ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }

    fun isDefaultDigitalAssistant(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) {
                if (roleManager.isRoleHeld(RoleManager.ROLE_ASSISTANT)) return true
            }
        }
        return AndroidSettingsNavigator.isDefaultAssistant(context)
    }

    private fun hasPerm(context: Context, perm: String): Boolean {
        return ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }
}
