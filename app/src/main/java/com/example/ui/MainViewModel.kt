package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.JarvisApplication
import com.example.ai.ConnectionDiagnostics
import com.example.ai.InterruptionType
import com.example.data.entity.MessageEntity
import com.example.diagnostics.DiagnosticItem
import com.example.model.ActionReceipt
import com.example.model.AppConfig
import com.example.model.AssistantState
import com.example.model.SetupProgress
import com.example.model.ToolCallRequest
import com.example.model.VerificationStatus
import com.example.model.VoiceMatchStatus
import com.example.model.WeatherState
import com.example.model.WorkTask
import com.example.service.JarvisBackgroundService
import com.example.util.PermissionCardModel
import com.example.util.PermissionStatusChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as JarvisApplication
    private val repo = app.repository
    private val voiceMgr = app.voiceManager
    private val ttsMgr = app.ttsManager
    private val aiMgr = app.aiProviderManager
    private val workMgr = app.workTaskManager
    private val diagMgr = app.diagnosticsManager
    private val voiceMatchMgr = app.voiceMatchManager
    private val interruptionMgr = app.taskInterruptionManager

    private val weatherHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    val config: StateFlow<AppConfig> = repo.configFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AppConfig.default()
    )

    val setupProgress: StateFlow<SetupProgress> = repo.setupProgressFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SetupProgress()
    )

    val messages: StateFlow<List<MessageEntity>> = repo.messagesFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val tasks: StateFlow<List<WorkTask>> = repo.tasksFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val receipts: StateFlow<List<ActionReceipt>> = repo.receiptsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val activeTasks: StateFlow<Map<String, WorkTask>> = workMgr.activeTasks

    private val _assistantState = MutableStateFlow(AssistantState.ACTIVE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _isFullScreenCharacter = MutableStateFlow(false)
    val isFullScreenCharacter: StateFlow<Boolean> = _isFullScreenCharacter.asStateFlow()

    private val _diagnostics = MutableStateFlow<List<DiagnosticItem>>(emptyList())
    val diagnostics: StateFlow<List<DiagnosticItem>> = _diagnostics.asStateFlow()

    private val _tenPermissionCards = MutableStateFlow<List<PermissionCardModel>>(emptyList())
    val tenPermissionCards: StateFlow<List<PermissionCardModel>> = _tenPermissionCards.asStateFlow()

    private val _fiveAdvancedCards = MutableStateFlow<List<PermissionCardModel>>(emptyList())
    val fiveAdvancedCards: StateFlow<List<PermissionCardModel>> = _fiveAdvancedCards.asStateFlow()

    private val _weatherState = MutableStateFlow(WeatherState())
    val weatherState: StateFlow<WeatherState> = _weatherState.asStateFlow()

    private val _lastCommandSummary = MutableStateFlow("None yet")
    val lastCommandSummary: StateFlow<String> = _lastCommandSummary.asStateFlow()

    private val _lastToolSummary = MutableStateFlow("None")
    val lastToolSummary: StateFlow<String> = _lastToolSummary.asStateFlow()

    private val _lastVerificationSummary = MutableStateFlow("Ready")
    val lastVerificationSummary: StateFlow<String> = _lastVerificationSummary.asStateFlow()

    val rmsDb: StateFlow<Float> = voiceMgr.rmsDb
    val isListeningState: StateFlow<Boolean> = voiceMgr.isListeningState
    val lastRecognizedText: StateFlow<String> = voiceMgr.lastRecognizedText
    val voiceError: StateFlow<String?> = voiceMgr.voiceError
    val wakeWordModeStatus: StateFlow<String> = voiceMgr.wakeWordModeStatus

    val isSpeaking: StateFlow<Boolean> = ttsMgr.isSpeaking
    val availableVoices = ttsMgr.availableVoices
    val voiceMatchStatus: StateFlow<VoiceMatchStatus> = voiceMatchMgr.status
    val voiceMatchStep: StateFlow<Int> = voiceMatchMgr.enrollmentStep
    val interruption: StateFlow<InterruptionType> = interruptionMgr.currentInterruption
    val isServiceRunning: StateFlow<Boolean> = JarvisBackgroundService.isServiceRunning
    val serviceStatusText: StateFlow<String> = JarvisBackgroundService.serviceStatusText

    val connectionDiagnostics: StateFlow<ConnectionDiagnostics?> = aiMgr.lastDiagnostics
    val isTestingConnection: StateFlow<Boolean> = aiMgr.isTestingConnection

    init {
        setupVoiceCallbacks()
        refreshAllStates()
        refreshWeather()
    }

    private fun setupVoiceCallbacks() {
        voiceMgr.isTtsSpeakingProvider = { ttsMgr.isSpeaking.value }
        voiceMgr.onInterruptTtsRequested = { ttsMgr.stop() }

        voiceMgr.onSpeechRecognized = { text ->
            processUserQuery(text)
        }
        voiceMgr.onEmergencyStop = {
            emergencyStop()
        }
        voiceMgr.onWakeWordDetected = {
            _assistantState.value = AssistantState.LISTENING
        }
        voiceMgr.onJarvisOnCommand = {
            turnJarvisOn(app)
        }
        voiceMgr.onJarvisOffCommand = {
            turnJarvisOff(app)
        }
        voiceMgr.onPauseCommand = {
            workMgr.interruptActiveTasks("Voice Command Pause")
            _assistantState.value = AssistantState.PAUSED
            speakResponse("চলমান কাজ স্থগিত করা হয়েছে।", "Active tasks paused.")
        }
        voiceMgr.onResumeCommand = {
            val paused = activeTasks.value.values.firstOrNull { it.status == com.example.model.TaskStatus.PAUSED }
            if (paused != null) {
                workMgr.resumeTask(paused.id)
                _assistantState.value = AssistantState.EXECUTING
                speakResponse("কাজ আবার শুরু করা হয়েছে।", "Task execution resumed.")
            } else {
                speakResponse("কোনো স্থগিত কাজ নেই।", "No paused tasks to resume.")
            }
        }
        voiceMgr.onCancelCommand = {
            val paused = activeTasks.value.values.firstOrNull()
            if (paused != null) {
                workMgr.cancelTask(paused.id)
                _assistantState.value = AssistantState.ACTIVE
                speakResponse("কাজ বাতিল করা হয়েছে।", "Task cancelled.")
            }
        }
        voiceMgr.onStateChanged = { state ->
            _assistantState.value = state
        }
    }

    fun setFullScreenCharacter(fullScreen: Boolean) {
        _isFullScreenCharacter.value = fullScreen
    }

    fun refreshAllStates() {
        refreshPermissionCards()
        refreshDiagnostics()
        // Prevent stale LISTENING state if recognizer is not actively listening
        if (_assistantState.value == AssistantState.LISTENING && !voiceMgr.isListeningState.value) {
            _assistantState.value = if (isServiceRunning.value) AssistantState.ACTIVE else AssistantState.OFF
        }
    }

    fun refreshPermissionCards() {
        _tenPermissionCards.value = PermissionStatusChecker.getTenRuntimePermissionCards(app)
        _fiveAdvancedCards.value = PermissionStatusChecker.getFiveAdvancedBackgroundCards(app)
        voiceMgr.checkAvailability()
    }

    fun turnJarvisOn(context: Context) {
        _assistantState.value = AssistantState.STARTING
        refreshPermissionCards()
        startBackgroundService(context, "JARVIS Active • Ready for commands")
        _assistantState.value = AssistantState.ACTIVE
        speakResponse("জার্ভিস সক্রিয় হয়েছে। আমি কীভাবে সাহায্য করতে পারি?", "JARVIS is now online. How can I help you?")
    }

    fun turnJarvisOff(context: Context) {
        _assistantState.value = AssistantState.STOPPING
        ttsMgr.stop()
        voiceMgr.stopListening()
        workMgr.interruptActiveTasks("JARVIS turned OFF")
        stopBackgroundService(context)
        _assistantState.value = AssistantState.OFF
    }

    fun toggleJarvisPower(context: Context) {
        if (_assistantState.value == AssistantState.OFF) {
            turnJarvisOn(context)
        } else {
            turnJarvisOff(context)
        }
    }

    fun startListening() {
        if (_assistantState.value == AssistantState.OFF) {
            _assistantState.value = AssistantState.ACTIVE
        }
        voiceMgr.startListening(config.value.language, userInitiatedTap = true)
    }

    fun stopListening() {
        voiceMgr.stopListening()
        if (_assistantState.value == AssistantState.LISTENING) {
            _assistantState.value = AssistantState.ACTIVE
        }
    }

    fun processUserQuery(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _lastCommandSummary.value = query
            repo.insertMessage(sender = "USER", content = query)
            _assistantState.value = AssistantState.PROCESSING

            // Check speaker authorization when strict mode is enabled
            if (config.value.strictModeEnabled && !voiceMatchMgr.isSpeakerAuthorized(true)) {
                val msg = "Strict Mode সক্রিয়: স্পিকার ভেরিফিকেশন সম্পন্ন হয়নি। দয়া করে সেটিংস থেকে কণ্ঠ যাচাই করুন।"
                repo.insertMessage(sender = "JARVIS", content = msg)
                _lastVerificationSummary.value = "Blocked by Strict Mode"
                speakResponse(msg, "Strict Mode active: speaker verification required.")
                _assistantState.value = AssistantState.ACTIVE
                return@launch
            }

            val recentHistory = messages.value.takeLast(6).map { it.sender to it.content }
            val screenText = app.toolExecutor.execute(ToolCallRequest("readScreen")).outputData["screenText"]
            val aiResult = aiMgr.infer(query, screenText, recentHistory)

            if (aiResult.isToolCall && aiResult.toolCall != null) {
                _assistantState.value = AssistantState.EXECUTING
                _lastToolSummary.value = "${aiResult.toolCall.tool}(${aiResult.toolCall.arguments})"
                val toolExec = app.toolExecutor.execute(aiResult.toolCall)
                _lastVerificationSummary.value = "${toolExec.verificationStatus.name}: ${toolExec.messageEn}"

                val receipt = ActionReceipt(
                    id = UUID.randomUUID().toString(),
                    command = query,
                    tool = toolExec.tool,
                    argumentsSummary = aiResult.toolCall.arguments.toString(),
                    executionResult = toolExec.messageEn,
                    verificationStatus = toolExec.verificationStatus,
                    finalStatus = if (toolExec.success) "SUCCESS" else "FAILED"
                )
                repo.addReceipt(receipt)

                val replyText = if (config.value.language == "bn") {
                    "${aiResult.spokenResponseBn}\n[${toolExec.verificationStatus.name}: ${toolExec.messageBn}]"
                } else {
                    "${aiResult.spokenResponseEn}\n[${toolExec.verificationStatus.name}: ${toolExec.messageEn}]"
                }
                repo.insertMessage(sender = "JARVIS", content = replyText)
                speakResponse(aiResult.spokenResponseBn, aiResult.spokenResponseEn)
                _assistantState.value = AssistantState.ACTIVE
            } else if (aiResult.isWorkTask) {
                val newTask = WorkTask(
                    id = UUID.randomUUID().toString(),
                    name = aiResult.workTaskName ?: "Automation Plan",
                    description = query,
                    taskType = aiResult.workTaskType ?: "AUTOMATION",
                    originalRequest = query
                )
                _lastToolSummary.value = "WorkTask(${newTask.name})"
                _lastVerificationSummary.value = "Task Queued & Planned"
                workMgr.startTask(newTask)
                repo.insertMessage(sender = "JARVIS", content = aiResult.spokenResponseBn)
                speakResponse(aiResult.spokenResponseBn, aiResult.spokenResponseEn)
                _assistantState.value = AssistantState.EXECUTING
            } else {
                val text = if (config.value.language == "bn") aiResult.spokenResponseBn else aiResult.spokenResponseEn
                if (aiResult.isError) {
                    _lastVerificationSummary.value = aiResult.technicalDiagnostic ?: "API Error"
                } else {
                    _lastVerificationSummary.value = "Response delivered"
                }
                repo.insertMessage(sender = "JARVIS", content = text)
                speakResponse(aiResult.spokenResponseBn, aiResult.spokenResponseEn)
                _assistantState.value = if (aiResult.isError) AssistantState.ERROR else AssistantState.ACTIVE
            }
        }
    }

    private fun speakResponse(bn: String, en: String) {
        if (!config.value.assistantVoiceEnabled) return
        val isBn = config.value.language == "bn"
        val textToSpeak = if (isBn) bn else en
        // Stop listening before speaking to prevent feedback loop
        voiceMgr.stopListening()
        _assistantState.value = AssistantState.SPEAKING
        ttsMgr.speak(textToSpeak, isBengali = isBn) {
            if (_assistantState.value == AssistantState.SPEAKING) {
                _assistantState.value = AssistantState.ACTIVE
            }
        }
    }

    fun emergencyStop() {
        ttsMgr.stop()
        voiceMgr.stopListening()
        workMgr.interruptActiveTasks("Emergency Stop Triggered")
        _assistantState.value = AssistantState.OFF
        _lastVerificationSummary.value = "EMERGENCY STOP HALTED ALL ACTIONS"
        viewModelScope.launch {
            repo.insertMessage(sender = "JARVIS", content = "জরুরী থামো (EMERGENCY STOP): সকল কাজ ও অডিও তৎক্ষণাৎ বন্ধ করা হয়েছে।")
        }
    }

    fun startBackgroundService(context: Context, statusText: String = "JARVIS is active and monitoring") {
        val intent = Intent(context, JarvisBackgroundService::class.java).apply {
            action = JarvisBackgroundService.ACTION_START_SERVICE
            putExtra(JarvisBackgroundService.EXTRA_STATUS_TEXT, statusText)
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (_: Exception) {}
    }

    fun stopBackgroundService(context: Context) {
        val intent = Intent(context, JarvisBackgroundService::class.java).apply {
            action = JarvisBackgroundService.ACTION_STOP_SERVICE
        }
        try {
            context.startService(intent)
        } catch (_: Exception) {}
    }

    fun refreshDiagnostics() {
        viewModelScope.launch {
            _diagnostics.value = diagMgr.runFullDiagnostics()
        }
    }

    fun runConnectionTest(overrideConfig: AppConfig? = null) {
        viewModelScope.launch {
            aiMgr.runConnectionDiagnostics(overrideConfig)
            refreshDiagnostics()
        }
    }

    fun migrateLegacyModel() {
        viewModelScope.launch {
            app.preferences.migrateLegacyModelToCurrentDefault()
        }
    }

    fun refreshWeather() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val req = Request.Builder()
                        .url("https://api.open-meteo.com/v1/forecast?latitude=23.8103&longitude=90.4125&current_weather=true")
                        .build()
                    weatherHttpClient.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body?.string() ?: ""
                            val json = JSONObject(body)
                            val cw = json.optJSONObject("current_weather")
                            if (cw != null) {
                                val temp = cw.optDouble("temperature", 28.0).toInt()
                                val code = cw.optInt("weathercode", 0)
                                val cond = when (code) {
                                    0 -> "Clear Sky"
                                    1, 2, 3 -> "Partly Cloudy"
                                    45, 48 -> "Mist / Fog"
                                    51, 53, 55, 61, 63, 65 -> "Rain Showers"
                                    95, 96, 99 -> "Thunderstorm"
                                    else -> "Clear Sky"
                                }
                                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                                WeatherState(
                                    city = "Dhaka",
                                    tempCelsius = temp,
                                    condition = cond,
                                    highCelsius = temp + 4,
                                    lowCelsius = temp - 5,
                                    isLive = true,
                                    lastUpdated = "Live • $timeStr"
                                )
                            } else null
                        } else null
                    }
                } catch (_: Exception) {
                    null
                }
            }
            if (result != null) {
                _weatherState.value = result
            }
        }
    }

    fun enrollVoiceSample() {
        if (voiceMatchMgr.status.value != VoiceMatchStatus.ENROLLING) {
            voiceMatchMgr.startEnrollment()
        }
        val sample = voiceMgr.getRecentAudioFeatures()
        voiceMatchMgr.addEnrollmentSample(sample)
    }

    fun resetVoiceMatch() {
        voiceMatchMgr.resetProfile()
    }

    fun createWorkTask(name: String, description: String, targetApp: String?) {
        val task = WorkTask(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            taskType = "CUSTOM",
            targetApp = targetApp,
            originalRequest = description
        )
        workMgr.startTask(task)
    }

    fun pauseTask(taskId: String) {
        workMgr.interruptActiveTasks("User requested pause")
    }

    fun resumeTask(taskId: String) {
        workMgr.resumeTask(taskId)
    }

    fun cancelTask(taskId: String) {
        workMgr.cancelTask(taskId)
    }

    fun saveSetupStep(step: Int) {
        viewModelScope.launch { repo.saveSetupStep(step) }
    }

    fun completeSetup() {
        viewModelScope.launch { repo.completeSetup() }
    }

    fun updateConfig(newConfig: AppConfig) {
        viewModelScope.launch {
            repo.commitConfig(newConfig)
            ttsMgr.setSpeechRate(newConfig.ttsSpeechRate)
            ttsMgr.setPitch(newConfig.ttsPitch)
            if (newConfig.selectedVoiceId.isNotBlank()) {
                ttsMgr.setVoice(newConfig.selectedVoiceId)
            }
        }
    }

    fun restoreSettings() {
        viewModelScope.launch { repo.restorePreviousSettings() }
    }

    fun resetSettings(keepApiKey: Boolean = true) {
        viewModelScope.launch { repo.resetToDefault(keepApiKey) }
    }

    fun clearHistory() {
        viewModelScope.launch { repo.clearMessages() }
    }

    fun clearMemories() {
        viewModelScope.launch { repo.clearMemories() }
    }
}
