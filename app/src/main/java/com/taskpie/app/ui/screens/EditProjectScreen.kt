package com.taskpie.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
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
    var selectedRoutineIds by remember(existingProject) {
        mutableStateOf(existingProject?.routineIds?.toSet() ?: emptySet())
    }

    var activityToStart by remember { mutableStateOf<ActivityItem?>(null) }
    var routineToStart by remember { mutableStateOf<RoutineItem?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (existingProject != null) "Proyecto" else "Nuevo Proyecto",
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
                                routineIds = selectedRoutineIds.toList(),
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

            // Linked Activities Selection
            item {
                Column {
                    Text(
                        text = "Actividades Vinculadas",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Selecciona las actividades que forman parte de este proyecto",
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

            // Linked Routines Selection
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    Text(
                        text = "Rutinas Vinculadas",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Selecciona las rutinas secuenciales para este proyecto",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 12.sp,
                            color = LightTextSecondary
                        ),
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                    )
                }
            }

            items(routines) { routine ->
                val isSelected = selectedRoutineIds.contains(routine.id)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedRoutineIds = if (isSelected) {
                                selectedRoutineIds - routine.id
                            } else {
                                selectedRoutineIds + routine.id
                            }
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) AccentTealLight else LightSurface
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
                                    selectedRoutineIds = if (it) {
                                        selectedRoutineIds + routine.id
                                    } else {
                                        selectedRoutineIds - routine.id
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = routine.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                )
                                Text(
                                    text = "${routine.steps.size} actividades • ${routine.formattedTotalDuration()}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 11.sp,
                                        color = LightTextSecondary
                                    )
                                )
                            }
                        }

                        if (existingProject != null && isSelected) {
                            FilledTonalButton(
                                onClick = { routineToStart = routine },
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

    // Start Activity Dialog
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
