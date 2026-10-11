package com.taskpie.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskpie.app.service.TimerStatus
import com.taskpie.app.ui.components.CircularTimerView
import com.taskpie.app.ui.components.KeepScreenOn
import com.taskpie.app.ui.components.SessionSummaryDialog
import com.taskpie.app.ui.components.StageProgressBar
import com.taskpie.app.ui.theme.*
import com.taskpie.app.ui.viewmodel.TaskPieViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionScreen(
    viewModel: TaskPieViewModel,
    onNavigateBack: () -> Unit
) {
    val timerState by viewModel.timerState.collectAsState()
    var showStopConfirmation by remember { mutableStateOf(false) }

    var keepScreenOnEnabled by remember { mutableStateOf(true) }
    if (keepScreenOnEnabled && (timerState.status == TimerStatus.RUNNING || timerState.status == TimerStatus.PAUSED)) {
        KeepScreenOn(enabled = true)
    }

    // If session is completed, show summary dialog
    if (timerState.status == TimerStatus.COMPLETED && timerState.completedSummary != null) {
        SessionSummaryDialog(
            summary = timerState.completedSummary!!,
            onDismiss = {
                viewModel.stopSession()
                onNavigateBack()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = timerState.sessionTitle.ifBlank { "Sesión Activa" },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        if (timerState.parentTitle != null && timerState.parentTitle != timerState.sessionTitle) {
                            Text(
                                text = timerState.parentTitle!!,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 11.sp,
                                    color = PrimaryBlue
                                )
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleVoice() }) {
                        Icon(
                            imageVector = if (timerState.isVoiceEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (timerState.isVoiceEnabled) "Avisos por voz activos" else "Avisos por voz silenciados",
                            tint = if (timerState.isVoiceEnabled) PrimaryBlue else LightTextSecondary.copy(alpha = 0.4f)
                        )
                    }
                    IconButton(onClick = { keepScreenOnEnabled = !keepScreenOnEnabled }) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = if (keepScreenOnEnabled) "Pantalla encendida (Activo)" else "Pantalla encendida (Inactivo)",
                            tint = if (keepScreenOnEnabled) AccentAmber else LightTextSecondary.copy(alpha = 0.4f)
                        )
                    }
                    IconButton(onClick = { showStopConfirmation = true }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Detener sesión",
                            tint = AccentCoral
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightBackground)
            )
        },
        containerColor = LightBackground
    ) { paddingValues ->
        if (timerState.status == TimerStatus.IDLE) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No hay ninguna sesión activa en este momento",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onNavigateBack,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Ir al Inicio")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Current Activity Indicator (in Routine)
                    if (timerState.routineSteps.size > 1) {
                        val currentAct = timerState.currentActivity
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = AccentTealLight)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Actividad ${timerState.currentActivityIndex + 1}/${timerState.routineSteps.size}:",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = AccentTeal
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = currentAct?.activityTitle ?: "",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = LightTextPrimary
                                        )
                                    )
                                }

                                Text(
                                    text = "${currentAct?.stages?.size ?: 0} etapas",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 11.sp,
                                        color = LightTextSecondary
                                    )
                                )
                            }
                        }
                    }

                    // Waiting Manual Advance Alert Banner
                    AnimatedVisibility(visible = timerState.status == TimerStatus.WAITING_MANUAL_ADVANCE) {
                        val nextActIdx = timerState.currentActivityIndex + 1
                        val nextAct = timerState.routineSteps.getOrNull(nextActIdx)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = AccentAmberLight)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "¡Actividad completada!",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = AccentAmber
                                    )
                                )
                                Text(
                                    text = "Siguiente actividad: ${nextAct?.activityTitle ?: ""}. Presiona 'Iniciar Siguiente' cuando estés listo.",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 12.sp,
                                        color = LightTextPrimary
                                    ),
                                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                                )
                                Button(
                                    onClick = { viewModel.advanceManualNextActivity() },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Iniciar Siguiente Actividad", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Big Circular Timer
                    CircularTimerView(
                        timerState = timerState,
                        size = 250.dp,
                        strokeWidth = 12.dp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Notes / Tips Card (if any)
                    val notes = timerState.currentStage?.notes ?: ""
                    if (notes.isNotBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = LightSurface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Objetivo de la etapa",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = LightTextPrimary
                                        )
                                    )
                                    Text(
                                        text = notes,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = LightTextSecondary,
                                            fontSize = 13.sp
                                        ),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Stepper / Progress of Stages
                    timerState.currentActivity?.stages?.let { stages ->
                        StageProgressBar(
                            stages = stages,
                            currentStageIndex = timerState.currentStageIndex,
                            timerState = timerState
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Bottom Control Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = LightSurface,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Skip Stage Action
                        OutlinedButton(
                            onClick = { viewModel.skipCurrentStage() },
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (timerState.currentStage?.isOptional == true) "Omitir" else "Saltar",
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Play / Pause Main Action
                        Button(
                            onClick = {
                                if (timerState.status == TimerStatus.RUNNING) {
                                    viewModel.pauseTimer()
                                } else if (timerState.status == TimerStatus.PAUSED) {
                                    viewModel.resumeTimer()
                                } else if (timerState.status == TimerStatus.WAITING_MANUAL_ADVANCE) {
                                    viewModel.advanceManualNextActivity()
                                }
                            },
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (timerState.status == TimerStatus.PAUSED) AccentEmerald else PrimaryBlue
                            ),
                            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)
                        ) {
                            Icon(
                                imageVector = if (timerState.status == TimerStatus.RUNNING) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (timerState.status) {
                                    TimerStatus.RUNNING -> "Pausar"
                                    TimerStatus.PAUSED -> "Reanudar"
                                    TimerStatus.WAITING_MANUAL_ADVANCE -> "Continuar"
                                    else -> "Iniciar"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }

    // Stop Confirmation Dialog
    if (showStopConfirmation) {
        AlertDialog(
            onDismissRequest = { showStopConfirmation = false },
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("¿Detener sesión?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Si detienes la sesión ahora, el temporizador se cancelará y volverás a la pantalla principal.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStopConfirmation = false
                        viewModel.stopSession()
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCoral),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Detener", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showStopConfirmation = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Continuar sesión", color = LightTextSecondary)
                }
            }
        )
    }
}
