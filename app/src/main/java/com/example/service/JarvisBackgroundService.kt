package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.JarvisApplication
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class JarvisBackgroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        _isServiceRunning.value = true
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_STOP_SERVICE -> {
                _serviceStatusText.value = "STOPPING"
                try {
                    JarvisApplication.instance?.voiceManager?.stopListening()
                    JarvisApplication.instance?.ttsManager?.stop()
                    JarvisApplication.instance?.workTaskManager?.interruptActiveTasks("JARVIS turned OFF")
                } catch (_: Exception) {}
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                _isServiceRunning.value = false
                _serviceStatusText.value = "OFF"
                return START_NOT_STICKY
            }
            ACTION_PAUSE_TASK -> {
                try {
                    JarvisApplication.instance?.workTaskManager?.interruptActiveTasks("Notification Pause")
                    updateNotification("Tasks Paused • Tap to resume")
                } catch (_: Exception) {}
            }
            ACTION_RESUME_TASK -> {
                try {
                    val activeTasks = JarvisApplication.instance?.workTaskManager?.activeTasks?.value
                    val pausedTask = activeTasks?.values?.firstOrNull()
                    if (pausedTask != null) {
                        JarvisApplication.instance?.workTaskManager?.resumeTask(pausedTask.id)
                        updateNotification("Resumed task: ${pausedTask.name}")
                    }
                } catch (_: Exception) {}
            }
            ACTION_UPDATE_STATUS -> {
                val statusText = intent.getStringExtra(EXTRA_STATUS_TEXT) ?: "JARVIS Active"
                _serviceStatusText.value = statusText
                if (_isServiceRunning.value) {
                    updateNotification(statusText)
                }
            }
            ACTION_START_SERVICE, null -> {
                val statusText = intent?.getStringExtra(EXTRA_STATUS_TEXT) ?: "JARVIS Active • Tap to Talk Ready"
                _serviceStatusText.value = statusText
                val notification = buildNotification(statusText)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                        )
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                    _isServiceRunning.value = true
                    _lastServiceError.value = null
                } catch (e: Exception) {
                    _isServiceRunning.value = false
                    _lastServiceError.value = "Foreground start error: ${e.message}"
                }
            }
        }
        return START_STICKY
    }

    private fun updateNotification(statusText: String) {
        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, buildNotification(statusText))
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceRunning.value = false
        _serviceStatusText.value = "OFF"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "JARVIS Background Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors system events, voice commands and active tasks"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, JarvisBackgroundService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = Intent(this, JarvisBackgroundService::class.java).apply {
            action = ACTION_PAUSE_TASK
        }
        val pausePendingIntent = PendingIntent.getService(
            this,
            2,
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JARVIS AI Assistant")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .addAction(R.drawable.ic_launcher_foreground, "Pause", pausePendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Turn OFF", stopPendingIntent)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "jarvis_bg_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_SERVICE = "com.example.action.START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.action.STOP_SERVICE"
        const val ACTION_UPDATE_STATUS = "com.example.action.UPDATE_STATUS"
        const val ACTION_PAUSE_TASK = "com.example.action.PAUSE_TASK"
        const val ACTION_RESUME_TASK = "com.example.action.RESUME_TASK"
        const val EXTRA_STATUS_TEXT = "extra_status_text"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _serviceStatusText = MutableStateFlow("OFF")
        val serviceStatusText: StateFlow<String> = _serviceStatusText.asStateFlow()

        private val _lastServiceError = MutableStateFlow<String?>(null)
        val lastServiceError: StateFlow<String?> = _lastServiceError.asStateFlow()
    }
}
