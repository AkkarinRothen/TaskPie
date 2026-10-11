package com.taskpie.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskpie.app.ui.theme.LightBorder
import com.taskpie.app.ui.theme.LightTextSecondary
import com.taskpie.app.ui.theme.PrimaryBlue

@Composable
fun StartSessionDialog(
    defaultTitle: String,
    totalDurationFormatted: String,
    initialVoiceEnabled: Boolean = true,
    onDismiss: () -> Unit,
    onStart: (sessionName: String, voiceEnabled: Boolean) -> Unit
) {
    var sessionName by remember { mutableStateOf(defaultTitle) }
    var voiceEnabled by remember { mutableStateOf(initialVoiceEnabled) }

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
                    text = "Duración estimada: $totalDurationFormatted",
                    style = MaterialTheme.typography.bodyMedium.copy(color = LightTextSecondary),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Nombre para esta sesión (opcional):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier.padding(bottom = 8.dp)
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

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
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
                                    text = "Avisar hitos y etapas",
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
                onClick = { onStart(sessionName, voiceEnabled) },
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

