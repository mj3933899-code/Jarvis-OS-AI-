package com.example.service

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class JarvisNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isListenerConnected.value = true
        instance = this
        Log.d(TAG, "JARVIS NotificationListener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isListenerConnected.value = false
        if (instance == this) {
            instance = null
        }
        Log.d(TAG, "JARVIS NotificationListener disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        val extras = sbn.notification?.extras ?: return

        val title = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) return

        val notif = ReceivedNotification(
            id = sbn.id,
            packageName = sbn.packageName ?: "",
            title = title,
            text = text,
            postTime = sbn.postTime
        )

        _lastNotification.value = notif
        val currentList = _recentNotifications.value.toMutableList()
        currentList.add(0, notif)
        _recentNotifications.value = currentList.take(20)
    }

    companion object {
        private const val TAG = "JarvisNotifListener"
        var instance: JarvisNotificationListenerService? = null
            private set

        private val _isListenerConnected = MutableStateFlow(false)
        val isListenerConnected: StateFlow<Boolean> = _isListenerConnected.asStateFlow()

        private val _lastNotification = MutableStateFlow<ReceivedNotification?>(null)
        val lastNotification: StateFlow<ReceivedNotification?> = _lastNotification.asStateFlow()

        private val _recentNotifications = MutableStateFlow<List<ReceivedNotification>>(emptyList())
        val recentNotifications: StateFlow<List<ReceivedNotification>> = _recentNotifications.asStateFlow()

        fun isPermissionGranted(context: Context): Boolean {
            return NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        }
    }
}
