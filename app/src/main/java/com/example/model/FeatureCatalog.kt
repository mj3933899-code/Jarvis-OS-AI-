package com.example.model

enum class FeatureAuditStatus(val badgeText: String) {
    VERIFIED_WORKING("WORKING"),
    PARTIAL_CONFIG_REQUIRED("PARTIAL"),
    REQUIRES_ASSET_OR_ROLE("NEEDS ASSET / ROLE")
}

data class FeatureCategoryItem(
    val number: Int,
    val titleEn: String,
    val titleBn: String,
    val bullets: List<String>,
    val auditStatus: FeatureAuditStatus,
    val honestStatusNote: String,
    val quickCommand: String? = null
)

data class WeatherState(
    val city: String = "Dhaka",
    val tempCelsius: Int = 28,
    val condition: String = "Clear Sky",
    val highCelsius: Int = 32,
    val lowCelsius: Int = 22,
    val humidityPercent: Int = 65,
    val isLive: Boolean = false,
    val lastUpdated: String = "Default baseline"
)

object FeatureCatalog {
    val allCategories: List<FeatureCategoryItem> = listOf(
        FeatureCategoryItem(
            number = 1,
            titleEn = "1. Home Screen & 3D Character",
            titleBn = "১. হোম স্ক্রিন ও ক্যারেক্টার ভিউ",
            bullets = listOf(
                "Character avatar on Home Page",
                "3D & 2D display modes",
                "Full screen character view (Same Page)",
                "Bengali & English greeting message",
                "Chat, settings, weather shortcuts",
                "Live voice status indicator"
            ),
            auditStatus = FeatureAuditStatus.PARTIAL_CONFIG_REQUIRED,
            honestStatusNote = "2D character illustration (Reference Image 2) + holographic state effects implemented; true rigged .glb/.gltf 3D mesh asset is not bundled.",
            quickCommand = null
        ),
        FeatureCategoryItem(
            number = 2,
            titleEn = "2. Theme & Appearance Settings",
            titleBn = "২. থিম ও অ্যাপিয়ারেন্স সেটিংস",
            bullets = listOf(
                "Light / Dark cosmic themes",
                "Dynamic neon cyan & purple schemes",
                "Custom HEX background (#RRGGBB)",
                "Character display & scale controls",
                "Fullscreen close & back button"
            ),
            auditStatus = FeatureAuditStatus.VERIFIED_WORKING,
            honestStatusNote = "Light, Dark, Full Character, and Custom HEX background color picker persisted in DataStore.",
            quickCommand = null
        ),
        FeatureCategoryItem(
            number = 3,
            titleEn = "3. Voice Assistant & Audio",
            titleBn = "৩. ভয়েস অ্যাসিস্ট্যান্ট ও অডিও",
            bullets = listOf(
                "Microphone permission check",
                "Speech-to-text (STT bn-BD / en-US)",
                "Text-to-speech (TTS) with Bangla check",
                "TTS-to-STT feedback loop prevention",
                "Heuristic Voice Match profile",
                "Tap to Talk & session listening"
            ),
            auditStatus = FeatureAuditStatus.VERIFIED_WORKING,
            honestStatusNote = "Android SpeechRecognizer & TextToSpeech integrated; uses Tap-to-Talk session STT (no offline DSP wake-word binary bundled).",
            quickCommand = "জার্ভিস অন"
        ),
        FeatureCategoryItem(
            number = 4,
            titleEn = "4. AI Chat & Conversation",
            titleBn = "৪. এআই চ্যাট ও কথোপকথন",
            bullets = listOf(
                "Multi-turn conversation context",
                "User & JARVIS speech bubbles",
                "Persistent Room chat history",
                "Voice & text input bar",
                "Typed error & fallback handling"
            ),
            auditStatus = FeatureAuditStatus.VERIFIED_WORKING,
            honestStatusNote = "Full chat transcript persisted in Room with Gemini 3.8 Flash / 4 Argon & local rule fallback.",
            quickCommand = "হ্যালো জার্ভিস"
        ),
        FeatureCategoryItem(
            number = 5,
            titleEn = "5. Search & Memory",
            titleBn = "৫. সার্চ ও মেমোরি",
            bullets = listOf(
                "Web search & browser launch",
                "Local SQLite/Room memory database",
                "Saved preferences & snapshots",
                "Searchable conversation history",
                "Clear memory controls"
            ),
            auditStatus = FeatureAuditStatus.VERIFIED_WORKING,
            honestStatusNote = "Browser search tool and Room memory table implemented with clear/reset controls.",
            quickCommand = "Search Android 15 features"
        ),
        FeatureCategoryItem(
            number = 6,
            titleEn = "6. Settings & Intelligence",
            titleBn = "৬. সেটিংস ও এআই ইন্টেলিজেন্স",
            bullets = listOf(
                "Gemini 3.8 Flash & 4 Argon selector",
                "OpenAI, DeepSeek, OpenRouter & Custom",
                "Masked API key & header auth",
                "6-stage real Test Connection check",
                "Legacy model migration helper"
            ),
            auditStatus = FeatureAuditStatus.VERIFIED_WORKING,
            honestStatusNote = "Per-provider keys, models, endpoints, and live HTTP generation diagnostics verified.",
            quickCommand = null
        ),
        FeatureCategoryItem(
            number = 7,
            titleEn = "7. Phone & App Automation",
            titleBn = "৭. ফোন ও অ্যাপ অটোমেশন",
            bullets = listOf(
                "Open installed apps & websites",
                "Click / scroll / type / back / home",
                "Contacts & dialer integration",
                "Media volume & mute control",
                "Post-action verification receipts"
            ),
            auditStatus = FeatureAuditStatus.VERIFIED_WORKING,
            honestStatusNote = "ToolExecutor + VerificationEngine execute intents and Accessibility actions with receipts.",
            quickCommand = "ভলিউম ৫০% করো"
        ),
        FeatureCategoryItem(
            number = 8,
            titleEn = "8. Screen Understanding & Visual Assistance",
            titleBn = "৮. স্ক্রিন রিডিং ও ভিজ্যুয়াল সহায়তা",
            bullets = listOf(
                "Read visible UI hierarchy via Accessibility",
                "Find text elements on screen",
                "Foreground package & activity tracking",
                "Screen context injection into AI prompt",
                "Action verification status"
            ),
            auditStatus = FeatureAuditStatus.PARTIAL_CONFIG_REQUIRED,
            honestStatusNote = "Accessibility hierarchy text extraction works when JARVIS Accessibility Service is enabled by the user.",
            quickCommand = "স্ক্রিনে কী আছে পড়ো"
        ),
        FeatureCategoryItem(
            number = 9,
            titleEn = "9. WhatsApp & Auto Reply",
            titleBn = "৯. হোয়াটসঅ্যাপ ও নোটিফিকেশন লিসেনার",
            bullets = listOf(
                "Notification listener permission check",
                "Capture incoming notification titles/text",
                "Open WhatsApp via package intent",
                "Reply drafting assistance",
                "Opt-in notification monitor"
            ),
            auditStatus = FeatureAuditStatus.PARTIAL_CONFIG_REQUIRED,
            honestStatusNote = "NotificationListenerService captures posted notifications when granted; direct automated background sending is restricted for user safety.",
            quickCommand = "Open WhatsApp"
        ),
        FeatureCategoryItem(
            number = 10,
            titleEn = "10. YouTube & Media",
            titleBn = "১০. ইউটিউব ও মিডিয়া কন্ট্রোল",
            bullets = listOf(
                "Launch YouTube & search queries",
                "System media volume % control",
                "Mute / unmute with previous level restore",
                "Camera & Gallery intent helpers"
            ),
            auditStatus = FeatureAuditStatus.VERIFIED_WORKING,
            honestStatusNote = "AudioManager volume control and YouTube/Media intent launching verified.",
            quickCommand = "Open YouTube"
        ),
        FeatureCategoryItem(
            number = 11,
            titleEn = "11. Minecraft & Game Coach",
            titleBn = "১১. মাইনক্রাফট ও গেম কোচ",
            bullets = listOf(
                "AI strategy & crafting guide queries",
                "Objectives & troubleshooting tips",
                "Voice query support while gaming",
                "Silent media mode option"
            ),
            auditStatus = FeatureAuditStatus.PARTIAL_CONFIG_REQUIRED,
            honestStatusNote = "Conversational coaching via AI chat works; real-time in-game input injection is not implemented.",
            quickCommand = "Give me Minecraft survival tips"
        ),
        FeatureCategoryItem(
            number = 12,
            titleEn = "12. Smart Task Planner",
            titleBn = "১২. স্মার্ট টাস্ক প্ল্যানার",
            bullets = listOf(
                "Persistent Room task queue",
                "Multi-step plan generation",
                "Checkpoint-safe step tracking",
                "Pause, resume & cancel controls",
                "Call interruption auto-pause"
            ),
            auditStatus = FeatureAuditStatus.VERIFIED_WORKING,
            honestStatusNote = "WorkTaskManager + TaskInterruptionManager persist checkpoints and receipts in Room.",
            quickCommand = null
        ),
        FeatureCategoryItem(
            number = 13,
            titleEn = "13. Anti-Theft & Device Security",
            titleBn = "১৩. অ্যান্টি-থেফট ও ডিভাইস সিকিউরিটি",
            bullets = listOf(
                "Speaker verification heuristic check",
                "Strict mode for sensitive tools",
                "Emergency Stop immediate halt",
                "Permission & risk-level gating"
            ),
            auditStatus = FeatureAuditStatus.REQUIRES_ASSET_OR_ROLE,
            honestStatusNote = "Strict mode & Emergency Stop work; DeviceAdmin lock/wipe is intentionally not registered to comply with Play policy.",
            quickCommand = null
        ),
        FeatureCategoryItem(
            number = 14,
            titleEn = "14. Premium Lock & Permissions",
            titleBn = "১৪. প্রিমিয়াম লক ও ১৫টি পারমিশন কার্ড",
            bullets = listOf(
                "10 runtime permission cards with real OS status",
                "5 special access & background cards",
                "Strict mode toggle for protected settings",
                "Auto-refresh on Settings return"
            ),
            auditStatus = FeatureAuditStatus.VERIFIED_WORKING,
            honestStatusNote = "All 15 permission & system access cards query live Android OS APIs via PermissionStatusChecker.",
            quickCommand = null
        ),
        FeatureCategoryItem(
            number = 15,
            titleEn = "15. Weather & Location",
            titleBn = "১৫. আবহাওয়া ও লোকেশন (Weather)",
            bullets = listOf(
                "Location permission check",
                "Live weather fetch (Open-Meteo API)",
                "Temperature (°C), High/Low & condition",
                "Retry on failure & offline baseline"
            ),
            auditStatus = FeatureAuditStatus.VERIFIED_WORKING,
            honestStatusNote = "Home screen Weather card fetches live Dhaka/local weather via Open-Meteo with retry.",
            quickCommand = null
        ),
        FeatureCategoryItem(
            number = 16,
            titleEn = "16. Background Service & Permissions",
            titleBn = "১৬. ব্যাকগ্রাউন্ড সার্ভিস ও রিকভারি",
            bullets = listOf(
                "Foreground service with persistent notification",
                "Synchronized ON / OFF / PAUSED states",
                "Task pause & resume on call interruption",
                "Notification Stop & Pause actions"
            ),
            auditStatus = FeatureAuditStatus.VERIFIED_WORKING,
            honestStatusNote = "JarvisBackgroundService synchronizes with MainViewModel and stops cleanly on OFF.",
            quickCommand = null
        ),
        FeatureCategoryItem(
            number = 17,
            titleEn = "17. Audio, Sound & Headset",
            titleBn = "১৭. অডিও, সাউন্ড ও হেডসেট",
            bullets = listOf(
                "AudioManager stream volume control",
                "TTS speech rate & pitch sliders",
                "Bengali & English voice selector",
                "Bluetooth permission & settings shortcut"
            ),
            auditStatus = FeatureAuditStatus.VERIFIED_WORKING,
            honestStatusNote = "TtsManager, AudioManager volume tools, and Bluetooth settings navigator implemented.",
            quickCommand = "ব্লুটুথ সেটিংস খোল"
        ),
        FeatureCategoryItem(
            number = 18,
            titleEn = "18. Custom 3D / Web Screens & Assets",
            titleBn = "১৮. কাস্টম ৩ডি / ওয়েব স্ক্রিন ও অ্যাসেট",
            bullets = listOf(
                "Reference Image 2 character avatar asset",
                "Reference Image 1 blueprint & pose guide",
                "Holographic Canvas energy rings",
                "Asset loading status verification"
            ),
            auditStatus = FeatureAuditStatus.PARTIAL_CONFIG_REQUIRED,
            honestStatusNote = "Bundled 2D character & pose assets render with Canvas shaders; no Three.js/.glb 3D mesh file is present in assets.",
            quickCommand = null
        ),
        FeatureCategoryItem(
            number = 19,
            titleEn = "19. Social / Community & Navigation",
            titleBn = "১৯. সোশ্যাল ও নেভিগেশন বার",
            bullets = listOf(
                "Home / Chat / Tasks / Perms / Settings navigation",
                "Full Screen Character view toggle",
                "System Settings deep-link fallbacks",
                "Contacts & dialer shortcuts"
            ),
            auditStatus = FeatureAuditStatus.VERIFIED_WORKING,
            honestStatusNote = "Bottom navigation bar and in-page Full Screen Character mode preserve conversation state.",
            quickCommand = "Open Contacts"
        ),
        FeatureCategoryItem(
            number = 20,
            titleEn = "20. Android Project & Technical",
            titleBn = "২০. অ্যান্ড্রয়েড প্রজেক্ট ও টেকনিক্যাল আর্কিটেকচার",
            bullets = listOf(
                "Kotlin + Jetpack Compose Material 3",
                "AGP 9.1.1 + Room 2.6.1 + DataStore",
                "OkHttp 4.12 REST client",
                "Unit & Robolectric JVM test suite"
            ),
            auditStatus = FeatureAuditStatus.VERIFIED_WORKING,
            honestStatusNote = "Compiles cleanly with assembleDebug and passes testDebugUnitTest.",
            quickCommand = null
        ),
        FeatureCategoryItem(
            number = 21,
            titleEn = "21. Diagnostics, Testing & Future Features",
            titleBn = "২১. ডায়াগনস্টিকস, টেস্টিং ও অডিট রিপোর্ট",
            bullets = listOf(
                "10-point live system diagnostics",
                "6-stage API connection verifier",
                "Verified vs Partial feature audit",
                "Action execution & verification log"
            ),
            auditStatus = FeatureAuditStatus.VERIFIED_WORKING,
            honestStatusNote = "DiagnosticsManager & AiProviderManager provide real-time health and honest audit reporting.",
            quickCommand = null
        )
    )
}
