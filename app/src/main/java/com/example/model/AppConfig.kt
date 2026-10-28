package com.example.model

data class ProviderProfile(
    val id: String,
    val displayName: String,
    val defaultModel: String,
    val supportedModels: List<String>,
    val apiKey: String = "",
    val endpoint: String = "",
    val modelName: String = "",
    val isConfigured: Boolean = false
)

data class AppConfig(
    val language: String = "bn",
    val assistantVoiceEnabled: Boolean = true,
    val wakeWordEnabled: Boolean = false,
    val wakePhrase: String = "JARVIS",
    val voiceMatchEnabled: Boolean = false,
    val strictModeEnabled: Boolean = false,
    val backgroundServiceEnabled: Boolean = true,
    val aiProvider: String = "GEMINI",
    val apiKey: String = "",
    val customEndpoint: String = "",
    val modelName: String = DEFAULT_GEMINI_MODEL,
    val memoryEnabled: Boolean = true,
    val silentMediaMode: Boolean = true,
    val ttsSpeechRate: Float = 1.0f,
    val ttsPitch: Float = 1.0f,
    val selectedVoiceId: String = "",
    val geminiApiKey: String = "",
    val geminiModel: String = DEFAULT_GEMINI_MODEL,
    val geminiProtocol: String = "V1BETA_INTERACTIONS_COMPAT",
    val openaiApiKey: String = "",
    val openaiModel: String = "gpt-4o-mini",
    val deepseekApiKey: String = "",
    val deepseekModel: String = "deepseek-chat",
    val openrouterApiKey: String = "",
    val openrouterModel: String = "google/gemini-3.8-flash",
    val customDisplayName: String = "Custom OpenAI Endpoint",
    val customApiKey: String = "",
    val customModel: String = "",
    val otherDisplayName: String = "Other Custom AI",
    val otherApiKey: String = "",
    val otherEndpoint: String = "",
    val otherModel: String = "",
    // Theme & 3D/Character Avatar Appearance Settings (Reference Image 1)
    val themeMode: String = "DARK", // LIGHT, DARK, 3D_MODEL_FULL, 3D_MODEL, CUSTOM_BG
    val showCharacterAvatar: Boolean = true,
    val customBgHex: String = "#070B13",
    val accentHex: String = "#00E5FF",
    val characterScale: Float = 1.0f,
    val animationIntensity: Float = 1.0f,
    val lastSuccessfulTestTimestamp: Long = 0L,
    val lastTestStatusSummary: String = "Not tested yet"
) {
    fun getKeyForProvider(prov: String): String {
        return when (prov) {
            "GEMINI" -> if (geminiApiKey.isNotBlank()) geminiApiKey else if (aiProvider == "GEMINI") apiKey else ""
            "OPENAI" -> if (openaiApiKey.isNotBlank()) openaiApiKey else if (aiProvider == "OPENAI") apiKey else ""
            "DEEPSEEK" -> if (deepseekApiKey.isNotBlank()) deepseekApiKey else if (aiProvider == "DEEPSEEK") apiKey else ""
            "OPENROUTER" -> if (openrouterApiKey.isNotBlank()) openrouterApiKey else if (aiProvider == "OPENROUTER") apiKey else ""
            "CUSTOM" -> if (customApiKey.isNotBlank()) customApiKey else if (aiProvider == "CUSTOM") apiKey else ""
            "OTHER" -> if (otherApiKey.isNotBlank()) otherApiKey else if (aiProvider == "OTHER") apiKey else ""
            else -> apiKey
        }
    }

    fun getModelForProvider(prov: String): String {
        return when (prov) {
            "GEMINI" -> if (geminiModel.isBlank()) DEFAULT_GEMINI_MODEL else geminiModel
            "OPENAI" -> if (openaiModel.isBlank()) "gpt-4o-mini" else openaiModel
            "DEEPSEEK" -> if (deepseekModel.isBlank()) "deepseek-chat" else deepseekModel
            "OPENROUTER" -> if (openrouterModel.isBlank()) "google/gemini-3.8-flash" else openrouterModel
            "CUSTOM" -> customModel
            "OTHER" -> otherModel
            else -> if (modelName.isBlank()) DEFAULT_GEMINI_MODEL else modelName
        }
    }

    fun getEndpointForProvider(prov: String): String {
        return when (prov) {
            "GEMINI" -> "https://generativelanguage.googleapis.com/v1beta"
            "OPENAI" -> "https://api.openai.com/v1/chat/completions"
            "DEEPSEEK" -> "https://api.deepseek.com/chat/completions"
            "OPENROUTER" -> "https://openrouter.ai/api/v1/chat/completions"
            "CUSTOM" -> customEndpoint.ifBlank { "https://api.openai.com/v1/chat/completions" }
            "OTHER" -> otherEndpoint.ifBlank { "https://api.openai.com/v1/chat/completions" }
            else -> customEndpoint
        }
    }

    fun isLegacyGeminiModel(): Boolean {
        val m = getModelForProvider("GEMINI")
        return m == "gemini-2.5-flash" || m == "gemini-1.5-flash" || m == "gemini-pro"
    }

    companion object {
        const val DEFAULT_GEMINI_MODEL = "gemini-3.8-flash"
        const val ADVANCED_GEMINI_MODEL = "gemini-4-argon"

        val OFFICIAL_GEMINI_MODELS = listOf(
            "gemini-3.8-flash",
            "gemini-4-argon",
            "gemini-2.5-flash",
            "gemini-2.0-flash"
        )

        val OPENAI_MODELS = listOf(
            "gpt-4o-mini",
            "gpt-4o",
            "o3-mini"
        )

        val DEEPSEEK_MODELS = listOf(
            "deepseek-chat",
            "deepseek-reasoner"
        )

        val OPENROUTER_MODELS = listOf(
            "google/gemini-3.8-flash",
            "google/gemini-2.5-flash",
            "openai/gpt-4o-mini",
            "deepseek/deepseek-chat"
        )

        fun default(): AppConfig = AppConfig()
    }
}
