package com.taskpie.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskpie.app.service.TimerState
import com.taskpie.app.service.TimerStatus
import com.taskpie.app.ui.theme.*

@Composable
fun PipTimerView(
    timerState: TimerState,
    modifier: Modifier = Modifier
) {
    val stageColor = timerState.currentStage?.let { stage ->
        runCatching {
            Color(android.graphics.Color.parseColor(stage.colorHex))
        }.getOrDefault(PrimaryBlue)
    } ?: PrimaryBlue

    val isPaused = timerState.status == TimerStatus.PAUSED
    val isWaiting = timerState.status == TimerStatus.WAITING_MANUAL_ADVANCE

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LightBackground)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Stage Name with Color Dot & Progress tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(stageColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                val stageTitle = timerState.currentStage?.name ?: "TaskPie"
                val totalStages = timerState.currentActivity?.stages?.size ?: 1
                val currentStep = timerState.routineSteps.getOrNull(timerState.currentActivityIndex)
                val roundText = if (currentStep != null && currentStep.totalIterations > 1) {
                    "[R${currentStep.iterationIndex + 1}/${currentStep.totalIterations}] "
                } else ""
                val stageIndexText = if (totalStages > 1) {
                    "${timerState.currentStageIndex + 1}/$totalStages • "
                } else ""

                Text(
                    text = "$roundText$stageIndexText$stageTitle",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = LightTextPrimary,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Large Digital Countdown (MM:SS)
            Text(
                text = timerState.formattedRemainingTime(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    color = when {
                        isPaused -> AccentAmber
                        isWaiting -> AccentEmerald
                        else -> PrimaryBlueHover
                    }
                ),
                maxLines = 1
            )

            // Status label if Paused or Waiting
            if (isPaused || isWaiting) {
                Spacer(modifier = Modifier.height(1.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isPaused) AccentAmberLight else AccentEmeraldLight)
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = if (isPaused) "PAUSA" else "SIGUIENTE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isPaused) AccentAmber else AccentEmerald,
                            fontSize = 9.sp
                        )
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                // Progress Bar of Current Stage
                LinearProgressIndicator(
                    progress = { timerState.currentStageProgress },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = stageColor,
                    trackColor = LightBorder
                )
            }
        }
    }
}
