package com.taskpie.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskpie.app.model.Stage
import com.taskpie.app.service.TimerState
import com.taskpie.app.ui.theme.*

@Composable
fun StageProgressBar(
    stages: List<Stage>,
    currentStageIndex: Int,
    timerState: TimerState,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Segmented bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            stages.forEachIndexed { index, stage ->
                val stageColor = try {
                    Color(android.graphics.Color.parseColor(stage.colorHex))
                } catch (e: Exception) {
                    PrimaryBlue
                }

                val weight = (stage.durationSeconds.toFloat() / 60f).coerceAtLeast(1f)
                val isCompleted = index < currentStageIndex
                val isCurrent = index == currentStageIndex

                Box(
                    modifier = Modifier
                        .weight(weight)
                        .fillMaxHeight()
                        .background(
                            when {
                                isCompleted -> AccentEmerald
                                isCurrent -> stageColor
                                else -> LightBorder
                            }
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stage List Stepper
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            stages.forEachIndexed { index, stage ->
                val isCompleted = index < currentStageIndex
                val isCurrent = index == currentStageIndex
                val isUpcoming = index > currentStageIndex

                val stageColor = try {
                    Color(android.graphics.Color.parseColor(stage.colorHex))
                } catch (e: Exception) {
                    PrimaryBlue
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isCurrent) PrimaryBlueLight else Color.Transparent
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Indicator Circle
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isCompleted -> AccentEmerald
                                    isCurrent -> stageColor
                                    else -> LightSurfaceVariant
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent) Color.White else LightTextSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Title and info
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stage.name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isUpcoming) LightTextSecondary else LightTextPrimary
                                )
                            )
                            if (stage.isOptional) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AccentAmberLight)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Opcional",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentAmber
                                        )
                                    )
                                }
                            }
                        }

                        if (stage.notes.isNotBlank() && isCurrent) {
                            Text(
                                text = stage.notes,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 12.sp,
                                    color = LightTextSecondary
                                ),
                                maxLines = 2,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    // Duration
                    Text(
                        text = if (isCurrent) timerState.formattedRemainingTime() else stage.formattedDuration(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrent) stageColor else LightTextSecondary
                        )
                    )
                }
            }
        }
    }
}
