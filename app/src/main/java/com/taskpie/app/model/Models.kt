package com.taskpie.app.model

import java.util.UUID

data class Stage(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val durationSeconds: Long, // Duration in seconds
    val notes: String = "",
    val isOptional: Boolean = false,
    val colorHex: String = "#3B82F6"
) {
    val durationMinutes: Long
        get() = durationSeconds / 60

    val remainingSecondsInMinute: Long
        get() = durationSeconds % 60

    fun formattedDuration(): String {
        val mins = durationSeconds / 60
        val secs = durationSeconds % 60
        return if (secs == 0L) "${mins}m" else String.format("%02d:%02d", mins, secs)
    }
}

data class ActivityItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val stages: List<Stage> = emptyList(),
    val isTemplate: Boolean = false,
    val category: String = "General",
    val createdAt: Long = System.currentTimeMillis()
) {
    val totalDurationSeconds: Long
        get() = stages.sumOf { it.durationSeconds }

    fun formattedTotalDuration(): String {
        val totalSecs = totalDurationSeconds
        val hours = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        return when {
            hours > 0 -> "${hours}h ${mins}m"
            mins > 0 && secs > 0 -> "${mins}m ${secs}s"
            mins > 0 -> "${mins} min"
            else -> "${secs} s"
        }
    }
}

data class RoutineStep(
    val id: String = UUID.randomUUID().toString(),
    val activityId: String,
    val activityTitle: String,
    val stages: List<Stage>,
    val autoAdvance: Boolean = true // true = auto start next activity, false = wait for user confirmation
) {
    val totalDurationSeconds: Long
        get() = stages.sumOf { it.durationSeconds }
}

data class RoutineItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val steps: List<RoutineStep> = emptyList(),
    val isTemplate: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val totalDurationSeconds: Long
        get() = steps.sumOf { it.totalDurationSeconds }

    val totalStagesCount: Int
        get() = steps.sumOf { it.stages.size }

    fun formattedTotalDuration(): String {
        val totalSecs = totalDurationSeconds
        val hours = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        return if (hours > 0) "${hours}h ${mins}m" else "${mins} min"
    }
}

data class ProjectItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val activityIds: List<String> = emptyList(),
    val routineIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class StageExecutionRecord(
    val stageId: String,
    val stageName: String,
    val activityName: String,
    val plannedSeconds: Long,
    val actualSeconds: Long,
    val isSkipped: Boolean = false,
    val isCompleted: Boolean = true
)

data class SessionSummary(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val contextType: String, // "ACTIVITY", "ROUTINE", "PROJECT"
    val parentTitle: String? = null,
    val totalPlannedSeconds: Long,
    val totalActualSeconds: Long,
    val completedStagesCount: Int,
    val skippedStagesCount: Int,
    val stageRecords: List<StageExecutionRecord>,
    val completedAt: Long = System.currentTimeMillis()
)

data class FolderItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorHex: String = "#3B82F6",
    val createdAt: Long = System.currentTimeMillis()
)
