package com.example.ai

import android.content.Context
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TaskInterruptionManager(
    private val context: Context,
    private val onInterrupted: (InterruptionType, String) -> Unit,
    private val onInterruptionCleared: (InterruptionType) -> Unit
) {
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    private val _currentInterruption = MutableStateFlow(InterruptionType.NONE)
    val currentInterruption: StateFlow<InterruptionType> = _currentInterruption.asStateFlow()

    var isPhoneCallActive: Boolean = false
        private set

    init {
        registerCallStateListener()
    }

    private fun registerCallStateListener() {
        if (telephonyManager == null) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                telephonyManager.registerTelephonyCallback(
                    context.mainExecutor,
                    object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                        override fun onCallStateChanged(state: Int) {
                            handleCallState(state)
                        }
                    }
                )
            } catch (_: SecurityException) {}
        } else {
            @Suppress("DEPRECATION")
            telephonyManager.listen(
                object : PhoneStateListener() {
                    @Deprecated("Deprecated in Java")
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        handleCallState(state)
                    }
                },
                @Suppress("DEPRECATION") PhoneStateListener.LISTEN_CALL_STATE
            )
        }
    }

    private fun handleCallState(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                isPhoneCallActive = true
                _currentInterruption.value = InterruptionType.INCOMING_CALL
                onInterrupted(InterruptionType.INCOMING_CALL, "Incoming phone call")
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                isPhoneCallActive = true
                _currentInterruption.value = InterruptionType.CALL_ACTIVE
                onInterrupted(InterruptionType.CALL_ACTIVE, "Active phone call")
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                if (isPhoneCallActive) {
                    val prev = _currentInterruption.value
                    isPhoneCallActive = false
                    _currentInterruption.value = InterruptionType.NONE
                    onInterruptionCleared(prev)
                }
            }
        }
    }

    fun triggerManualInterruption(reason: String) {
        _currentInterruption.value = InterruptionType.USER_MANUAL
        onInterrupted(InterruptionType.USER_MANUAL, reason)
    }

    fun clearInterruption() {
        val prev = _currentInterruption.value
        _currentInterruption.value = InterruptionType.NONE
        onInterruptionCleared(prev)
    }
}
