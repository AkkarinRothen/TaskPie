package com.taskpie.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskpie.app.model.ActivityItem
import com.taskpie.app.model.FolderItem
import com.taskpie.app.model.ProjectItem
import com.taskpie.app.model.RoutineItem
import com.taskpie.app.service.TimerState
import com.taskpie.app.service.TimerStatus
import com.taskpie.app.ui.components.*
import com.taskpie.app.ui.theme.*
import com.taskpie.app.ui.viewmodel.TaskPieViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TaskPieViewModel,
    onNavigateToActiveSession: () -> Unit,
    onNavigateToCreateActivity: () -> Unit,
    onNavigateToEditActivity: (activityId: String) -> Unit,
    onNavigateToCreateRoutine: () -> Unit,
    onNavigateToEditRoutine: (routineId: String) -> Unit,
    onNavigateToCreateProject: () -> Unit,
    onNavigateToEditProject: (projectId: String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Actividades", "Rutinas", "Proyectos", "Historial")

    val folders by viewModel.folders.collectAsState()
    val activities by viewModel.activities.collectAsState()
    val routines by viewModel.routines.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val history by viewModel.sessionHistory.collectAsState()
    val timerState by viewModel.timerState.collectAsState()

    var activityToStart by remember { mutableStateOf<ActivityItem?>(null) }
    var routineToStart by remember { mutableStateOf<RoutineItem?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf("Todas") }

    // Search state
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Folder Dialog state
    var showFolderDialog by remember { mutableStateOf(false) }
    var folderToEdit by remember { mutableStateOf<FolderItem?>(null) }

    // Keep Screen On state
    var keepScreenOnEnabled by remember { mutableStateOf(true) }
    if (keepScreenOnEnabled && (timerState.status == TimerStatus.RUNNING || timerState.status == TimerStatus.PAUSED)) {
        KeepScreenOn(enabled = true)
    }

    // Filtered items
    val filteredActivities = remember(activities, selectedCategoryFilter, searchQuery) {
        activities.filter { activity ->
            val matchesFolder = (selectedCategoryFilter == "Todas") || (activity.category.equals(selectedCategoryFilter, ignoreCase = true))
            val matchesSearch = searchQuery.isBlank() ||
                activity.title.contains(searchQuery, ignoreCase = true) ||
                activity.description.contains(searchQuery, ignoreCase = true) ||
                activity.stages.any { it.name.contains(searchQuery, ignoreCase = true) }
            matchesFolder && matchesSearch
        }
    }

    val filteredRoutines = remember(routines, searchQuery) {
        routines.filter { routine ->
            searchQuery.isBlank() ||
                routine.title.contains(searchQuery, ignoreCase = true) ||
                routine.description.contains(searchQuery, ignoreCase = true) ||
                routine.steps.any { it.activityTitle.contains(searchQuery, ignoreCase = true) }
        }
    }

    val filteredProjects = remember(projects, searchQuery) {
        projects.filter { project ->
            searchQuery.isBlank() ||
                project.title.contains(searchQuery, ignoreCase = true) ||
                project.description.contains(searchQuery, ignoreCase = true)
        }
    }

    // Selected items for Master-Detail tablet preview
    var selectedActivityId by remember { mutableStateOf<String?>(null) }
    var selectedRoutineId by remember { mutableStateOf<String?>(null) }
    var selectedProjectId by remember { mutableStateOf<String?>(null) }

    var showStopConfirmation by remember { mutableStateOf(false) }

    // Auto-select first item if available and none selected
    LaunchedEffect(filteredActivities) {
        if ((selectedActivityId == null || filteredActivities.none { it.id == selectedActivityId }) && filteredActivities.isNotEmpty()) {
            selectedActivityId = filteredActivities.first().id
        }
    }
    LaunchedEffect(filteredRoutines) {
        if ((selectedRoutineId == null || filteredRoutines.none { it.id == selectedRoutineId }) && filteredRoutines.isNotEmpty()) {
            selectedRoutineId = filteredRoutines.first().id
        }
    }
    LaunchedEffect(filteredProjects) {
        if ((selectedProjectId == null || filteredProjects.none { it.id == selectedProjectId }) && filteredProjects.isNotEmpty()) {
            selectedProjectId = filteredProjects.first().id
        }
    }

    // Session Summary Dialog when session completes
    if (timerState.status == TimerStatus.COMPLETED && timerState.completedSummary != null) {
        SessionSummaryDialog(
            summary = timerState.completedSummary!!,
            onDismiss = {
                viewModel.stopSession()
            }
        )
    }

    Scaffold(
        topBar = {
            if (isSearchActive) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Buscar actividades, rutinas...", fontSize = 14.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Limpiar", modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = LightBorder,
                                focusedBorderColor = PrimaryBlue
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSearchActive = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cerrar búsqueda")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = LightBackground
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PrimaryBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "TP",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "TaskPie",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Temporizador por etapas",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 11.sp,
                                        color = LightTextSecondary
                                    )
                                )
                            }
                        }
                    },
                    actions = {
                        // Search Button
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar",
                                tint = LightTextPrimary
                            )
                        }

                        // Keep Screen On Toggle
                        IconButton(onClick = { keepScreenOnEnabled = !keepScreenOnEnabled }) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = if (keepScreenOnEnabled) "Pantalla siempre encendida (Activo)" else "Pantalla siempre encendida (Inactivo)",
                                tint = if (keepScreenOnEnabled) AccentAmber else LightTextSecondary.copy(alpha = 0.4f)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = LightBackground
                    )
                )
            }
        },
        floatingActionButton = {
            when (selectedTab) {
                0 -> {
                    FloatingActionButton(
                        onClick = onNavigateToCreateActivity,
                        containerColor = PrimaryBlue,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Nueva Actividad", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                1 -> {
                    FloatingActionButton(
                        onClick = onNavigateToCreateRoutine,
                        containerColor = PrimaryBlue,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Nueva Rutina", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                2 -> {
                    FloatingActionButton(
                        onClick = onNavigateToCreateProject,
                        containerColor = PrimaryBlue,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Nuevo Proyecto", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        },
        containerColor = LightBackground
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val isWideScreen = maxWidth >= 700.dp
            val isSessionActive = timerState.status != TimerStatus.IDLE && timerState.status != TimerStatus.COMPLETED

            if (isWideScreen) {
                // ==========================================
                // MASTER-DETAIL TABLET LAYOUT (2 COLUMNS)
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // LEFT COLUMN (List & Tabs)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        // Tabs Row
                        ScrollableTabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = LightBackground,
                            contentColor = PrimaryBlue,
                            edgePadding = 0.dp,
                            divider = { HorizontalDivider(color = LightBorder) }
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    text = {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Folder Chips for Activities
                        if (selectedTab == 0) {
                            FolderChipsBar(
                                folders = folders,
                                selectedFolder = selectedCategoryFilter,
                                onSelectFolder = { selectedCategoryFilter = it },
                                onNewFolderClick = {
                                    folderToEdit = null
                                    showFolderDialog = true
                                },
                                onEditFolderClick = { folder ->
                                    folderToEdit = folder
                                    showFolderDialog = true
                                },
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        // Left Column List Content
                        Box(modifier = Modifier.weight(1f)) {
                            when (selectedTab) {
                                0 -> {
                                    if (filteredActivities.isEmpty()) {
                                        if (searchQuery.isNotBlank()) {
                                            EmptyTabPlaceholder(
                                                title = "Sin resultados",
                                                description = "No se encontraron actividades para \"$searchQuery\"",
                                                buttonText = "Limpiar búsqueda",
                                                onButtonClick = { searchQuery = "" }
                                            )
                                        } else {
                                            EmptyTabPlaceholder(
                                                title = if (selectedCategoryFilter != "Todas") "Carpeta vacía" else "No hay actividades",
                                                description = if (selectedCategoryFilter != "Todas") "No hay actividades en '$selectedCategoryFilter'" else "Crea tu primera actividad con etapas para comenzar",
                                                buttonText = "Crear Actividad",
                                                onButtonClick = onNavigateToCreateActivity
                                            )
                                        }
                                    } else {
                                        LazyColumn(
                                            contentPadding = PaddingValues(bottom = 80.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            items(filteredActivities, key = { it.id }) { activity ->
                                                val isSelected = activity.id == selectedActivityId
                                                ActivityCard(
                                                    activity = activity,
                                                    isSelected = isSelected,
                                                    onCardClick = { selectedActivityId = activity.id },
                                                    onStartClick = { activityToStart = activity },
                                                    onEditClick = { onNavigateToEditActivity(activity.id) },
                                                    onDuplicateClick = { viewModel.duplicateActivity(activity) },
                                                    onDeleteClick = { viewModel.deleteActivity(activity.id) }
                                                )
                                            }
                                        }
                                    }
                                }
                                1 -> {
                                    if (filteredRoutines.isEmpty()) {
                                        if (searchQuery.isNotBlank()) {
                                            EmptyTabPlaceholder(
                                                title = "Sin resultados",
                                                description = "No se encontraron rutinas para \"$searchQuery\"",
                                                buttonText = "Limpiar búsqueda",
                                                onButtonClick = { searchQuery = "" }
                                            )
                                        } else {
                                            EmptyTabPlaceholder(
                                                title = "No hay rutinas",
                                                description = "Encadena varias actividades en secuencia (ej: Escritura → Descanso → Limpiar)",
                                                buttonText = "Crear Rutina",
                                                onButtonClick = onNavigateToCreateRoutine
                                            )
                                        }
                                    } else {
                                        LazyColumn(
                                            contentPadding = PaddingValues(bottom = 80.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            items(filteredRoutines, key = { it.id }) { routine ->
                                                val isSelected = routine.id == selectedRoutineId
                                                RoutineCard(
                                                    routine = routine,
                                                    isSelected = isSelected,
                                                    onCardClick = { selectedRoutineId = routine.id },
                                                    onStartClick = { routineToStart = routine },
                                                    onEditClick = { onNavigateToEditRoutine(routine.id) },
                                                    onDuplicateClick = { viewModel.duplicateRoutine(routine) },
                                                    onDeleteClick = { viewModel.deleteRoutine(routine.id) }
                                                )
                                            }
                                        }
                                    }
                                }
                                2 -> {
                                    if (filteredProjects.isEmpty()) {
                                        if (searchQuery.isNotBlank()) {
                                            EmptyTabPlaceholder(
                                                title = "Sin resultados",
                                                description = "No se encontraron proyectos para \"$searchQuery\"",
                                                buttonText = "Limpiar búsqueda",
                                                onButtonClick = { searchQuery = "" }
                                            )
                                        } else {
                                            EmptyTabPlaceholder(
                                                title = "No hay proyectos",
                                                description = "Agrupa actividades y rutinas en torno a un objetivo",
                                                buttonText = "Crear Proyecto",
                                                onButtonClick = onNavigateToCreateProject
                                            )
                                        }
                                    } else {
                                        LazyColumn(
                                            contentPadding = PaddingValues(bottom = 80.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            items(filteredProjects, key = { it.id }) { project ->
                                                val isSelected = project.id == selectedProjectId
                                                ProjectCard(
                                                    project = project,
                                                    linkedActivitiesCount = project.activityIds.size,
                                                    linkedRoutinesCount = project.routineIds.size,
                                                    isSelected = isSelected,
                                                    onCardClick = { selectedProjectId = project.id },
                                                    onOpenClick = { onNavigateToEditProject(project.id) },
                                                    onEditClick = { onNavigateToEditProject(project.id) },
                                                    onDeleteClick = { viewModel.deleteProject(project.id) }
                                                )
                                            }
                                        }
                                    }
                                }
                                3 -> {
                                    if (history.isEmpty()) {
                                        EmptyTabPlaceholder(
                                            title = "Sin historial aún",
                                            description = "Cuando completes una actividad, verás aquí el resumen detallado",
                                            buttonText = null,
                                            onButtonClick = {}
                                        )
                                    } else {
                                        LazyColumn(
                                            contentPadding = PaddingValues(bottom = 80.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            items(history, key = { it.id }) { summary ->
                                                HistoryCard(
                                                    summary = summary,
                                                    onDelete = { viewModel.deleteSessionSummary(summary.id) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))
                    VerticalDivider(color = LightBorder, modifier = Modifier.fillMaxHeight().padding(vertical = 4.dp))
                    Spacer(modifier = Modifier.width(16.dp))

                    // RIGHT COLUMN (Action Panel: Live Timer OR Detail Preview)
                    Box(
                        modifier = Modifier
                            .weight(1.15f)
                            .fillMaxHeight()
                    ) {
                        if (isSessionActive) {
                            LiveTimerPanel(
                                timerState = timerState,
                                onExpandFullScreen = onNavigateToActiveSession,
                                onStopSession = { showStopConfirmation = true },
                                onTogglePlayPause = {
                                    if (timerState.status == TimerStatus.RUNNING) {
                                        viewModel.pauseTimer()
                                    } else if (timerState.status == TimerStatus.PAUSED) {
                                        viewModel.resumeTimer()
                                    } else if (timerState.status == TimerStatus.WAITING_MANUAL_ADVANCE) {
                                        viewModel.advanceManualNextActivity()
                                    }
                                },
                                onSkipStage = { viewModel.skipCurrentStage() },
                                onAdvanceManual = { viewModel.advanceManualNextActivity() },
                                onToggleVoice = { viewModel.toggleVoice() }
                            )
                        } else {
                            DetailPreviewPanel(
                                selectedActivity = activities.find { it.id == selectedActivityId } ?: activities.firstOrNull(),
                                selectedRoutine = routines.find { it.id == selectedRoutineId } ?: routines.firstOrNull(),
                                selectedProject = projects.find { it.id == selectedProjectId } ?: projects.firstOrNull(),
                                selectedTab = selectedTab,
                                onStartActivity = { act -> activityToStart = act },
                                onStartRoutine = { rt -> routineToStart = rt },
                                onStartProject = { prj -> onNavigateToEditProject(prj.id) },
                                onEditActivity = onNavigateToEditActivity,
                                onEditRoutine = onNavigateToEditRoutine,
                                onEditProject = onNavigateToEditProject
                            )
                        }
                    }
                }
            } else {
                // ==========================================
                // COMPACT SINGLE-COLUMN LAYOUT (Phones / Portrait)
                // ==========================================
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Active Session Mini Banner
                    AnimatedVisibility(visible = isSessionActive) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clickable { onNavigateToActiveSession() },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = PrimaryBlueLight),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (timerState.status == TimerStatus.PAUSED) AccentAmber else AccentEmerald
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = timerState.sessionTitle,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryBlueHover
                                            )
                                        )
                                        Text(
                                            text = "${timerState.currentStage?.name ?: "Etapa"} • ${timerState.formattedRemainingTime()}",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 12.sp,
                                                color = LightTextSecondary
                                            )
                                        )
                                    }
                                }

                                Button(
                                    onClick = onNavigateToActiveSession,
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Ver", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }

                    // Tabs Selector
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = LightBackground,
                        contentColor = PrimaryBlue,
                        edgePadding = 16.dp,
                        divider = { HorizontalDivider(color = LightBorder) }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        maxLines = 1
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tab Content
                    when (selectedTab) {
                        0 -> {
                            FolderChipsBar(
                                folders = folders,
                                selectedFolder = selectedCategoryFilter,
                                onSelectFolder = { selectedCategoryFilter = it },
                                onNewFolderClick = {
                                    folderToEdit = null
                                    showFolderDialog = true
                                },
                                onEditFolderClick = { folder ->
                                    folderToEdit = folder
                                    showFolderDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            if (filteredActivities.isEmpty()) {
                                if (searchQuery.isNotBlank()) {
                                    EmptyTabPlaceholder(
                                        title = "Sin resultados",
                                        description = "No se encontraron actividades para \"$searchQuery\"",
                                        buttonText = "Limpiar búsqueda",
                                        onButtonClick = { searchQuery = "" }
                                    )
                                } else {
                                    EmptyTabPlaceholder(
                                        title = if (selectedCategoryFilter != "Todas") "Carpeta vacía" else "No hay actividades",
                                        description = if (selectedCategoryFilter != "Todas") "No hay actividades en '$selectedCategoryFilter'" else "Crea tu primera actividad con etapas para comenzar",
                                        buttonText = "Crear Actividad",
                                        onButtonClick = onNavigateToCreateActivity
                                    )
                                }
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(filteredActivities, key = { it.id }) { activity ->
                                        ActivityCard(
                                            activity = activity,
                                            onStartClick = { activityToStart = activity },
                                            onEditClick = { onNavigateToEditActivity(activity.id) },
                                            onDuplicateClick = { viewModel.duplicateActivity(activity) },
                                            onDeleteClick = { viewModel.deleteActivity(activity.id) }
                                        )
                                    }
                                }
                            }
                        }
                        1 -> {
                            if (filteredRoutines.isEmpty()) {
                                if (searchQuery.isNotBlank()) {
                                    EmptyTabPlaceholder(
                                        title = "Sin resultados",
                                        description = "No se encontraron rutinas para \"$searchQuery\"",
                                        buttonText = "Limpiar búsqueda",
                                        onButtonClick = { searchQuery = "" }
                                    )
                                } else {
                                    EmptyTabPlaceholder(
                                        title = "No hay rutinas",
                                        description = "Encadena varias actividades en secuencia (ej: Escritura → Descanso → Limpiar)",
                                        buttonText = "Crear Rutina",
                                        onButtonClick = onNavigateToCreateRoutine
                                    )
                                }
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(filteredRoutines, key = { it.id }) { routine ->
                                        RoutineCard(
                                            routine = routine,
                                            onStartClick = { routineToStart = routine },
                                            onEditClick = { onNavigateToEditRoutine(routine.id) },
                                            onDuplicateClick = { viewModel.duplicateRoutine(routine) },
                                            onDeleteClick = { viewModel.deleteRoutine(routine.id) }
                                        )
                                    }
                                }
                            }
                        }
                        2 -> {
                            if (filteredProjects.isEmpty()) {
                                if (searchQuery.isNotBlank()) {
                                    EmptyTabPlaceholder(
                                        title = "Sin resultados",
                                        description = "No se encontraron proyectos para \"$searchQuery\"",
                                        buttonText = "Limpiar búsqueda",
                                        onButtonClick = { searchQuery = "" }
                                    )
                                } else {
                                    EmptyTabPlaceholder(
                                        title = "No hay proyectos",
                                        description = "Agrupa actividades y rutinas en torno a un objetivo",
                                        buttonText = "Crear Proyecto",
                                        onButtonClick = onNavigateToCreateProject
                                    )
                                }
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(filteredProjects, key = { it.id }) { project ->
                                        ProjectCard(
                                            project = project,
                                            linkedActivitiesCount = project.activityIds.size,
                                            linkedRoutinesCount = project.routineIds.size,
                                            onOpenClick = { onNavigateToEditProject(project.id) },
                                            onEditClick = { onNavigateToEditProject(project.id) },
                                            onDeleteClick = { viewModel.deleteProject(project.id) }
                                        )
                                    }
                                }
                            }
                        }
                        3 -> {
                            if (history.isEmpty()) {
                                EmptyTabPlaceholder(
                                    title = "Sin historial aún",
                                    description = "Cuando completes una actividad, verás aquí el resumen detallado",
                                    buttonText = null,
                                    onButtonClick = {}
                                )
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(history, key = { it.id }) { summary ->
                                        HistoryCard(
                                            summary = summary,
                                            onDelete = { viewModel.deleteSessionSummary(summary.id) }
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

    // Start Activity Dialog (Naming)
    activityToStart?.let { activity ->
        StartSessionDialog(
            defaultTitle = activity.title,
            totalDurationFormatted = activity.formattedTotalDuration(),
            initialVoiceEnabled = viewModel.getVoiceEnabledPreference(),
            onDismiss = { activityToStart = null },
            onStart = { customName, voiceEnabled ->
                viewModel.startActivitySession(activity, customName, voiceEnabled)
                activityToStart = null
            }
        )
    }

    // Start Routine Dialog (Naming)
    routineToStart?.let { routine ->
        StartSessionDialog(
            defaultTitle = routine.title,
            totalDurationFormatted = routine.formattedTotalDuration(),
            initialVoiceEnabled = viewModel.getVoiceEnabledPreference(),
            onDismiss = { routineToStart = null },
            onStart = { customName, voiceEnabled ->
                viewModel.startRoutineSession(routine, customName, voiceEnabled)
                routineToStart = null
            }
        )
    }

    // Stop Confirmation Dialog
    if (showStopConfirmation) {
        AlertDialog(
            onDismissRequest = { showStopConfirmation = false },
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("¿Detener sesión?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Si detienes la sesión ahora, el temporizador se cancelará.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStopConfirmation = false
                        viewModel.stopSession()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCoral),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Detener", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showStopConfirmation = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Continuar sesión", color = LightTextSecondary)
                }
            }
        )
    }

    // Folder Dialog
    if (showFolderDialog) {
        FolderDialog(
            initialFolder = folderToEdit,
            onDismiss = {
                showFolderDialog = false
                folderToEdit = null
            },
            onSave = { folder ->
                viewModel.saveFolder(folder)
                showFolderDialog = false
                folderToEdit = null
            },
            onDelete = { id ->
                viewModel.deleteFolder(id)
                if (folderToEdit?.name == selectedCategoryFilter) {
                    selectedCategoryFilter = "Todas"
                }
                showFolderDialog = false
                folderToEdit = null
            }
        )
    }
}

@Composable
fun EmptyTabPlaceholder(
    title: String,
    description: String,
    buttonText: String?,
    onButtonClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(LightSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Timelapse,
                contentDescription = null,
                tint = LightTextSecondary,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = LightTextPrimary
            ),
            textAlign = TextAlign.Center
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium.copy(color = LightTextSecondary),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )

        if (buttonText != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onButtonClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(buttonText, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun HistoryCard(
    summary: com.taskpie.app.model.SessionSummary,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = summary.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (summary.parentTitle != null && summary.parentTitle != summary.title) {
                        Text(
                            text = summary.parentTitle,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 11.sp,
                                color = PrimaryBlue
                            )
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Eliminar del historial",
                        tint = LightTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val mins = summary.totalActualSeconds / 60
            val secs = summary.totalActualSeconds % 60
            val timeStr = if (mins > 0) "${mins}m ${secs}s" else "${secs}s"

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Duración: $timeStr",
                    style = MaterialTheme.typography.bodyMedium.copy(color = LightTextSecondary)
                )
                Text(
                    text = "Etapas: ${summary.completedStagesCount} completadas",
                    style = MaterialTheme.typography.bodyMedium.copy(color = AccentEmerald)
                )
                if (summary.skippedStagesCount > 0) {
                    Text(
                        text = "${summary.skippedStagesCount} omitidas",
                        style = MaterialTheme.typography.bodyMedium.copy(color = AccentAmber)
                    )
                }
            }
        }
    }
}
