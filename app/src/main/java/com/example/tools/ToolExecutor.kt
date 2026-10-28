package com.example.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings
import com.example.model.ToolCallRequest
import com.example.model.ToolExecutionResult
import com.example.model.VerificationStatus
import com.example.service.JarvisAccessibilityService
import com.example.util.AndroidSettingsNavigator

class ToolExecutor(
    private val context: Context,
    private val verificationEngine: VerificationEngine
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var savedVolumeLevel = -1

    suspend fun execute(request: ToolCallRequest): ToolExecutionResult {
        return when (request.tool) {
            "openApp" -> executeOpenApp(request.arguments["app"] ?: request.arguments["package"] ?: "")
            "closeApp" -> executeCloseApp()
            "openWebsite" -> executeOpenWebsite(request.arguments["url"] ?: "")
            "browserSearch" -> executeBrowserSearch(request.arguments["query"] ?: "")
            "click" -> executeClick(request.arguments["text"] ?: request.arguments["id"] ?: "")
            "scroll" -> executeScroll(request.arguments["direction"] ?: "forward")
            "typeText" -> executeTypeText(request.arguments["text"] ?: "")
            "pressBack" -> executePressBack()
            "pressHome" -> executePressHome()
            "readScreen" -> executeReadScreen()
            "setVolume" -> {
                val percent = request.arguments["percent"]?.toIntOrNull() ?: 50
                executeSetVolume(percent)
            }
            "increaseVolume" -> executeAdjustVolume(1)
            "decreaseVolume" -> executeAdjustVolume(-1)
            "mute" -> executeMute()
            "unmute" -> executeUnmute()
            "openSettings" -> executeOpenSettings()
            "openWifiSettings" -> executeOpenWifiSettings()
            "openBluetoothSettings" -> executeOpenBluetoothSettings()
            "openDisplaySettings" -> executeOpenDisplaySettings()
            "makePhoneCall" -> executeMakePhoneCall(request.arguments["number"] ?: "")
            "openContacts" -> executeOpenContacts()
            "openPlayStore" -> executeOpenPlayStore(request.arguments["query"] ?: "")
            "camera" -> executeCamera()
            "gallery" -> executeGallery()
            "copy" -> executeCopy(request.arguments["text"] ?: "")
            "paste" -> executePaste()
            else -> ToolExecutionResult(
                tool = request.tool,
                success = false,
                messageEn = "Unknown tool: ${request.tool}",
                messageBn = "অজানা টুল: ${request.tool}",
                verificationStatus = VerificationStatus.FAILED
            )
        }
    }

    suspend fun executeOpenApp(appQuery: String): ToolExecutionResult {
        if (appQuery.isBlank()) {
            return ToolExecutionResult("openApp", false, "Missing app name", "অ্যাপের নাম দেওয়া হয়নি", verificationStatus = VerificationStatus.FAILED)
        }
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(appQuery) ?: run {
            val installed = pm.getInstalledApplications(0)
            val match = installed.firstOrNull {
                it.packageName.contains(appQuery, ignoreCase = true) ||
                pm.getApplicationLabel(it).toString().contains(appQuery, ignoreCase = true)
            }
            match?.let { pm.getLaunchIntentForPackage(it.packageName) }
        }

        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            val (verif, detail) = verificationEngine.verifyAppOpen(launchIntent.component?.packageName ?: appQuery)
            ToolExecutionResult(
                tool = "openApp",
                success = true,
                messageEn = "Opened app '$appQuery'. $detail",
                messageBn = "'$appQuery' অ্যাপটি খোলা হয়েছে।",
                verificationStatus = verif
            )
        } else {
            ToolExecutionResult(
                tool = "openApp",
                success = false,
                messageEn = "Could not find app '$appQuery' on device",
                messageBn = "ডিভাইসে '$appQuery' অ্যাপটি পাওয়া যায়নি",
                verificationStatus = VerificationStatus.FAILED
            )
        }
    }

    private fun executeCloseApp(): ToolExecutionResult {
        val service = JarvisAccessibilityService.instance
        return if (service != null && service.pressHome()) {
            ToolExecutionResult("closeApp", true, "Closed current app and returned home", "বর্তমান অ্যাপ বন্ধ করে হোম স্ক্রিনে ফিরে যাওয়া হয়েছে", verificationStatus = VerificationStatus.CONFIRMED)
        } else {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(homeIntent)
            ToolExecutionResult("closeApp", true, "Returned home via intent", "হোম স্ক্রিনে ফিরে যাওয়া হয়েছে", verificationStatus = VerificationStatus.LIKELY)
        }
    }

    private fun executeOpenWebsite(url: String): ToolExecutionResult {
        val validUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(validUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ToolExecutionResult("openWebsite", true, "Opened $validUrl", "$validUrl ওয়েবসাইট খোলা হয়েছে", verificationStatus = VerificationStatus.LIKELY)
        } catch (e: Exception) {
            ToolExecutionResult("openWebsite", false, "Failed to open $url: ${e.message}", "ওয়েবসাইট খুলতে ব্যর্থ হয়েছে", verificationStatus = VerificationStatus.FAILED)
        }
    }

    private fun executeBrowserSearch(query: String): ToolExecutionResult {
        val searchUrl = "https://www.google.com/search?q=${Uri.encode(query)}"
        return executeOpenWebsite(searchUrl)
    }

    private suspend fun executeClick(target: String): ToolExecutionResult {
        val service = JarvisAccessibilityService.instance
            ?: return ToolExecutionResult("click", false, "Accessibility service disconnected", "অ্যাক্সেসিবিলিটি সার্ভিস বন্ধ রয়েছে", verificationStatus = VerificationStatus.NOT_AVAILABLE)

        val clicked = service.clickByText(target) || service.clickById(target)
        val (verif, detail) = verificationEngine.verifyAccessibilityAction("Click on '$target'")
        return if (clicked) {
            ToolExecutionResult("click", true, "Clicked '$target'. $detail", "'$target'-এ ক্লিক করা হয়েছে", verificationStatus = verif)
        } else {
            ToolExecutionResult("click", false, "Element '$target' not found or not clickable", "'$target' এলিমেন্ট পাওয়া যায়নি", verificationStatus = VerificationStatus.FAILED)
        }
    }

    private suspend fun executeScroll(direction: String): ToolExecutionResult {
        val service = JarvisAccessibilityService.instance
            ?: return ToolExecutionResult("scroll", false, "Accessibility service disconnected", "অ্যাক্সেসিবিলিটি সার্ভিস বন্ধ রয়েছে", verificationStatus = VerificationStatus.NOT_AVAILABLE)

        val forward = !direction.equals("backward", ignoreCase = true) && !direction.equals("up", ignoreCase = true)
        val scrolled = service.scroll(forward)
        val (verif, detail) = verificationEngine.verifyAccessibilityAction("Scroll $direction")
        return ToolExecutionResult("scroll", scrolled, "Scrolled $direction. $detail", "স্ক্রোল করা হয়েছে", verificationStatus = verif)
    }

    private suspend fun executeTypeText(text: String): ToolExecutionResult {
        val service = JarvisAccessibilityService.instance
            ?: return ToolExecutionResult("typeText", false, "Accessibility service disconnected", "অ্যাক্সেসিবিলিটি সার্ভিস বন্ধ রয়েছে", verificationStatus = VerificationStatus.NOT_AVAILABLE)

        val typed = service.typeTextIntoFocused(text)
        val (verif, detail) = verificationEngine.verifyAccessibilityAction("Type '$text'")
        return if (typed) {
            ToolExecutionResult("typeText", true, "Typed text into focused field. $detail", "টেক্সট টাইপ করা হয়েছে", verificationStatus = verif)
        } else {
            ToolExecutionResult("typeText", false, "No active editable input field found", "কোনো ইনপুট ফিল্ড ফোকাসড নেই", verificationStatus = VerificationStatus.FAILED)
        }
    }

    private fun executePressBack(): ToolExecutionResult {
        val service = JarvisAccessibilityService.instance
            ?: return ToolExecutionResult("pressBack", false, "Accessibility service disconnected", "অ্যাক্সেসিবিলিটি সার্ভিস বন্ধ রয়েছে", verificationStatus = VerificationStatus.NOT_AVAILABLE)
        val success = service.pressBack()
        return ToolExecutionResult("pressBack", success, "Back action performed", "পেছনে ফিরে যাওয়া হয়েছে", verificationStatus = if (success) VerificationStatus.CONFIRMED else VerificationStatus.FAILED)
    }

    private fun executePressHome(): ToolExecutionResult {
        val service = JarvisAccessibilityService.instance
            ?: return ToolExecutionResult("pressHome", false, "Accessibility service disconnected", "অ্যাক্সেসিবিলিটি সার্ভিস বন্ধ রয়েছে", verificationStatus = VerificationStatus.NOT_AVAILABLE)
        val success = service.pressHome()
        return ToolExecutionResult("pressHome", success, "Home action performed", "হোম বাটন চাপ দেওয়া হয়েছে", verificationStatus = if (success) VerificationStatus.CONFIRMED else VerificationStatus.FAILED)
    }

    private fun executeReadScreen(): ToolExecutionResult {
        val service = JarvisAccessibilityService.instance
            ?: return ToolExecutionResult("readScreen", false, "Accessibility service disconnected", "অ্যাক্সেসিবিলিটি সার্ভিস বন্ধ রয়েছে", verificationStatus = VerificationStatus.NOT_AVAILABLE)
        val items = service.inspectScreenContent()
        val summary = items.take(15).joinToString(" | ")
        return ToolExecutionResult(
            "readScreen",
            true,
            "Visible on screen: $summary",
            "স্ক্রিনের কনটেন্ট পড়া হয়েছে",
            outputData = mapOf("screenText" to items.joinToString("\n")),
            verificationStatus = VerificationStatus.CONFIRMED
        )
    }

    suspend fun executeSetVolume(percent: Int): ToolExecutionResult {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val initial = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val target = ((percent.coerceIn(0, 100) / 100.0) * max).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
        val (verif, detail) = verificationEngine.verifyVolumeChange(initial)
        return ToolExecutionResult("setVolume", true, "Volume set to $percent%. $detail", "ভলিউম $percent% নির্ধারণ করা হয়েছে।", verificationStatus = verif)
    }

    suspend fun executeAdjustVolume(step: Int): ToolExecutionResult {
        val initial = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val dir = if (step > 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, dir, AudioManager.FLAG_SHOW_UI)
        val (verif, detail) = verificationEngine.verifyVolumeChange(initial)
        val action = if (step > 0) "increased" else "decreased"
        val actionBn = if (step > 0) "বাড়ানো" else "কমানো"
        return ToolExecutionResult("adjustVolume", true, "Volume $action. $detail", "ভলিউম $actionBn হয়েছে।", verificationStatus = verif)
    }

    suspend fun executeMute(): ToolExecutionResult {
        savedVolumeLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
        val (verif, detail) = verificationEngine.verifyVolumeChange(savedVolumeLevel)
        return ToolExecutionResult("mute", true, "Media muted. $detail", "মিডিয়া মিউট করা হয়েছে।", verificationStatus = verif)
    }

    suspend fun executeUnmute(): ToolExecutionResult {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = if (savedVolumeLevel > 0) savedVolumeLevel else max / 2
        val initial = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
        val (verif, detail) = verificationEngine.verifyVolumeChange(initial)
        return ToolExecutionResult("unmute", true, "Media unmuted to volume $target. $detail", "মিডিয়া আনমিউট করা হয়েছে।", verificationStatus = verif)
    }

    private fun executeOpenSettings(): ToolExecutionResult {
        val res = AndroidSettingsNavigator.openAppDetailsSettings(context)
        return ToolExecutionResult("openSettings", res.success, res.actionAttempted, "সেটিংস খোলা হয়েছে", verificationStatus = if (res.success) VerificationStatus.CONFIRMED else VerificationStatus.FAILED)
    }

    private fun executeOpenWifiSettings(): ToolExecutionResult {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        return try {
            context.startActivity(intent)
            ToolExecutionResult("openWifiSettings", true, "Opened Wi-Fi settings", "ওয়াইফাই সেটিংস খোলা হয়েছে", verificationStatus = VerificationStatus.CONFIRMED)
        } catch (_: Exception) {
            ToolExecutionResult("openWifiSettings", false, "Failed to open Wi-Fi settings", "ওয়াইফাই সেটিংস খুলতে ব্যর্থ", verificationStatus = VerificationStatus.FAILED)
        }
    }

    private fun executeOpenBluetoothSettings(): ToolExecutionResult {
        val res = AndroidSettingsNavigator.openBluetoothSettings(context)
        return ToolExecutionResult("openBluetoothSettings", res.success, res.actionAttempted, "ব্লুটুথ সেটিংস খোলা হয়েছে", verificationStatus = if (res.success) VerificationStatus.CONFIRMED else VerificationStatus.FAILED)
    }

    private fun executeOpenDisplaySettings(): ToolExecutionResult {
        val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        return try {
            context.startActivity(intent)
            ToolExecutionResult("openDisplaySettings", true, "Opened Display settings", "ডিসপ্লে সেটিংস খোলা হয়েছে", verificationStatus = VerificationStatus.CONFIRMED)
        } catch (_: Exception) {
            ToolExecutionResult("openDisplaySettings", false, "Failed to open Display settings", "ডিসপ্লে সেটিংস খুলতে ব্যর্থ", verificationStatus = VerificationStatus.FAILED)
        }
    }

    private fun executeMakePhoneCall(number: String): ToolExecutionResult {
        return try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ToolExecutionResult("makePhoneCall", true, "Opened dialer for $number", "$number নম্বরে ডায়াল প্যাড খোলা হয়েছে", verificationStatus = VerificationStatus.CONFIRMED)
        } catch (e: Exception) {
            ToolExecutionResult("makePhoneCall", false, "Failed to open dialer: ${e.message}", "ডায়াল করতে ব্যর্থ", verificationStatus = VerificationStatus.FAILED)
        }
    }

    private fun executeOpenContacts(): ToolExecutionResult {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("content://contacts/people")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            ToolExecutionResult("openContacts", true, "Opened contacts list", "যোগাযোগ তালিকা খোলা হয়েছে", verificationStatus = VerificationStatus.CONFIRMED)
        } catch (_: Exception) {
            ToolExecutionResult("openContacts", false, "Failed to open contacts", "কন্ট্যাক্টস খুলতে ব্যর্থ", verificationStatus = VerificationStatus.FAILED)
        }
    }

    private fun executeOpenPlayStore(query: String): ToolExecutionResult {
        val uri = if (query.isNotBlank()) {
            Uri.parse("market://search?q=${Uri.encode(query)}")
        } else {
            Uri.parse("market://details?id=${context.packageName}")
        }
        val intent = Intent(Intent.ACTION_VIEW, uri).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        return try {
            context.startActivity(intent)
            ToolExecutionResult("openPlayStore", true, "Opened Play Store", "প্লে স্টোর খোলা হয়েছে", verificationStatus = VerificationStatus.CONFIRMED)
        } catch (_: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=${Uri.encode(query)}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
            ToolExecutionResult("openPlayStore", true, "Opened Play Store web fallback", "প্লে স্টোর খোলা হয়েছে", verificationStatus = VerificationStatus.LIKELY)
        }
    }

    private fun executeCamera(): ToolExecutionResult {
        val intent = Intent("android.media.action.IMAGE_CAPTURE").apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        return try {
            context.startActivity(intent)
            ToolExecutionResult("camera", true, "Opened camera app", "ক্যামেরা খোলা হয়েছে", verificationStatus = VerificationStatus.CONFIRMED)
        } catch (_: Exception) {
            ToolExecutionResult("camera", false, "No camera app found", "ক্যামেরা অ্যাপ পাওয়া যায়নি", verificationStatus = VerificationStatus.FAILED)
        }
    }

    private fun executeGallery(): ToolExecutionResult {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            type = "image/*"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            ToolExecutionResult("gallery", true, "Opened photos gallery", "গ্যালারি খোলা হয়েছে", verificationStatus = VerificationStatus.CONFIRMED)
        } catch (_: Exception) {
            ToolExecutionResult("gallery", false, "Failed to open gallery", "গ্যালারি খুলতে ব্যর্থ", verificationStatus = VerificationStatus.FAILED)
        }
    }

    private fun executeCopy(text: String): ToolExecutionResult {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("JARVIS Copy", text)
        clipboard.setPrimaryClip(clip)
        return ToolExecutionResult("copy", true, "Copied to clipboard", "ক্লিপবোর্ডে কপি করা হয়েছে", verificationStatus = VerificationStatus.CONFIRMED)
    }

    private suspend fun executePaste(): ToolExecutionResult {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        val text = clip?.getItemAt(0)?.text?.toString() ?: ""
        return if (text.isNotBlank()) {
            executeTypeText(text)
        } else {
            ToolExecutionResult("paste", false, "Clipboard is empty", "ক্লিপবোর্ড খালি", verificationStatus = VerificationStatus.FAILED)
        }
    }
}
