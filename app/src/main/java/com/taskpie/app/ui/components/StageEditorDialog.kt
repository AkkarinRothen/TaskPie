package com.taskpie.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskpie.app.model.Stage
import com.taskpie.app.ui.theme.LightBorder
import com.taskpie.app.ui.theme.LightTextSecondary
import com.taskpie.app.ui.theme.StageColors

@Composable
fun StageEditorDialog(
    initialStage: Stage? = null,
    onDismiss: () -> Unit,
    onSave: (stage: Stage) -> Unit
) {
    var name by remember { mutableStateOf(initialStage?.name ?: "") }
    var minutesText by remember {
        mutableStateOf(
            if (initialStage != null) (initialStage.durationSeconds / 60).toString() else "5"
        )
    }
    var secondsText by remember {
        mutableStateOf(
            if (initialStage != null) (initialStage.durationSeconds % 60).toString() else "0"
        )
    }
    var notes by remember { mutableStateOf(initialStage?.notes ?: "") }
    var isOptional by remember { mutableStateOf(initialStage?.isOptional ?: false) }
    var selectedColor by remember { mutableStateOf(initialStage?.colorHex ?: StageColors.first()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = if (initialStage != null) "Editar Etapa" else "Nueva Etapa",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Name
                Column {
                    Text(
                        text = "Nombre de la etapa",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            errorMessage = null
                        },
                        placeholder = { Text("Ej: Borrador, Lluvia de ideas...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = LightBorder),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Duration (Minutes & Seconds)
                Column {
                    Text(
                        text = "Duración",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Minutes
                        OutlinedTextField(
                            value = minutesText,
                            onValueChange = { if (it.all { char -> char.isDigit() }) minutesText = it },
                            label = { Text("Minutos") },
                            placeholder = { Text("5") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = LightBorder),
                            modifier = Modifier.weight(1f)
                        )
                        // Seconds
                        OutlinedTextField(
                            value = secondsText,
                            onValueChange = { if (it.all { char -> char.isDigit() }) secondsText = it },
                            label = { Text("Segundos") },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = LightBorder),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Notes
                Column {
                    Text(
                        text = "Notas / Instrucciones (opcional)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = { Text("Ej: Escribir sin editar ni volver atrás...") },
                        minLines = 2,
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = LightBorder),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Optional Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Etapa Opcional",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp)
                        )
                        Text(
                            text = "Se puede omitir durante la sesión",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 12.sp,
                                color = LightTextSecondary
                            )
                        )
                    }
                    Switch(
                        checked = isOptional,
                        onCheckedChange = { isOptional = it }
                    )
                }

                // Color Selection
                Column {
                    Text(
                        text = "Color de la etapa",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(StageColors) { colorHex ->
                            val color = try {
                                Color(android.graphics.Color.parseColor(colorHex))
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.primary
                            }
                            val isSelected = selectedColor.equals(colorHex, ignoreCase = true)

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColor = colorHex }
                                    .then(
                                        if (isSelected) Modifier.border(2.dp, Color.Black, CircleShape)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Error Message
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Ingresa un nombre para la etapa"
                        return@Button
                    }
                    val mins = minutesText.toLongOrNull() ?: 0L
                    val secs = secondsText.toLongOrNull() ?: 0L
                    val totalSeconds = mins * 60 + secs

                    if (totalSeconds <= 0) {
                        errorMessage = "La duración debe ser mayor a 0 segundos"
                        return@Button
                    }

                    val stage = (initialStage ?: Stage(name = "", durationSeconds = 0)).copy(
                        name = name.trim(),
                        durationSeconds = totalSeconds,
                        notes = notes.trim(),
                        isOptional = isOptional,
                        colorHex = selectedColor
                    )
                    onSave(stage)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Guardar", fontWeight = FontWeight.SemiBold)
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
