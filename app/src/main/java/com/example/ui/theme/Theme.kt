package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = NavyDark,
    primaryContainer = NavySurfaceElevated,
    onPrimaryContainer = CyanGlow,
    secondary = ElectricBlue,
    onSecondary = TextWhite,
    background = NavyDark,
    onBackground = TextWhite,
    surface = NavySurface,
    onSurface = TextWhite,
    surfaceVariant = NavySurfaceElevated,
    onSurfaceVariant = TextMuted,
    error = CoralError,
    onError = TextWhite
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricBlue,
    onPrimary = TextWhite,
    primaryContainer = LightSurfaceElevated,
    onPrimaryContainer = LightTextPrimary,
    secondary = CyanAccent,
    onSecondary = NavyDark,
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightTextSecondary,
    error = CoralError,
    onError = TextWhite
)

@Composable
fun MyApplicationTheme(
    themeMode: String = "3D_MODEL",
    customBgHex: String = "#070B13",
    accentHex: String = "#00E5FF",
    content: @Composable () -> Unit
) {
    val isLight = themeMode == "LIGHT"
    val accentColor = parseHexColorOrDefault(accentHex, CyanAccent)
    val customBgColor = if (themeMode == "CUSTOM_HEX") {
        parseHexColorOrDefault(customBgHex, NavyDark)
    } else if (isLight) {
        LightBg
    } else {
        NavyDark
    }

    val colorScheme = if (isLight) {
        LightColorScheme.copy(
            primary = accentColor,
            background = customBgColor
        )
    } else {
        DarkColorScheme.copy(
            primary = accentColor,
            background = customBgColor
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = customBgColor.toArgb()
                window.navigationBarColor = (if (isLight) LightSurface else NavySurface).toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLight
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
