package com.example.tools

import com.example.model.ActionRiskLevel
import com.example.model.ToolDefinition

object ToolRegistry {
    val tools: Map<String, ToolDefinition>

    init {
        val list = listOf(
            ToolDefinition("openApp", "Opens an installed Android application by package name or search label", null, ActionRiskLevel.LOW, false, false),
            ToolDefinition("closeApp", "Navigates to Android Home screen to exit current foreground view", null, ActionRiskLevel.LOW, true, false),
            ToolDefinition("openWebsite", "Opens a specified website URL in the default browser", null, ActionRiskLevel.LOW, false, false),
            ToolDefinition("browserSearch", "Performs a web search using the user query in the browser", null, ActionRiskLevel.LOW, false, false),
            ToolDefinition("click", "Clicks an element on the screen by its text or accessibility ID", null, ActionRiskLevel.MEDIUM, true, false),
            ToolDefinition("scroll", "Scrolls forward or backward on the current active window", null, ActionRiskLevel.LOW, true, false),
            ToolDefinition("typeText", "Types text into the currently focused editable input field", null, ActionRiskLevel.MEDIUM, true, false),
            ToolDefinition("pressBack", "Simulates the Android system Back button", null, ActionRiskLevel.LOW, true, false),
            ToolDefinition("pressHome", "Simulates the Android system Home button", null, ActionRiskLevel.LOW, true, false),
            ToolDefinition("readScreen", "Inspects and reads the visible hierarchy and text on current screen", null, ActionRiskLevel.LOW, true, false),
            ToolDefinition("setVolume", "Sets media volume level (0 to 100%)", null, ActionRiskLevel.LOW, false, true),
            ToolDefinition("increaseVolume", "Increases media volume by 1 step", null, ActionRiskLevel.LOW, false, true),
            ToolDefinition("decreaseVolume", "Decreases media volume by 1 step", null, ActionRiskLevel.LOW, false, true),
            ToolDefinition("mute", "Mutes media volume and saves previous level", null, ActionRiskLevel.LOW, false, true),
            ToolDefinition("unmute", "Restores media volume from previous level", null, ActionRiskLevel.LOW, false, true),
            ToolDefinition("openSettings", "Opens device system settings", null, ActionRiskLevel.LOW, false, false),
            ToolDefinition("openWifiSettings", "Opens Wi-Fi network settings", null, ActionRiskLevel.LOW, false, false),
            ToolDefinition("openBluetoothSettings", "Opens Bluetooth device settings", null, ActionRiskLevel.LOW, false, false),
            ToolDefinition("openDisplaySettings", "Opens Display & brightness settings", null, ActionRiskLevel.LOW, false, false),
            ToolDefinition("makePhoneCall", "Opens phone dialer with specified number for user review", null, ActionRiskLevel.MEDIUM, false, false),
            ToolDefinition("openContacts", "Opens device contacts list", null, ActionRiskLevel.LOW, false, false),
            ToolDefinition("openPlayStore", "Opens Google Play Store or searches for an app", null, ActionRiskLevel.LOW, false, false),
            ToolDefinition("camera", "Opens device camera app", null, ActionRiskLevel.LOW, false, false),
            ToolDefinition("gallery", "Opens device photo gallery", null, ActionRiskLevel.LOW, false, false),
            ToolDefinition("copy", "Copies text to system clipboard", null, ActionRiskLevel.LOW, false, false),
            ToolDefinition("paste", "Pastes clipboard text into focused element", null, ActionRiskLevel.MEDIUM, true, false)
        )
        tools = list.associateBy { it.name }
    }

    fun getTool(name: String): ToolDefinition? = tools[name]
}
