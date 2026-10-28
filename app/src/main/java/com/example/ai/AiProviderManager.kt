package com.example.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.JarvisPreferences
import com.example.model.AppConfig
import com.example.model.ToolCallRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class AiProviderManager(
    private val context: Context,
    private val preferences: JarvisPreferences
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val _lastDiagnostics = MutableStateFlow<ConnectionDiagnostics?>(null)
    val lastDiagnostics: StateFlow<ConnectionDiagnostics?> = _lastDiagnostics.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun testConnection(): Pair<Boolean, String> {
        val diag = runConnectionDiagnostics()
        return Pair(diag.generationSucceeded, "${diag.outcome.summaryLabel} — ${diag.sampleOutput.ifBlank { diag.model }}")
    }

    suspend fun runConnectionDiagnostics(overrideConfig: AppConfig? = null): ConnectionDiagnostics = withContext(Dispatchers.IO) {
        if (_isTestingConnection.value) {
            return@withContext _lastDiagnostics.value ?: ConnectionDiagnostics(
                provider = "GEMINI",
                model = AppConfig.DEFAULT_GEMINI_MODEL,
                endpoint = "",
                keySaved = false,
                configValidated = false,
                networkReachable = false,
                authAccepted = false,
                modelReachable = false,
                generationSucceeded = false,
                outcome = ApiCallOutcome.Cancelled("Connection test already in progress")
            )
        }
        _isTestingConnection.value = true
        try {
            val config = overrideConfig ?: preferences.configFlow.first()
            val provider = config.aiProvider
            val apiKey = config.getKeyForProvider(provider).trim()
            val model = config.getModelForProvider(provider).trim()
            val endpoint = config.getEndpointForProvider(provider).trim()

            val keySaved = apiKey.isNotBlank()
            val configValid = model.isNotBlank() && endpoint.startsWith("http")
            val netReachable = isNetworkAvailable()

            if (!keySaved) {
                val diag = ConnectionDiagnostics(
                    provider = provider,
                    model = model,
                    endpoint = endpoint,
                    keySaved = false,
                    configValidated = configValid,
                    networkReachable = netReachable,
                    authAccepted = false,
                    modelReachable = false,
                    generationSucceeded = false,
                    outcome = ApiCallOutcome.AuthenticationError(0, "No API key saved for $provider. Local offline rule engine is active."),
                    sampleOutput = "Local Rule Engine Ready (Add API key for cloud LLM)"
                )
                _lastDiagnostics.value = diag
                return@withContext diag
            }

            if (!configValid) {
                val diag = ConnectionDiagnostics(
                    provider = provider,
                    model = model,
                    endpoint = endpoint,
                    keySaved = true,
                    configValidated = false,
                    networkReachable = netReachable,
                    authAccepted = false,
                    modelReachable = false,
                    generationSucceeded = false,
                    outcome = ApiCallOutcome.ModelOrEndpointError(0, model, endpoint, "Invalid model ID or endpoint URL format.")
                )
                _lastDiagnostics.value = diag
                return@withContext diag
            }

            if (!netReachable) {
                val diag = ConnectionDiagnostics(
                    provider = provider,
                    model = model,
                    endpoint = endpoint,
                    keySaved = true,
                    configValidated = true,
                    networkReachable = false,
                    authAccepted = false,
                    modelReachable = false,
                    generationSucceeded = false,
                    outcome = ApiCallOutcome.NetworkError("Device has no active internet connection.")
                )
                _lastDiagnostics.value = diag
                return@withContext diag
            }

            val outcome = executeRawGeneration(
                provider = provider,
                apiKey = apiKey,
                model = model,
                endpoint = endpoint,
                protocol = config.geminiProtocol,
                systemPrompt = "You are JARVIS. Reply with a short confirmation: 'JARVIS_ONLINE'.",
                userPrompt = "Ping connection test. Reply JARVIS_ONLINE.",
                history = emptyList()
            )

            val authOk = outcome is ApiCallOutcome.Success ||
                outcome is ApiCallOutcome.ModelOrEndpointError ||
                outcome is ApiCallOutcome.QuotaOrRateLimitError ||
                outcome is ApiCallOutcome.MalformedResponseError ||
                outcome is ApiCallOutcome.EmptyResponseError

            val modelOk = outcome is ApiCallOutcome.Success ||
                outcome is ApiCallOutcome.EmptyResponseError

            val genOk = outcome is ApiCallOutcome.Success

            val sampleText = if (outcome is ApiCallOutcome.Success) outcome.content.take(120) else ""

            val diag = ConnectionDiagnostics(
                provider = provider,
                model = model,
                endpoint = endpoint,
                keySaved = true,
                configValidated = true,
                networkReachable = true,
                authAccepted = authOk,
                modelReachable = modelOk,
                generationSucceeded = genOk,
                outcome = outcome,
                sampleOutput = sampleText
            )
            _lastDiagnostics.value = diag

            if (genOk) {
                val now = System.currentTimeMillis()
                preferences.saveActiveConfig(
                    config.copy(
                        lastSuccessfulTestTimestamp = now,
                        lastTestStatusSummary = "Verified $provider ($model) • HTTP 200"
                    )
                )
            } else {
                preferences.saveActiveConfig(
                    config.copy(
                        lastTestStatusSummary = "Failed: ${outcome.summaryLabel}"
                    )
                )
            }

            return@withContext diag
        } finally {
            _isTestingConnection.value = false
        }
    }

    suspend fun infer(
        prompt: String,
        screenContext: String? = null,
        history: List<Pair<String, String>> = emptyList()
    ): AiInferenceResult = withContext(Dispatchers.IO) {
        val config = preferences.configFlow.first()
        val provider = config.aiProvider
        val apiKey = config.getKeyForProvider(provider).trim()
        val model = config.getModelForProvider(provider).trim()
        val endpoint = config.getEndpointForProvider(provider).trim()

        // If no API key is configured, use local rule-based assistant engine transparently
        if (apiKey.isBlank()) {
            return@withContext ruleBasedInference(prompt, screenContext)
        }

        val systemPrompt = buildSystemPrompt(config, screenContext)
        val outcome = executeWithBoundedRetry(
            provider = provider,
            apiKey = apiKey,
            model = model,
            endpoint = endpoint,
            protocol = config.geminiProtocol,
            systemPrompt = systemPrompt,
            userPrompt = prompt,
            history = history
        )

        when (outcome) {
            is ApiCallOutcome.Success -> {
                parseStructuredAiText(outcome.content, prompt, outcome)
            }
            is ApiCallOutcome.ModelOrEndpointError -> {
                AiInferenceResult(
                    spokenResponseBn = "মডেল '$model' বা এন্ডপয়েন্ট পাওয়া যায়নি (HTTP ${outcome.httpCode})। দয়া করে সেটিংস থেকে সমর্থিত মডেল (যেমন ${AppConfig.DEFAULT_GEMINI_MODEL}) নির্বাচন করুন।",
                    spokenResponseEn = "Model '$model' or endpoint returned HTTP ${outcome.httpCode}. Please select a supported model (such as ${AppConfig.DEFAULT_GEMINI_MODEL}) in Settings.",
                    isError = true,
                    httpStatusCode = outcome.httpCode,
                    technicalDiagnostic = outcome.detail,
                    outcome = outcome
                )
            }
            is ApiCallOutcome.AuthenticationError -> {
                AiInferenceResult(
                    spokenResponseBn = "এপিআই কি (API Key) অনুমোদিত নয় (HTTP ${outcome.httpCode})। সেটিংস থেকে আপনার API Key যাচাই করুন।",
                    spokenResponseEn = "API authentication failed (HTTP ${outcome.httpCode}). Please verify your API key in Settings.",
                    isError = true,
                    httpStatusCode = outcome.httpCode,
                    technicalDiagnostic = outcome.detail,
                    outcome = outcome
                )
            }
            is ApiCallOutcome.QuotaOrRateLimitError -> {
                AiInferenceResult(
                    spokenResponseBn = "এপিআই কোটা বা রেট লিমিট শেষ হয়েছে (HTTP 429)। কিছুক্ষণ পর আবার চেষ্টা করুন।",
                    spokenResponseEn = "API quota or rate limit exceeded (HTTP 429). Please retry shortly or check billing/quota.",
                    isError = true,
                    httpStatusCode = outcome.httpCode,
                    technicalDiagnostic = outcome.detail,
                    outcome = outcome
                )
            }
            else -> {
                // Check if local command can handle urgent device control while surfacing the exact error
                val localAttempt = ruleBasedInference(prompt, screenContext)
                if (localAttempt.isToolCall) {
                    localAttempt.copy(
                        isError = true,
                        technicalDiagnostic = "Cloud API warning (${outcome.summaryLabel}); executed local device command.",
                        outcome = outcome
                    )
                } else {
                    AiInferenceResult(
                        spokenResponseBn = "এপিআই সংযোগে ত্রুটি: ${outcome.summaryLabel}।",
                        spokenResponseEn = "API request failed: ${outcome.summaryLabel}.",
                        isError = true,
                        technicalDiagnostic = outcome.summaryLabel,
                        outcome = outcome
                    )
                }
            }
        }
    }

    private suspend fun executeWithBoundedRetry(
        provider: String,
        apiKey: String,
        model: String,
        endpoint: String,
        protocol: String,
        systemPrompt: String,
        userPrompt: String,
        history: List<Pair<String, String>>
    ): ApiCallOutcome {
        var attempt = 0
        val maxRetries = 2
        var lastOutcome: ApiCallOutcome = ApiCallOutcome.NetworkError("Not attempted")

        while (attempt <= maxRetries) {
            if (attempt > 0) {
                delay(600L * attempt)
            }
            lastOutcome = executeRawGeneration(
                provider = provider,
                apiKey = apiKey,
                model = model,
                endpoint = endpoint,
                protocol = protocol,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                history = history
            )

            // Only retry transient network/timeout/503 errors; NEVER retry 401, 403, 404, 429, or Malformed
            val shouldRetry = when (lastOutcome) {
                is ApiCallOutcome.TimeoutError,
                is ApiCallOutcome.NetworkError -> true
                is ApiCallOutcome.ProviderServerError -> lastOutcome.httpCode in listOf(502, 503, 504)
                else -> false
            }

            if (!shouldRetry) {
                return lastOutcome
            }
            attempt++
        }
        return lastOutcome
    }

    private fun executeRawGeneration(
        provider: String,
        apiKey: String,
        model: String,
        endpoint: String,
        protocol: String,
        systemPrompt: String,
        userPrompt: String,
        history: List<Pair<String, String>>
    ): ApiCallOutcome {
        val startMs = System.currentTimeMillis()
        return try {
            when (provider) {
                "GEMINI" -> executeGeminiRequest(apiKey, model, endpoint, protocol, systemPrompt, userPrompt, history, startMs)
                "OPENAI", "DEEPSEEK", "OPENROUTER", "CUSTOM", "OTHER" ->
                    executeOpenAiCompatibleRequest(apiKey, model, endpoint, systemPrompt, userPrompt, history, startMs)
                else -> ApiCallOutcome.ModelOrEndpointError(0, model, endpoint, "Unsupported provider: $provider")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: SocketTimeoutException) {
            ApiCallOutcome.TimeoutError("Timed out waiting for $provider ($model): ${e.message}")
        } catch (e: IOException) {
            ApiCallOutcome.NetworkError("Network I/O failure: ${e.message ?: "Unable to reach host"}")
        } catch (e: Exception) {
            ApiCallOutcome.MalformedResponseError("Unexpected client error: ${e.message}")
        }
    }

    private fun executeGeminiRequest(
        apiKey: String,
        model: String,
        baseEndpoint: String,
        protocol: String,
        systemPrompt: String,
        userPrompt: String,
        history: List<Pair<String, String>>,
        startMs: Long
    ): ApiCallOutcome {
        val cleanBase = baseEndpoint.trimEnd('/')
        val cleanModel = model.removePrefix("models/")

        // Use official header authentication (x-goog-api-key) rather than exposing the key in URL query params
        if (protocol == "V1BETA_INTERACTIONS_ONLY") {
            val interactionsUrl = "$cleanBase/interactions"
            val payload = JSONObject().apply {
                put("model", "models/$cleanModel")
                put("system_instruction", systemPrompt)
                put("input", userPrompt)
            }
            val req = Request.Builder()
                .url(interactionsUrl)
                .header("x-goog-api-key", apiKey)
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(req).execute().use { resp ->
                return handleGeminiHttpResponse(resp.code, resp.body?.string() ?: "", cleanModel, interactionsUrl, startMs)
            }
        }

        // Standard v1beta generateContent with multi-turn contents & Interactions schema compatibility
        val url = "$cleanBase/models/$cleanModel:generateContent"
        val contentsArray = JSONArray()
        for ((role, msg) in history.takeLast(8)) {
            val geminiRole = if (role.equals("USER", ignoreCase = true)) "user" else "model"
            contentsArray.put(JSONObject().apply {
                put("role", geminiRole)
                put("parts", JSONArray().put(JSONObject().put("text", msg)))
            })
        }
        contentsArray.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(JSONObject().put("text", userPrompt)))
        })

        val bodyJson = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
            })
            put("contents", contentsArray)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.4)
            })
        }

        val req = Request.Builder()
            .url(url)
            .header("x-goog-api-key", apiKey)
            .header("Content-Type", "application/json")
            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(req).execute().use { resp ->
            val rawBody = resp.body?.string() ?: ""
            return handleGeminiHttpResponse(resp.code, rawBody, cleanModel, url, startMs)
        }
    }

    private fun handleGeminiHttpResponse(
        code: Int,
        rawBody: String,
        model: String,
        endpointUrl: String,
        startMs: Long
    ): ApiCallOutcome {
        val latency = System.currentTimeMillis() - startMs
        val apiErrorMsg = extractErrorDetailFromJson(rawBody)

        if (code == 200) {
            if (rawBody.isBlank()) {
                return ApiCallOutcome.EmptyResponseError("HTTP 200 with empty body from $model")
            }
            return try {
                val root = JSONObject(rawBody)
                // Support both Interactions API ('outputs') and generateContent ('candidates')
                val extractedText = when {
                    root.has("outputs") -> {
                        val outputs = root.getJSONArray("outputs")
                        if (outputs.length() > 0) {
                            val firstOut = outputs.getJSONObject(0)
                            firstOut.optString("text", firstOut.optJSONObject("content")?.optString("text") ?: "")
                        } else ""
                    }
                    root.has("candidates") -> {
                        val candidates = root.getJSONArray("candidates")
                        if (candidates.length() > 0) {
                            val content = candidates.getJSONObject(0).optJSONObject("content")
                            val parts = content?.optJSONArray("parts")
                            if (parts != null && parts.length() > 0) {
                                parts.getJSONObject(0).optString("text", "")
                            } else ""
                        } else ""
                    }
                    else -> ""
                }

                if (extractedText.isBlank()) {
                    ApiCallOutcome.EmptyResponseError("Response parsed but contained no text output.")
                } else {
                    ApiCallOutcome.Success(extractedText, code, latency)
                }
            } catch (e: Exception) {
                ApiCallOutcome.MalformedResponseError("Failed to parse Gemini JSON: ${e.message}")
            }
        }

        return when (code) {
            401, 403 -> ApiCallOutcome.AuthenticationError(
                code,
                apiErrorMsg.ifBlank { "Invalid API key or unauthorized project access." }
            )
            404 -> ApiCallOutcome.ModelOrEndpointError(
                code,
                model,
                endpointUrl,
                apiErrorMsg.ifBlank { "Model '$model' not found on endpoint '$endpointUrl' (HTTP 404). Verify model ID or switch to ${AppConfig.DEFAULT_GEMINI_MODEL}." }
            )
            429 -> ApiCallOutcome.QuotaOrRateLimitError(
                code,
                apiErrorMsg.ifBlank { "Rate limit or quota exhausted for model '$model'." }
            )
            in 500..599 -> ApiCallOutcome.ProviderServerError(
                code,
                apiErrorMsg.ifBlank { "Gemini server error (HTTP $code)." }
            )
            else -> ApiCallOutcome.ModelOrEndpointError(
                code,
                model,
                endpointUrl,
                apiErrorMsg.ifBlank { "HTTP $code error from Gemini endpoint." }
            )
        }
    }

    private fun executeOpenAiCompatibleRequest(
        apiKey: String,
        model: String,
        endpoint: String,
        systemPrompt: String,
        userPrompt: String,
        history: List<Pair<String, String>>,
        startMs: Long
    ): ApiCallOutcome {
        val messagesArray = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", systemPrompt))
            for ((role, msg) in history.takeLast(6)) {
                val r = if (role.equals("USER", ignoreCase = true)) "user" else "assistant"
                put(JSONObject().put("role", r).put("content", msg))
            }
            put(JSONObject().put("role", "user").put("content", userPrompt))
        }

        val bodyJson = JSONObject().apply {
            put("model", model)
            put("messages", messagesArray)
            put("temperature", 0.4)
        }

        val req = Request.Builder()
            .url(endpoint)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(req).execute().use { resp ->
            val rawBody = resp.body?.string() ?: ""
            val code = resp.code
            val latency = System.currentTimeMillis() - startMs
            val errDetail = extractErrorDetailFromJson(rawBody)

            if (code == 200) {
                if (rawBody.isBlank()) return ApiCallOutcome.EmptyResponseError("Empty HTTP 200 response")
                return try {
                    val root = JSONObject(rawBody)
                    val choices = root.optJSONArray("choices")
                    if (choices == null || choices.length() == 0) {
                        ApiCallOutcome.EmptyResponseError("No choices returned in response")
                    } else {
                        val content = choices.getJSONObject(0).optJSONObject("message")?.optString("content") ?: ""
                        if (content.isBlank()) {
                            ApiCallOutcome.EmptyResponseError("Empty message content")
                        } else {
                            ApiCallOutcome.Success(content, code, latency)
                        }
                    }
                } catch (e: Exception) {
                    ApiCallOutcome.MalformedResponseError("Invalid OpenAI-compatible JSON: ${e.message}")
                }
            }

            return when (code) {
                401, 403 -> ApiCallOutcome.AuthenticationError(code, errDetail.ifBlank { "Unauthorized API key" })
                404 -> ApiCallOutcome.ModelOrEndpointError(code, model, endpoint, errDetail.ifBlank { "Model '$model' or endpoint not found (HTTP 404)" })
                429 -> ApiCallOutcome.QuotaOrRateLimitError(code, errDetail.ifBlank { "Rate limit or quota exceeded" })
                in 500..599 -> ApiCallOutcome.ProviderServerError(code, errDetail.ifBlank { "Server error HTTP $code" })
                else -> ApiCallOutcome.ModelOrEndpointError(code, model, endpoint, errDetail.ifBlank { "HTTP $code error" })
            }
        }
    }

    private fun extractErrorDetailFromJson(raw: String): String {
        if (raw.isBlank()) return ""
        return try {
            val obj = JSONObject(raw)
            val err = obj.optJSONObject("error")
            if (err != null) {
                val status = err.optString("status", "")
                val msg = err.optString("message", "")
                if (status.isNotBlank()) "$status: $msg" else msg
            } else {
                obj.optString("message", "")
            }
        } catch (_: Exception) {
            raw.take(140)
        }
    }

    private fun parseStructuredAiText(
        rawText: String,
        originalPrompt: String,
        outcome: ApiCallOutcome.Success
    ): AiInferenceResult {
        val cleaned = rawText.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        if (cleaned.startsWith("{") && cleaned.endsWith("}")) {
            try {
                val obj = JSONObject(cleaned)
                val bn = obj.optString("bn", obj.optString("spokenResponseBn", ""))
                val en = obj.optString("en", obj.optString("spokenResponseEn", ""))
                val tool = obj.optString("tool", "")
                val isWork = obj.optBoolean("isWorkTask", false)
                val workType = obj.optString("workTaskType", "")
                val workName = obj.optString("workTaskName", "")
                val toolArgs = mutableMapOf<String, String>()
                val argsObj = obj.optJSONObject("arguments")
                if (argsObj != null) {
                    for (k in argsObj.keys()) {
                        toolArgs[k] = argsObj.getString(k)
                    }
                }

                if (tool.isNotBlank() && tool != "none" && tool != "null") {
                    return AiInferenceResult(
                        spokenResponseBn = bn.ifBlank { "আপনার নির্দেশনাটি পালন করছি।" },
                        spokenResponseEn = en.ifBlank { "Executing your command." },
                        isToolCall = true,
                        toolCall = ToolCallRequest(tool = tool, arguments = toolArgs),
                        isWorkTask = isWork,
                        workTaskType = workType.takeIf { it.isNotBlank() },
                        workTaskName = workName.takeIf { it.isNotBlank() },
                        httpStatusCode = outcome.httpCode,
                        outcome = outcome
                    )
                }

                return AiInferenceResult(
                    spokenResponseBn = bn.ifBlank { rawText },
                    spokenResponseEn = en.ifBlank { rawText },
                    isWorkTask = isWork,
                    workTaskType = workType.takeIf { it.isNotBlank() },
                    workTaskName = workName.takeIf { it.isNotBlank() },
                    httpStatusCode = outcome.httpCode,
                    outcome = outcome
                )
            } catch (_: Exception) {}
        }

        return AiInferenceResult(
            spokenResponseBn = rawText,
            spokenResponseEn = rawText,
            httpStatusCode = outcome.httpCode,
            outcome = outcome
        )
    }

    fun ruleBasedInference(prompt: String, screenContext: String?): AiInferenceResult {
        val lower = prompt.lowercase().trim()

        // 1. Open App
        if (lower.startsWith("open ") || lower.contains("খোল") || lower.contains("চালু কর")) {
            val appName = extractAppName(lower)
            return AiInferenceResult(
                spokenResponseBn = "$appName অ্যাপটি চালু করা হচ্ছে।",
                spokenResponseEn = "Opening $appName.",
                isToolCall = true,
                toolCall = ToolCallRequest("openApp", mapOf("app" to appName))
            )
        }

        // 2. Web Search
        if (lower.startsWith("search ") || lower.contains("খুঁজ") || lower.contains("সার্চ কর")) {
            val query = prompt.replace(Regex("(?i)^(search|খুঁজুন|সার্চ করো|সার্চ কর)\\s*"), "").trim()
            return AiInferenceResult(
                spokenResponseBn = "'$query' সার্চ করছি।",
                spokenResponseEn = "Searching for '$query'.",
                isToolCall = true,
                toolCall = ToolCallRequest("browserSearch", mapOf("query" to query))
            )
        }

        // 3. Volume
        if (lower.contains("volume") || lower.contains("ভলিউম") || lower.contains("আওয়াজ")) {
            val percentMatch = Regex("(\\d{1,3})%?").find(lower)
            val percent = percentMatch?.groupValues?.get(1)?.toIntOrNull() ?: 50
            return AiInferenceResult(
                spokenResponseBn = "ভলিউম $percent% সেট করা হচ্ছে।",
                spokenResponseEn = "Setting volume to $percent percent.",
                isToolCall = true,
                toolCall = ToolCallRequest("setVolume", mapOf("percent" to percent.toString()))
            )
        }

        // 4. Mute / Unmute
        if (lower.contains("unmute") || lower.contains("আনমিউট")) {
            return AiInferenceResult(
                spokenResponseBn = "মিডিয়া আনমিউট করা হচ্ছে।",
                spokenResponseEn = "Unmuting media.",
                isToolCall = true,
                toolCall = ToolCallRequest("unmute", emptyMap())
            )
        }
        if (lower.contains("mute") || lower.contains("মিউট")) {
            return AiInferenceResult(
                spokenResponseBn = "মিডিয়া মিউট করা হচ্ছে।",
                spokenResponseEn = "Muting media.",
                isToolCall = true,
                toolCall = ToolCallRequest("mute", emptyMap())
            )
        }

        // 5. Phone Call
        if (lower.startsWith("call ") || lower.contains("কল কর") || lower.contains("ফোন কর")) {
            val num = lower.replace(Regex("[^0-9+]"), "")
            return AiInferenceResult(
                spokenResponseBn = if (num.isNotBlank()) "$num নম্বরে ডায়াল প্যাড খোলা হচ্ছে।" else "ডায়াল প্যাড খোলা হচ্ছে।",
                spokenResponseEn = if (num.isNotBlank()) "Opening dialer for $num." else "Opening dialer.",
                isToolCall = true,
                toolCall = ToolCallRequest("makePhoneCall", mapOf("number" to num))
            )
        }

        // 6. Settings Shortcuts
        if (lower.contains("wifi") || lower.contains("ওয়াইফাই")) {
            return AiInferenceResult(
                spokenResponseBn = "ওয়াইফাই সেটিংস খুলছি।",
                spokenResponseEn = "Opening Wi-Fi settings.",
                isToolCall = true,
                toolCall = ToolCallRequest("openWifiSettings", emptyMap())
            )
        }
        if (lower.contains("bluetooth") || lower.contains("ব্লুটুথ")) {
            return AiInferenceResult(
                spokenResponseBn = "ব্লুটুথ সেটিংস খুলছি।",
                spokenResponseEn = "Opening Bluetooth settings.",
                isToolCall = true,
                toolCall = ToolCallRequest("openBluetoothSettings", emptyMap())
            )
        }

        // 7. Read Screen
        if (lower.contains("read screen") || lower.contains("স্ক্রিন পড়") || lower.contains("স্ক্রিনে কী আছে")) {
            return AiInferenceResult(
                spokenResponseBn = "স্ক্রিনের কনটেন্ট পরীক্ষা করছি।",
                spokenResponseEn = "Inspecting current screen content.",
                isToolCall = true,
                toolCall = ToolCallRequest("readScreen", emptyMap())
            )
        }

        // 8. Work Task / Plan
        if (lower.contains("task") || lower.contains("কাজ") || lower.contains("project") || lower.contains("প্রজেক্ট") || lower.contains("পরিকল্পনা")) {
            return AiInferenceResult(
                spokenResponseBn = "একটি নতুন ওয়ার্ক টাস্ক তৈরি এবং পরিকল্পনা প্রস্তুত করা হয়েছে।",
                spokenResponseEn = "A new work task has been planned and queued.",
                isWorkTask = true,
                workTaskType = "AUTOMATION",
                workTaskName = prompt.take(32),
                workTaskPrompt = prompt
            )
        }

        return AiInferenceResult(
            spokenResponseBn = "হ্যালো! আমি জার্ভিস। আপনার নির্দেশনা: '$prompt' — ক্লাউড AI উত্তরের জন্য সেটিংসে Gemini (${AppConfig.DEFAULT_GEMINI_MODEL}) এপিআই কি সংযুক্ত করুন, অথবা সরাসরি ভয়েস কমান্ড দিন।",
            spokenResponseEn = "Hello! I am JARVIS. Received: '$prompt'. Connect your Gemini (${AppConfig.DEFAULT_GEMINI_MODEL}) API key in Settings for full LLM responses, or use direct system commands."
        )
    }

    private fun extractAppName(lower: String): String {
        return lower.replace(Regex("^(open|খোল|চালু করো|চালু কর)\\s*"), "")
            .replace("app", "")
            .replace("অ্যাপ", "")
            .trim()
    }

    private fun buildSystemPrompt(config: AppConfig, screenContext: String?): String {
        return """
            You are JARVIS, an advanced personal AI Android companion, voice assistant, and automation agent.
            Primary response language: Bengali (${config.language}) with clear English technical terms.
            Respond strictly in valid JSON object format:
            {
              "bn": "Natural Bengali response to speak aloud",
              "en": "Natural English response",
              "tool": "optionalToolNameOrEmpty",
              "arguments": { "key": "value" },
              "isWorkTask": false,
              "workTaskType": "AUTOMATION",
              "workTaskName": "Short task name"
            }
            Supported tools: openApp, closeApp, openWebsite, browserSearch, click, scroll, typeText, pressBack, pressHome, readScreen, setVolume, increaseVolume, decreaseVolume, mute, unmute, openSettings, openWifiSettings, openBluetoothSettings, openDisplaySettings, makePhoneCall, openContacts, openPlayStore, camera, gallery, copy, paste.
            Current foreground screen context: ${screenContext ?: "Not inspected"}
        """.trimIndent()
    }
}
