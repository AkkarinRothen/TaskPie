package com.taskpie.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.taskpie.app.model.BuiltInTemplates
import com.taskpie.app.model.FolderItem
import com.taskpie.app.model.Stage
import com.taskpie.app.data.local.ActivityEditorDraft
import com.taskpie.app.data.local.EditorDraftRead
import com.taskpie.app.ui.components.FolderDialog
import com.taskpie.app.ui.components.CreationStepIndicator
import com.taskpie.app.ui.components.EditorDraftRecoveryDialog
import com.taskpie.app.ui.components.InvalidEditorDraftDialog
import com.taskpie.app.ui.components.StageEditorDialog
import com.taskpie.app.ui.components.EditorUndoHistory
import com.taskpie.app.ui.components.DragReorderHandle
import com.taskpie.app.ui.theme.*
import com.taskpie.app.ui.viewmodel.TaskPieViewModel
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import androidx.compose.runtime.snapshotFlow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EditActivityScreen(
    viewModel: TaskPieViewModel,
    activityId: String?,
    onNavigateBack: () -> Unit,
    onActivitySaved: ((ActivityItem) -> Unit)? = null
) {
    val activities by viewModel.activities.collectAsState()
    val existingActivity = remember(activityId, activities) {
        activities.find { it.id == activityId }
    }

    val draftKey = remember(activityId) { viewModel.editorDraftKey("activity", activityId) }
    val draftRead = remember(draftKey) { viewModel.readActivityDraft(draftKey) }
    var draftPromptVisible by remember(draftKey) {
        mutableStateOf(draftRead is EditorDraftRead.Found)
    }
    var draftReadyToPersist by remember(draftKey) {
        mutableStateOf(draftRead is EditorDraftRead.Missing)
    }
    var invalidDraftVisible by remember(draftKey) { mutableStateOf(false) }

    val baseDraft = remember(existingActivity) {
        existingActivity?.let {
            ActivityEditorDraft(
                title = it.title,
                description = it.description,
                category = it.category,
                isTemplate = it.isTemplate,
                stages = it.stages,
                hasStartedSetup = true,
                creationStep = 1
            )
        }
    }

    var title by remember(existingActivity) { mutableStateOf(existingActivity?.title ?: "") }
    var description by remember(existingActivity) { mutableStateOf(existingActivity?.description ?: "") }
    var category by remember(existingActivity) { mutableStateOf(existingActivity?.category ?: "General") }
    var isTemplate by remember(existingActivity) { mutableStateOf(existingActivity?.isTemplate ?: false) }
    var stages by remember(existingActivity) { mutableStateOf(existingActivity?.stages ?: emptyList()) }
    var hasStartedSetup by remember(activityId) { mutableStateOf(activityId != null) }
    var showTemplatePicker by remember { mutableStateOf(false) }
    var creationStep by remember(activityId) { mutableIntStateOf(if (activityId != null) 1 else 0) }
    val editorListState = rememberLazyListState()
    var reviewScrollTarget by remember { mutableIntStateOf(-1) }

    var stageBeingEdited by remember { mutableStateOf<Pair<Int, Stage>?>(null) }
    var selectedStageIds by remember { mutableStateOf(setOf<String>()) }
    var isCreatingNewStage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val folders by viewModel.folders.collectAsState()
    var showFolderDialog by remember { mutableStateOf(false) }
    val builtInTemplateIds = remember { BuiltInTemplates.DEFAULT_ACTIVITIES.map { it.id }.toSet() }
    val availableTemplates = remember(activities) {
        BuiltInTemplates.DEFAULT_ACTIVITIES.map { it to true } +
            activities.filter { it.isTemplate && it.id !in builtInTemplateIds }.map { it to false }
    }

    LaunchedEffect(draftKey, draftRead) {
        if (draftRead is EditorDraftRead.Corrupt) {
            viewModel.deleteEditorDraft(draftKey)
            invalidDraftVisible = true
            draftReadyToPersist = true
        }
    }

    val currentDraft = ActivityEditorDraft(title, description, category, isTemplate, stages, hasStartedSetup, creationStep)
    val history = remember(draftKey) { EditorUndoHistory(currentDraft.copy(creationStep = 0)) }
    LaunchedEffect(draftKey, draftReadyToPersist, draftPromptVisible) {
        if (!draftReadyToPersist || draftPromptVisible) return@LaunchedEffect
        snapshotFlow { ActivityEditorDraft(title, description, category, isTemplate, stages, hasStartedSetup, 0) }
            .drop(1)
            .collectLatest { snapshot ->
                delay(500)
                history.record(snapshot)
            }
    }
    fun restoreHistory(snapshot: ActivityEditorDraft) {
        title = snapshot.title
        description = snapshot.description
        category = snapshot.category
        isTemplate = snapshot.isTemplate
        stages = snapshot.stages
        hasStartedSetup = snapshot.hasStartedSetup
    }
    fun moveStage(from: Int, to: Int) {
        if (from !in stages.indices || to !in stages.indices) return
        val reordered = stages.toMutableList()
        reordered.add(to, reordered.removeAt(from))
        stages = reordered
    }
    fun toggleStageSelection(id: String) {
        selectedStageIds = if (id in selectedStageIds) selectedStageIds - id else selectedStageIds + id
    }
    LaunchedEffect(creationStep, reviewScrollTarget, errorMessage) {
        if (creationStep == 1 && reviewScrollTarget >= 0) {
            delay(100)
            editorListState.animateScrollToItem(reviewScrollTarget.coerceAtMost(editorListState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0))
            reviewScrollTarget = -1
        }
    }
    val hasDraftChanges = if (activityId == null) {
        hasStartedSetup || title.isNotBlank() || description.isNotBlank() || stages.isNotEmpty() || isTemplate
    } else {
        baseDraft != null && currentDraft != baseDraft
    }

    LaunchedEffect(draftKey, title, description, category, isTemplate, stages, hasStartedSetup, creationStep, draftReadyToPersist, hasDraftChanges, existingActivity) {
        if (!draftReadyToPersist || (activityId != null && existingActivity == null)) return@LaunchedEffect
        delay(700)
        if (hasDraftChanges) viewModel.saveEditorDraft(draftKey, currentDraft)
        else viewModel.deleteEditorDraft(draftKey)
    }

    BackHandler(enabled = hasStartedSetup && creationStep > 0) {
        creationStep -= 1
        errorMessage = null
    }

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
                    IconButton(onClick = {
                        if (hasStartedSetup && creationStep > 0) {
                            creationStep -= 1
                            errorMessage = null
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (hasStartedSetup && creationStep > 0) "Paso anterior" else "Volver"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { history.undo()?.let(::restoreHistory) }, enabled = history.canUndo) {
                        Icon(Icons.Default.Undo, contentDescription = "Deshacer")
                    }
                    IconButton(onClick = { history.redo()?.let(::restoreHistory) }, enabled = history.canRedo) {
                        Icon(Icons.Default.Redo, contentDescription = "Rehacer")
                    }
                    if (hasStartedSetup || existingActivity != null) Button(
                        onClick = {
                            when (creationStep) {
                                0 -> if (title.isBlank()) {
                                    errorMessage = "Por favor ingresa un título"
                                } else {
                                    errorMessage = null
                                    creationStep = 1
                                }
                                1 -> if (stages.isEmpty()) {
                                    errorMessage = "Agrega al menos una etapa con duración"
                                } else {
                                    errorMessage = null
                                    creationStep = 2
                                }
                                else -> {
                                    if (title.isBlank()) {
                                        creationStep = 0
                                        errorMessage = "Por favor ingresa un título"
                                        return@Button
                                    }
                                    if (stages.isEmpty()) {
                                        creationStep = 1
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
                                    viewModel.deleteEditorDraft(draftKey)
                                    viewModel.saveActivity(item)
                                    onActivitySaved?.invoke(item) ?: onNavigateBack()
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = when (creationStep) {
                                0 -> "Siguiente"
                                1 -> "Revisar"
                                else -> "Guardar"
                            },
                            fontWeight = FontWeight.Bold
                        )
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
            if (existingActivity == null && !hasStartedSetup) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "¿Cómo quieres empezar?",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Puedes crear una actividad desde cero o partir de una plantilla editable.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = LightTextSecondary)
                        )
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { hasStartedSetup = true },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = LightSurface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Icon(Icons.Default.EditNote, contentDescription = null, tint = PrimaryBlue)
                                Column {
                                    Text("Empezar desde cero", fontWeight = FontWeight.SemiBold)
                                    Text("Define tus propias etapas y tiempos", style = MaterialTheme.typography.bodySmall.copy(color = LightTextSecondary))
                                }
                            }
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { showTemplatePicker = true },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = LightSurface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryBlue)
                                Column {
                                    Text("Usar una plantilla", fontWeight = FontWeight.SemiBold)
                                    Text("Empieza con etapas y tiempos ya preparados", style = MaterialTheme.typography.bodySmall.copy(color = LightTextSecondary))
                                }
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = LightTextSecondary)
                            }
                        }
                    }
                }
            } else {
            item { CreationStepIndicator(creationStep) }
            if (creationStep == 0) {
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

            if (creationStep == 1) {
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
                if (selectedStageIds.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = {
                            stages = stages.filterNot { it.id in selectedStageIds }
                            selectedStageIds = emptySet()
                        }) { Text("Quitar ${selectedStageIds.size} seleccionadas") }
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
                            DragReorderHandle(index, stages.size, ::moveStage, Modifier.size(32.dp))

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
                                    .combinedClickable(
                                        onClick = {
                                            if (selectedStageIds.isNotEmpty()) toggleStageSelection(stage.id)
                                            else stageBeingEdited = Pair(index, stage)
                                        },
                                        onLongClick = { toggleStageSelection(stage.id) }
                                    )
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
                                if (selectedStageIds.isNotEmpty()) {
                                    Checkbox(
                                        checked = stage.id in selectedStageIds,
                                        onCheckedChange = { toggleStageSelection(stage.id) }
                                    )
                                } else IconButton(
                                    onClick = { stageBeingEdited = Pair(index, stage) }, modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = LightTextSecondary, modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = {
                                        val copy = stage.copy(
                                            id = UUID.randomUUID().toString()
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
            if (creationStep == 2) {
                item {
                    val totalSeconds = stages.filter { it.isEnabled }.sumOf { it.durationSeconds }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = LightSurface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Revisar actividad", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.clickable { creationStep = 1; reviewScrollTarget = 1 })
                            Text(title.ifBlank { "Sin título" }, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.clickable { creationStep = 0 })
                            if (description.isNotBlank()) Text(description, color = LightTextSecondary, modifier = Modifier.clickable { creationStep = 0 })
                            val mins = totalSeconds / 60
                            val secs = totalSeconds % 60
                            val duration = if (secs > 0) "${mins}m ${secs}s" else "${mins}m"
                            Text("${category.ifBlank { "General" }} • ${stages.size} etapas • $duration")
                            stages.forEachIndexed { index, stage ->
                                Row(modifier = Modifier.fillMaxWidth().clickable { creationStep = 1; reviewScrollTarget = 2 + (if (errorMessage != null) 1 else 0) + index }, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${index + 1}. ${stage.name}${if (stage.isOptional) " (opcional)" else ""}", modifier = Modifier.weight(1f))
                                    Text(stage.formattedDuration(), color = LightTextSecondary)
                                }
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

    if (showTemplatePicker) {
        AlertDialog(
            onDismissRequest = { showTemplatePicker = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("Elegir plantilla", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableTemplates, key = { it.first.id }) { (template, isBuiltIn) ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                title = template.title
                                description = template.description
                                category = template.category
                                stages = template.stages.map { it.copy(id = UUID.randomUUID().toString()) }
                                isTemplate = false
                                errorMessage = null
                                hasStartedSetup = true
                                showTemplatePicker = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = LightSurfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(template.title, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = if (isBuiltIn) "Integrada" else "Tu plantilla",
                                        style = MaterialTheme.typography.labelSmall.copy(color = PrimaryBlue)
                                    )
                                }
                                Text(
                                    text = template.description.ifBlank { template.category },
                                    style = MaterialTheme.typography.bodySmall.copy(color = LightTextSecondary),
                                    maxLines = 2
                                )
                                Text(
                                    text = "${template.formattedTotalDuration()} • ${template.stages.size} etapas",
                                    style = MaterialTheme.typography.labelSmall.copy(color = LightTextSecondary)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTemplatePicker = false }) { Text("Cancelar") }
            }
        )
    }

    when (val loadedDraft = draftRead) {
        is EditorDraftRead.Found -> if (draftPromptVisible) {
            EditorDraftRecoveryDialog(
                itemType = "actividad",
                onContinue = {
                    loadedDraft.value.let { draft ->
                        title = draft.title
                        description = draft.description
                        category = draft.category
                        isTemplate = draft.isTemplate
                        stages = draft.stages
                        hasStartedSetup = draft.hasStartedSetup
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
