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
import com.taskpie.app.model.ProjectItem
import com.taskpie.app.model.RoutineItem
import com.taskpie.app.ui.components.StartSessionDialog
import com.taskpie.app.ui.theme.*
import com.taskpie.app.ui.viewmodel.TaskPieViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
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

    var title by remember(existingProject) { mutableStateOf(existingProject?.title ?: "") }
    var description by remember(existingProject) { mutableStateOf(existingProject?.description ?: "") }
    var selectedActivityIds by remember(existingProject) {
        mutableStateOf(existingProject?.activityIds?.toSet() ?: emptySet())
    }
    var routineSequence by remember(existingProject) {
        mutableStateOf(existingProject?.routineIds ?: emptyList())
    }

    var isSelectingRoutineForSequence by remember { mutableStateOf(false) }
    var activityToStart by remember { mutableStateOf<ActivityItem?>(null) }
    var routineToStart by remember { mutableStateOf<RoutineItem?>(null) }
    var isStartingFullProject by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val totalSequenceSeconds = remember(routineSequence, routines) {
        routineSequence.sumOf { rId ->
            routines.find { it.id == rId }?.totalDurationSeconds ?: 0L
        }
    }
    val totalSequenceMinutes = totalSequenceSeconds / 60

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
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                errorMessage = "Ingresa un título para el proyecto"
                                return@Button
                            }

                            val item = ProjectItem(
                                id = existingProject?.id ?: UUID.randomUUID().toString(),
                                title = title.trim(),
                                description = description.trim(),
                                activityIds = selectedActivityIds.toList(),
                                routineIds = routineSequence,
                                createdAt = existingProject?.createdAt ?: System.currentTimeMillis()
                            )
                            viewModel.saveProject(item)
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

                        if (existingProject != null && routineSequence.isNotEmpty()) {
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
                            text = "${routineSequence.size} rutinas ordenadas • Total: $totalSequenceMinutes min",
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
            }

            // Routine Sequence Items
            if (routineSequence.isEmpty()) {
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
                                text = "Añade una rutina. Puedes repetir la misma rutina tantas veces como desees.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = LightTextSecondary),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(routineSequence) { index, routineId ->
                    val routine = routines.find { it.id == routineId }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = LightSurface),
                        border = BorderStroke(1.dp, LightBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Reorder buttons
                                Column {
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val list = routineSequence.toMutableList()
                                                val item = list.removeAt(index)
                                                list.add(index - 1, item)
                                                routineSequence = list
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
                                            if (index < routineSequence.size - 1) {
                                                val list = routineSequence.toMutableList()
                                                val item = list.removeAt(index)
                                                list.add(index + 1, item)
                                                routineSequence = list
                                            }
                                        },
                                        enabled = index < routineSequence.size - 1,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Bajar",
                                            tint = if (index < routineSequence.size - 1) LightTextPrimary else LightTextTertiary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

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
                                    Text(
                                        text = routine?.title ?: "Rutina (no encontrada)",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                    )
                                    val duration = routine?.formattedTotalDuration() ?: "-"
                                    Text(
                                        text = "${routine?.steps?.size ?: 0} actividades • $duration",
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
                                        val list = routineSequence.toMutableList()
                                        list.add(index + 1, routineId)
                                        routineSequence = list
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
                                        val list = routineSequence.toMutableList()
                                        list.removeAt(index)
                                        routineSequence = list
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
                    }
                }
            }

            // Linked Activities Section (Optional Individual Activities)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    Text(
                        text = "Actividades Vinculadas",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Selecciona actividades individuales de referencia para este proyecto",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 12.sp,
                            color = LightTextSecondary
                        ),
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                    )
                }
            }

            items(activities) { activity ->
                val isSelected = selectedActivityIds.contains(activity.id)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedActivityIds = if (isSelected) {
                                selectedActivityIds - activity.id
                            } else {
                                selectedActivityIds + activity.id
                            }
                        },
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(routines) { routine ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    routineSequence = routineSequence + routine.id
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
            routineIds = routineSequence,
            activityIds = selectedActivityIds.toList()
        )
        StartSessionDialog(
            defaultTitle = proj.title,
            totalDurationFormatted = "${totalSequenceMinutes}m",
            initialVoiceEnabled = viewModel.getVoiceEnabledPreference(),
            onDismiss = { isStartingFullProject = false },
            onStart = { customName, voiceEnabled ->
                viewModel.startProjectSession(proj, customName, voiceEnabled)
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
            onStart = { customName, voiceEnabled ->
                viewModel.startProjectActivitySession(title, activity, customName, voiceEnabled)
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
            onDismiss = { routineToStart = null },
            onStart = { customName, voiceEnabled ->
                viewModel.startRoutineSession(routine, customName, voiceEnabled)
                routineToStart = null
                onNavigateToActiveSession()
            }
        )
    }
}
