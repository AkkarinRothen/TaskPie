package com.taskpie.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskpie.app.service.TimerState
import com.taskpie.app.service.TimerStatus
import com.taskpie.app.ui.theme.AccentAmber
import com.taskpie.app.ui.theme.AccentCoral
import com.taskpie.app.ui.theme.LightBorder
import com.taskpie.app.ui.theme.LightTextSecondary
import com.taskpie.app.ui.theme.PrimaryBlue

@Composable
fun CircularTimerView(
    timerState: TimerState,
    modifier: Modifier = Modifier,
    size: Dp = 260.dp,
    strokeWidth: Dp = 14.dp
) {
    val currentStage = timerState.currentStage
    val stageColorHex = currentStage?.colorHex ?: "#2563EB"
    val stageColor = when {
        timerState.isInCycleBreak -> AccentAmber
        timerState.status == TimerStatus.PAUSED -> AccentCoral
        else -> try {
            Color(android.graphics.Color.parseColor(stageColorHex))
        } catch (e: Exception) {
            PrimaryBlue
        }
    }

    val targetProgress = if (timerState.isInCycleBreak) {
        if (timerState.cycleBreakSeconds > 0) {
            1f - (timerState.cycleBreakRemainingSeconds.toFloat() / timerState.cycleBreakSeconds.toFloat()).coerceIn(0f, 1f)
        } else 0f
    } else {
        1f - timerState.currentStageProgress
    }

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 300),
        label = "timer_progress"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Canvas for Circular Progress
        Canvas(modifier = Modifier.fillMaxSize().padding(strokeWidth / 2)) {
            val canvasSize = this.size
            val arcSize = Size(canvasSize.width, canvasSize.height)
            val topLeft = Offset(0f, 0f)
            val strokePx = strokeWidth.toPx()

            // Background Track
            drawArc(
                color = LightBorder,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Progress Arc
            drawArc(
                color = stageColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        // Center Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            if (timerState.isInCycleBreak) {
                Text(
                    text = "☕ DESCANSO",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = AccentAmber,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                val mins = timerState.cycleBreakRemainingSeconds / 60
                val secs = timerState.cycleBreakRemainingSeconds % 60
                Text(
                    text = String.format("%02d:%02d", mins, secs),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentAmber
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Siguiente: Ciclo ${timerState.currentCycle + 1} de ${timerState.totalCycles}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = LightTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                )

                Text(
                    text = "Tómate un respiro antes de continuar",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = LightTextSecondary,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                if (timerState.status == TimerStatus.PAUSED) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pausado",
                            tint = AccentCoral,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PAUSADO",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = AccentCoral,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }

                // Big Countdown Time
                Text(
                    text = timerState.formattedRemainingTime(),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Stage Index Badge
                val totalStages = timerState.currentActivity?.stages?.size ?: 1
                val currentStageNum = timerState.currentStageIndex + 1
                Text(
                    text = "Etapa $currentStageNum de $totalStages",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = LightTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                )

                // Current Stage Name
                Text(
                    text = currentStage?.name ?: "",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
