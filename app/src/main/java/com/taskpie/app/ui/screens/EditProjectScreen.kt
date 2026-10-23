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
import com.taskpie.app.model.ProjectItem
import com.taskpie.app.model.RoutineItem
import com.taskpie.app.data.local.EditorDraftRead
import com.taskpie.app.data.local.ProjectEditorDraft
import com.taskpie.app.ui.components.StartSessionDialog
import com.taskpie.app.ui.components.CreationStepIndicator
import com.taskpie.app.ui.components.EditorDraftRecoveryDialog
import com.taskpie.app.ui.components.InvalidEditorDraftDialog
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
fun EditProjectScreen(
    viewModel: TaskPieViewModel,
    projectId: String?,
    onNavigateBack: () -> Unit,
    onNavigateToActiveSession: () -> Unit
) {
    val projects by viewModel.projects.collectAsState()
    val activities by viewModel.activities.collectAsState()
    val routines by viewModel.routines.collectAsState()

    val existingProject = remember(projectId, projects) {
        projects.find { it.id == projectId }
    }

    val draftKey = remember(projectId) { viewModel.editorDraftKey("project", projectId) }
    val draftRead = remember(draftKey) { viewModel.readProjectDraft(draftKey) }
    var draftPromptVisible by remember(draftKey) {
        mutableStateOf(draftRead is EditorDraftRead.Found)
    }
    var draftReadyToPersist by remember(draftKey) {
        mutableStateOf(draftRead is EditorDraftRead.Missing)
    }
    var invalidDraftVisible by remember(draftKey) { mutableStateOf(false) }

    var title by remember(existingProject) { mutableStateOf(existingProject?.title ?: "") }
    var description by remember(existingProject) { mutableStateOf(existingProject?.description ?: "") }
    var selectedActivityIds by remember(existingProject) {
        mutableStateOf(existingProject?.activityIds?.toSet() ?: emptySet())
    }
    val baseProjectRoutines = remember(existingProject, routines) {
        if (!existingProject?.projectRoutines.isNullOrEmpty()) {
            existingProject!!.projectRoutines
        } else if (!existingProject?.routineIds.isNullOrEmpty()) {
            existingProject!!.routineIds.mapNotNull { rId ->
                val routine = routines.find { it.id == rId } ?: return@mapNotNull null
                com.taskpie.app.model.ProjectRoutineItem(
                    routineId = routine.id,
                    title = routine.title,
                    steps = routine.steps.map { step -> step.copy(stages = step.stages.map { it.copy() }) },
                    isCustomized = false
                )
            }
        } else {
            emptyList()
        }
    }
    var projectRoutines by remember(existingProject, routines) { mutableStateOf(baseProjectRoutines) }
    var selectedProjectRoutineIds by remember { mutableStateOf(setOf<String>()) }
    var selectedLinkedActivityIds by remember { mutableStateOf(setOf<String>()) }

    var expandedProjectRoutineIds by remember { mutableStateOf(setOf<String>()) }
    var isSelectingRoutineForSequence by remember { mutableStateOf(false) }
    var routineSearchQuery by remember { mutableStateOf("") }
    var activitySearchQuery by remember { mutableStateOf("") }
    var activityToStart by remember { mutableStateOf<ActivityItem?>(null) }
    var routineToStart by remember { mutableStateOf<RoutineItem?>(null) }
    var isStartingFullProject by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var creationStep by remember(projectId) { mutableIntStateOf(if (projectId != null) 1 else 0) }
    val editorListState = rememberLazyListState()
    var reviewScrollTarget by remember { mutableIntStateOf(-1) }
    val filteredRoutines = remember(routines, routineSearchQuery) {
        if (routineSearchQuery.isBlank()) routines
        else routines.filter { it.title.contains(routineSearchQuery.trim(), ignoreCase = true) }
    }
    val filteredActivities = remember(activities, activitySearchQuery) {
        if (activitySearchQuery.isBlank()) activities
        else activities.filter { it.title.contains(activitySearchQuery.trim(), ignoreCase = true) }
    }
    val baseDraft = remember(existingProject, baseProjectRoutines) {
        existingProject?.let {
            ProjectEditorDraft(
                title = it.title,
                description = it.description,
                selectedActivityIds = it.activityIds.sorted(),
                projectRoutines = baseProjectRoutines,
                creationStep = 1
            )
        }
    }

    LaunchedEffect(draftKey, draftRead) {
        if (draftRead is EditorDraftRead.Corrupt) {
            viewModel.deleteEditorDraft(draftKey)
            invalidDraftVisible = true
            draftReadyToPersist = true
        }
    }

    val currentDraft = ProjectEditorDraft(title, description, selectedActivityIds.sorted(), projectRoutines, creationStep)
    val history = remember(draftKey) { EditorUndoHistory(currentDraft.copy(creationStep = 0)) }
    LaunchedEffect(draftKey, draftReadyToPersist, draftPromptVisible) {
        if (!draftReadyToPersist || draftPromptVisible) return@LaunchedEffect
        snapshotFlow {
            ProjectEditorDraft(title, description, selectedActivityIds.sorted(), projectRoutines, 0)
        }.drop(1).collectLatest { snapshot ->
            delay(500)
            history.record(snapshot)
        }
    }
    fun restoreHistory(snapshot: ProjectEditorDraft) {
        title = snapshot.title
        description = snapshot.description
        selectedActivityIds = snapshot.selectedActivityIds.toSet()
        projectRoutines = snapshot.projectRoutines
    }
    fun moveProjectRoutine(from: Int, to: Int) {
        if (from !in projectRoutines.indices || to !in projectRoutines.indices) return
        val reordered = projectRoutines.toMutableList()
        reordered.add(to, reordered.removeAt(from))
        projectRoutines = reordered
    }
    LaunchedEffect(creationStep, reviewScrollTarget, errorMessage, activitySearchQuery) {
        if (creationStep == 1 && reviewScrollTarget >= 0) {
            delay(100)
            editorListState.animateScrollToItem(reviewScrollTarget.coerceAtMost(editorListState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0))
            reviewScrollTarget = -1
        }
    }
    val hasDraftChanges = if (projectId == null) {
        title.isNotBlank() || description.isNotBlank() || selectedActivityIds.isNotEmpty() || projectRoutines.isNotEmpty()
    } else {
        baseDraft != null && currentDraft != baseDraft
    }

    LaunchedEffect(draftKey, title, description, selectedActivityIds, projectRoutines, creationStep, draftReadyToPersist, hasDraftChanges, existingProject) {
        if (!draftReadyToPersist || (projectId != null && existingProject == null)) return@LaunchedEffect
        delay(700)
        if (hasDraftChanges) viewModel.saveEditorDraft(draftKey, currentDraft)
        else viewModel.deleteEditorDraft(draftKey)
    }

    val totalSequenceSeconds = remember(projectRoutines) {
        projectRoutines.sumOf { pr ->
            pr.steps.sumOf { st ->
                st.stages.filter { it.isEnabled }.sumOf { it.durationSeconds }
            }
        }
    }
    val totalSequenceMinutes = totalSequenceSeconds / 60

    BackHandler(enabled = creationStep > 0) {
        creationStep -= 1
        errorMessage = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (existingProject != null) "Editar Proyecto" else "Nuevo Proyecto",
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
                                    errorMessage = "Ingresa un título para el proyecto"
                                } else {
                                    errorMessage = null
                                    creationStep = 1
                                }
                                1 -> {
                                    errorMessage = null
                                    creationStep = 2
                                }
                                else -> {
                                    if (title.isBlank()) {
                                        creationStep = 0
                                        errorMessage = "Ingresa un título para el proyecto"
                                        return@Button
                                    }
                                    val item = ProjectItem(
                                        id = existingProject?.id ?: UUID.randomUUID().toString(),
                                        title = title.trim(),
                                        description = description.trim(),
                                        activityIds = selectedActivityIds.toList(),
                                        routineIds = projectRoutines.map { it.routineId },
                                        projectRoutines = projectRoutines,
                                        createdAt = existingProject?.createdAt ?: System.currentTimeMillis()
                                    )
                                    viewModel.deleteEditorDraft(draftKey)
                                    viewModel.saveProject(item)
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
                            text = "Detalles del Proyecto",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        OutlinedTextField(
                            value = title,
                            onValueChange = {
                                title = it
                                errorMessage = null
                            },
                            label = { Text("Título del proyecto") },
                            placeholder = { Text("Ej: Novela de Ciencia Ficción, Tesis") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = LightBorder),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Descripción / Objetivos") },
                            placeholder = { Text("Ej: Redacción de 10 capítulos y revisión completa") },
                            minLines = 2,
                            maxLines = 3,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = LightBorder),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (existingProject != null && projectRoutines.isNotEmpty()) {
                            FilledTonalButton(
                                onClick = { isStartingFullProject = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Lanzar Secuencia Completa ($totalSequenceMinutes min)", fontWeight = FontWeight.Bold)
                            }
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
            // Routine Sequence Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Secuencia de Rutinas",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${projectRoutines.size} rutinas ordenadas • Total: $totalSequenceMinutes min",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 12.sp,
                                color = LightTextSecondary
                            )
                        )
                    }

                    FilledTonalButton(
                        onClick = { isSelectingRoutineForSequence = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Añadir Rutina", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (selectedProjectRoutineIds.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = {
                            projectRoutines = projectRoutines.filterNot { it.routineId in selectedProjectRoutineIds }
                            selectedProjectRoutineIds = emptySet()
                        }) { Text("Quitar ${selectedProjectRoutineIds.size} seleccionadas") }
                    }
                }
            }

            // Routine Sequence Items
            if (projectRoutines.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isSelectingRoutineForSequence = true },
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
                                imageVector = Icons.Default.Repeat,
                                contentDescription = null,
                                tint = AccentIndigo,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No hay rutinas en la secuencia del proyecto",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Añade una rutina. Puedes personalizar etapas sin afectar la rutina original.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = LightTextSecondary),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(projectRoutines) { index, projectRoutine ->
                    val routineKey = "${projectRoutine.routineId}_$index"
                    val isExpanded = expandedProjectRoutineIds.contains(routineKey)
                    val activeStagesCount = projectRoutine.steps.sumOf { st -> st.stages.count { it.isEnabled } }
                    val totalStagesCount = projectRoutine.steps.sumOf { it.stages.size }
                    val activeDurationSeconds = projectRoutine.steps.sumOf { st ->
                        st.stages.filter { it.isEnabled }.sumOf { it.durationSeconds }
                    }
                    val durationMins = activeDurationSeconds / 60

                    Card(
                        modifier = Modifier.fillMaxWidth().combinedClickable(
                            onClick = {
                                if (selectedProjectRoutineIds.isNotEmpty()) {
                                    val id = projectRoutine.routineId
                                    selectedProjectRoutineIds = if (id in selectedProjectRoutineIds) selectedProjectRoutineIds - id else selectedProjectRoutineIds + id
                                }
                            },
                            onLongClick = {
                                val id = projectRoutine.routineId
                                selectedProjectRoutineIds = if (id in selectedProjectRoutineIds) selectedProjectRoutineIds - id else selectedProjectRoutineIds + id
                            }
                        ),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = LightSurface),
                        border = BorderStroke(1.dp, LightBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (selectedProjectRoutineIds.isNotEmpty()) {
                                    Checkbox(
                                        checked = projectRoutine.routineId in selectedProjectRoutineIds,
                                        onCheckedChange = {
                                            val id = projectRoutine.routineId
                                            selectedProjectRoutineIds = if (id in selectedProjectRoutineIds) selectedProjectRoutineIds - id else selectedProjectRoutineIds + id
                                        }
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Reorder buttons
                                    Column {
                                        IconButton(
                                            onClick = {
                                                if (index > 0) {
                                                    val list = projectRoutines.toMutableList()
                                                    val item = list.removeAt(index)
                                                    list.add(index - 1, item)
                                                    projectRoutines = list
                                                }
                                            },
                                            enabled = index > 0,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowUp,
                                                contentDescription = "Subir",
                                                tint = if (index > 0) LightTextPrimary else LightTextTertiary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                if (index < projectRoutines.size - 1) {
                                                    val list = projectRoutines.toMutableList()
                                                    val item = list.removeAt(index)
                                                    list.add(index + 1, item)
                                                    projectRoutines = list
                                                }
                                            },
                                            enabled = index < projectRoutines.size - 1,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Bajar",
                                                tint = if (index < projectRoutines.size - 1) LightTextPrimary else LightTextTertiary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    DragReorderHandle(index, projectRoutines.size, ::moveProjectRoutine, Modifier.size(32.dp))

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Number badge
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(AccentIndigoLight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "#${index + 1}",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = AccentIndigo,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = projectRoutine.title,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp
                                                )
                                            )
                                            if (projectRoutine.isCustomized) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Personalizada",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AccentAmber,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(AccentAmber.copy(alpha = 0.15f))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${projectRoutine.steps.size} actividades • $durationMins min",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 11.sp,
                                                color = LightTextSecondary
                                            )
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Duplicate button
                                    IconButton(
                                        onClick = {
                                            val list = projectRoutines.toMutableList()
                                            list.add(
                                                index + 1,
                                                projectRoutine.copy(
                                                    steps = projectRoutine.steps.map { st ->
                                                        st.copy(stages = st.stages.map { s -> s.copy() })
                                                    }
                                                )
                                            )
                                            projectRoutines = list
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Repetir rutina",
                                            tint = LightTextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Delete button
                                    IconButton(
                                        onClick = {
                                            val list = projectRoutines.toMutableList()
                                            list.removeAt(index)
                                            projectRoutines = list
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Quitar",
                                            tint = AccentCoral,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Accordion Toggle for Activities & Stages in this Routine
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedProjectRoutineIds = if (isExpanded) {
                                            expandedProjectRoutineIds - routineKey
                                        } else {
                                            expandedProjectRoutineIds + routineKey
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isExpanded) "Ocultar contenido" else "Personalizar actividades ($activeStagesCount/$totalStagesCount etapas)",
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

                            // Expanded Content: Activities & Stages inside this routine
                            if (isExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(LightBackground)
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (projectRoutine.isCustomized) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    val originalRout = routines.find { it.id == projectRoutine.routineId }
                                                    if (originalRout != null) {
                                                        val list = projectRoutines.toMutableList()
                                                        list[index] = projectRoutine.copy(
                                                            steps = originalRout.steps.map { st ->
                                                                st.copy(stages = st.stages.map { s -> s.copy(isEnabled = true) })
                                                            },
                                                            isCustomized = false
                                                        )
                                                        projectRoutines = list
                                                    }
                                                },
                                                contentPadding = PaddingValues(0.dp),
                                                modifier = Modifier.height(24.dp)
                                            ) {
                                                Text("Restablecer a rutina original", fontSize = 10.sp, color = AccentAmber)
                                            }
                                        }
                                    }

                                    projectRoutine.steps.forEachIndexed { stepIdx, step ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(containerColor = LightSurface),
                                            border = BorderStroke(1.dp, LightBorder.copy(alpha = 0.5f))
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text(
                                                    text = step.activityTitle,
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 12.sp
                                                    )
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                step.stages.forEachIndexed { stageIdx, stage ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .clickable {
                                                                val nextEnabled = !stage.isEnabled
                                                                val newStages = step.stages.toMutableList()
                                                                newStages[stageIdx] = stage.copy(isEnabled = nextEnabled)
                                                                val newSteps = projectRoutine.steps.toMutableList()
                                                                newSteps[stepIdx] = step.copy(stages = newStages)
                                                                val list = projectRoutines.toMutableList()
                                                                list[index] = projectRoutine.copy(steps = newSteps, isCustomized = true)
                                                                projectRoutines = list
                                                            }
                                                            .padding(vertical = 3.dp, horizontal = 4.dp),
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
                                                                    val newSteps = projectRoutine.steps.toMutableList()
                                                                    newSteps[stepIdx] = step.copy(stages = newStages)
                                                                    val list = projectRoutines.toMutableList()
                                                                    list[index] = projectRoutine.copy(steps = newSteps, isCustomized = true)
                                                                    projectRoutines = list
                                                                },
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(8.dp)
                                                                    .clip(RoundedCornerShape(2.dp))
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
                                                                fontSize = 11.sp,
                                                                color = if (stage.isEnabled) LightTextPrimary else LightTextTertiary
                                                            )
                                                        }
                                                        val sMins = stage.durationSeconds / 60
                                                        val sSecs = stage.durationSeconds % 60
                                                        Text(
                                                            text = if (sSecs > 0) "${sMins}m ${sSecs}s" else "${sMins}m",
                                                            fontSize = 10.sp,
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
                }
            }

            // Linked Activities Section (Optional Individual Activities)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    Text(
                        text = "Accesos individuales (opcional)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Puedes iniciar estas actividades por separado; no forman parte de la secuencia completa.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 12.sp,
                            color = LightTextSecondary
                        ),
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                    )
                    if (selectedLinkedActivityIds.isNotEmpty()) {
                        TextButton(onClick = {
                            selectedActivityIds = selectedActivityIds - selectedLinkedActivityIds
                            selectedLinkedActivityIds = emptySet()
                        }) { Text("Desvincular ${selectedLinkedActivityIds.size} seleccionadas") }
                    }
                    OutlinedTextField(
                        value = activitySearchQuery,
                        onValueChange = { activitySearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Buscar actividad vinculada") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (activitySearchQuery.isNotEmpty()) IconButton(onClick = { activitySearchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda")
                            }
                        }
                    )
                }
            }

            if (activities.isEmpty()) {
                item { Text("Todavía no hay actividades para vincular.", color = LightTextSecondary) }
            } else if (filteredActivities.isEmpty()) {
                item { Text("No se encontraron actividades con ese nombre.", color = LightTextSecondary) }
            }

            items(filteredActivities) { activity ->
                val isSelected = selectedActivityIds.contains(activity.id)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                if (selectedLinkedActivityIds.isNotEmpty()) {
                                    selectedLinkedActivityIds = if (activity.id in selectedLinkedActivityIds) selectedLinkedActivityIds - activity.id else selectedLinkedActivityIds + activity.id
                                } else {
                            selectedActivityIds = if (isSelected) {
                                selectedActivityIds - activity.id
                            } else {
                                selectedActivityIds + activity.id
                            }
                                }
                            },
                            onLongClick = {
                                selectedLinkedActivityIds = if (activity.id in selectedLinkedActivityIds) selectedLinkedActivityIds - activity.id else selectedLinkedActivityIds + activity.id
                            }
                        ),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) PrimaryBlueLight else LightSurface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (selectedLinkedActivityIds.isNotEmpty()) {
                            Checkbox(
                                checked = activity.id in selectedLinkedActivityIds,
                                onCheckedChange = {
                                    selectedLinkedActivityIds = if (activity.id in selectedLinkedActivityIds) selectedLinkedActivityIds - activity.id else selectedLinkedActivityIds + activity.id
                                }
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = {
                                    selectedActivityIds = if (it) {
                                        selectedActivityIds + activity.id
                                    } else {
                                        selectedActivityIds - activity.id
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = activity.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                )
                                Text(
                                    text = "${activity.stages.size} etapas • ${activity.formattedTotalDuration()}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 11.sp,
                                        color = LightTextSecondary
                                    )
                                )
                            }
                        }

                        if (existingProject != null && isSelected) {
                            FilledTonalButton(
                                onClick = { activityToStart = activity },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Lanzar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            }
            if (creationStep == 2) {
                item {
                    val totalMins = totalSequenceSeconds / 60
                    val totalSecs = totalSequenceSeconds % 60
                    val durationLabel = if (totalSecs > 0) "${totalMins}m ${totalSecs}s" else "${totalMins}m"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = LightSurface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Revisar proyecto", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.clickable { creationStep = 1; reviewScrollTarget = 1 })
                            Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.clickable { creationStep = 0 })
                            if (description.isNotBlank()) Text(description, color = LightTextSecondary, modifier = Modifier.clickable { creationStep = 0 })
                            Text("${projectRoutines.size} rutinas • $durationLabel de secuencia")
                            if (projectRoutines.isEmpty()) {
                                Text("Todavía no hay rutinas. Puedes guardar el proyecto y completarlo después.", color = LightTextSecondary)
                            } else {
                                projectRoutines.forEachIndexed { index, routine ->
                                    val includedStages = routine.steps.sumOf { step -> step.stages.count { it.isEnabled } }
                                    val allStages = routine.steps.sumOf { it.stages.size }
                                    Text("${index + 1}. ${routine.title}", fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth().clickable {
                                        creationStep = 1
                                        reviewScrollTarget = 2 + (if (errorMessage != null) 1 else 0) + index
                                    })
                                    routine.steps.forEach { step ->
                                        val active = step.stages.count { it.isEnabled }
                                        Text(
                                            text = "  • ${step.activityTitle}: $active de ${step.stages.size} etapas",
                                            style = MaterialTheme.typography.bodySmall.copy(color = LightTextSecondary),
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                creationStep = 1
                                                reviewScrollTarget = 2 + (if (errorMessage != null) 1 else 0) + projectRoutines.indexOf(routine)
                                            }
                                        )
                                    }
                                    if (includedStages < allStages) {
                                        Text("  $includedStages de $allStages etapas incluidas", style = MaterialTheme.typography.labelSmall.copy(color = LightTextSecondary))
                                    }
                                }
                            }
                            HorizontalDivider()
                            Text("Actividades vinculadas (${selectedActivityIds.size})", fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth().clickable {
                                creationStep = 1
                                reviewScrollTarget = 2 + (if (errorMessage != null) 1 else 0) + maxOf(projectRoutines.size, 1)
                            })
                            Text("Se pueden iniciar por separado y no se suman a la secuencia.", style = MaterialTheme.typography.bodySmall.copy(color = LightTextSecondary))
                            val linkedActivities = activities.filter { it.id in selectedActivityIds }
                            if (linkedActivities.isEmpty()) {
                                Text("Ninguna actividad vinculada", style = MaterialTheme.typography.bodySmall.copy(color = LightTextSecondary))
                            } else {
                                linkedActivities.forEach { activity ->
                                    Text("• ${activity.title} — ${activity.formattedTotalDuration()}", modifier = Modifier.fillMaxWidth().clickable {
                                        activitySearchQuery = ""
                                        creationStep = 1
                                        val activityIndex = activities.indexOfFirst { it.id == activity.id }.coerceAtLeast(0)
                                        reviewScrollTarget = 3 + (if (errorMessage != null) 1 else 0) + maxOf(projectRoutines.size, 1) + activityIndex
                                    })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal to pick a routine to append to sequence
    if (isSelectingRoutineForSequence) {
        AlertDialog(
            onDismissRequest = { isSelectingRoutineForSequence = false },
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Añadir Rutina a la Secuencia",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = routineSearchQuery,
                        onValueChange = { routineSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Buscar rutina") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (routineSearchQuery.isNotEmpty()) IconButton(onClick = { routineSearchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda")
                            }
                        }
                    )
                    if (routines.isEmpty()) {
                        Text("Todavía no hay rutinas para añadir.", color = LightTextSecondary)
                    } else if (filteredRoutines.isEmpty()) {
                        Text("No se encontraron rutinas con ese nombre.", color = LightTextSecondary)
                    } else LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                    items(filteredRoutines) { routine ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newPR = com.taskpie.app.model.ProjectRoutineItem(
                                        routineId = routine.id,
                                        title = routine.title,
                                        steps = routine.steps.map { st ->
                                            st.copy(stages = st.stages.map { s -> s.copy(isEnabled = true) })
                                        },
                                        isCustomized = false
                                    )
                                    projectRoutines = projectRoutines + newPR
                                    isSelectingRoutineForSequence = false
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = LightSurfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = routine.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "${routine.steps.size} actividades • ${routine.formattedTotalDuration()}",
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
                TextButton(onClick = { isSelectingRoutineForSequence = false }) {
                    Text("Cancelar", color = LightTextSecondary)
                }
            }
        )
    }

    // Start Full Project Dialog
    if (isStartingFullProject && existingProject != null) {
        val proj = existingProject.copy(
            title = title,
            description = description,
            routineIds = projectRoutines.map { it.routineId },
            projectRoutines = projectRoutines,
            activityIds = selectedActivityIds.toList()
        )
        StartSessionDialog(
            defaultTitle = proj.title,
            totalDurationFormatted = "${totalSequenceMinutes}m",
            initialVoiceEnabled = viewModel.getVoiceEnabledPreference(),
            initialRepeatCount = proj.defaultRepeatCount,
            initialCycleBreakSeconds = proj.defaultCycleBreakSeconds,
            onDismiss = { isStartingFullProject = false },
            onStart = { customName, voiceEnabled, repeatCount, cycleBreakSeconds ->
                viewModel.startProjectSession(proj, customName, voiceEnabled, repeatCount, cycleBreakSeconds)
                isStartingFullProject = false
                onNavigateToActiveSession()
            }
        )
    }

    // Start Individual Activity Dialog
    activityToStart?.let { activity ->
        StartSessionDialog(
            defaultTitle = "${title}: ${activity.title}",
            totalDurationFormatted = activity.formattedTotalDuration(),
            initialVoiceEnabled = viewModel.getVoiceEnabledPreference(),
            onDismiss = { activityToStart = null },
            onStart = { customName, voiceEnabled, repeatCount, cycleBreakSeconds ->
                viewModel.startProjectActivitySession(title, activity, customName, voiceEnabled, repeatCount, cycleBreakSeconds)
                activityToStart = null
                onNavigateToActiveSession()
            }
        )
    }

    // Start Routine Dialog
    routineToStart?.let { routine ->
        StartSessionDialog(
            defaultTitle = "${title}: ${routine.title}",
            totalDurationFormatted = routine.formattedTotalDuration(),
            initialVoiceEnabled = viewModel.getVoiceEnabledPreference(),
            initialRepeatCount = routine.defaultRepeatCount,
            initialCycleBreakSeconds = routine.defaultCycleBreakSeconds,
            onDismiss = { routineToStart = null },
            onStart = { customName, voiceEnabled, repeatCount, cycleBreakSeconds ->
                viewModel.startRoutineSession(routine, customName, voiceEnabled, repeatCount, cycleBreakSeconds)
                routineToStart = null
                onNavigateToActiveSession()
            }
        )
    }

    when (val loadedDraft = draftRead) {
        is EditorDraftRead.Found -> if (draftPromptVisible) {
            EditorDraftRecoveryDialog(
                itemType = "proyecto",
                onContinue = {
                    loadedDraft.value.let { draft ->
                        title = draft.title
                        description = draft.description
                        selectedActivityIds = draft.selectedActivityIds.toSet()
                        projectRoutines = draft.projectRoutines
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
