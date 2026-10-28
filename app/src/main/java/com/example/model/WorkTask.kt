package com.example.model

data class TaskPlanStep(
    val id: String,
    val title: String,
    val description: String,
    val targetAppOrUrl: String? = null,
    val expectedResult: String = "",
    val isCompleted: Boolean = false,
    val isCheckpointSafe: Boolean = false,
    val verificationResult: String? = null,
    val executedTimestamp: Long? = null
)

data class WorkTask(
    val id: String,
    val name: String,
    val description: String,
    val taskType: String,
    val status: TaskStatus = TaskStatus.QUEUED,
    val priority: Int = 1,
    val createdTime: Long = System.currentTimeMillis(),
    val currentStep: String = "Initializing",
    val progress: Int = 0,
    val checkpoint: String? = null,
    val logs: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
    val retryCount: Int = 0,
    val artifactPath: String? = null,
    val artifactContent: String? = null,
    val verificationStatus: VerificationStatus = VerificationStatus.CONFIRMED,
    val originalRequest: String = "",
    val taskObjective: String = "",
    val currentPhase: String = "PLANNING",
    val stepIndex: Int = 0,
    val totalSteps: Int = 0,
    val planSteps: List<TaskPlanStep> = emptyList(),
    val lastSuccessfulAction: String? = null,
    val lastObservedScreen: String? = null,
    val targetApp: String? = null,
    val expectedResult: String? = null,
    val verificationDetail: String? = null,
    val isResumeEligible: Boolean = true,
    val interruptionReason: String? = null
)

data class ActionReceipt(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val command: String,
    val tool: String,
    val argumentsSummary: String,
    val executionResult: String,
    val verificationStatus: VerificationStatus,
    val finalStatus: String
)

data class ProjectItem(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val createdTime: Long = System.currentTimeMillis(),
    val filesSummary: String = "",
    val memoryNotes: String = ""
)
