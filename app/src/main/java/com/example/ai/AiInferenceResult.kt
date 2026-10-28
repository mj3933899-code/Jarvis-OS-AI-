package com.example.ai

import com.example.model.ToolCallRequest

sealed class ApiCallOutcome(val summaryLabel: String, val isSuccess: Boolean = false) {
    data class Success(
        val content: String,
        val httpCode: Int = 200,
        val latencyMs: Long = 0L
    ) : ApiCallOutcome("Success (HTTP $httpCode)", true)

    data class AuthenticationError(
        val httpCode: Int,
        val detail: String
    ) : ApiCallOutcome("Authentication Error (HTTP $httpCode)")

    data class ModelOrEndpointError(
        val httpCode: Int,
        val modelId: String,
        val endpoint: String,
        val detail: String
    ) : ApiCallOutcome("Model or Endpoint Error (HTTP $httpCode)")

    data class QuotaOrRateLimitError(
        val httpCode: Int,
        val detail: String
    ) : ApiCallOutcome("Quota / Rate Limit Exceeded (HTTP $httpCode)")

    data class NetworkError(
        val detail: String
    ) : ApiCallOutcome("Network / DNS / TLS Error")

    data class TimeoutError(
        val detail: String
    ) : ApiCallOutcome("Request Timeout")

    data class MalformedResponseError(
        val detail: String
    ) : ApiCallOutcome("Malformed JSON Response")

    data class EmptyResponseError(
        val detail: String
    ) : ApiCallOutcome("Empty Model Response")

    data class ProviderServerError(
        val httpCode: Int,
        val detail: String
    ) : ApiCallOutcome("Provider Server Error (HTTP $httpCode)")

    data class Cancelled(
        val detail: String = "Operation cancelled"
    ) : ApiCallOutcome("Cancelled")
}

data class ConnectionDiagnostics(
    val provider: String,
    val model: String,
    val endpoint: String,
    val keySaved: Boolean,
    val configValidated: Boolean,
    val networkReachable: Boolean,
    val authAccepted: Boolean,
    val modelReachable: Boolean,
    val generationSucceeded: Boolean,
    val outcome: ApiCallOutcome,
    val sampleOutput: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class AiInferenceResult(
    val spokenResponseBn: String,
    val spokenResponseEn: String,
    val isToolCall: Boolean = false,
    val toolCall: ToolCallRequest? = null,
    val isWorkTask: Boolean = false,
    val workTaskType: String? = null,
    val workTaskName: String? = null,
    val workTaskPrompt: String? = null,
    val isError: Boolean = false,
    val httpStatusCode: Int? = null,
    val technicalDiagnostic: String? = null,
    val outcome: ApiCallOutcome? = null
)
