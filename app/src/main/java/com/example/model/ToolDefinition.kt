package com.example.model

data class ToolDefinition(
    val name: String,
    val description: String,
    val requiredPermission: String? = null,
    val riskLevel: ActionRiskLevel = ActionRiskLevel.LOW,
    val requiresAccessibility: Boolean = false,
    val canUndo: Boolean = false
)

data class ToolCallRequest(
    val tool: String,
    val arguments: Map<String, String> = emptyMap(),
    val reason: String = "",
    val risk: ActionRiskLevel = ActionRiskLevel.LOW,
    val verificationRequired: Boolean = true
)

data class ToolExecutionResult(
    val tool: String,
    val success: Boolean,
    val messageEn: String,
    val messageBn: String,
    val outputData: Map<String, String> = emptyMap(),
    val verificationStatus: VerificationStatus = VerificationStatus.CONFIRMED,
    val errorDetails: String? = null
)
