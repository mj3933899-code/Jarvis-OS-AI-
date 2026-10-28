package com.example

import android.app.Application
import com.example.ai.AiProviderManager
import com.example.ai.TaskInterruptionManager
import com.example.ai.WorkTaskManager
import com.example.data.JarvisDatabase
import com.example.data.JarvisPreferences
import com.example.data.JarvisRepository
import com.example.diagnostics.DiagnosticsManager
import com.example.tools.ToolExecutor
import com.example.tools.VerificationEngine
import com.example.voice.TtsManager
import com.example.voice.VoiceManager
import com.example.voice.VoiceMatchManager

class JarvisApplication : Application() {

    lateinit var database: JarvisDatabase
        private set
    lateinit var preferences: JarvisPreferences
        private set
    lateinit var repository: JarvisRepository
        private set
    lateinit var verificationEngine: VerificationEngine
        private set
    lateinit var toolExecutor: ToolExecutor
        private set
    lateinit var voiceMatchManager: VoiceMatchManager
        private set
    lateinit var ttsManager: TtsManager
        private set
    lateinit var voiceManager: VoiceManager
        private set
    lateinit var aiProviderManager: AiProviderManager
        private set
    lateinit var diagnosticsManager: DiagnosticsManager
        private set
    lateinit var workTaskManager: WorkTaskManager
        private set
    lateinit var taskInterruptionManager: TaskInterruptionManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = JarvisDatabase.getInstance(this)
        preferences = JarvisPreferences(this)
        repository = JarvisRepository(database.jarvisDao(), preferences)

        verificationEngine = VerificationEngine(this)
        toolExecutor = ToolExecutor(this, verificationEngine)

        voiceMatchManager = VoiceMatchManager(this)
        ttsManager = TtsManager(this)
        voiceManager = VoiceManager(this, voiceMatchManager)

        aiProviderManager = AiProviderManager(this, preferences)
        diagnosticsManager = DiagnosticsManager(this, voiceManager, ttsManager, voiceMatchManager, aiProviderManager)

        workTaskManager = WorkTaskManager(repository, toolExecutor)

        taskInterruptionManager = TaskInterruptionManager(
            context = this,
            onInterrupted = { type, reason ->
                workTaskManager.interruptActiveTasks(reason)
            },
            onInterruptionCleared = { _ ->
                // Check if any paused task can auto-resume
                val active = workTaskManager.activeTasks.value
                val paused = active.values.firstOrNull { it.status == com.example.model.TaskStatus.PAUSED }
                if (paused != null && paused.isResumeEligible) {
                    val eval = workTaskManager.evaluateTaskForResume(paused)
                    if (eval is com.example.ai.ResumeEvaluationResult.CanAutoResume) {
                        workTaskManager.resumeTask(paused.id)
                    }
                }
            }
        )
    }

    override fun onTerminate() {
        super.onTerminate()
        ttsManager.shutdown()
        voiceManager.stopListening()
    }

    companion object {
        var instance: JarvisApplication? = null
            private set
    }
}
