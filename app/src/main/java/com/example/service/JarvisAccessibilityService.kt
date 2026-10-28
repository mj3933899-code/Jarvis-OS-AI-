package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class JarvisAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isAccessibilityConnected.value = true

        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.notificationTimeout = 100L
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        event.packageName?.let { _currentPackage.value = it.toString() }
        event.className?.let { _currentActivity.value = it.toString() }
    }

    override fun onInterrupt() {
        _isAccessibilityConnected.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        _isAccessibilityConnected.value = false
        if (instance == this) {
            instance = null
        }
    }

    fun clickByText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(text)
        for (node in nodes) {
            if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    return true
                }
                parent = parent.parent
            }
        }
        return false
    }

    fun clickById(viewId: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
        for (node in nodes) {
            if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    return true
                }
                parent = parent.parent
            }
        }
        return false
    }

    fun typeTextIntoFocused(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun scroll(forward: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false
        val action = if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        return performScrollOnNode(root, action)
    }

    private fun performScrollOnNode(node: AccessibilityNodeInfo, action: Int): Boolean {
        if (node.isScrollable && node.performAction(action)) {
            return true
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (performScrollOnNode(child, action)) {
                return true
            }
        }
        return false
    }

    fun pressBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun pressHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun inspectScreenContent(): List<String> {
        val root = rootInActiveWindow ?: return emptyList()
        val list = mutableListOf<String>()
        collectNodeText(root, list)
        return list
    }

    private fun collectNodeText(node: AccessibilityNodeInfo, list: MutableList<String>) {
        node.text?.toString()?.takeIf { it.isNotBlank() }?.let { list.add(it) }
        node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { list.add(it) }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectNodeText(child, list)
        }
    }

    companion object {
        var instance: JarvisAccessibilityService? = null
            private set

        private val _isAccessibilityConnected = MutableStateFlow(false)
        val isAccessibilityConnected: StateFlow<Boolean> = _isAccessibilityConnected.asStateFlow()

        private val _currentPackage = MutableStateFlow("Unknown")
        val currentPackage: StateFlow<String> = _currentPackage.asStateFlow()

        private val _currentActivity = MutableStateFlow("Unknown")
        val currentActivity: StateFlow<String> = _currentActivity.asStateFlow()
    }
}
