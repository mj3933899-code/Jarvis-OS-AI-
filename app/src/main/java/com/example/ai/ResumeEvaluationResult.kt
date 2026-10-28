package com.example.ai

import com.example.model.WorkTask

sealed class ResumeEvaluationResult {
    data class CanAutoResume(
        val task: WorkTask,
        val confidence: Float,
        val resumeActionBn: String,
        val resumeActionEn: String
    ) : ResumeEvaluationResult()

    data class NeedsUserClarification(
        val task: WorkTask,
        val reasonBn: String,
        val reasonEn: String
    ) : ResumeEvaluationResult()

    data class Ineligible(
        val task: WorkTask,
        val reason: String
    ) : ResumeEvaluationResult()
}
