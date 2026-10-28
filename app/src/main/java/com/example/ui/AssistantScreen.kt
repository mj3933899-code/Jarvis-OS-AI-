package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ai.InterruptionType
import com.example.model.AssistantState
import com.example.model.FeatureCatalog
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToPermissions: () -> Unit = {},
    onNavigateToDiagnostics: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val assistantState by viewModel.assistantState.collectAsState()
    val isFullScreen by viewModel.isFullScreenCharacter.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val config by viewModel.config.collectAsState()
    val rmsDb by viewModel.rmsDb.collectAsState()
    val isListening by viewModel.isListeningState.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val voiceError by viewModel.voiceError.collectAsState()
    val wakeWordMode by viewModel.wakeWordModeStatus.collectAsState()
    val interruption by viewModel.interruption.collectAsState()
    val isServiceRunning by viewModel.isServiceRunning.collectAsState()
    val activeTasks by viewModel.activeTasks.collectAsState()
    val weather by viewModel.weatherState.collectAsState()
    val lastCommand by viewModel.lastCommandSummary.collectAsState()
    val lastTool by viewModel.lastToolSummary.collectAsState()
    val lastVerification by viewModel.lastVerificationSummary.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showFeaturesHub by remember { mutableStateOf(false) }
    var showPoseReferenceSheet by remember { mutableStateOf(false) }

    val effectiveFullScreen = isFullScreen || config.themeMode == "3D_MODEL_FULL"

    // System Back button exits Full Screen Character View back to Normal View on the same page
    BackHandler(enabled = isFullScreen) {
        viewModel.setFullScreenCharacter(false)
    }

    val micPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshPermissionCards()
        if (granted) {
            viewModel.startListening()
        }
    }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.processUserQuery("এই ছবিটি বিশ্লেষণ করো: $uri")
        }
    }

    val quickCommands = listOf(
        "জার্ভিস অন",
        "আজকের আবহাওয়া কেমন?",
        "স্ক্রিনে কী লেখা আছে?",
        "ইউটিউব খোলো",
        "ভলিউম বাড়াও",
        "ব্যাটারি কত শতাংশ?"
    )

    val bgColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val accentColor = parseHexColorOrDefault(config.accentHex, CyanAccent)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        if (effectiveFullScreen) {
            // =========================================================================
            // SCREEN B — FULL SCREEN CHARACTER VIEW (SAME PAGE, NO EXTRA ACTIVITY)
            // =========================================================================
            FullScreenCharacterSamePage(
                assistantState = assistantState,
                isListening = isListening,
                isSpeaking = isSpeaking,
                rmsDb = rmsDb,
                characterScale = config.characterScale,
                animationIntensity = config.animationIntensity,
                accentColor = accentColor,
                language = config.language,
                lastMessage = messages.lastOrNull()?.content
                    ?: if (config.language == "bn") "হ্যালো! আমি জার্ভিস। আজ আপনাকে কীভাবে সাহায্য করতে পারি?"
                    else "Hello! I'm JARVIS. How can I help you today?",
                onCloseFullScreen = {
                    viewModel.setFullScreenCharacter(false)
                    if (config.themeMode == "3D_MODEL_FULL") {
                        viewModel.updateConfig(config.copy(themeMode = "3D_MODEL"))
                    }
                },
                onMicClick = {
                    val hasMic = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!hasMic) {
                        micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else if (isListening) {
                        viewModel.stopListening()
                    } else {
                        viewModel.startListening()
                    }
                },
                onSettingsClick = onNavigateToSettings,
                onSendQuery = { q -> viewModel.processUserQuery(q) }
            )
        } else {
            // =========================================================================
            // SCREEN A — HOME PAGE (NORMAL VIEW)
            // =========================================================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                // 1. Top Header Bar matching Reference Image 1
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showFeaturesHub = true },
                            modifier = Modifier.testTag("btn_21_features_drawer")
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Open 21 Feature Categories",
                                tint = accentColor
                            )
                        }
                        Column {
                            Text(
                                text = "JARVIS",
                                style = MaterialTheme.typography.titleLarge,
                                color = accentColor,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = "AI Assistant • ${config.modelName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Explicit JARVIS ON / OFF Power Toggle Pill
                        val isJarvisOn = assistantState != AssistantState.OFF
                        Surface(
                            color = if (isJarvisOn) NeonGreen.copy(alpha = 0.16f) else CoralError.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, if (isJarvisOn) NeonGreen else CoralError),
                            modifier = Modifier
                                .clickable { viewModel.toggleJarvisPower(context) }
                                .testTag("jarvis_power_toggle_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.PowerSettingsNew,
                                    contentDescription = "JARVIS Power Toggle",
                                    tint = if (isJarvisOn) NeonGreen else CoralError,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isJarvisOn) "ON" else "OFF",
                                    color = if (isJarvisOn) NeonGreen else CoralError,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Emergency Stop
                        IconButton(
                            onClick = { viewModel.emergencyStop() },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("emergency_stop_button")
                        ) {
                            Icon(
                                Icons.Default.StopCircle,
                                contentDescription = "Emergency Stop",
                                tint = CoralError
                            )
                        }

                        // Settings Shortcut
                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("header_settings_button")
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = accentColor
                            )
                        }
                    }
                }

                // 2. Live Status Strip (Service, Mic, Wake Word, Active Task)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    item {
                        StatusChip(
                            label = "State: ${assistantState.name}",
                            color = when (assistantState) {
                                AssistantState.OFF, AssistantState.ERROR -> CoralError
                                AssistantState.LISTENING, AssistantState.SPEAKING -> accentColor
                                else -> NeonGreen
                            }
                        )
                    }
                    item {
                        StatusChip(
                            label = if (isServiceRunning) "BG Service: Active" else "BG Service: Idle",
                            color = if (isServiceRunning) NeonGreen else TextMuted,
                            onClick = {
                                if (isServiceRunning) viewModel.stopBackgroundService(context)
                                else viewModel.startBackgroundService(context)
                            }
                        )
                    }
                    item {
                        StatusChip(
                            label = if (isListening) "Mic: Listening" else "Mic: Tap-to-Talk",
                            color = if (isListening) accentColor else TextMuted
                        )
                    }
                    item {
                        StatusChip(
                            label = "21 Categories Hub",
                            color = accentColor,
                            onClick = { showFeaturesHub = true }
                        )
                    }
                    if (activeTasks.isNotEmpty()) {
                        item {
                            StatusChip(
                                label = "Tasks: ${activeTasks.size} Running",
                                color = AmberWarning
                            )
                        }
                    }
                }

                // Interruption or Voice Error Banner
                AnimatedVisibility(visible = interruption != InterruptionType.NONE || voiceError != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AmberWarning.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, AmberWarning),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = AmberWarning)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = voiceError ?: "Task interrupted: ${interruption.name}",
                                color = AmberWarning,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 3. Scrollable Main Content (Character Card + Greeting + Weather + Chat History)
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Character Showcase Card (Reference Image 1 & 2)
                    item {
                        CharacterHeroCard(
                            assistantState = assistantState,
                            isListening = isListening,
                            isSpeaking = isSpeaking,
                            rmsDb = rmsDb,
                            showCharacterAvatar = config.showCharacterAvatar && config.themeMode != "LIGHT",
                            characterScale = config.characterScale,
                            animationIntensity = config.animationIntensity,
                            accentColor = accentColor,
                            onExpandFullScreen = { viewModel.setFullScreenCharacter(true) },
                            onOpenPoseGuide = { showPoseReferenceSheet = true }
                        )
                    }

                    // Greeting Card ("Hello! I'm JARVIS / How can I help you?")
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = surfaceColor),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("jarvis_greeting_card")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = if (config.language == "bn")
                                        "হ্যালো! আমি JARVIS — আপনার ব্যক্তিগত এআই সহকারী"
                                    else
                                        "Hello! I'm JARVIS",
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (config.language == "bn")
                                        "আমি আপনাকে কীভাবে সাহায্য করতে পারি? (ভয়েস বা টেক্সটে কমান্ড দিন)"
                                    else
                                        "How can I help you?",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Last Action: $lastCommand • Status: $lastVerification",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Weather & Location Card matching Reference Image 1 ("28°C Dhaka Clear Sky H:32° L:22°")
                    item {
                        WeatherLocationCard(
                            weather = weather,
                            accentColor = accentColor,
                            surfaceColor = surfaceColor,
                            onRefresh = { viewModel.refreshWeather() }
                        )
                    }

                    // Quick Voice / Action Chips
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(quickCommands) { cmd ->
                                SuggestionChip(
                                    onClick = { viewModel.processUserQuery(cmd) },
                                    label = { Text(cmd, fontSize = 12.sp) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = NavySurfaceElevated,
                                        labelColor = TextWhite
                                    ),
                                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
                                )
                            }
                        }
                    }

                    // Conversation Messages
                    items(messages.takeLast(16)) { msg ->
                        ChatBubbleItem(msg = msg, accentColor = accentColor)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 4. Bottom Voice Mic Orb + Chat Input Row (matching Reference Image 1)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            mediaPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(NavySurfaceElevated)
                            .testTag("attach_image_button")
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "Attach Image",
                            tint = accentColor
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                if (config.language == "bn") "বাংলায় বা ইংরেজিতে বলুন বা লিখুন..."
                                else "Ask JARVIS anything...",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("query_input_field"),
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = surfaceColor,
                            unfocusedContainerColor = surfaceColor
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Glowing Center/Action Mic Button
                    IconButton(
                        onClick = {
                            val hasMic = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!hasMic) {
                                micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else if (isListening) {
                                viewModel.stopListening()
                            } else {
                                viewModel.startListening()
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isListening) CoralError else accentColor)
                            .testTag("mic_listen_button")
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = NavyDark
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.processUserQuery(inputText)
                                inputText = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(ElectricBlue)
                            .testTag("send_query_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Message",
                            tint = TextWhite
                        )
                    }
                }
            }
        }
    }

    // 21 Feature Categories Modal Sheet
    if (showFeaturesHub) {
        ModalBottomSheet(
            onDismissRequest = { showFeaturesHub = false },
            containerColor = NavySurface,
            contentColor = TextWhite
        ) {
            FeatureCatalogSheetContent(
                accentColor = accentColor,
                onSelectCategoryAction = { catId ->
                    showFeaturesHub = false
                    when (catId) {
                        1 -> viewModel.setFullScreenCharacter(true)
                        2, 6 -> onNavigateToSettings()
                        14, 16 -> onNavigateToPermissions()
                        21 -> onNavigateToDiagnostics()
                        15 -> viewModel.refreshWeather()
                    }
                }
            )
        }
    }

    // Character Expressions & Pose Guide Modal (Reference Image 2 & 3)
    if (showPoseReferenceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPoseReferenceSheet = false },
            containerColor = NavySurface,
            contentColor = TextWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "JARVIS Character & Pose Guide",
                    style = MaterialTheme.typography.titleMedium,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Honest Rendering Audit: Uses high-resolution 2D Character Art (R.drawable.img_jarvis_avatar) with real-time Compose Canvas holographic energy rings and audio waveform. True rigged 3D mesh (.glb/.gltf) requires bundling a 3D model asset in assets/.",
                    color = TextMuted,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Image(
                    painter = painterResource(id = R.drawable.img_jarvis_poses),
                    contentDescription = "Character Expressions and Hand Poses Reference",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, accentColor, RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun CharacterHeroCard(
    assistantState: AssistantState,
    isListening: Boolean,
    isSpeaking: Boolean,
    rmsDb: Float,
    showCharacterAvatar: Boolean,
    characterScale: Float,
    animationIntensity: Float,
    accentColor: Color,
    onExpandFullScreen: () -> Unit,
    onOpenPoseGuide: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero_anim")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val ringAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_angle"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(275.dp)
            .testTag("character_hero_card")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Animated Neon Holographic Ring Background
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = size.minDimension * 0.42f * (1f + (rmsDb.coerceIn(0f, 10f) / 40f) * animationIntensity)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accentColor.copy(alpha = 0.28f), Color.Transparent),
                        center = center,
                        radius = baseRadius * 1.4f
                    ),
                    radius = baseRadius * 1.4f,
                    center = center
                )
                drawCircle(
                    color = accentColor.copy(alpha = 0.45f),
                    radius = baseRadius,
                    center = center,
                    style = Stroke(width = 3f)
                )
                val rad = Math.toRadians(ringAngle.toDouble())
                val orbX = center.x + (baseRadius * cos(rad)).toFloat()
                val orbY = center.y + (baseRadius * sin(rad)).toFloat()
                drawCircle(
                    color = accentColor,
                    radius = 6f,
                    center = Offset(orbX, orbY)
                )
            }

            if (showCharacterAvatar) {
                Image(
                    painter = painterResource(id = R.drawable.img_jarvis_avatar),
                    contentDescription = "JARVIS Character Avatar",
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.BottomCenter)
                        .scale(characterScale.coerceIn(0.75f, 1.35f) * (if (isSpeaking || isListening) pulse else 1f)),
                    contentScale = ContentScale.Fit
                )
            }

            // Top-Left Character Mode Badge
            Surface(
                color = NavyDark.copy(alpha = 0.75f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clickable { onOpenPoseGuide() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showCharacterAvatar) "Character View • Pose Guide" else "Neural Orb View",
                        color = TextWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Top-Right Full Screen Expand Button (Same Page, No Extra Activity)
            Surface(
                color = NavyDark.copy(alpha = 0.8f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, accentColor),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clickable { onExpandFullScreen() }
                    .testTag("btn_expand_fullscreen_character")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Fullscreen,
                        contentDescription = "Full Screen 3D View (Same Page)",
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Full Screen",
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Bottom Audio Waveform Bar when Listening or Speaking
            if (isListening || isSpeaking) {
                AudioWaveformOverlay(
                    rmsDb = rmsDb,
                    accentColor = accentColor,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(36.dp)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun FullScreenCharacterSamePage(
    assistantState: AssistantState,
    isListening: Boolean,
    isSpeaking: Boolean,
    rmsDb: Float,
    characterScale: Float,
    animationIntensity: Float,
    accentColor: Color,
    language: String,
    lastMessage: String,
    onCloseFullScreen: () -> Unit,
    onMicClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSendQuery: (String) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fullscreen_anim")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDark)
            .testTag("fullscreen_character_container")
    ) {
        // Radial Holographic Backdrop
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.42f)
            val radius = size.minDimension * 0.55f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentColor.copy(alpha = 0.32f), Color.Transparent),
                    center = center,
                    radius = radius * 1.3f
                ),
                radius = radius * 1.3f,
                center = center
            )
            drawCircle(
                color = accentColor.copy(alpha = 0.45f),
                radius = radius * 0.85f,
                center = center,
                style = Stroke(width = 3.5f)
            )
        }

        // Full-Screen Character Artwork (Reference Image 2)
        Image(
            painter = painterResource(id = R.drawable.img_jarvis_avatar),
            contentDescription = "JARVIS Full Screen Character View",
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 36.dp, bottom = 120.dp)
                .scale(characterScale.coerceIn(0.8f, 1.4f) * breathScale),
            contentScale = ContentScale.Fit
        )

        // Top Bar with Close (X) Button as shown in Reference Image 1 ("Full Screen 3D View (Same Page)")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = NavySurface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isListening || isSpeaking) accentColor else NeonGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Full Screen View (Same Page) • ${assistantState.name}",
                        color = TextWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Close Button (X) matching Reference Image 1 callout
            IconButton(
                onClick = onCloseFullScreen,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(NavySurface.copy(alpha = 0.9f))
                    .border(1.5.dp, accentColor, CircleShape)
                    .testTag("btn_close_fullscreen_character")
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close Full Screen View",
                    tint = accentColor
                )
            }
        }

        // Bottom Floating Glass Card ("JARVIS — How can I help you today?" + Waveform + Controls)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface.copy(alpha = 0.92f)),
                border = BorderStroke(1.5.dp, accentColor),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "JARVIS",
                            color = accentColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (isListening) "Listening..." else if (isSpeaking) "Speaking..." else "Online",
                            color = NeonGreen,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = lastMessage,
                        color = TextWhite,
                        fontSize = 14.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    AudioWaveformOverlay(
                        rmsDb = if (isListening || isSpeaking) rmsDb + 4f else 1.5f,
                        accentColor = accentColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onCloseFullScreen,
                            border = BorderStroke(1.dp, accentColor),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor)
                        ) {
                            Icon(Icons.Default.FullscreenExit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Normal View", fontSize = 12.sp)
                        }

                        FloatingActionButton(
                            onClick = onMicClick,
                            containerColor = if (isListening) CoralError else accentColor,
                            contentColor = NavyDark,
                            shape = CircleShape,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(
                                if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Toggle Voice Mic"
                            )
                        }

                        OutlinedButton(
                            onClick = onSettingsClick,
                            border = BorderStroke(1.dp, BorderDark),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite)
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Theme", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherLocationCard(
    weather: com.example.model.WeatherState,
    accentColor: Color,
    surfaceColor: Color,
    onRefresh: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weather_location_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.WbSunny,
                    contentDescription = "Weather Icon",
                    tint = AmberWarning,
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "${weather.tempCelsius}°C",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = weather.city,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh Weather",
                            tint = accentColor,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Text(
                    text = weather.condition,
                    color = accentColor,
                    fontSize = 12.sp
                )
                Text(
                    text = "H:${weather.highCelsius}°  L:${weather.lowCelsius}° • ${weather.lastUpdated}",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun AudioWaveformOverlay(
    rmsDb: Float,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Canvas(modifier = modifier) {
        val barCount = 32
        val spacing = size.width / barCount
        val centerY = size.height / 2f
        val amp = (rmsDb.coerceIn(0.5f, 12f) / 12f) * (size.height * 0.45f)
        for (i in 0 until barCount) {
            val factor = sin(phase + i * 0.45f)
            val barH = (6f + kotlin.math.abs(factor) * amp).coerceAtMost(size.height)
            val x = i * spacing + spacing / 2f
            drawLine(
                color = accentColor,
                start = Offset(x, centerY - barH / 2f),
                end = Offset(x, centerY + barH / 2f),
                strokeWidth = 4f
            )
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    color: Color,
    onClick: (() -> Unit)? = null
) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ChatBubbleItem(
    msg: com.example.data.entity.MessageEntity,
    accentColor: Color
) {
    val isUser = msg.sender == "USER"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) ElectricBlue.copy(alpha = 0.25f) else NavySurfaceElevated
            ),
            border = BorderStroke(
                1.dp,
                if (isUser) ElectricBlue else accentColor.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isUser) 14.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 14.dp
            ),
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = if (isUser) "You" else "JARVIS",
                    color = if (isUser) CyanGlow else accentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = msg.content,
                    color = TextWhite,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun FeatureCatalogSheetContent(
    accentColor: Color,
    onSelectCategoryAction: (Int) -> Unit
) {
    val categories = FeatureCatalog.allCategories
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "JARVIS — 21 Feature Categories Architecture",
            style = MaterialTheme.typography.titleMedium,
            color = accentColor,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Normalized 1–21 from Reference Image 1 with honest status badges",
            color = TextMuted,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.heightIn(max = 460.dp)
        ) {
            items(categories) { cat ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = NavySurfaceElevated),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectCategoryAction(cat.number) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = cat.titleEn,
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = cat.auditStatus.badgeText,
                                color = when (cat.auditStatus) {
                                    com.example.model.FeatureAuditStatus.VERIFIED_WORKING -> NeonGreen
                                    com.example.model.FeatureAuditStatus.PARTIAL_CONFIG_REQUIRED -> AmberWarning
                                    com.example.model.FeatureAuditStatus.REQUIRES_ASSET_OR_ROLE -> CyanGlow
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = cat.bullets.joinToString(" • "),
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = cat.honestStatusNote,
                            color = accentColor,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
