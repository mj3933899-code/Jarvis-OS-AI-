package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppConfig
import com.example.model.SetupProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "jarvis_settings")

class JarvisPreferences(private val context: Context) {

    object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val ASSISTANT_VOICE_ENABLED = booleanPreferencesKey("assistant_voice_enabled")
        val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val WAKE_PHRASE = stringPreferencesKey("wake_phrase")
        val VOICE_MATCH_ENABLED = booleanPreferencesKey("voice_match_enabled")
        val STRICT_MODE_ENABLED = booleanPreferencesKey("strict_mode_enabled")
        val BACKGROUND_SERVICE_ENABLED = booleanPreferencesKey("background_service_enabled")
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val API_KEY = stringPreferencesKey("api_key")
        val CUSTOM_ENDPOINT = stringPreferencesKey("custom_endpoint")
        val MODEL_NAME = stringPreferencesKey("model_name")
        val MEMORY_ENABLED = booleanPreferencesKey("memory_enabled")
        val SILENT_MEDIA_MODE = booleanPreferencesKey("silent_media_mode")
        val TTS_RATE = floatPreferencesKey("tts_rate")
        val TTS_PITCH = floatPreferencesKey("tts_pitch")
        val SELECTED_VOICE_ID = stringPreferencesKey("selected_voice_id")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key_v1")
        val GEMINI_MODEL = stringPreferencesKey("gemini_model_v1")
        val GEMINI_PROTOCOL = stringPreferencesKey("gemini_protocol_v1")
        val OPENAI_API_KEY = stringPreferencesKey("openai_api_key_v1")
        val OPENAI_MODEL = stringPreferencesKey("openai_model_v1")
        val DEEPSEEK_API_KEY = stringPreferencesKey("deepseek_api_key_v1")
        val DEEPSEEK_MODEL = stringPreferencesKey("deepseek_model_v1")
        val OPENROUTER_API_KEY = stringPreferencesKey("openrouter_api_key_v1")
        val OPENROUTER_MODEL = stringPreferencesKey("openrouter_model_v1")
        val CUSTOM_DISPLAY_NAME = stringPreferencesKey("custom_display_name_v1")
        val CUSTOM_API_KEY = stringPreferencesKey("custom_api_key_v1")
        val CUSTOM_MODEL = stringPreferencesKey("custom_model_v1")
        val OTHER_DISPLAY_NAME = stringPreferencesKey("other_display_name_v1")
        val OTHER_API_KEY = stringPreferencesKey("other_api_key_v1")
        val OTHER_ENDPOINT = stringPreferencesKey("other_endpoint_v1")
        val OTHER_MODEL = stringPreferencesKey("other_model_v1")
        // Appearance & Theme
        val THEME_MODE = stringPreferencesKey("theme_mode_v1")
        val SHOW_CHARACTER_AVATAR = booleanPreferencesKey("show_character_avatar_v1")
        val CUSTOM_BG_HEX = stringPreferencesKey("custom_bg_hex_v1")
        val ACCENT_HEX = stringPreferencesKey("accent_hex_v1")
        val CHARACTER_SCALE = floatPreferencesKey("character_scale_v1")
        val ANIMATION_INTENSITY = floatPreferencesKey("animation_intensity_v1")
        val LAST_SUCCESSFUL_TEST_TS = longPreferencesKey("last_successful_test_ts_v1")
        val LAST_TEST_STATUS_SUMMARY = stringPreferencesKey("last_test_status_summary_v1")
        val SNAPSHOT_CONFIG_JSON = stringPreferencesKey("snapshot_config_json_v2")
        val SETUP_COMPLETED = booleanPreferencesKey("setup_completed_v1")
        val SETUP_STEP = intPreferencesKey("setup_current_step_v1")
    }

    val setupProgressFlow: Flow<SetupProgress> = context.dataStore.data.map { prefs ->
        SetupProgress(
            isCompleted = prefs[Keys.SETUP_COMPLETED] ?: false,
            currentStep = prefs[Keys.SETUP_STEP] ?: 0
        )
    }

    val configFlow: Flow<AppConfig> = context.dataStore.data.map { prefs ->
        AppConfig(
            language = prefs[Keys.LANGUAGE] ?: "bn",
            assistantVoiceEnabled = prefs[Keys.ASSISTANT_VOICE_ENABLED] ?: true,
            wakeWordEnabled = prefs[Keys.WAKE_WORD_ENABLED] ?: false,
            wakePhrase = prefs[Keys.WAKE_PHRASE] ?: "JARVIS",
            voiceMatchEnabled = prefs[Keys.VOICE_MATCH_ENABLED] ?: false,
            strictModeEnabled = prefs[Keys.STRICT_MODE_ENABLED] ?: false,
            backgroundServiceEnabled = prefs[Keys.BACKGROUND_SERVICE_ENABLED] ?: true,
            aiProvider = prefs[Keys.AI_PROVIDER] ?: "GEMINI",
            apiKey = prefs[Keys.API_KEY] ?: "",
            customEndpoint = prefs[Keys.CUSTOM_ENDPOINT] ?: "",
            modelName = prefs[Keys.MODEL_NAME] ?: AppConfig.DEFAULT_GEMINI_MODEL,
            memoryEnabled = prefs[Keys.MEMORY_ENABLED] ?: true,
            silentMediaMode = prefs[Keys.SILENT_MEDIA_MODE] ?: true,
            ttsSpeechRate = prefs[Keys.TTS_RATE] ?: 1.0f,
            ttsPitch = prefs[Keys.TTS_PITCH] ?: 1.0f,
            selectedVoiceId = prefs[Keys.SELECTED_VOICE_ID] ?: "",
            geminiApiKey = prefs[Keys.GEMINI_API_KEY] ?: "",
            geminiModel = prefs[Keys.GEMINI_MODEL] ?: AppConfig.DEFAULT_GEMINI_MODEL,
            geminiProtocol = prefs[Keys.GEMINI_PROTOCOL] ?: "V1BETA_INTERACTIONS_COMPAT",
            openaiApiKey = prefs[Keys.OPENAI_API_KEY] ?: "",
            openaiModel = prefs[Keys.OPENAI_MODEL] ?: "gpt-4o-mini",
            deepseekApiKey = prefs[Keys.DEEPSEEK_API_KEY] ?: "",
            deepseekModel = prefs[Keys.DEEPSEEK_MODEL] ?: "deepseek-chat",
            openrouterApiKey = prefs[Keys.OPENROUTER_API_KEY] ?: "",
            openrouterModel = prefs[Keys.OPENROUTER_MODEL] ?: "google/gemini-3.8-flash",
            customDisplayName = prefs[Keys.CUSTOM_DISPLAY_NAME] ?: "Custom OpenAI Endpoint",
            customApiKey = prefs[Keys.CUSTOM_API_KEY] ?: "",
            customModel = prefs[Keys.CUSTOM_MODEL] ?: "",
            otherDisplayName = prefs[Keys.OTHER_DISPLAY_NAME] ?: "Other Custom AI",
            otherApiKey = prefs[Keys.OTHER_API_KEY] ?: "",
            otherEndpoint = prefs[Keys.OTHER_ENDPOINT] ?: "",
            otherModel = prefs[Keys.OTHER_MODEL] ?: "",
            themeMode = prefs[Keys.THEME_MODE] ?: "DARK",
            showCharacterAvatar = prefs[Keys.SHOW_CHARACTER_AVATAR] ?: true,
            customBgHex = prefs[Keys.CUSTOM_BG_HEX] ?: "#070B13",
            accentHex = prefs[Keys.ACCENT_HEX] ?: "#00E5FF",
            characterScale = prefs[Keys.CHARACTER_SCALE] ?: 1.0f,
            animationIntensity = prefs[Keys.ANIMATION_INTENSITY] ?: 1.0f,
            lastSuccessfulTestTimestamp = prefs[Keys.LAST_SUCCESSFUL_TEST_TS] ?: 0L,
            lastTestStatusSummary = prefs[Keys.LAST_TEST_STATUS_SUMMARY] ?: "Not tested yet"
        )
    }

    suspend fun saveSetupStep(step: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SETUP_STEP] = step
        }
    }

    suspend fun completeSetup() {
        context.dataStore.edit { prefs ->
            prefs[Keys.SETUP_COMPLETED] = true
            prefs[Keys.SETUP_STEP] = 3
        }
    }

    suspend fun resetSetupProgress() {
        context.dataStore.edit { prefs ->
            prefs[Keys.SETUP_COMPLETED] = false
            prefs[Keys.SETUP_STEP] = 0
        }
    }

    suspend fun commitConfig(newConfig: AppConfig) {
        val current = configFlow.first()
        saveSnapshotJson(current)
        saveActiveConfig(newConfig)
    }

    suspend fun migrateLegacyModelToCurrentDefault(): AppConfig {
        val current = configFlow.first()
        saveSnapshotJson(current)
        val migrated = current.copy(
            modelName = AppConfig.DEFAULT_GEMINI_MODEL,
            geminiModel = AppConfig.DEFAULT_GEMINI_MODEL,
            geminiProtocol = "V1BETA_INTERACTIONS_COMPAT"
        )
        saveActiveConfig(migrated)
        return migrated
    }

    suspend fun saveActiveConfig(config: AppConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = config.language
            prefs[Keys.ASSISTANT_VOICE_ENABLED] = config.assistantVoiceEnabled
            prefs[Keys.WAKE_WORD_ENABLED] = config.wakeWordEnabled
            prefs[Keys.WAKE_PHRASE] = config.wakePhrase
            prefs[Keys.VOICE_MATCH_ENABLED] = config.voiceMatchEnabled
            prefs[Keys.STRICT_MODE_ENABLED] = config.strictModeEnabled
            prefs[Keys.BACKGROUND_SERVICE_ENABLED] = config.backgroundServiceEnabled
            prefs[Keys.AI_PROVIDER] = config.aiProvider
            prefs[Keys.API_KEY] = config.apiKey
            prefs[Keys.CUSTOM_ENDPOINT] = config.customEndpoint
            prefs[Keys.MODEL_NAME] = config.modelName
            prefs[Keys.MEMORY_ENABLED] = config.memoryEnabled
            prefs[Keys.SILENT_MEDIA_MODE] = config.silentMediaMode
            prefs[Keys.TTS_RATE] = config.ttsSpeechRate
            prefs[Keys.TTS_PITCH] = config.ttsPitch
            prefs[Keys.SELECTED_VOICE_ID] = config.selectedVoiceId
            prefs[Keys.GEMINI_API_KEY] = config.geminiApiKey
            prefs[Keys.GEMINI_MODEL] = config.geminiModel
            prefs[Keys.GEMINI_PROTOCOL] = config.geminiProtocol
            prefs[Keys.OPENAI_API_KEY] = config.openaiApiKey
            prefs[Keys.OPENAI_MODEL] = config.openaiModel
            prefs[Keys.DEEPSEEK_API_KEY] = config.deepseekApiKey
            prefs[Keys.DEEPSEEK_MODEL] = config.deepseekModel
            prefs[Keys.OPENROUTER_API_KEY] = config.openrouterApiKey
            prefs[Keys.OPENROUTER_MODEL] = config.openrouterModel
            prefs[Keys.CUSTOM_DISPLAY_NAME] = config.customDisplayName
            prefs[Keys.CUSTOM_API_KEY] = config.customApiKey
            prefs[Keys.CUSTOM_MODEL] = config.customModel
            prefs[Keys.OTHER_DISPLAY_NAME] = config.otherDisplayName
            prefs[Keys.OTHER_API_KEY] = config.otherApiKey
            prefs[Keys.OTHER_ENDPOINT] = config.otherEndpoint
            prefs[Keys.OTHER_MODEL] = config.otherModel
            prefs[Keys.THEME_MODE] = config.themeMode
            prefs[Keys.SHOW_CHARACTER_AVATAR] = config.showCharacterAvatar
            prefs[Keys.CUSTOM_BG_HEX] = config.customBgHex
            prefs[Keys.ACCENT_HEX] = config.accentHex
            prefs[Keys.CHARACTER_SCALE] = config.characterScale
            prefs[Keys.ANIMATION_INTENSITY] = config.animationIntensity
            prefs[Keys.LAST_SUCCESSFUL_TEST_TS] = config.lastSuccessfulTestTimestamp
            prefs[Keys.LAST_TEST_STATUS_SUMMARY] = config.lastTestStatusSummary
        }
    }

    private suspend fun saveSnapshotJson(config: AppConfig) {
        try {
            val json = JSONObject().apply {
                put("language", config.language)
                put("assistantVoiceEnabled", config.assistantVoiceEnabled)
                put("wakeWordEnabled", config.wakeWordEnabled)
                put("wakePhrase", config.wakePhrase)
                put("voiceMatchEnabled", config.voiceMatchEnabled)
                put("strictModeEnabled", config.strictModeEnabled)
                put("backgroundServiceEnabled", config.backgroundServiceEnabled)
                put("aiProvider", config.aiProvider)
                put("apiKey", config.apiKey)
                put("customEndpoint", config.customEndpoint)
                put("modelName", config.modelName)
                put("memoryEnabled", config.memoryEnabled)
                put("silentMediaMode", config.silentMediaMode)
                put("ttsSpeechRate", config.ttsSpeechRate.toDouble())
                put("ttsPitch", config.ttsPitch.toDouble())
                put("selectedVoiceId", config.selectedVoiceId)
                put("geminiApiKey", config.geminiApiKey)
                put("geminiModel", config.geminiModel)
                put("geminiProtocol", config.geminiProtocol)
                put("openaiApiKey", config.openaiApiKey)
                put("openaiModel", config.openaiModel)
                put("deepseekApiKey", config.deepseekApiKey)
                put("deepseekModel", config.deepseekModel)
                put("openrouterApiKey", config.openrouterApiKey)
                put("openrouterModel", config.openrouterModel)
                put("customDisplayName", config.customDisplayName)
                put("customApiKey", config.customApiKey)
                put("customModel", config.customModel)
                put("otherDisplayName", config.otherDisplayName)
                put("otherApiKey", config.otherApiKey)
                put("otherEndpoint", config.otherEndpoint)
                put("otherModel", config.otherModel)
                put("themeMode", config.themeMode)
                put("showCharacterAvatar", config.showCharacterAvatar)
                put("customBgHex", config.customBgHex)
                put("accentHex", config.accentHex)
            }
            context.dataStore.edit { prefs ->
                prefs[Keys.SNAPSHOT_CONFIG_JSON] = json.toString()
            }
        } catch (_: Exception) {}
    }

    suspend fun restorePreviousSettings(): Boolean {
        val prefs = context.dataStore.data.first()
        val jsonStr = prefs[Keys.SNAPSHOT_CONFIG_JSON] ?: return false
        return try {
            val json = JSONObject(jsonStr)
            val restored = AppConfig(
                language = json.optString("language", "bn"),
                assistantVoiceEnabled = json.optBoolean("assistantVoiceEnabled", true),
                wakeWordEnabled = json.optBoolean("wakeWordEnabled", false),
                wakePhrase = json.optString("wakePhrase", "JARVIS"),
                voiceMatchEnabled = json.optBoolean("voiceMatchEnabled", false),
                strictModeEnabled = json.optBoolean("strictModeEnabled", false),
                backgroundServiceEnabled = json.optBoolean("backgroundServiceEnabled", true),
                aiProvider = json.optString("aiProvider", "GEMINI"),
                apiKey = json.optString("apiKey", ""),
                customEndpoint = json.optString("customEndpoint", ""),
                modelName = json.optString("modelName", AppConfig.DEFAULT_GEMINI_MODEL),
                memoryEnabled = json.optBoolean("memoryEnabled", true),
                silentMediaMode = json.optBoolean("silentMediaMode", true),
                ttsSpeechRate = json.optDouble("ttsSpeechRate", 1.0).toFloat(),
                ttsPitch = json.optDouble("ttsPitch", 1.0).toFloat(),
                selectedVoiceId = json.optString("selectedVoiceId", ""),
                geminiApiKey = json.optString("geminiApiKey", ""),
                geminiModel = json.optString("geminiModel", AppConfig.DEFAULT_GEMINI_MODEL),
                geminiProtocol = json.optString("geminiProtocol", "V1BETA_INTERACTIONS_COMPAT"),
                openaiApiKey = json.optString("openaiApiKey", ""),
                openaiModel = json.optString("openaiModel", "gpt-4o-mini"),
                deepseekApiKey = json.optString("deepseekApiKey", ""),
                deepseekModel = json.optString("deepseekModel", "deepseek-chat"),
                openrouterApiKey = json.optString("openrouterApiKey", ""),
                openrouterModel = json.optString("openrouterModel", "google/gemini-3.8-flash"),
                customDisplayName = json.optString("customDisplayName", "Custom OpenAI Endpoint"),
                customApiKey = json.optString("customApiKey", ""),
                customModel = json.optString("customModel", ""),
                otherDisplayName = json.optString("otherDisplayName", "Other Custom AI"),
                otherApiKey = json.optString("otherApiKey", ""),
                otherEndpoint = json.optString("otherEndpoint", ""),
                otherModel = json.optString("otherModel", ""),
                themeMode = json.optString("themeMode", "DARK"),
                showCharacterAvatar = json.optBoolean("showCharacterAvatar", true),
                customBgHex = json.optString("customBgHex", "#070B13"),
                accentHex = json.optString("accentHex", "#00E5FF")
            )
            saveActiveConfig(restored)
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun resetToDefault(keepApiKey: Boolean = false) {
        val current = configFlow.first()
        saveSnapshotJson(current)
        val default = AppConfig.default().let {
            if (keepApiKey) it.copy(
                apiKey = current.apiKey,
                geminiApiKey = current.geminiApiKey,
                openaiApiKey = current.openaiApiKey,
                deepseekApiKey = current.deepseekApiKey,
                openrouterApiKey = current.openrouterApiKey,
                customApiKey = current.customApiKey,
                otherApiKey = current.otherApiKey
            ) else it
        }
        saveActiveConfig(default)
    }
}
