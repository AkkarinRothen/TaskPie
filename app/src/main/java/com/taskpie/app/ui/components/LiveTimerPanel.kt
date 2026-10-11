package com.taskpie.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskpie.app.service.TimerState
import com.taskpie.app.service.TimerStatus
import com.taskpie.app.ui.theme.*

@Composable
fun LiveTimerPanel(
    timerState: TimerState,
    onExpandFullScreen: () -> Unit,
    onStopSession: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipStage: () -> Unit,
    onAdvanceManual: () -> Unit,
    onToggleVoice: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(24.dp),
        color = LightSurface,
        tonalElevation = 3.dp,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (timerState.status == TimerStatus.PAUSED) AccentAmber else AccentEmerald
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = timerState.sessionTitle.ifBlank { "Sesión Activa" },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        if (timerState.parentTitle != null && timerState.parentTitle != timerState.sessionTitle) {
                            Text(
                                text = timerState.parentTitle!!,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = PrimaryBlue,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (onToggleVoice != null) {
                        FilledTonalIconButton(
                            onClick = onToggleVoice,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (timerState.isVoiceEnabled) PrimaryBlueLight else LightBorder.copy(alpha = 0.5f),
                                contentColor = if (timerState.isVoiceEnabled) PrimaryBlue else LightTextSecondary
                            )
                        ) {
                            Icon(
                                imageVector = if (timerState.isVoiceEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = if (timerState.isVoiceEnabled) "Silenciar voz" else "Activar voz"
                            )
                        }
                    }

                    // Fullscreen / Immersive mode button
                    FilledTonalIconButton(
                        onClick = onExpandFullScreen,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = PrimaryBlueLight,
                            contentColor = PrimaryBlue
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Modo Inmersivo (Pantalla Completa)"
                        )
                    }

                    // Stop session button
                    FilledTonalIconButton(
                        onClick = onStopSession,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = AccentCoralLight,
                            contentColor = AccentCoral
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Detener sesión"
                        )
                    }
                }
            }

            HorizontalDivider(color = LightBorder)

            // Scrollable Timer & Stage Body
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Routine Activity Indicator if routine
                if (timerState.routineSteps.size > 1) {
                    val currentAct = timerState.currentActivity
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AccentTealLight)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Actividad ${timerState.currentActivityIndex + 1}/${timerState.routineSteps.size}:",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = AccentTeal
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = currentAct?.activityTitle ?: "",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = LightTextPrimary
                                    )
                                )
                            }
                        }
                    }
                }

                // Waiting Manual Advance Alert
                AnimatedVisibility(visible = timerState.status == TimerStatus.WAITING_MANUAL_ADVANCE) {
                    val nextActIdx = timerState.currentActivityIndex + 1
                    val nextAct = timerState.routineSteps.getOrNull(nextActIdx)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AccentAmberLight)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "¡Actividad completada!",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AccentAmber
                                )
                            )
                            Text(
                                text = "Siguiente: ${nextAct?.activityTitle ?: ""}.",
                                style = MaterialTheme.typography.bodySmall.copy(color = LightTextPrimary),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Button(
                                onClick = onAdvanceManual,
                                colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Continuar siguiente", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Circular Timer
                CircularTimerView(
                    timerState = timerState,
                    size = 210.dp,
                    strokeWidth = 11.dp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Current Stage Objective / Notes
                val notes = timerState.currentStage?.notes ?: ""
                if (notes.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = LightSurfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Objetivo de la etapa",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = LightTextPrimary
                                    )
                                )
                                Text(
                                    text = notes,
                                    style = MaterialTheme.typography.bodySmall.copy(color = LightTextSecondary),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Progress Bar of Stages
                timerState.currentActivity?.stages?.let { stages ->
                    StageProgressBar(
                        stages = stages,
                        currentStageIndex = timerState.currentStageIndex,
                        timerState = timerState
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Bottom Action Controls
            Surface(
                color = LightSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = onSkipStage,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
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

                    Button(
                        onClick = onTogglePlayPause,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (timerState.status == TimerStatus.PAUSED) AccentEmerald else PrimaryBlue
                        ),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = if (timerState.status == TimerStatus.RUNNING) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
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
