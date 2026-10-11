package com.taskpie.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.unit.sp
import com.taskpie.app.model.ActivityItem
import com.taskpie.app.model.FolderItem
import com.taskpie.app.model.Stage
import com.taskpie.app.ui.components.FolderDialog
import com.taskpie.app.ui.components.StageEditorDialog
import com.taskpie.app.ui.theme.*
import com.taskpie.app.ui.viewmodel.TaskPieViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditActivityScreen(
    viewModel: TaskPieViewModel,
    activityId: String?,
    onNavigateBack: () -> Unit
) {
    val activities by viewModel.activities.collectAsState()
    val existingActivity = remember(activityId, activities) {
        activities.find { it.id == activityId }
    }

    var title by remember(existingActivity) { mutableStateOf(existingActivity?.title ?: "") }
    var description by remember(existingActivity) { mutableStateOf(existingActivity?.description ?: "") }
    var category by remember(existingActivity) { mutableStateOf(existingActivity?.category ?: "General") }
    var isTemplate by remember(existingActivity) { mutableStateOf(existingActivity?.isTemplate ?: false) }
    var stages by remember(existingActivity) { mutableStateOf(existingActivity?.stages ?: emptyList()) }

    var stageBeingEdited by remember { mutableStateOf<Pair<Int, Stage>?>(null) }
    var isCreatingNewStage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val folders by viewModel.folders.collectAsState()
    var showFolderDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (existingActivity != null) "Editar Actividad" else "Nueva Actividad",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                errorMessage = "Por favor ingresa un título"
                                return@Button
                            }
                            if (stages.isEmpty()) {
                                errorMessage = "Agrega al menos una etapa con duración"
                                return@Button
                            }

                            val item = ActivityItem(
                                id = existingActivity?.id ?: UUID.randomUUID().toString(),
                                title = title.trim(),
                                description = description.trim(),
                                category = category.trim().ifBlank { "General" },
                                isTemplate = isTemplate,
                                stages = stages,
                                createdAt = existingActivity?.createdAt ?: System.currentTimeMillis()
                            )
                            viewModel.saveActivity(item)
                            onNavigateBack()
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Guardar", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightBackground)
            )
        },
        containerColor = LightBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = LightSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Información General",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        // Title
                        OutlinedTextField(
                            value = title,
                            onValueChange = {
                                title = it
                                errorMessage = null
                            },
                            label = { Text("Nombre de la actividad") },
                            placeholder = { Text("Ej: Sprint de Escritura") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = LightBorder),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Description
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Descripción (opcional)") },
                            placeholder = { Text("Ej: Para escribir el capítulo 3 sin interrupciones") },
                            minLines = 2,
                            maxLines = 3,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = LightBorder),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Folder / Category Selection
                        Column {
                            Text(
                                text = "Carpeta / Categoría",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (folders.none { it.name.equals(category, ignoreCase = true) } && category.isNotBlank()) {
                                    item {
                                        FilterChip(
                                            selected = true,
                                            onClick = {},
                                            label = { Text(category, fontSize = 12.sp) },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }

                                items(folders) { fld ->
                                    val isSelected = category.equals(fld.name, ignoreCase = true)
                                    val chipColor = runCatching {
                                        Color(android.graphics.Color.parseColor(fld.colorHex))
                                    }.getOrDefault(PrimaryBlue)

                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { category = fld.name },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(chipColor)
                                            )
                                        },
                                        label = { Text(fld.name, fontSize = 12.sp) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }

                                item {
                                    OutlinedButton(
                                        onClick = { showFolderDialog = true },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Nueva", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Template toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Guardar como plantilla",
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp)
                                )
                                Text(
                                    text = "Reutilizable en futuras rutinas y sesiones",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 12.sp,
                                        color = LightTextSecondary
                                    )
                                )
                            }
                            Switch(
                                checked = isTemplate,
                                onCheckedChange = { isTemplate = it }
                            )
                        }
                    }
                }
            }

            // Error banner if any
            if (errorMessage != null) {
                item {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // Stages Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Etapas y Temporizadores",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        val totalSecs = stages.sumOf { it.durationSeconds }
                        val mins = totalSecs / 60
                        val secs = totalSecs % 60
                        Text(
                            text = "${stages.size} etapas • Total: ${if (mins > 0) "${mins}m ${secs}s" else "${secs}s"}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 12.sp,
                                color = LightTextSecondary
                            )
                        )
                    }

                    FilledTonalButton(
                        onClick = { isCreatingNewStage = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Agregar Etapa", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Stages List
            if (stages.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isCreatingNewStage = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = LightSurface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Aún no has agregado ninguna etapa",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Toca aquí para definir la primera parte con su tiempo",
                                style = MaterialTheme.typography.bodyMedium.copy(color = LightTextSecondary),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(stages) { index, stage ->
                    val color = try {
                        Color(android.graphics.Color.parseColor(stage.colorHex))
                    } catch (e: Exception) {
                        PrimaryBlue
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = LightSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Reorder Arrows
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                IconButton(
                                    onClick = {
                                        if (index > 0) {
                                            val list = stages.toMutableList()
                                            val item = list.removeAt(index)
                                            list.add(index - 1, item)
                                            stages = list
                                        }
                                    },
                                    enabled = index > 0,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Mover arriba",
                                        tint = if (index > 0) LightTextPrimary else LightTextTertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (index < stages.size - 1) {
                                            val list = stages.toMutableList()
                                            val item = list.removeAt(index)
                                            list.add(index + 1, item)
                                            stages = list
                                        }
                                    },
                                    enabled = index < stages.size - 1,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Mover abajo",
                                        tint = if (index < stages.size - 1) LightTextPrimary else LightTextTertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Color pill
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            // Details
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { stageBeingEdited = Pair(index, stage) }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${index + 1}. ${stage.name}",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp
                                        )
                                    )
                                    if (stage.isOptional) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(AccentAmberLight)
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
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

                                Text(
                                    text = "Duración: ${stage.formattedDuration()}${if (stage.notes.isNotBlank()) " • ${stage.notes}" else ""}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 12.sp,
                                        color = LightTextSecondary
                                    ),
                                    maxLines = 1
                                )
                            }

                            // Actions: Edit, Duplicate, Delete
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { stageBeingEdited = Pair(index, stage) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Editar",
                                        tint = LightTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val copy = stage.copy(
                                            id = UUID.randomUUID().toString(),
                                            name = "${stage.name} (Copia)"
                                        )
                                        val list = stages.toMutableList()
                                        list.add(index + 1, copy)
                                        stages = list
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Duplicar",
                                        tint = LightTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val list = stages.toMutableList()
                                        list.removeAt(index)
                                        stages = list
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Eliminar",
                                        tint = AccentCoral,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // New Stage Dialog
    if (isCreatingNewStage) {
        StageEditorDialog(
            initialStage = null,
            onDismiss = { isCreatingNewStage = false },
            onSave = { newStage ->
                stages = stages + newStage
                isCreatingNewStage = false
            }
        )
    }

    // Edit Stage Dialog
    stageBeingEdited?.let { (idx, stage) ->
        StageEditorDialog(
            initialStage = stage,
            onDismiss = { stageBeingEdited = null },
            onSave = { updatedStage ->
                val list = stages.toMutableList()
                list[idx] = updatedStage
                stages = list
                stageBeingEdited = null
            }
        )
    }

    if (showFolderDialog) {
        FolderDialog(
            initialFolder = null,
            onDismiss = { showFolderDialog = false },
            onSave = { newFolder ->
                viewModel.saveFolder(newFolder)
                category = newFolder.name
                showFolderDialog = false
            }
        )
    }
}
