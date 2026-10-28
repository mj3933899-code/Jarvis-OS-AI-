package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppConfig
import com.example.ui.theme.*
import com.example.util.PermissionCardModel
import com.example.util.PermissionStatusChecker

@Composable
fun SetupScreen(
    viewModel: MainViewModel,
    onSetupComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val setupProgress by viewModel.setupProgress.collectAsState()
    val config by viewModel.config.collectAsState()
    val tenCards by viewModel.tenPermissionCards.collectAsState()
    val fiveCards by viewModel.fiveAdvancedCards.collectAsState()

    var currentStep by remember(setupProgress.currentStep) {
        mutableIntStateOf(setupProgress.currentStep.coerceIn(0, 2))
    }

    var apiKeyInput by remember(config.apiKey) { mutableStateOf(config.apiKey) }
    var selectedModel by remember(config.modelName) { mutableStateOf(config.modelName) }

    val multiPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshPermissionCards()
    }

    LaunchedEffect(currentStep) {
        viewModel.refreshPermissionCards()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = NavyDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "JARVIS SETUP (${currentStep + 1}/3)",
                        style = MaterialTheme.typography.titleLarge,
                        color = CyanAccent,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = when (currentStep) {
                            0 -> "Screen E: Runtime Permissions (10 Cards)"
                            1 -> "Screen F: Always-On Background & Access (5 Cards)"
                            else -> "Screen C: Gemini 3.8 Flash & API Setup"
                        },
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                TextButton(
                    onClick = onSetupComplete,
                    modifier = Modifier.testTag("setup_skip_all_button")
                ) {
                    Text("Skip Setup", color = CyanGlow, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { (currentStep + 1) / 3f },
                modifier = Modifier.fillMaxWidth(),
                color = CyanAccent,
                trackColor = NavySurfaceElevated
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.weight(1f)) {
                when (currentStep) {
                    0 -> {
                        // SCREEN E — 10 RUNTIME PERMISSION CARDS
                        val grantedCount = tenCards.count { it.isGranted }
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Granted: $grantedCount / ${tenCards.size} Runtime Permissions",
                                    color = NeonGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Button(
                                    onClick = {
                                        val allPerms = PermissionStatusChecker.getAllMissingRuntimePermissions(context)
                                        if (allPerms.isNotEmpty()) {
                                            multiPermissionLauncher.launch(allPerms.toTypedArray())
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                                    modifier = Modifier.testTag("btn_allow_all_10_permissions")
                                ) {
                                    Icon(Icons.Default.DoneAll, contentDescription = null, tint = NavyDark, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Allow All", color = NavyDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(tenCards) { card ->
                                    PermissionCardRow(
                                        card = card,
                                        onActionClick = {
                                            if (card.runtimePermissions.isNotEmpty()) {
                                                multiPermissionLauncher.launch(card.runtimePermissions.toTypedArray())
                                            } else {
                                                launchSpecialPermissionSetting(context, card.id)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        // SCREEN F — 5 ALWAYS-ON BACKGROUND & SYSTEM ACCESS CARDS
                        val grantedCount = fiveCards.count { it.isGranted }
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "Configured: $grantedCount / ${fiveCards.size} Advanced System Capabilities",
                                color = CyanAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Each card opens the exact Android OS Settings page and auto-verifies when you return.",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(fiveCards) { card ->
                                    PermissionCardRow(
                                        card = card,
                                        onActionClick = {
                                            launchSpecialPermissionSetting(context, card.id)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        // STEP 3 — GEMINI 3.8 FLASH / API CONFIGURATION
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = NavySurface),
                                border = BorderStroke(1.dp, CyanAccent)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Gemini AI Engine Configuration",
                                        color = CyanAccent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "Default model is ${AppConfig.DEFAULT_GEMINI_MODEL} (fast multimodal). Optional advanced model: ${AppConfig.ADVANCED_GEMINI_MODEL}.",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        AppConfig.OFFICIAL_GEMINI_MODELS.take(2).forEach { m ->
                                            FilterChip(
                                                selected = selectedModel == m,
                                                onClick = { selectedModel = m },
                                                label = { Text(m, fontSize = 12.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = CyanAccent,
                                                    selectedLabelColor = NavyDark
                                                )
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = apiKeyInput,
                                        onValueChange = { apiKeyInput = it },
                                        label = { Text("Gemini API Key (Optional if set in Secrets)") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("setup_apikey_input"),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CyanAccent,
                                            unfocusedBorderColor = BorderDark,
                                            focusedTextColor = TextWhite,
                                            unfocusedTextColor = TextWhite
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentStep > 0) {
                    OutlinedButton(
                        onClick = {
                            currentStep -= 1
                            viewModel.saveSetupStep(currentStep)
                        },
                        modifier = Modifier.testTag("setup_back_button")
                    ) {
                        Text("Back", color = TextWhite)
                    }
                } else {
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Button(
                    onClick = {
                        if (currentStep < 2) {
                            currentStep += 1
                            viewModel.saveSetupStep(currentStep)
                        } else {
                            viewModel.updateConfig(
                                config.copy(
                                    modelName = selectedModel,
                                    geminiModel = selectedModel,
                                    apiKey = apiKeyInput.trim(),
                                    geminiApiKey = apiKeyInput.trim()
                                )
                            )
                            viewModel.startBackgroundService(context)
                            onSetupComplete()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                    modifier = Modifier.testTag("setup_next_button")
                ) {
                    Text(
                        text = if (currentStep < 2) "Next Step" else "Launch JARVIS",
                        color = NavyDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionCardRow(
    card: PermissionCardModel,
    onActionClick: () -> Unit
) {
    val badgeColor = if (card.isGranted) NeonGreen else if (card.isSpecialAccess) CyanGlow else AmberWarning

    Card(
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("perm_card_${card.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = card.titleEn,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = badgeColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, badgeColor)
                    ) {
                        Text(
                            text = card.statusBadge,
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = card.titleBn,
                    color = CyanGlow,
                    fontSize = 11.sp
                )
                Text(
                    text = card.descriptionEn,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (!card.isGranted) {
                Button(
                    onClick = onActionClick,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_grant_${card.id}")
                ) {
                    Text(
                        text = if (card.runtimePermissions.isNotEmpty()) "Allow" else "Settings",
                        color = NavyDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            } else {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Allowed",
                    tint = NeonGreen
                )
            }
        }
    }
}

fun launchSpecialPermissionSetting(context: Context, cardId: String) {
    try {
        val intent = when (cardId) {
            "adv_battery" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${context.packageName}")
                    )
                } else {
                    Intent(Settings.ACTION_SETTINGS)
                }
            }
            "adv_overlay" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                } else {
                    Intent(Settings.ACTION_SETTINGS)
                }
            }
            "adv_notif_access" -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            "adv_accessibility" -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            "adv_assistant" -> Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
            else -> Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}")
            )
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val fallback = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}")
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(fallback)
        } catch (_: Exception) {}
    }
}
