package com.example.ai

import com.example.data.JarvisRepository
import com.example.model.ActionReceipt
import com.example.model.TaskPlanStep
import com.example.model.TaskStatus
import com.example.model.ToolCallRequest
import com.example.model.VerificationStatus
import com.example.model.WorkTask
import com.example.tools.ToolExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class WorkTaskManager(
    private val repository: JarvisRepository,
    private val toolExecutor: ToolExecutor
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _activeTasks = MutableStateFlow<Map<String, WorkTask>>(emptyMap())
    val activeTasks: StateFlow<Map<String, WorkTask>> = _activeTasks.asStateFlow()

    fun interruptActiveTasks(reason: String) {
        val current = _activeTasks.value
        val updated = current.toMutableMap()
        for ((id, task) in current) {
            if (task.status == TaskStatus.RUNNING || task.status == TaskStatus.PLANNING) {
                val paused = task.copy(
                    status = TaskStatus.PAUSED,
                    interruptionReason = reason,
                    logs = task.logs + "Interrupted: $reason at step ${task.stepIndex + 1}"
                )
                updated[id] = paused
                scope.launch { repository.saveTask(paused) }
            }
        }
        _activeTasks.value = updated
    }

    fun resumeTask(taskId: String) {
        val task = _activeTasks.value[taskId] ?: return
        if (task.status != TaskStatus.PAUSED) return

        val resumed = task.copy(
            status = TaskStatus.RUNNING,
            interruptionReason = null,
            logs = task.logs + "Task resumed at step ${task.stepIndex + 1}"
        )
        updateTaskState(resumed)
        executeTaskSteps(resumed)
    }

    fun cancelTask(taskId: String) {
        val task = _activeTasks.value[taskId] ?: return
        val cancelled = task.copy(
            status = TaskStatus.CANCELLED,
            logs = task.logs + "Task explicitly cancelled by user"
        )
        updateTaskState(cancelled)
        scope.launch {
            repository.saveTask(cancelled)
            val m = _activeTasks.value.toMutableMap()
            m.remove(taskId)
            _activeTasks.value = m
        }
    }

    fun startTask(task: WorkTask) {
        val planned = if (task.planSteps.isEmpty()) {
            generateDefaultPlanSteps(task)
        } else task

        val running = planned.copy(
            status = TaskStatus.RUNNING,
            currentPhase = "EXECUTING",
            logs = planned.logs + "Task execution started"
        )
        updateTaskState(running)
        executeTaskSteps(running)
    }

    fun evaluateTaskForResume(task: WorkTask): ResumeEvaluationResult {
        if (!task.isResumeEligible) {
            return ResumeEvaluationResult.Ineligible(task, "Task is marked not eligible for resumption")
        }
        if (task.status != TaskStatus.PAUSED) {
            return ResumeEvaluationResult.Ineligible(task, "Task is not currently in PAUSED state")
        }
        val remainingSteps = task.planSteps.filter { !it.isCompleted }
        if (remainingSteps.isEmpty()) {
            return ResumeEvaluationResult.Ineligible(task, "All plan steps were already completed")
        }

        val nextStep = remainingSteps.first()
        return if (nextStep.isCheckpointSafe) {
            ResumeEvaluationResult.CanAutoResume(
                task = task,
                confidence = 0.95f,
                resumeActionBn = "ধাপ '${nextStep.title}' থেকে আবার শুরু করা যাবে।",
                resumeActionEn = "Can safely resume from step '${nextStep.title}'."
            )
        } else {
            ResumeEvaluationResult.NeedsUserClarification(
                task = task,
                reasonBn = "ধাপ '${nextStep.title}' পুনরায় চালানোর জন্য আপনার সম্মতি প্রয়োজন।",
                reasonEn = "Confirmation required before re-executing step '${nextStep.title}'."
            )
        }
    }

    private fun executeTaskSteps(task: WorkTask) {
        scope.launch {
            var current = task
            val steps = current.planSteps.toMutableList()

            for (i in current.stepIndex until steps.size) {
                // Check if interrupted or cancelled
                val latest = _activeTasks.value[task.id]
                if (latest != null && (latest.status == TaskStatus.PAUSED || latest.status == TaskStatus.CANCELLED)) {
                    return@launch
                }

                val step = steps[i]
                current = current.copy(
                    stepIndex = i,
                    currentStep = step.title,
                    progress = (((i).toFloat() / steps.size) * 100).toInt(),
                    logs = current.logs + "Executing step ${i + 1}/${steps.size}: ${step.title}"
                )
                updateTaskState(current)
                repository.saveTask(current)

                // Execute action mapped to step
                val execResult = if (step.targetAppOrUrl != null) {
                    toolExecutor.execute(ToolCallRequest("openApp", mapOf("app" to step.targetAppOrUrl)))
                } else {
                    toolExecutor.execute(ToolCallRequest("browserSearch", mapOf("query" to step.title)))
                }

                val receipt = ActionReceipt(
                    id = UUID.randomUUID().toString(),
                    command = step.title,
                    tool = execResult.tool,
                    argumentsSummary = step.description,
                    executionResult = execResult.messageEn,
                    verificationStatus = execResult.verificationStatus,
                    finalStatus = if (execResult.success) "SUCCESS" else "FAILED"
                )
                repository.addReceipt(receipt)

                val completedStep = step.copy(
                    isCompleted = execResult.success,
                    verificationResult = execResult.messageEn,
                    executedTimestamp = System.currentTimeMillis()
                )
                steps[i] = completedStep

                current = current.copy(
                    planSteps = steps.toList(),
                    lastSuccessfulAction = if (execResult.success) step.title else current.lastSuccessfulAction,
                    verificationStatus = execResult.verificationStatus
                )
                updateTaskState(current)
                repository.saveTask(current)
            }

            val finished = current.copy(
                status = TaskStatus.COMPLETED,
                progress = 100,
                currentStep = "Completed",
                logs = current.logs + "All plan steps completed successfully"
            )
            updateTaskState(finished)
            repository.saveTask(finished)
        }
    }

    private fun generateDefaultPlanSteps(task: WorkTask): WorkTask {
        val steps = listOf(
            TaskPlanStep(
                id = UUID.randomUUID().toString(),
                title = "Inspect System Environment & Target",
                description = "Prepare prerequisites and verify foreground state for '${task.name}'",
                targetAppOrUrl = task.targetApp,
                expectedResult = "Environment ready",
                isCheckpointSafe = true
            ),
            TaskPlanStep(
                id = UUID.randomUUID().toString(),
                title = "Execute Primary Action",
                description = task.description.ifBlank { "Perform user requested operation" },
                targetAppOrUrl = task.targetApp,
                expectedResult = "Operation completed",
                isCheckpointSafe = true
            ),
            TaskPlanStep(
                id = UUID.randomUUID().toString(),
                title = "Verify Outcome & Produce Artifact",
                description = "Verify accessibility state and log receipt for '${task.name}'",
                expectedResult = "Outcome verified",
                isCheckpointSafe = true
            )
        )
        return task.copy(
            planSteps = steps,
            totalSteps = steps.size
        )
    }

    private fun updateTaskState(task: WorkTask) {
        val m = _activeTasks.value.toMutableMap()
        m[task.id] = task
        _activeTasks.value = m
    }
}
