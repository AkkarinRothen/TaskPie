package com.taskpie.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.taskpie.app.model.ActivityItem
import com.taskpie.app.model.RoutineBlock
import com.taskpie.app.model.RoutineItem
import com.taskpie.app.model.RoutineStep
import com.taskpie.app.model.toBlocks
import com.taskpie.app.model.toFlatSteps
import com.taskpie.app.data.local.EditorDraftRead
import com.taskpie.app.data.local.RoutineEditorDraft
import com.taskpie.app.ui.theme.*
import com.taskpie.app.ui.components.CreationStepIndicator
import com.taskpie.app.ui.components.EditorDraftRecoveryDialog
import com.taskpie.app.ui.components.InvalidEditorDraftDialog
import com.taskpie.app.ui.components.EditorUndoHistory
import com.taskpie.app.ui.components.DragReorderHandle
import com.taskpie.app.ui.viewmodel.TaskPieViewModel
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import androidx.compose.runtime.snapshotFlow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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

    val draftKey = remember(routineId) { viewModel.editorDraftKey("routine", routineId) }
    val draftRead = remember(draftKey) { viewModel.readRoutineDraft(draftKey) }
    var draftPromptVisible by remember(draftKey) {
        mutableStateOf(draftRead is EditorDraftRead.Found)
    }
    var draftReadyToPersist by remember(draftKey) {
        mutableStateOf(draftRead is EditorDraftRead.Missing)
    }
    var invalidDraftVisible by remember(draftKey) { mutableStateOf(false) }
    val baseBlocks = remember(existingRoutine) { existingRoutine?.steps?.toBlocks() ?: emptyList() }
    val baseDraft = remember(existingRoutine, baseBlocks) {
        existingRoutine?.let {
            RoutineEditorDraft(it.title, it.description, it.isTemplate, baseBlocks, 1)
        }
    }

    var title by remember(existingRoutine) { mutableStateOf(existingRoutine?.title ?: "") }
    var description by remember(existingRoutine) { mutableStateOf(existingRoutine?.description ?: "") }
    var isTemplate by remember(existingRoutine) { mutableStateOf(existingRoutine?.isTemplate ?: false) }
    var blocks by remember(existingRoutine) {
        mutableStateOf(existingRoutine?.steps?.toBlocks() ?: emptyList())
    }

    var selectingActivityForBlockIndex by remember { mutableStateOf<Int?>(null) }
    var isSelectingActivity by remember { mutableStateOf(false) }
    var isCreatingActivity by remember { mutableStateOf(false) }
    var activitySearchQuery by remember { mutableStateOf("") }
    var expandedStepIds by remember { mutableStateOf(setOf<String>()) }
    var selectedRoutineItemIds by remember { mutableStateOf(setOf<String>()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var creationStep by remember(routineId) { mutableIntStateOf(if (routineId != null) 1 else 0) }
    val editorListState = rememberLazyListState()
    var reviewScrollTarget by remember { mutableIntStateOf(-1) }

    val totalSeconds = remember(blocks) {
        blocks.sumOf { b ->
            b.repeatCount.coerceAtLeast(1).toLong() * b.steps.sumOf { it.totalDurationSeconds }
        }
    }
    val totalMinutes = totalSeconds / 60
    val totalDurationLabel = if (totalSeconds % 60 > 0) {
        "${totalMinutes}m ${totalSeconds % 60}s"
    } else {
        "${totalMinutes}m"
    }
    val totalActivitiesCount = remember(blocks) {
        blocks.sumOf { it.steps.size * it.repeatCount.coerceAtLeast(1) }
    }
    val filteredActivities = remember(activities, activitySearchQuery) {
        if (activitySearchQuery.isBlank()) activities
        else activities.filter { it.title.contains(activitySearchQuery.trim(), ignoreCase = true) }
    }

    LaunchedEffect(draftKey, draftRead) {
        if (draftRead is EditorDraftRead.Corrupt) {
            viewModel.deleteEditorDraft(draftKey)
            invalidDraftVisible = true
            draftReadyToPersist = true
        }
    }

    val currentDraft = RoutineEditorDraft(title, description, isTemplate, blocks, creationStep)
    val history = remember(draftKey) { EditorUndoHistory(currentDraft.copy(creationStep = 0)) }
    LaunchedEffect(draftKey, draftReadyToPersist, draftPromptVisible) {
        if (!draftReadyToPersist || draftPromptVisible) return@LaunchedEffect
        snapshotFlow { RoutineEditorDraft(title, description, isTemplate, blocks, 0) }
            .drop(1)
            .collectLatest { snapshot ->
                delay(500)
                history.record(snapshot)
            }
    }
    fun restoreHistory(snapshot: RoutineEditorDraft) {
        title = snapshot.title
        description = snapshot.description
        isTemplate = snapshot.isTemplate
        blocks = snapshot.blocks
    }
    fun moveBlock(from: Int, to: Int) {
        if (from !in blocks.indices || to !in blocks.indices) return
        val reordered = blocks.toMutableList()
        reordered.add(to, reordered.removeAt(from))
        blocks = reordered
    }
    fun moveStep(blockIndex: Int, from: Int, to: Int) {
        val block = blocks.getOrNull(blockIndex) ?: return
        if (from !in block.steps.indices || to !in block.steps.indices) return
        val steps = block.steps.toMutableList()
        steps.add(to, steps.removeAt(from))
        val updated = blocks.toMutableList()
        updated[blockIndex] = block.copy(steps = steps)
        blocks = updated
    }
    fun toggleRoutineItemSelection(key: String) {
        selectedRoutineItemIds = if (key in selectedRoutineItemIds) selectedRoutineItemIds - key else selectedRoutineItemIds + key
    }
    fun removeSelectedRoutineItems() {
        blocks = blocks.mapIndexedNotNull { blockIndex, block ->
            if ("block:${block.id}" in selectedRoutineItemIds) return@mapIndexedNotNull null
            val keptSteps = block.steps.filterNot { "step:${it.id}" in selectedRoutineItemIds }
            val repeatable = block.repeatCount > 1 || block.title.isNotBlank()
            if (keptSteps.isEmpty() && !repeatable) null else block.copy(steps = keptSteps)
        }
        selectedRoutineItemIds = emptySet()
    }
    LaunchedEffect(creationStep, reviewScrollTarget, errorMessage) {
        if (creationStep == 1 && reviewScrollTarget >= 0) {
            delay(100)
            editorListState.animateScrollToItem(reviewScrollTarget.coerceAtMost(editorListState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0))
            reviewScrollTarget = -1
        }
    }
    val hasDraftChanges = if (routineId == null) {
        title.isNotBlank() || description.isNotBlank() || isTemplate || blocks.isNotEmpty()
    } else {
        baseDraft != null && currentDraft != baseDraft
    }

    LaunchedEffect(draftKey, title, description, isTemplate, blocks, creationStep, draftReadyToPersist, hasDraftChanges, existingRoutine) {
        if (!draftReadyToPersist || (routineId != null && existingRoutine == null)) return@LaunchedEffect
        delay(700)
        if (hasDraftChanges) viewModel.saveEditorDraft(draftKey, currentDraft)
        else viewModel.deleteEditorDraft(draftKey)
    }

    fun addActivityToSelectedBlock(activity: ActivityItem) {
        val step = RoutineStep(
            activityId = activity.id,
            activityTitle = activity.title,
            stages = activity.stages,
            autoAdvance = true
        )
        val blockIndex = selectingActivityForBlockIndex
        if (blockIndex != null && blockIndex in blocks.indices) {
            val updated = blocks.toMutableList()
            updated[blockIndex] = updated[blockIndex].copy(steps = updated[blockIndex].steps + step)
            blocks = updated
        } else {
            blocks = blocks + RoutineBlock(
                id = "block_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}",
                title = "",
                repeatCount = 1,
                steps = listOf(step)
            )
        }
        isSelectingActivity = false
        selectingActivityForBlockIndex = null
        activitySearchQuery = ""
    }

    fun duplicateBlock(blockIndex: Int) {
        val source = blocks.getOrNull(blockIndex) ?: return
        val newBlockId = UUID.randomUUID().toString()
        val copiedSteps = source.steps.map { step ->
            step.copy(
                id = UUID.randomUUID().toString(),
                stages = step.stages.map { stage -> stage.copy(id = UUID.randomUUID().toString()) },
                blockId = newBlockId,
                blockTitle = source.title,
                blockRepeatCount = source.repeatCount
            )
        }
        val updatedBlocks = blocks.toMutableList()
        updatedBlocks.add(blockIndex + 1, source.copy(id = newBlockId, steps = copiedSteps))
        blocks = updatedBlocks
    }

    BackHandler(enabled = creationStep > 0) {
        creationStep -= 1
        errorMessage = null
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
                    IconButton(onClick = {
                        if (creationStep > 0) {
                            creationStep -= 1
                            errorMessage = null
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = if (creationStep > 0) "Paso anterior" else "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { history.undo()?.let(::restoreHistory) }, enabled = history.canUndo) {
                        Icon(Icons.Default.Undo, contentDescription = "Deshacer")
                    }
                    IconButton(onClick = { history.redo()?.let(::restoreHistory) }, enabled = history.canRedo) {
                        Icon(Icons.Default.Redo, contentDescription = "Rehacer")
                    }
                    Button(
                        onClick = {
                            when (creationStep) {
                                0 -> if (title.isBlank()) {
                                    errorMessage = "Ingresa un título para la rutina"
                                } else {
                                    errorMessage = null
                                    creationStep = 1
                                }
                                1 -> if (blocks.none { it.steps.isNotEmpty() }) {
                                    errorMessage = "Agrega al menos una actividad a la rutina"
                                } else {
                                    errorMessage = null
                                    creationStep = 2
                                }
                                else -> {
                                    if (title.isBlank()) {
                                        creationStep = 0
                                        errorMessage = "Ingresa un título para la rutina"
                                        return@Button
                                    }
                                    val validBlocks = blocks.filter { it.steps.isNotEmpty() }
                                    if (validBlocks.isEmpty()) {
                                        creationStep = 1
                                        errorMessage = "Agrega al menos una actividad a la rutina"
                                        return@Button
                                    }
                                    val item = RoutineItem(
                                        id = existingRoutine?.id ?: UUID.randomUUID().toString(),
                                        title = title.trim(),
                                        description = description.trim(),
                                        steps = validBlocks.toFlatSteps(),
                                        isTemplate = isTemplate,
                                        createdAt = existingRoutine?.createdAt ?: System.currentTimeMillis()
                                    )
                                    viewModel.deleteEditorDraft(draftKey)
                                    viewModel.saveRoutine(item)
                                    onNavigateBack()
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(when (creationStep) { 0 -> "Siguiente"; 1 -> "Revisar"; else -> "Guardar" }, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightBackground)
            )
        },
        containerColor = LightBackground
    ) { paddingValues ->
        LazyColumn(
            state = editorListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { CreationStepIndicator(creationStep) }
            if (creationStep == 0) {
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

            if (creationStep == 1) {
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
                            text = "$totalActivitiesCount actividades programadas • Total: $totalDurationLabel",
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
                if (selectedRoutineItemIds.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = ::removeSelectedRoutineItems) {
                            Text("Quitar ${selectedRoutineItemIds.size} seleccionados")
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
                        modifier = Modifier.fillMaxWidth().combinedClickable(
                            onClick = {
                                if (selectedRoutineItemIds.isNotEmpty()) toggleRoutineItemSelection("block:${block.id}")
                            },
                            onLongClick = { toggleRoutineItemSelection("block:${block.id}") }
                        ),
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
                                if (selectedRoutineItemIds.isNotEmpty()) {
                                    Checkbox(
                                        checked = "block:${block.id}" in selectedRoutineItemIds,
                                        onCheckedChange = { toggleRoutineItemSelection("block:${block.id}") }
                                    )
                                }
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

                                    DragReorderHandle(blockIndex, blocks.size, ::moveBlock, Modifier.size(32.dp))

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

                                // Repetitions counter and block actions
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

                                    IconButton(
                                        onClick = { duplicateBlock(blockIndex) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Duplicar bloque",
                                            tint = LightTextSecondary,
                                            modifier = Modifier.size(17.dp)
                                        )
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
                                            modifier = Modifier.fillMaxWidth().combinedClickable(
                                                onClick = {
                                                    if (selectedRoutineItemIds.isNotEmpty()) toggleRoutineItemSelection("step:${step.id}")
                                                },
                                                onLongClick = { toggleRoutineItemSelection("step:${step.id}") }
                                            )
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
                                                    if (selectedRoutineItemIds.isNotEmpty()) {
                                                        Checkbox(
                                                            checked = "step:${step.id}" in selectedRoutineItemIds,
                                                            onCheckedChange = { toggleRoutineItemSelection("step:${step.id}") }
                                                        )
                                                    }
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
                                                    DragReorderHandle(stepIdx, block.steps.size, { from, to -> moveStep(blockIndex, from, to) }, Modifier.size(28.dp))

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

                                                // Auto Advance Switch & Customize Button Row
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
                                                        text = if (step.autoAdvance) "Continuar auto." else "Pausar al terminar",
                                                        style = MaterialTheme.typography.labelMedium.copy(
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = if (step.autoAdvance) AccentEmerald else AccentAmber
                                                        )
                                                    )

                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        if (step.isCustomized) {
                                                            Text(
                                                                text = "Personalizada",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = AccentAmber,
                                                                modifier = Modifier
                                                                    .padding(end = 6.dp)
                                                                    .clip(RoundedCornerShape(4.dp))
                                                                    .background(AccentAmber.copy(alpha = 0.15f))
                                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                                            )
                                                        }

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

                                                // Accordion Toggle for Stages
                                                val isExpanded = expandedStepIds.contains(step.id)
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            expandedStepIds = if (isExpanded) {
                                                                expandedStepIds - step.id
                                                            } else {
                                                                expandedStepIds + step.id
                                                            }
                                                        }
                                                        .padding(vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    val activeStagesCount = step.stages.count { it.isEnabled }
                                                    Text(
                                                        text = if (isExpanded) "Ocultar etapas" else "Personalizar etapas ($activeStagesCount/${step.stages.size})",
                                                        fontSize = 11.sp,
                                                        color = PrimaryBlue,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    Icon(
                                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                        contentDescription = null,
                                                        tint = PrimaryBlue,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }

                                                // Expanded Stages Content
                                                if (isExpanded) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(LightBackground)
                                                            .padding(8.dp),
                                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        if (step.isCustomized) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.End
                                                            ) {
                                                                TextButton(
                                                                    onClick = {
                                                                        val originalAct = activities.find { it.id == (step.originalActivityId ?: step.activityId) }
                                                                        if (originalAct != null) {
                                                                            val restoredStages = originalAct.stages.map { it.copy(isEnabled = true) }
                                                                            val stepList = block.steps.toMutableList()
                                                                            stepList[stepIdx] = step.copy(
                                                                                stages = restoredStages,
                                                                                isCustomized = false
                                                                            )
                                                                            val list = blocks.toMutableList()
                                                                            list[blockIndex] = block.copy(steps = stepList)
                                                                            blocks = list
                                                                        }
                                                                    },
                                                                    contentPadding = PaddingValues(0.dp),
                                                                    modifier = Modifier.height(24.dp)
                                                                ) {
                                                                    Text("Restablecer a original", fontSize = 10.sp, color = AccentAmber)
                                                                }
                                                            }
                                                        }

                                                        step.stages.forEachIndexed { stageIdx, stage ->
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .clip(RoundedCornerShape(6.dp))
                                                                    .background(LightSurface)
                                                                    .clickable {
                                                                        val nextEnabled = !stage.isEnabled
                                                                        val newStages = step.stages.toMutableList()
                                                                        newStages[stageIdx] = stage.copy(isEnabled = nextEnabled)
                                                                        val stepList = block.steps.toMutableList()
                                                                        stepList[stepIdx] = step.copy(stages = newStages, isCustomized = true)
                                                                        val list = blocks.toMutableList()
                                                                        list[blockIndex] = block.copy(steps = stepList)
                                                                        blocks = list
                                                                    }
                                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                                    modifier = Modifier.weight(1f)
                                                                ) {
                                                                    Checkbox(
                                                                        checked = stage.isEnabled,
                                                                        onCheckedChange = { checked ->
                                                                            val newStages = step.stages.toMutableList()
                                                                            newStages[stageIdx] = stage.copy(isEnabled = checked)
                                                                            val stepList = block.steps.toMutableList()
                                                                            stepList[stepIdx] = step.copy(stages = newStages, isCustomized = true)
                                                                            val list = blocks.toMutableList()
                                                                            list[blockIndex] = block.copy(steps = stepList)
                                                                            blocks = list
                                                                        },
                                                                        modifier = Modifier.size(18.dp)
                                                                    )
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .size(8.dp)
                                                                            .clip(CircleShape)
                                                                            .background(
                                                                                try {
                                                                                    Color(android.graphics.Color.parseColor(stage.colorHex))
                                                                                } catch (e: Exception) {
                                                                                    PrimaryBlue
                                                                                }
                                                                            )
                                                                    )
                                                                    Text(
                                                                        text = stage.name,
                                                                        fontSize = 12.sp,
                                                                        fontWeight = if (stage.isEnabled) FontWeight.Medium else FontWeight.Normal,
                                                                        color = if (stage.isEnabled) LightTextPrimary else LightTextTertiary
                                                                    )
                                                                }
                                                                Text(
                                                                    text = stage.formattedDuration(),
                                                                    fontSize = 11.sp,
                                                                    color = if (stage.isEnabled) LightTextSecondary else LightTextTertiary
                                                                )
                                                            }
                                                        }
                                                    }
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
            if (creationStep == 2) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = LightSurface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Revisar rutina", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.clickable { creationStep = 1; reviewScrollTarget = 1 })
                            Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.clickable { creationStep = 0 })
                            if (description.isNotBlank()) Text(description, color = LightTextSecondary, modifier = Modifier.clickable { creationStep = 0 })
                            Text("$totalActivitiesCount actividades programadas • $totalDurationLabel • ${blocks.size} bloques")
                            blocks.forEachIndexed { blockIndex, block ->
                                Text(
                                    text = "${blockIndex + 1}. ${block.title.ifBlank { block.steps.firstOrNull()?.activityTitle ?: "Bloque" }}${if (block.repeatCount > 1) " ×${block.repeatCount}" else ""}",
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.fillMaxWidth().clickable { creationStep = 1; reviewScrollTarget = 2 + (if (errorMessage != null) 1 else 0) + blockIndex }
                                )
                                block.steps.forEach { step ->
                                    Text(
                                        text = "  • ${step.activityTitle} — ${step.stages.count { it.isEnabled }} etapas activas",
                                        style = MaterialTheme.typography.bodySmall.copy(color = LightTextSecondary),
                                        modifier = Modifier.fillMaxWidth().clickable { creationStep = 1; reviewScrollTarget = 2 + (if (errorMessage != null) 1 else 0) + blockIndex }
                                    )
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = activitySearchQuery,
                        onValueChange = { activitySearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Buscar actividad") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (activitySearchQuery.isNotEmpty()) IconButton(onClick = { activitySearchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda")
                            }
                        }
                    )
                    TextButton(onClick = {
                        isSelectingActivity = false
                        isCreatingActivity = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Crear actividad nueva")
                    }
                    if (activities.isEmpty()) {
                        Text("Todavía no hay actividades. Crea una para añadirla a esta rutina.", color = LightTextSecondary)
                    } else if (filteredActivities.isEmpty()) {
                        Text("No se encontraron actividades con ese nombre.", color = LightTextSecondary)
                    } else LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                    items(filteredActivities) { activity ->
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

    if (isCreatingActivity) {
        Dialog(
            onDismissRequest = {
                isCreatingActivity = false
                isSelectingActivity = true
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = LightBackground) {
                EditActivityScreen(
                    viewModel = viewModel,
                    activityId = null,
                    onNavigateBack = {
                        isCreatingActivity = false
                        isSelectingActivity = true
                    },
                    onActivitySaved = { activity ->
                        addActivityToSelectedBlock(activity)
                        isCreatingActivity = false
                    }
                )
            }
        }
    }

    when (val loadedDraft = draftRead) {
        is EditorDraftRead.Found -> if (draftPromptVisible) {
            EditorDraftRecoveryDialog(
                itemType = "rutina",
                onContinue = {
                    loadedDraft.value.let { draft ->
                        title = draft.title
                        description = draft.description
                        isTemplate = draft.isTemplate
                        blocks = draft.blocks
                        creationStep = draft.creationStep.coerceIn(0, 2)
                        history.reset(draft.copy(creationStep = 0))
                    }
                    draftPromptVisible = false
                    draftReadyToPersist = true
                },
                onDiscard = {
                    viewModel.deleteEditorDraft(draftKey)
                    history.reset(currentDraft.copy(creationStep = 0))
                    draftPromptVisible = false
                    draftReadyToPersist = true
                }
            )
        }
        EditorDraftRead.Corrupt -> if (invalidDraftVisible) {
            InvalidEditorDraftDialog(onDismiss = { invalidDraftVisible = false })
        }
        EditorDraftRead.Missing -> Unit
    }
}
