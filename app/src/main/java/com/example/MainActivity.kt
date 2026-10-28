package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: MainViewModel = viewModel()
            val config by viewModel.config.collectAsState()
            val setupProgress by viewModel.setupProgress.collectAsState()
            val lifecycleOwner = LocalLifecycleOwner.current

            // Refresh all 15 permission & system states whenever user returns from OS Settings
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        viewModel.refreshAllStates()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            MyApplicationTheme(
                themeMode = config.themeMode,
                customBgHex = config.customBgHex,
                accentHex = config.accentHex
            ) {
                if (!setupProgress.isCompleted) {
                    SetupScreen(
                        viewModel = viewModel,
                        onSetupComplete = {
                            viewModel.completeSetup()
                        }
                    )
                } else {
                    MainAppScaffold(viewModel = viewModel)
                }
            }
        }
    }
}

enum class NavigationTab(val label: String, val icon: ImageVector, val testTag: String) {
    ASSISTANT("Home", Icons.Default.Home, "tab_assistant"),
    WORK("Tasks", Icons.Default.Checklist, "tab_work"),
    DIAGNOSTICS("Permissions", Icons.Default.VerifiedUser, "tab_diagnostics"),
    SETTINGS("Settings", Icons.Default.Settings, "tab_settings")
}

@Composable
fun MainAppScaffold(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val config by viewModel.config.collectAsState()
    var selectedTab by remember { mutableStateOf(NavigationTab.ASSISTANT) }
    val accentColor = parseHexColorOrDefault(config.accentHex, CyanAccent)

    // If on a secondary tab and system back is pressed, return to Home/Assistant tab
    BackHandler(enabled = selectedTab != NavigationTab.ASSISTANT) {
        selectedTab = NavigationTab.ASSISTANT
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                NavigationTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        modifier = Modifier.testTag(tab.testTag),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NavyDark,
                            selectedTextColor = accentColor,
                            indicatorColor = accentColor,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        val innerModifier = Modifier.padding(paddingValues)
        when (selectedTab) {
            NavigationTab.ASSISTANT -> AssistantScreen(
                viewModel = viewModel,
                onNavigateToSettings = { selectedTab = NavigationTab.SETTINGS },
                onNavigateToPermissions = { selectedTab = NavigationTab.DIAGNOSTICS },
                onNavigateToDiagnostics = { selectedTab = NavigationTab.DIAGNOSTICS },
                modifier = innerModifier
            )
            NavigationTab.WORK -> WorkScreen(viewModel = viewModel, modifier = innerModifier)
            NavigationTab.DIAGNOSTICS -> DiagnosticsScreen(viewModel = viewModel, modifier = innerModifier)
            NavigationTab.SETTINGS -> SettingsScreen(viewModel = viewModel, modifier = innerModifier)
        }
    }
}
