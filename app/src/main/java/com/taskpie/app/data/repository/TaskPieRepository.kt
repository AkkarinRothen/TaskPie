package com.taskpie.app.data.repository

import com.taskpie.app.data.local.ActivityEntity
import com.taskpie.app.data.local.FolderEntity
import com.taskpie.app.data.local.ProjectEntity
import com.taskpie.app.data.local.RoutineEntity
import com.taskpie.app.data.local.SessionSummaryEntity
import com.taskpie.app.data.local.TaskPieDao
import com.taskpie.app.model.ActivityItem
import com.taskpie.app.model.BuiltInTemplates
import com.taskpie.app.model.FolderItem
import com.taskpie.app.model.ProjectItem
import com.taskpie.app.model.RoutineItem
import com.taskpie.app.model.SessionSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskPieRepository(private val dao: TaskPieDao) {

    // --- Folders ---
    val allFolders: Flow<List<FolderItem>> = dao.getAllFoldersFlow().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun saveFolder(folder: FolderItem) {
        dao.insertFolder(FolderEntity.fromDomain(folder))
    }

    suspend fun deleteFolder(id: String) {
        dao.deleteFolderById(id)
    }

    suspend fun checkAndSeedFolders() {
        val existing = dao.getAllFolders()
        val defaultFolders = listOf(
            FolderEntity(name = "Anti-Inercia", colorHex = "#EC4899"),
            FolderEntity(name = "Escritura", colorHex = "#3B82F6"),
            FolderEntity(name = "Bienestar", colorHex = "#10B981"),
            FolderEntity(name = "Hogar", colorHex = "#F97316"),
            FolderEntity(name = "Estudio", colorHex = "#6366F1"),
            FolderEntity(name = "Trabajo", colorHex = "#8B5CF6")
        )
        if (existing.isEmpty()) {
            dao.insertFolders(defaultFolders)
        } else {
            if (existing.none { it.name.equals("Anti-Inercia", ignoreCase = true) }) {
                dao.insertFolder(FolderEntity(name = "Anti-Inercia", colorHex = "#EC4899"))
            }
        }
    }

    // --- Activities ---
    val allActivities: Flow<List<ActivityItem>> = dao.getAllActivitiesFlow().map { list ->
        list.map { it.toDomain() }
    }

    val templateActivities: Flow<List<ActivityItem>> = dao.getTemplateActivitiesFlow().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getActivityById(id: String): ActivityItem? {
        return dao.getActivityById(id)?.toDomain()
    }

    suspend fun saveActivity(activity: ActivityItem) {
        dao.insertActivity(ActivityEntity.fromDomain(activity))
    }

    suspend fun deleteActivity(id: String) {
        dao.deleteActivityById(id)
    }

    // --- Routines ---
    val allRoutines: Flow<List<RoutineItem>> = dao.getAllRoutinesFlow().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getRoutineById(id: String): RoutineItem? {
        return dao.getRoutineById(id)?.toDomain()
    }

    suspend fun saveRoutine(routine: RoutineItem) {
        dao.insertRoutine(RoutineEntity.fromDomain(routine))
    }

    suspend fun deleteRoutine(id: String) {
        dao.deleteRoutineById(id)
    }

    // --- Projects ---
    val allProjects: Flow<List<ProjectItem>> = dao.getAllProjectsFlow().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getProjectById(id: String): ProjectItem? {
        return dao.getProjectById(id)?.toDomain()
    }

    suspend fun saveProject(project: ProjectItem) {
        dao.insertProject(ProjectEntity.fromDomain(project))
    }

    suspend fun deleteProject(id: String) {
        dao.deleteProjectById(id)
    }

    // --- Session History ---
    val sessionHistory: Flow<List<SessionSummary>> = dao.getAllSessionSummariesFlow().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun saveSessionSummary(summary: SessionSummary) {
        dao.insertSessionSummary(SessionSummaryEntity.fromDomain(summary))
    }

    suspend fun deleteSessionSummary(id: String) {
        dao.deleteSessionSummaryById(id)
    }

    suspend fun checkAndSeedDefaults() {
        val activityEntities = BuiltInTemplates.DEFAULT_ACTIVITIES.map { ActivityEntity.fromDomain(it) }
        dao.insertActivities(activityEntities)
        val routineEntities = BuiltInTemplates.DEFAULT_ROUTINES.map { RoutineEntity.fromDomain(it) }
        dao.insertRoutines(routineEntities)
        dao.insertProject(ProjectEntity.fromDomain(BuiltInTemplates.DEFAULT_PROJECT))
        dao.insertProject(ProjectEntity.fromDomain(BuiltInTemplates.PROJECT_BALANCED_DAY))
        checkAndSeedFolders()
    }
}
