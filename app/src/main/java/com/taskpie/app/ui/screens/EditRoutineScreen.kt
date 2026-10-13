package com.taskpie.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.unit.sp
import com.taskpie.app.model.ActivityItem
import com.taskpie.app.model.RoutineBlock
import com.taskpie.app.model.RoutineItem
import com.taskpie.app.model.RoutineStep
import com.taskpie.app.model.toBlocks
import com.taskpie.app.model.toFlatSteps
import com.taskpie.app.ui.theme.*
import com.taskpie.app.ui.viewmodel.TaskPieViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRoutineScreen(
    viewModel: TaskPieViewModel,
    routineId: String?,
    onNavigateBack: () -> Unit
) {
    val routines by viewModel.routines.collectAsState()
    val activities by viewModel.activities.collectAsState()

    val existingRoutine = remember(routineId, routines) {
        routines.find { it.id == routineId }
    }

    var title by remember(existingRoutine) { mutableStateOf(existingRoutine?.title ?: "") }
    var description by remember(existingRoutine) { mutableStateOf(existingRoutine?.description ?: "") }
    var isTemplate by remember(existingRoutine) { mutableStateOf(existingRoutine?.isTemplate ?: false) }
    var blocks by remember(existingRoutine) {
        mutableStateOf(existingRoutine?.steps?.toBlocks() ?: emptyList())
    }

    // targetBlockIndex: null = new single activity block, int = append to that block
    var selectingActivityForBlockIndex by remember { mutableStateOf<Int?>(null) }
    var isSelectingActivity by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val totalSeconds = remember(blocks) {
        blocks.sumOf { b ->
            b.repeatCount.coerceAtLeast(1).toLong() * b.steps.sumOf { it.totalDurationSeconds }
        }
    }
    val totalMinutes = totalSeconds / 60
    val totalActivitiesCount = remember(blocks) {
        blocks.sumOf { it.steps.size * it.repeatCount.coerceAtLeast(1) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (existingRoutine != null) "Editar Rutina" else "Nueva Rutina",
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
                                errorMessage = "Ingresa un título para la rutina"
                                return@Button
                            }
                            val validBlocks = blocks.filter { it.steps.isNotEmpty() }
                            if (validBlocks.isEmpty()) {
                                errorMessage = "Agrega al menos una actividad a la rutina"
                                return@Button
                            }

                            val flattenedSteps = validBlocks.toFlatSteps()
                            val item = RoutineItem(
                                id = existingRoutine?.id ?: UUID.randomUUID().toString(),
                                title = title.trim(),
                                description = description.trim(),
                                steps = flattenedSteps,
                                isTemplate = isTemplate,
                                createdAt = existingRoutine?.createdAt ?: System.currentTimeMillis()
                            )
                            viewModel.saveRoutine(item)
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
            // Routine Information
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
                            text = "Información de la Rutina",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        OutlinedTextField(
                            value = title,
                            onValueChange = {
                                title = it
                                errorMessage = null
                            },
                            label = { Text("Nombre de la rutina") },
                            placeholder = { Text("Ej: Bloque de Trabajo + Pausas") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = LightBorder),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Descripción (opcional)") },
                            placeholder = { Text("Ej: Ciclos de enfoque con descansos intercalados") },
                            minLines = 2,
                            maxLines = 3,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = LightBorder),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Guardar como plantilla de rutina",
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp)
                                )
                                Text(
                                    text = "Estará disponible como modelo reutilizable",
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

            // Steps Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Secuencia y Bloques",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "$totalActivitiesCount actividades programadas • Total: $totalMinutes min",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 12.sp,
                                color = LightTextSecondary
                            )
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = {
                                selectingActivityForBlockIndex = null
                                isSelectingActivity = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Actividad", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val newBlock = RoutineBlock(
                                    id = "block_${System.currentTimeMillis()}",
                                    title = "Bloque ${blocks.size + 1}",
                                    repeatCount = 2,
                                    steps = emptyList()
                                )
                                val nextIndex = blocks.size
                                blocks = blocks + newBlock
                                selectingActivityForBlockIndex = nextIndex
                                isSelectingActivity = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Repeat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Bloque", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Blocks List
            if (blocks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectingActivityForBlockIndex = null
                                isSelectingActivity = true
                            },
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
                                imageVector = Icons.Default.PostAdd,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Aún no has agregado actividades ni bloques",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Toca aquí para seleccionar una actividad o crea un bloque repetitivo arriba",
                                style = MaterialTheme.typography.bodyMedium.copy(color = LightTextSecondary),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(blocks) { blockIndex, block ->
                    val isRepeatableBlock = block.repeatCount > 1 || block.steps.size > 1 || block.title.isNotBlank()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isRepeatableBlock) AccentIndigoLight.copy(alpha = 0.35f) else LightSurface
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isRepeatableBlock) AccentIndigo.copy(alpha = 0.4f) else LightBorder
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Block Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Move block up/down
                                    Column {
                                        IconButton(
                                            onClick = {
                                                if (blockIndex > 0) {
                                                    val list = blocks.toMutableList()
                                                    val item = list.removeAt(blockIndex)
                                                    list.add(blockIndex - 1, item)
                                                    blocks = list
                                                }
                                            },
                                            enabled = blockIndex > 0,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowUp,
                                                contentDescription = "Subir bloque",
                                                tint = if (blockIndex > 0) LightTextPrimary else LightTextTertiary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                if (blockIndex < blocks.size - 1) {
                                                    val list = blocks.toMutableList()
                                                    val item = list.removeAt(blockIndex)
                                                    list.add(blockIndex + 1, item)
                                                    blocks = list
                                                }
                                            },
                                            enabled = blockIndex < blocks.size - 1,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Bajar bloque",
                                                tint = if (blockIndex < blocks.size - 1) LightTextPrimary else LightTextTertiary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        if (isRepeatableBlock) {
                                            OutlinedTextField(
                                                value = block.title,
                                                onValueChange = { newTitle ->
                                                    val list = blocks.toMutableList()
                                                    list[blockIndex] = block.copy(title = newTitle)
                                                    blocks = list
                                                },
                                                placeholder = { Text("Nombre del bloque", fontSize = 13.sp) },
                                                singleLine = true,
                                                textStyle = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                ),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    unfocusedBorderColor = Color.Transparent,
                                                    focusedBorderColor = AccentIndigo
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        } else {
                                            Text(
                                                text = "Paso ${blockIndex + 1}",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            )
                                        }

                                        val blockSecs = block.steps.sumOf { it.totalDurationSeconds } * block.repeatCount.coerceAtLeast(1)
                                        Text(
                                            text = "${block.steps.size} actividades • Subtotal: ${blockSecs / 60} min",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 11.sp,
                                                color = LightTextSecondary
                                            )
                                        )
                                    }
                                }

                                // Repetitions counter and Delete block
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Repeat multiplier controls
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (block.repeatCount > 1) AccentIndigo.copy(alpha = 0.15f) else LightSurfaceVariant)
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                val next = (block.repeatCount - 1).coerceAtLeast(1)
                                                val list = blocks.toMutableList()
                                                list[blockIndex] = block.copy(repeatCount = next)
                                                blocks = list
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Menos repeticiones", modifier = Modifier.size(14.dp))
                                        }

                                        Text(
                                            text = "x${block.repeatCount}",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (block.repeatCount > 1) AccentIndigo else LightTextPrimary
                                            ),
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )

                                        IconButton(
                                            onClick = {
                                                val next = (block.repeatCount + 1).coerceAtMost(99)
                                                val list = blocks.toMutableList()
                                                val currentTitle = if (block.title.isBlank() && next > 1) "Bloque ${blockIndex + 1}" else block.title
                                                list[blockIndex] = block.copy(repeatCount = next, title = currentTitle)
                                                blocks = list
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Más repeticiones", modifier = Modifier.size(14.dp))
                                        }
                                    }

                                    // Delete Block
                                    IconButton(
                                        onClick = {
                                            val list = blocks.toMutableList()
                                            list.removeAt(blockIndex)
                                            blocks = list
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Eliminar bloque",
                                            tint = AccentCoral,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Steps inside this block
                            if (block.steps.isEmpty()) {
                                Text(
                                    text = "Este bloque aún no tiene actividades.",
                                    fontSize = 12.sp,
                                    color = LightTextSecondary,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    block.steps.forEachIndexed { stepIdx, step ->
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = LightSurface),
                                            border = BorderStroke(1.dp, LightBorder),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Move step inside block
                                                    if (block.steps.size > 1) {
                                                        Column {
                                                            IconButton(
                                                                onClick = {
                                                                    if (stepIdx > 0) {
                                                                        val stepList = block.steps.toMutableList()
                                                                        val moved = stepList.removeAt(stepIdx)
                                                                        stepList.add(stepIdx - 1, moved)
                                                                        val list = blocks.toMutableList()
                                                                        list[blockIndex] = block.copy(steps = stepList)
                                                                        blocks = list
                                                                    }
                                                                },
                                                                enabled = stepIdx > 0,
                                                                modifier = Modifier.size(20.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.KeyboardArrowUp,
                                                                    contentDescription = "Subir paso",
                                                                    tint = if (stepIdx > 0) LightTextPrimary else LightTextTertiary,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }

                                                            IconButton(
                                                                onClick = {
                                                                    if (stepIdx < block.steps.size - 1) {
                                                                        val stepList = block.steps.toMutableList()
                                                                        val moved = stepList.removeAt(stepIdx)
                                                                        stepList.add(stepIdx + 1, moved)
                                                                        val list = blocks.toMutableList()
                                                                        list[blockIndex] = block.copy(steps = stepList)
                                                                        blocks = list
                                                                    }
                                                                },
                                                                enabled = stepIdx < block.steps.size - 1,
                                                                modifier = Modifier.size(20.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                                    contentDescription = "Bajar paso",
                                                                    tint = if (stepIdx < block.steps.size - 1) LightTextPrimary else LightTextTertiary,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                        }
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                    }

                                                    // Title and duration
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = step.activityTitle,
                                                            style = MaterialTheme.typography.titleMedium.copy(
                                                                fontWeight = FontWeight.SemiBold,
                                                                fontSize = 14.sp
                                                            )
                                                        )
                                                        val mins = step.totalDurationSeconds / 60
                                                        Text(
                                                            text = "${step.stages.size} etapas • $mins min",
                                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                                fontSize = 11.sp,
                                                                color = LightTextSecondary
                                                            )
                                                        )
                                                    }

                                                    // Delete step from block
                                                    IconButton(
                                                        onClick = {
                                                            val stepList = block.steps.toMutableList()
                                                            stepList.removeAt(stepIdx)
                                                            val list = blocks.toMutableList()
                                                            if (stepList.isEmpty() && !isRepeatableBlock) {
                                                                list.removeAt(blockIndex)
                                                            } else {
                                                                list[blockIndex] = block.copy(steps = stepList)
                                                            }
                                                            blocks = list
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Quitar actividad",
                                                            tint = AccentCoral,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                // Auto Advance Switch
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(LightSurfaceVariant)
                                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = if (step.autoAdvance) "Continuar automáticamente" else "Pausar al terminar",
                                                        style = MaterialTheme.typography.labelMedium.copy(
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = if (step.autoAdvance) AccentEmerald else AccentAmber
                                                        )
                                                    )

                                                    Switch(
                                                        checked = step.autoAdvance,
                                                        onCheckedChange = { checked ->
                                                            val stepList = block.steps.toMutableList()
                                                            stepList[stepIdx] = step.copy(autoAdvance = checked)
                                                            val list = blocks.toMutableList()
                                                            list[blockIndex] = block.copy(steps = stepList)
                                                            blocks = list
                                                        },
                                                        modifier = Modifier.height(24.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Add activity to this block button
                            if (isRepeatableBlock) {
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = {
                                        selectingActivityForBlockIndex = blockIndex
                                        isSelectingActivity = true
                                    },
                                    modifier = Modifier.align(Alignment.End),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Añadir a este bloque", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet or Dialog to Pick an Activity
    if (isSelectingActivity) {
        AlertDialog(
            onDismissRequest = {
                isSelectingActivity = false
                selectingActivityForBlockIndex = null
            },
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Seleccionar Actividad",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activities) { activity ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val step = RoutineStep(
                                        activityId = activity.id,
                                        activityTitle = activity.title,
                                        stages = activity.stages,
                                        autoAdvance = true
                                    )
                                    val bIdx = selectingActivityForBlockIndex
                                    if (bIdx != null && bIdx >= 0 && bIdx < blocks.size) {
                                        val list = blocks.toMutableList()
                                        val currentBlock = list[bIdx]
                                        list[bIdx] = currentBlock.copy(steps = currentBlock.steps + step)
                                        blocks = list
                                    } else {
                                        // New single activity block
                                        val newBlock = RoutineBlock(
                                            id = "block_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}",
                                            title = "",
                                            repeatCount = 1,
                                            steps = listOf(step)
                                        )
                                        blocks = blocks + newBlock
                                    }
                                    isSelectingActivity = false
                                    selectingActivityForBlockIndex = null
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = LightSurfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = activity.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "${activity.stages.size} etapas • ${activity.formattedTotalDuration()}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 12.sp,
                                        color = LightTextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    isSelectingActivity = false
                    selectingActivityForBlockIndex = null
                }) {
                    Text("Cancelar", color = LightTextSecondary)
                }
            }
        )
    }
}
