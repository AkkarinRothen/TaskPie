package com.taskpie.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskpie.app.ui.theme.AccentAmber
import com.taskpie.app.ui.theme.LightBorder
import com.taskpie.app.ui.theme.LightTextSecondary
import com.taskpie.app.ui.theme.PrimaryBlue

@Composable
fun StartSessionDialog(
    defaultTitle: String,
    totalDurationFormatted: String,
    initialVoiceEnabled: Boolean = true,
    initialRepeatCount: Int = 1,
    initialCycleBreakSeconds: Long = 0L,
    onDismiss: () -> Unit,
    onStart: (sessionName: String, voiceEnabled: Boolean, repeatCount: Int, cycleBreakSeconds: Long) -> Unit
) {
    var sessionName by remember { mutableStateOf(defaultTitle) }
    var voiceEnabled by remember { mutableStateOf(initialVoiceEnabled) }
    var repeatCount by remember { mutableStateOf(initialRepeatCount.coerceAtLeast(1)) }
    var cycleBreakSeconds by remember { mutableStateOf(initialCycleBreakSeconds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Text(
                    text = "Iniciar Sesión",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = if (repeatCount > 1) {
                        "Duración por ciclo: $totalDurationFormatted • ${repeatCount} ciclos"
                    } else {
                        "Duración estimada: $totalDurationFormatted"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(color = LightTextSecondary),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Session Name
                Column {
                    Text(
                        text = "Nombre para esta sesión (opcional):",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = sessionName,
                        onValueChange = { sessionName = it },
                        placeholder = { Text("Ej: Capítulo 3, Tesis bloque 1...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = LightBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Repeat Cycles Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, LightBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Repeticiones del Ciclo",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Repetir secuencia completa",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = LightTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = { if (repeatCount > 1) repeatCount-- },
                                    enabled = repeatCount > 1,
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text("-", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }

                                Text(
                                    text = "x$repeatCount",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (repeatCount > 1) PrimaryBlue else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                )

                                OutlinedButton(
                                    onClick = { if (repeatCount < 99) repeatCount++ },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }

                        // Cycle Break Selector
                        if (repeatCount > 1) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = LightBorder.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Descanso entre ciclos:",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 11.sp,
                                    color = LightTextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(
                                    0L to "0m",
                                    60L to "1m",
                                    120L to "2m",
                                    180L to "3m",
                                    300L to "5m"
                                ).forEach { (secs, label) ->
                                    val isSelected = cycleBreakSeconds == secs
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { cycleBreakSeconds = secs },
                                        label = { Text(label, fontSize = 11.sp) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(30.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Voice Announcements Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, LightBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { voiceEnabled = !voiceEnabled }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (voiceEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = null,
                                tint = if (voiceEnabled) PrimaryBlue else LightTextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Avisos por voz",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Avisar hitos, ciclos y etapas",
                                    style = MaterialTheme.typography.bodySmall.copy(color = LightTextSecondary)
                                )
                            }
                        }
                        Switch(
                            checked = voiceEnabled,
                            onCheckedChange = { voiceEnabled = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onStart(sessionName, voiceEnabled, repeatCount, cycleBreakSeconds) },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Comenzar", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancelar", color = LightTextSecondary)
            }
        }
    )
}
