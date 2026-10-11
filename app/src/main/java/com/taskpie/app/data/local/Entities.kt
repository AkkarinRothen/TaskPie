package com.taskpie.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.taskpie.app.model.ActivityItem
import com.taskpie.app.model.FolderItem
import com.taskpie.app.model.ProjectItem
import com.taskpie.app.model.RoutineItem
import com.taskpie.app.model.RoutineStep
import com.taskpie.app.model.SessionSummary
import com.taskpie.app.model.Stage
import com.taskpie.app.model.StageExecutionRecord

@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val stages: List<Stage>,
    val isTemplate: Boolean,
    val category: String,
    val createdAt: Long
) {
    fun toDomain(): ActivityItem = ActivityItem(
        id = id,
        title = title,
        description = description,
        stages = stages,
        isTemplate = isTemplate,
        category = category,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(item: ActivityItem): ActivityEntity = ActivityEntity(
            id = item.id,
            title = item.title,
            description = item.description,
            stages = item.stages,
            isTemplate = item.isTemplate,
            category = item.category,
            createdAt = item.createdAt
        )
    }
}

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val steps: List<RoutineStep>,
    val isTemplate: Boolean,
    val createdAt: Long
) {
    fun toDomain(): RoutineItem = RoutineItem(
        id = id,
        title = title,
        description = description,
        steps = steps,
        isTemplate = isTemplate,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(item: RoutineItem): RoutineEntity = RoutineEntity(
            id = item.id,
            title = item.title,
            description = item.description,
            steps = item.steps,
            isTemplate = item.isTemplate,
            createdAt = item.createdAt
        )
    }
}

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val activityIds: List<String>,
    val routineIds: List<String>,
    val createdAt: Long
) {
    fun toDomain(): ProjectItem = ProjectItem(
        id = id,
        title = title,
        description = description,
        activityIds = activityIds,
        routineIds = routineIds,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(item: ProjectItem): ProjectEntity = ProjectEntity(
            id = item.id,
            title = item.title,
            description = item.description,
            activityIds = item.activityIds,
            routineIds = item.routineIds,
            createdAt = item.createdAt
        )
    }
}

@Entity(tableName = "session_summaries")
data class SessionSummaryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val contextType: String,
    val parentTitle: String?,
    val totalPlannedSeconds: Long,
    val totalActualSeconds: Long,
    val completedStagesCount: Int,
    val skippedStagesCount: Int,
    val stageRecords: List<StageExecutionRecord>,
    val completedAt: Long
) {
    fun toDomain(): SessionSummary = SessionSummary(
        id = id,
        title = title,
        contextType = contextType,
        parentTitle = parentTitle,
        totalPlannedSeconds = totalPlannedSeconds,
        totalActualSeconds = totalActualSeconds,
        completedStagesCount = completedStagesCount,
        skippedStagesCount = skippedStagesCount,
        stageRecords = stageRecords,
        completedAt = completedAt
    )

    companion object {
        fun fromDomain(summary: SessionSummary): SessionSummaryEntity = SessionSummaryEntity(
            id = summary.id,
            title = summary.title,
            contextType = summary.contextType,
            parentTitle = summary.parentTitle,
            totalPlannedSeconds = summary.totalPlannedSeconds,
            totalActualSeconds = summary.totalActualSeconds,
            completedStagesCount = summary.completedStagesCount,
            skippedStagesCount = summary.skippedStagesCount,
            stageRecords = summary.stageRecords,
            completedAt = summary.completedAt
        )
    }
}

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val colorHex: String = "#3B82F6",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): FolderItem = FolderItem(
        id = id,
        name = name,
        colorHex = colorHex,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(item: FolderItem): FolderEntity = FolderEntity(
            id = item.id,
            name = item.name,
            colorHex = item.colorHex,
            createdAt = item.createdAt
        )
    }
}
