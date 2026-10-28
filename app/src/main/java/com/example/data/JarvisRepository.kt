package com.example.data

import com.example.data.dao.JarvisDao
import com.example.data.entity.MemoryEntity
import com.example.data.entity.MessageEntity
import com.example.data.entity.ProjectEntity
import com.example.data.entity.ReceiptEntity
import com.example.data.entity.TaskEntity
import com.example.model.ActionReceipt
import com.example.model.AppConfig
import com.example.model.ProjectItem
import com.example.model.SetupProgress
import com.example.model.TaskPlanStep
import com.example.model.TaskStatus
import com.example.model.VerificationStatus
import com.example.model.WorkTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class JarvisRepository(
    private val dao: JarvisDao,
    private val preferences: JarvisPreferences
) {
    val configFlow: Flow<AppConfig> = preferences.configFlow
    val setupProgressFlow: Flow<SetupProgress> = preferences.setupProgressFlow
    val messagesFlow: Flow<List<MessageEntity>> = dao.getAllMessages()

    val tasksFlow: Flow<List<WorkTask>> = dao.getAllTasks().map { list ->
        list.map { it.toDomain() }
    }

    val projectsFlow: Flow<List<ProjectItem>> = dao.getAllProjects().map { list ->
        list.map {
            ProjectItem(
                id = it.id,
                name = it.name,
                description = it.description,
                category = it.category,
                createdTime = it.createdTime,
                filesSummary = it.filesSummary,
                memoryNotes = it.memoryNotes
            )
        }
    }

    val memoriesFlow: Flow<List<MemoryEntity>> = dao.getAllMemories()

    val receiptsFlow: Flow<List<ActionReceipt>> = dao.getAllReceipts().map { list ->
        list.map {
            ActionReceipt(
                id = it.id,
                timestamp = it.timestamp,
                command = it.command,
                tool = it.tool,
                argumentsSummary = it.argumentsSummary,
                executionResult = it.executionResult,
                verificationStatus = try {
                    VerificationStatus.valueOf(it.verificationStatus)
                } catch (_: Exception) {
                    VerificationStatus.CONFIRMED
                },
                finalStatus = it.finalStatus
            )
        }
    }

    suspend fun saveSetupStep(step: Int) = preferences.saveSetupStep(step)
    suspend fun completeSetup() = preferences.completeSetup()
    suspend fun resetSetupProgress() = preferences.resetSetupProgress()

    suspend fun commitConfig(config: AppConfig) = preferences.commitConfig(config)
    suspend fun saveActiveConfig(config: AppConfig) = preferences.saveActiveConfig(config)
    suspend fun restorePreviousSettings(): Boolean = preferences.restorePreviousSettings()
    suspend fun resetToDefault(keepApiKey: Boolean = false) = preferences.resetToDefault(keepApiKey)

    suspend fun insertMessage(sender: String, content: String, isSpoken: Boolean = false) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        dao.insertMessage(
            MessageEntity(
                sender = sender,
                content = content,
                timestamp = System.currentTimeMillis(),
                isSpoken = isSpoken
            )
        )
    }

    suspend fun clearMessages() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { dao.clearAllMessages() }

    suspend fun saveTask(task: WorkTask) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        dao.insertOrUpdateTask(task.toEntity())
    }

    suspend fun getTask(id: String): WorkTask? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        dao.getTaskById(id)?.toDomain()
    }

    suspend fun deleteTask(id: String) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { dao.deleteTask(id) }
    suspend fun clearTasks() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { dao.clearAllTasks() }

    suspend fun saveProject(project: ProjectItem) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        dao.insertOrUpdateProject(
            ProjectEntity(
                id = project.id,
                name = project.name,
                description = project.description,
                category = project.category,
                createdTime = project.createdTime,
                filesSummary = project.filesSummary,
                memoryNotes = project.memoryNotes
            )
        )
    }

    suspend fun deleteProject(id: String) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { dao.deleteProject(id) }

    suspend fun addMemory(category: String, content: String) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        dao.insertMemory(
            MemoryEntity(
                category = category,
                content = content,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearMemories() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { dao.clearAllMemories() }

    suspend fun addReceipt(receipt: ActionReceipt) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        dao.insertReceipt(
            ReceiptEntity(
                id = receipt.id,
                timestamp = receipt.timestamp,
                command = receipt.command,
                tool = receipt.tool,
                argumentsSummary = receipt.argumentsSummary,
                executionResult = receipt.executionResult,
                verificationStatus = receipt.verificationStatus.name,
                finalStatus = receipt.finalStatus
            )
        )
    }

    private fun TaskEntity.toDomain(): WorkTask {
        val taskStatus = try {
            TaskStatus.valueOf(status)
        } catch (_: Exception) {
            TaskStatus.RUNNING
        }
        val verifStatus = try {
            VerificationStatus.valueOf(verificationStatus)
        } catch (_: Exception) {
            VerificationStatus.CONFIRMED
        }
        val parsedLogs = if (logsSerialized.isBlank()) emptyList() else logsSerialized.split("\n---\n")
        val parsedErrors = if (errorsSerialized.isBlank()) emptyList() else errorsSerialized.split("\n---\n")
        val parsedSteps = deserializeSteps(planStepsSerialized)

        return WorkTask(
            id = id,
            name = name,
            description = description,
            taskType = taskType,
            status = taskStatus,
            priority = priority,
            createdTime = createdTime,
            currentStep = currentStep,
            progress = progress,
            checkpoint = checkpoint,
            logs = parsedLogs,
            errors = parsedErrors,
            retryCount = retryCount,
            artifactPath = artifactPath,
            artifactContent = artifactContent,
            verificationStatus = verifStatus,
            originalRequest = originalRequest,
            taskObjective = taskObjective,
            currentPhase = currentPhase,
            stepIndex = stepIndex,
            totalSteps = totalSteps,
            planSteps = parsedSteps,
            lastSuccessfulAction = lastSuccessfulAction,
            lastObservedScreen = lastObservedScreen,
            targetApp = targetApp,
            expectedResult = expectedResult,
            verificationDetail = verificationDetail,
            isResumeEligible = isResumeEligible,
            interruptionReason = interruptionReason
        )
    }

    private fun WorkTask.toEntity(): TaskEntity {
        return TaskEntity(
            id = id,
            name = name,
            description = description,
            taskType = taskType,
            status = status.name,
            priority = priority,
            createdTime = createdTime,
            currentStep = currentStep,
            progress = progress,
            checkpoint = checkpoint,
            logsSerialized = logs.joinToString("\n---\n"),
            errorsSerialized = errors.joinToString("\n---\n"),
            retryCount = retryCount,
            artifactPath = artifactPath,
            artifactContent = artifactContent,
            verificationStatus = verificationStatus.name,
            originalRequest = originalRequest,
            taskObjective = taskObjective,
            currentPhase = currentPhase,
            stepIndex = stepIndex,
            totalSteps = totalSteps,
            planStepsSerialized = serializeSteps(planSteps),
            lastSuccessfulAction = lastSuccessfulAction,
            lastObservedScreen = lastObservedScreen,
            targetApp = targetApp,
            expectedResult = expectedResult,
            verificationDetail = verificationDetail,
            isResumeEligible = isResumeEligible,
            interruptionReason = interruptionReason
        )
    }

    private fun serializeSteps(steps: List<TaskPlanStep>): String {
        if (steps.isEmpty()) return ""
        val arr = JSONArray()
        for (s in steps) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("title", s.title)
            obj.put("desc", s.description)
            obj.put("target", s.targetAppOrUrl ?: "")
            obj.put("exp", s.expectedResult)
            obj.put("done", s.isCompleted)
            obj.put("safe", s.isCheckpointSafe)
            obj.put("ver", s.verificationResult ?: "")
            obj.put("time", s.executedTimestamp ?: 0L)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun deserializeSteps(raw: String): List<TaskPlanStep> {
        if (raw.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<TaskPlanStep>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("id", "")
                val title = obj.optString("title", "")
                val desc = obj.optString("desc", "")
                val target = obj.optString("target").takeIf { it.isNotBlank() }
                val exp = obj.optString("exp", "")
                val done = obj.optBoolean("done", false)
                val safe = obj.optBoolean("safe", true)
                val ver = obj.optString("ver").takeIf { it.isNotBlank() }
                val time = obj.optLong("time", 0L)
                list.add(
                    TaskPlanStep(
                        id = id,
                        title = title,
                        description = desc,
                        targetAppOrUrl = target,
                        expectedResult = exp,
                        isCompleted = done,
                        isCheckpointSafe = safe,
                        verificationResult = ver,
                        executedTimestamp = if (time > 0) time else null
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
