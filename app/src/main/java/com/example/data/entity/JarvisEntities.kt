package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sender: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSpoken: Boolean = false
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val taskType: String,
    val status: String,
    val priority: Int,
    val createdTime: Long,
    val currentStep: String,
    val progress: Int,
    val checkpoint: String?,
    val logsSerialized: String,
    val errorsSerialized: String,
    val retryCount: Int,
    val artifactPath: String?,
    val artifactContent: String?,
    val verificationStatus: String,
    val originalRequest: String,
    val taskObjective: String,
    val currentPhase: String,
    val stepIndex: Int,
    val totalSteps: Int,
    val planStepsSerialized: String,
    val lastSuccessfulAction: String?,
    val lastObservedScreen: String?,
    val targetApp: String?,
    val expectedResult: String?,
    val verificationDetail: String?,
    val isResumeEligible: Boolean,
    val interruptionReason: String?
)

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val category: String,
    val createdTime: Long,
    val filesSummary: String,
    val memoryNotes: String
)

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val category: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val command: String,
    val tool: String,
    val argumentsSummary: String,
    val executionResult: String,
    val verificationStatus: String,
    val finalStatus: String
)
