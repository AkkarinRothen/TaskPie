package com.taskpie.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskpie.app.model.FolderItem
import com.taskpie.app.ui.theme.LightBorder
import com.taskpie.app.ui.theme.LightTextSecondary
import java.util.UUID

val FOLDER_PALETTE = listOf(
    "#3B82F6", // Azul
    "#10B981", // Esmeralda
    "#F97316", // Coral
    "#6366F1", // Índigo
    "#8B5CF6", // Violeta
    "#06B6D4", // Turquesa
    "#EC4899", // Rosa
    "#F59E0B"  // Ámbar
)

@Composable
fun FolderDialog(
    initialFolder: FolderItem? = null,
    onDismiss: () -> Unit,
    onSave: (FolderItem) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialFolder?.name ?: "") }
    var selectedColor by remember { mutableStateOf(initialFolder?.colorHex ?: FOLDER_PALETTE.first()) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = if (initialFolder == null) "Nueva Carpeta" else "Editar Carpeta",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Nombre
                Column {
                    Text(
                        text = "Nombre de la carpeta",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (it.isNotBlank()) isError = false
                        },
                        placeholder = { Text("Ej: Trabajo, Tesis, Fitness...") },
                        singleLine = true,
                        isError = isError,
                        supportingText = if (isError) {
                            { Text("El nombre no puede estar vacío") }
                        } else null,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = LightBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Selector de color
                Column {
                    Text(
                        text = "Color identificador",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(FOLDER_PALETTE) { hex ->
                            val color = runCatching {
                                Color(android.graphics.Color.parseColor(hex))
                            }.getOrDefault(Color(0xFF3B82F6))
                            val isSelected = selectedColor.equals(hex, ignoreCase = true)

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        } else {
                                            Modifier.border(1.dp, LightBorder, CircleShape)
                                        }
                                    )
                                    .clickable { selectedColor = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Seleccionado",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isEmpty()) {
                        isError = true
                    } else {
                        val folder = initialFolder?.copy(
                            name = trimmed,
                            colorHex = selectedColor
                        ) ?: FolderItem(
                            id = UUID.randomUUID().toString(),
                            name = trimmed,
                            colorHex = selectedColor
                        )
                        onSave(folder)
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (initialFolder == null) "Crear" else "Guardar", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (initialFolder != null && onDelete != null) {
                    IconButton(
                        onClick = { onDelete(initialFolder.id) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar carpeta",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancelar", color = LightTextSecondary)
                }
            }
        }
    )
}
