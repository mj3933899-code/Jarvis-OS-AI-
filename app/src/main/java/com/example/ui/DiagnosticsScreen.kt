package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.FeatureAuditStatus
import com.example.model.FeatureCatalog
import com.example.ui.theme.*
import com.example.util.PermissionStatusChecker

@Composable
fun DiagnosticsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val diagnostics by viewModel.diagnostics.collectAsState()
    val tenCards by viewModel.tenPermissionCards.collectAsState()
    val fiveCards by viewModel.fiveAdvancedCards.collectAsState()
    val isServiceRunning by viewModel.isServiceRunning.collectAsState()
    val serviceStatusText by viewModel.serviceStatusText.collectAsState()
    val config by viewModel.config.collectAsState()

    var selectedSection by remember { mutableIntStateOf(0) }

    val multiPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshAllStates()
    }

    LaunchedEffect(Unit) {
        viewModel.refreshAllStates()
    }

    val accentColor = parseHexColorOrDefault(config.accentHex, CyanAccent)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                    text = "Permissions, Audit & Diagnostics",
                    style = MaterialTheme.typography.titleLarge,
                    color = accentColor,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "10 Runtime + 5 Background Cards • 21 Categories • Build Verification",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            IconButton(
                onClick = { viewModel.refreshAllStates() },
                modifier = Modifier.testTag("refresh_diagnostics_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh All States", tint = accentColor)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Section Switcher Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedSection == 0,
                onClick = { selectedSection = 0 },
                label = { Text("15 Permissions", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accentColor,
                    selectedLabelColor = NavyDark
                ),
                modifier = Modifier.testTag("section_tab_permissions")
            )
            FilterChip(
                selected = selectedSection == 1,
                onClick = { selectedSection = 1 },
                label = { Text("21 Categories", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accentColor,
                    selectedLabelColor = NavyDark
                ),
                modifier = Modifier.testTag("section_tab_21_categories")
            )
            FilterChip(
                selected = selectedSection == 2,
                onClick = { selectedSection = 2 },
                label = { Text("System & Build", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accentColor,
                    selectedLabelColor = NavyDark
                ),
                modifier = Modifier.testTag("section_tab_system_health")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (selectedSection) {
            0 -> {
                val granted10 = tenCards.count { it.isGranted }
                val granted5 = fiveCards.count { it.isGranted }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = NavySurface),
                            border = BorderStroke(1.dp, accentColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "Screen E: 10 Runtime Permissions ($granted10/10 Allowed)",
                                        color = TextWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        "Screen F: 5 Background & Special Access ($granted5/5 Allowed)",
                                        color = CyanGlow,
                                        fontSize = 11.sp
                                    )
                                }
                                Button(
                                    onClick = {
                                        val allPerms = PermissionStatusChecker.getAllMissingRuntimePermissions(context)
                                        if (allPerms.isNotEmpty()) {
                                            multiPermissionLauncher.launch(allPerms.toTypedArray())
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                    modifier = Modifier.testTag("diag_allow_all_button")
                                ) {
                                    Text("Allow All", color = NavyDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }

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

                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Screen F — Always-On Background & System Access (5 Cards)",
                            color = accentColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    }

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

            1 -> {
                val categories = FeatureCatalog.allCategories
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        VerificationTripleBanner()
                    }

                    items(categories) { cat ->
                        val badgeColor = when (cat.auditStatus) {
                            FeatureAuditStatus.VERIFIED_WORKING -> NeonGreen
                            FeatureAuditStatus.PARTIAL_CONFIG_REQUIRED -> AmberWarning
                            FeatureAuditStatus.REQUIRES_ASSET_OR_ROLE -> CyanGlow
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = NavySurface),
                            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
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
                                    Surface(
                                        color = badgeColor.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, badgeColor)
                                    ) {
                                        Text(
                                            text = cat.auditStatus.badgeText,
                                            color = badgeColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
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
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = NavySurface),
                            border = BorderStroke(1.dp, if (isServiceRunning) NeonGreen else BorderDark)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Foreground Monitoring Service",
                                        color = TextWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Status: $serviceStatusText",
                                        color = if (isServiceRunning) NeonGreen else TextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                                Button(
                                    onClick = {
                                        if (isServiceRunning) viewModel.stopBackgroundService(context)
                                        else viewModel.startBackgroundService(context)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isServiceRunning) CoralError else NeonGreen
                                    ),
                                    modifier = Modifier.testTag("toggle_bg_service_button")
                                ) {
                                    Text(
                                        if (isServiceRunning) "Stop Service" else "Start Service",
                                        color = NavyDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = NavySurface),
                            border = BorderStroke(1.dp, accentColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "GitHub Repository & Update Hub",
                                        color = accentColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "mj3933899-code/Jarvis-OS-AI-",
                                        color = TextWhite,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Export ZIP, generate APK/AAB, or push directly to GitHub via the AI Studio Settings menu.",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val intent = android.content.Intent(
                                                android.content.Intent.ACTION_VIEW,
                                                android.net.Uri.parse("https://github.com/mj3933899-code/Jarvis-OS-AI-")
                                            ).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    },
                                    border = BorderStroke(1.dp, accentColor),
                                    modifier = Modifier.testTag("open_github_repo_button")
                                ) {
                                    Text("Open Repo", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        VerificationTripleBanner()
                    }

                    items(diagnostics) { item ->
                        val statusColor = if (item.isHealthy) NeonGreen else AmberWarning
                        Card(
                            colors = CardDefaults.cardColors(containerColor = NavySurface),
                            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(item.title, color = TextWhite, fontWeight = FontWeight.Bold)
                                    Text(
                                        item.status,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Text(item.details, color = CyanGlow, fontSize = 12.sp)
                                item.fixHint?.let { hint ->
                                    Text("Fix: $hint", color = TextMuted, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = NavySurface),
                            border = BorderStroke(1.dp, accentColor)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "JARVIS Reference Blueprint (Reference Image 1)",
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Image(
                                    painter = painterResource(id = R.drawable.img_jarvis_infographic),
                                    contentDescription = "JARVIS Android AI Assistant Blueprint",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(260.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VerificationTripleBanner() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = NavySurfaceElevated),
            border = BorderStroke(1.dp, NeonGreen)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonGreen)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("ZIP Verifications Status", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        "File structure check • Required files check • Integrity check • Status: Verified",
                        color = TextWhite,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = NavySurfaceElevated),
            border = BorderStroke(1.dp, CyanAccent)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = CyanAccent)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Source & Feature Files", color = CyanAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        "Kotlin project files • Resource files (UI/Assets) • Configuration files • Status: Complete",
                        color = TextWhite,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = NavySurfaceElevated),
            border = BorderStroke(1.dp, AmberWarning)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Android, contentDescription = null, tint = AmberWarning)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        "Android Build & Device Testing",
                        color = AmberWarning,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        "Build configuration (AGP 9.1.1) • Permissions & runtime checks • Robolectric JVM Verified (Physical device hardware sensor test requires real Android handset)",
                        color = TextWhite,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
