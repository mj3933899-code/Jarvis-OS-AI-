package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActionReceipt
import com.example.model.TaskPlanStep
import com.example.model.TaskStatus
import com.example.model.VerificationStatus
import com.example.model.WorkTask
import com.example.ui.theme.*

@Composable
fun WorkScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.tasks.collectAsState()
    val activeTasks by viewModel.activeTasks.collectAsState()
    val receipts by viewModel.receipts.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var taskNameInput by remember { mutableStateOf("") }
    var taskDescInput by remember { mutableStateOf("") }
    var targetAppInput by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NavyDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = CyanAccent,
                contentColor = NavyDark,
                modifier = Modifier.testTag("create_task_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Task")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                        Text("Work Tasks & Automation", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Autonomous plan execution & receipts", color = TextMuted, fontSize = 13.sp)
                    }
                    Badge(containerColor = if (activeTasks.isNotEmpty()) NeonGreen else NavySurfaceElevated) {
                        Text("${activeTasks.size} Active", color = NavyDark, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Active or Recent Tasks Section
            if (tasks.isEmpty() && activeTasks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = NavySurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Engineering, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No Active Tasks", color = TextWhite, fontWeight = FontWeight.SemiBold)
                            Text("Tap the '+' button or ask JARVIS to create a work plan", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                // Combine tasks from active state and repository
                val allDisplayTasks = (activeTasks.values + tasks).distinctBy { it.id }
                items(allDisplayTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onPause = { viewModel.pauseTask(task.id) },
                        onResume = { viewModel.resumeTask(task.id) },
                        onCancel = { viewModel.cancelTask(task.id) }
                    )
                }
            }

            // Receipts Log Section
            if (receipts.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Action Receipts & Verifications", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                items(receipts.take(10), key = { it.id }) { receipt ->
                    ReceiptCard(receipt = receipt)
                }
            }
        }

        // Create Task Dialog
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                containerColor = NavySurface,
                title = { Text("New Work Task", color = TextWhite, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = taskNameInput,
                            onValueChange = { taskNameInput = it },
                            label = { Text("Task Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = taskDescInput,
                            onValueChange = { taskDescInput = it },
                            label = { Text("Objective / Description") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = targetAppInput,
                            onValueChange = { targetAppInput = it },
                            label = { Text("Target App or URL (Optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (taskNameInput.isNotBlank()) {
                                viewModel.createWorkTask(
                                    name = taskNameInput,
                                    description = taskDescInput,
                                    targetApp = targetAppInput.takeIf { it.isNotBlank() }
                                )
                                taskNameInput = ""
                                taskDescInput = ""
                                targetAppInput = ""
                                showCreateDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = NavyDark)
                    ) {
                        Text("Launch Task")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("Cancel", color = TextMuted)
                    }
                }
            )
        }
    }
}

@Composable
private fun TaskCard(
    task: WorkTask,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val statusColor = when (task.status) {
        TaskStatus.RUNNING -> NeonGreen
        TaskStatus.PAUSED -> AmberWarning
        TaskStatus.COMPLETED -> CyanAccent
        TaskStatus.FAILED, TaskStatus.CANCELLED -> CoralError
        else -> TextMuted
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(task.name, color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(task.currentStep, color = CyanGlow, fontSize = 12.sp)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(task.status.name, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (task.progress / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = CyanAccent,
                trackColor = NavySurfaceElevated
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${task.planSteps.count { it.isCompleted }}/${task.planSteps.size} steps done",
                    color = TextMuted,
                    fontSize = 11.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (task.status == TaskStatus.RUNNING) {
                        IconButton(onClick = onPause, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause", tint = AmberWarning, modifier = Modifier.size(18.dp))
                        }
                    } else if (task.status == TaskStatus.PAUSED) {
                        IconButton(onClick = onResume, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = NeonGreen, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (task.status == TaskStatus.RUNNING || task.status == TaskStatus.PAUSED) {
                        IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = CoralError, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Step Details Toggle
            if (task.planSteps.isNotEmpty()) {
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(if (expanded) "Hide Steps" else "View Steps", color = CyanAccent, fontSize = 11.sp)
                }

                AnimatedVisibility(visible = expanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        task.planSteps.forEachIndexed { idx, step ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (step.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (step.isCompleted) NeonGreen else TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${idx + 1}. ${step.title}",
                                    color = if (step.isCompleted) TextMuted else TextWhite,
                                    fontSize = 12.sp
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
private fun ReceiptCard(receipt: ActionReceipt) {
    val verifColor = when (receipt.verificationStatus) {
        VerificationStatus.CONFIRMED -> NeonGreen
        VerificationStatus.LIKELY -> CyanAccent
        VerificationStatus.UNCERTAIN -> AmberWarning
        VerificationStatus.FAILED, VerificationStatus.NOT_AVAILABLE -> CoralError
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NavySurfaceElevated),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(receipt.command, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text("Tool: ${receipt.tool} • ${receipt.executionResult}", color = TextMuted, fontSize = 11.sp)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(verifColor.copy(alpha = 0.2f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(receipt.verificationStatus.name, color = verifColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
