package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val NavyDark = Color(0xFF070B13)
val NavySurface = Color(0xFF0E1729)
val NavySurfaceElevated = Color(0xFF16233B)
val CyanAccent = Color(0xFF00E5FF)
val CyanGlow = Color(0xFF7DD3FC)
val ElectricBlue = Color(0xFF2979FF)
val NeonGreen = Color(0xFF34D399)
val AmberWarning = Color(0xFFFBBF24)
val CoralError = Color(0xFFF87171)
val TextWhite = Color(0xFFFFFFFF)
val TextMuted = Color(0xFF94A3B8)
val BorderDark = Color(0x4D00E5FF)

val LightBg = Color(0xFFF4F8FD)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFE6F0FA)
val LightTextPrimary = Color(0xFF0B1528)
val LightTextSecondary = Color(0xFF475569)

fun parseHexColorOrDefault(hex: String, fallback: Color = NavyDark): Color {
    return try {
        val cleaned = hex.trim().let { if (it.startsWith("#")) it else "#$it" }
        Color(android.graphics.Color.parseColor(cleaned))
    } catch (_: Exception) {
        fallback
    }
}
