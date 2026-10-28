package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ai.ConnectionDiagnostics
import com.example.model.AppConfig
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val savedConfig by viewModel.config.collectAsState()
    val voiceMatchStatus by viewModel.voiceMatchStatus.collectAsState()
    val voiceMatchStep by viewModel.voiceMatchStep.collectAsState()
    val wakeWordModeStatus by viewModel.wakeWordModeStatus.collectAsState()
    val connectionDiag by viewModel.connectionDiagnostics.collectAsState()
    val isTestingConnection by viewModel.isTestingConnection.collectAsState()

    var draft by remember(savedConfig) { mutableStateOf(savedConfig) }
    var showApiKey by remember { mutableStateOf(false) }
    var saveFeedback by remember { mutableStateOf<String?>(null) }
    var hexInputText by remember(savedConfig.customBgHex) { mutableStateOf(savedConfig.customBgHex) }

    val providers = listOf("GEMINI", "OPENAI", "DEEPSEEK", "OPENROUTER", "CUSTOM")
    val geminiPresetModels = AppConfig.OFFICIAL_GEMINI_MODELS
    val accentColor = parseHexColorOrDefault(draft.accentHex, CyanAccent)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Settings & Intelligence",
                        style = MaterialTheme.typography.headlineMedium,
                        color = accentColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "3D Model Theme Options • Gemini 3.8 Flash / 4 Argon • Voice & Security",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }

        // =========================================================================
        // SCREEN D — 3D MODEL THEME OPTIONS (MATCHING REFERENCE IMAGE 1)
        // =========================================================================
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = BorderStroke(1.5.dp, accentColor),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("theme_options_panel")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SettingsSuggest, contentDescription = null, tint = accentColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "3D Model Theme Options",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextWhite,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Select App Theme — Restyles the entire app and the neural orb",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Option 1: Light
                    ThemeOptionCard(
                        title = "Light",
                        subtitle = "Crisp and clean - Blue & White accents",
                        selected = draft.themeMode == "LIGHT",
                        accentColor = accentColor,
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(2.dp, ElectricBlue, CircleShape)
                            )
                        },
                        onClick = {
                            val updated = draft.copy(themeMode = "LIGHT", showCharacterAvatar = false)
                            draft = updated
                            viewModel.updateConfig(updated)
                        },
                        testTag = "theme_option_light"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option 2: Dark
                    ThemeOptionCard(
                        title = "Dark",
                        subtitle = "Deep, midnight cosmic - Neon cyan & purple",
                        selected = draft.themeMode == "DARK",
                        accentColor = accentColor,
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(NavyDark)
                                    .border(2.dp, CyanAccent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("C", color = CyanAccent, fontWeight = FontWeight.Bold)
                            }
                        },
                        onClick = {
                            val updated = draft.copy(themeMode = "DARK", showCharacterAvatar = true)
                            draft = updated
                            viewModel.updateConfig(updated)
                        },
                        testTag = "theme_option_dark"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option 3: 3D Model Full
                    ThemeOptionCard(
                        title = "3D Model Full",
                        subtitle = "Large character view (Same Page Full Screen)",
                        selected = draft.themeMode == "3D_MODEL_FULL",
                        accentColor = accentColor,
                        leadingContent = {
                            Image(
                                painter = painterResource(id = R.drawable.img_jarvis_avatar),
                                contentDescription = "3D Model Full Thumbnail",
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, accentColor, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        },
                        onClick = {
                            val updated = draft.copy(themeMode = "3D_MODEL_FULL", showCharacterAvatar = true)
                            draft = updated
                            viewModel.updateConfig(updated)
                            viewModel.setFullScreenCharacter(true)
                        },
                        testTag = "theme_option_3d_model_full"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option 4: 3D Model
                    ThemeOptionCard(
                        title = "3D Model",
                        subtitle = "Show the character instead of the orb",
                        selected = draft.themeMode == "3D_MODEL",
                        accentColor = accentColor,
                        leadingContent = {
                            Image(
                                painter = painterResource(id = R.drawable.img_jarvis_avatar),
                                contentDescription = "3D Model Thumbnail",
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, accentColor, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        },
                        onClick = {
                            val updated = draft.copy(themeMode = "3D_MODEL", showCharacterAvatar = true)
                            draft = updated
                            viewModel.updateConfig(updated)
                            viewModel.setFullScreenCharacter(false)
                        },
                        testTag = "theme_option_3d_model"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Option 5: Custom Background (#RRGGBB + Apply + Color Swatches)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NavySurfaceElevated),
                        border = BorderStroke(
                            1.5.dp,
                            if (draft.themeMode == "CUSTOM_HEX") accentColor else BorderDark
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Custom Background",
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Choose app HEX colour (#RRGGBB)",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = hexInputText,
                                    onValueChange = { hexInputText = it },
                                    singleLine = true,
                                    placeholder = { Text("#00E5FF", color = TextMuted) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("custom_hex_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = accentColor,
                                        unfocusedBorderColor = BorderDark,
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val cleaned = hexInputText.trim().let {
                                            if (it.startsWith("#")) it else "#$it"
                                        }
                                        val updated = draft.copy(
                                            themeMode = "CUSTOM_HEX",
                                            customBgHex = cleaned
                                        )
                                        draft = updated
                                        viewModel.updateConfig(updated)
                                        saveFeedback = "Custom HEX Background ($cleaned) Applied!"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.testTag("btn_apply_custom_hex")
                                ) {
                                    Text("Apply", color = NavyDark, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            val swatches = listOf(
                                "#00E5FF" to Color(0xFF00E5FF),
                                "#7C4DFF" to Color(0xFF7C4DFF),
                                "#1A103C" to Color(0xFF1A103C),
                                "#0D1B2A" to Color(0xFF0D1B2A),
                                "#1E293B" to Color(0xFF1E293B),
                                "#070B13" to Color(0xFF070B13)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                swatches.forEach { (hexStr, col) ->
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(col)
                                            .border(
                                                width = if (hexInputText.equals(hexStr, ignoreCase = true)) 2.5.dp else 1.dp,
                                                color = if (hexInputText.equals(hexStr, ignoreCase = true)) TextWhite else BorderDark,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                hexInputText = hexStr
                                                val updated = draft.copy(
                                                    themeMode = "CUSTOM_HEX",
                                                    customBgHex = hexStr
                                                )
                                                draft = updated
                                                viewModel.updateConfig(updated)
                                            }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "Character Scale: ${String.format("%.2f", draft.characterScale)}x",
                        color = TextWhite,
                        fontSize = 12.sp
                    )
                    Slider(
                        value = draft.characterScale,
                        onValueChange = { draft = draft.copy(characterScale = it) },
                        valueRange = 0.8f..1.35f,
                        colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                    )
                    Text(
                        "Animation Intensity: ${String.format("%.2f", draft.animationIntensity)}x",
                        color = TextWhite,
                        fontSize = 12.sp
                    )
                    Slider(
                        value = draft.animationIntensity,
                        onValueChange = { draft = draft.copy(animationIntensity = it) },
                        valueRange = 0.2f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                    )
                }
            }
        }

        // =========================================================================
        // SCREEN C — AI PROVIDER & MODEL SETTINGS (GEMINI 3.8 FLASH / 4 ARGON)
        // =========================================================================
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = BorderStroke(1.5.dp, accentColor),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_provider_settings_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Memory, contentDescription = null, tint = accentColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "AI Provider & Gemini Model Configuration",
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Default Model: ${AppConfig.DEFAULT_GEMINI_MODEL} • Advanced: ${AppConfig.ADVANCED_GEMINI_MODEL}. Uses secure x-goog-api-key header authentication.",
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    // Legacy Model Migration Banner if user has gemini-2.5-flash or gemini-2.0-flash selected
                    if (draft.aiProvider.equals("GEMINI", ignoreCase = true) &&
                        (draft.modelName == "gemini-2.5-flash" || draft.modelName == "gemini-2.0-flash" || draft.isLegacyGeminiModel())
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AmberWarning.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, AmberWarning)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Upgrade, contentDescription = null, tint = AmberWarning)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Legacy Model Selected (${draft.modelName})",
                                        color = AmberWarning,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        "Upgrade to ${AppConfig.DEFAULT_GEMINI_MODEL} for current Gemini API compatibility.",
                                        color = TextWhite,
                                        fontSize = 11.sp
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        draft = draft.copy(
                                            modelName = AppConfig.DEFAULT_GEMINI_MODEL,
                                            geminiModel = AppConfig.DEFAULT_GEMINI_MODEL
                                        )
                                        viewModel.migrateLegacyModel()
                                    },
                                    modifier = Modifier.testTag("btn_migrate_legacy_model")
                                ) {
                                    Text("Migrate", color = accentColor, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Provider:", color = TextMuted, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        providers.forEach { p ->
                            val selected = draft.aiProvider.equals(p, ignoreCase = true)
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    val defaultModel = when (p) {
                                        "GEMINI" -> AppConfig.DEFAULT_GEMINI_MODEL
                                        "OPENAI" -> "gpt-4o-mini"
                                        "DEEPSEEK" -> "deepseek-chat"
                                        "OPENROUTER" -> "google/gemini-3.8-flash"
                                        else -> draft.modelName
                                    }
                                    draft = draft.copy(
                                        aiProvider = p,
                                        modelName = defaultModel
                                    )
                                },
                                label = { Text(p, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accentColor,
                                    selectedLabelColor = NavyDark
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Official Gemini Models Quick Selector
                    if (draft.aiProvider.equals("GEMINI", ignoreCase = true)) {
                        Text("Official Gemini Model Presets:", color = TextMuted, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            geminiPresetModels.chunked(2).forEach { rowModels ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowModels.forEach { modelId ->
                                        val isSelected = draft.modelName == modelId
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                draft = draft.copy(
                                                    modelName = modelId,
                                                    geminiModel = modelId
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = when (modelId) {
                                                        AppConfig.DEFAULT_GEMINI_MODEL -> "$modelId (Default)"
                                                        AppConfig.ADVANCED_GEMINI_MODEL -> "$modelId (Adv)"
                                                        else -> "$modelId (Legacy)"
                                                    },
                                                    fontSize = 11.sp
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = accentColor,
                                                selectedLabelColor = NavyDark
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("model_chip_$modelId")
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedTextField(
                        value = draft.modelName,
                        onValueChange = {
                            draft = draft.copy(
                                modelName = it,
                                geminiModel = if (draft.aiProvider.equals("GEMINI", ignoreCase = true)) it else draft.geminiModel
                            )
                        },
                        label = { Text("Active Model ID (or Custom Override)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_model_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = draft.apiKey,
                        onValueChange = {
                            draft = draft.copy(
                                apiKey = it,
                                geminiApiKey = if (draft.aiProvider.equals("GEMINI", ignoreCase = true)) it else draft.geminiApiKey
                            )
                        },
                        label = { Text("API Key (Stored locally or via Secrets GEMINI_API_KEY)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_apikey_input"),
                        singleLine = true,
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle API Key Visibility",
                                    tint = TextMuted
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = draft.customEndpoint,
                        onValueChange = { draft = draft.copy(customEndpoint = it) },
                        label = { Text("Custom Endpoint / Base URL (Optional)") },
                        placeholder = { Text("https://generativelanguage.googleapis.com/v1beta") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_baseurl_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.updateConfig(draft)
                                saveFeedback = "Settings Saved (${draft.aiProvider} • ${draft.modelName})"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_settings_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, tint = NavyDark)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Config", color = NavyDark, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.updateConfig(draft)
                                viewModel.runConnectionTest(draft)
                            },
                            enabled = !isTestingConnection,
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_connection_button")
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = NavyDark,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = NavyDark)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isTestingConnection) "Testing..." else "Test Connection",
                                color = NavyDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    saveFeedback?.let { msg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(msg, color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    connectionDiag?.let { diag ->
                        Spacer(modifier = Modifier.height(12.dp))
                        ConnectionDiagnosticsCard(diag = diag, accentColor = accentColor)
                    }
                }
            }
        }

        // =========================================================================
        // VOICE, WAKE WORD & SPEAKER ENROLLMENT SETTINGS
        // =========================================================================
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = BorderStroke(1.dp, BorderDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Voice, Language & Speaker Verification",
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Wake-Word Mode: $wakeWordModeStatus",
                        color = AmberWarning,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Primary Language (Bengali / English)", color = TextWhite)
                        Switch(
                            checked = draft.language == "bn",
                            onCheckedChange = { isBn ->
                                val updated = draft.copy(language = if (isBn) "bn" else "en")
                                draft = updated
                                viewModel.updateConfig(updated)
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Spoken TTS Responses", color = TextWhite)
                        Switch(
                            checked = draft.assistantVoiceEnabled,
                            onCheckedChange = {
                                val updated = draft.copy(assistantVoiceEnabled = it)
                                draft = updated
                                viewModel.updateConfig(updated)
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Strict Speaker Gate", color = TextWhite)
                            Text(
                                "Uses RMS/energy heuristic profile ($voiceMatchStatus • $voiceMatchStep/4 samples)",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = draft.strictModeEnabled,
                            onCheckedChange = {
                                val updated = draft.copy(strictModeEnabled = it)
                                draft = updated
                                viewModel.updateConfig(updated)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.enrollVoiceSample() },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            modifier = Modifier.testTag("enroll_voice_button")
                        ) {
                            Text("Record Voice Sample", color = NavyDark, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(onClick = { viewModel.resetVoiceMatch() }) {
                            Text("Reset Profile", color = CoralError)
                        }
                    }
                }
            }
        }

        // =========================================================================
        // BACKUP, RESTORE & MEMORY CONTROLS
        // =========================================================================
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.restoreSettings()
                        saveFeedback = "Restored previous configuration snapshot."
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("restore_settings_button")
                ) {
                    Text("Restore Prev", color = accentColor, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        viewModel.resetSettings(keepApiKey = true)
                        saveFeedback = "Reset to defaults (API key preserved)."
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("reset_settings_button")
                ) {
                    Text("Reset Defaults", color = CoralError, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    accentColor: Color,
    leadingContent: @Composable () -> Unit,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) NavySurfaceElevated else NavyDark.copy(alpha = 0.65f)
        ),
        border = BorderStroke(
            width = if (selected) 1.8.dp else 1.dp,
            color = if (selected) accentColor else BorderDark
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingContent()
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = subtitle,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
            if (selected) {
                Icon(
                    Icons.Default.CheckBox,
                    contentDescription = "Selected",
                    tint = accentColor
                )
            }
        }
    }
}

@Composable
private fun ConnectionDiagnosticsCard(
    diag: ConnectionDiagnostics,
    accentColor: Color
) {
    val allOk = diag.generationSucceeded
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (allOk) NeonGreen.copy(alpha = 0.1f) else CoralError.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, if (allOk) NeonGreen else CoralError),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("connection_diagnostics_card")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "6-Stage Connection Verification",
                    color = if (allOk) NeonGreen else CoralError,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = diag.outcome.summaryLabel,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            StageCheckRow("1. API Key Saved", diag.keySaved)
            StageCheckRow("2. Config Validated (${diag.provider}/${diag.model})", diag.configValidated)
            StageCheckRow("3. Network Reachable", diag.networkReachable)
            StageCheckRow("4. Auth Accepted (x-goog-api-key)", diag.authAccepted)
            StageCheckRow("5. Model Endpoint Reachable", diag.modelReachable)
            StageCheckRow("6. Live Generation Succeeded", diag.generationSucceeded)

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Endpoint: ${diag.endpoint}",
                color = TextMuted,
                fontSize = 11.sp
            )
            if (diag.sampleOutput.isNotBlank()) {
                Text(
                    text = "Diagnostic Output: ${diag.sampleOutput}",
                    color = if (allOk) accentColor else CoralError,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun StageCheckRow(label: String, passed: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 1.5.dp)
    ) {
        Icon(
            imageVector = if (passed) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (passed) NeonGreen else CoralError,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = if (passed) TextWhite else TextMuted,
            fontSize = 11.sp
        )
    }
}
