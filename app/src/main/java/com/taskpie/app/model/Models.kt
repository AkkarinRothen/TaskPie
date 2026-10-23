package com.taskpie.app.model

import java.util.UUID

data class Stage(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val durationSeconds: Long, // Duration in seconds
    val notes: String = "",
    val isOptional: Boolean = false,
    val colorHex: String = "#3B82F6",
    val isEnabled: Boolean = true
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
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) {
    val totalDurationSeconds: Long
        get() = stages.filter { it.isEnabled }.sumOf { it.durationSeconds }

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
    val autoAdvance: Boolean = true, // true = auto start next activity, false = wait for user confirmation
    val blockId: String? = null,
    val blockTitle: String? = null,
    val blockRepeatCount: Int = 1,
    val iterationIndex: Int = 0,
    val totalIterations: Int = 1,
    val isCustomized: Boolean = false,
    val originalActivityId: String? = null
) {
    val totalDurationSeconds: Long
        get() = stages.filter { it.isEnabled }.sumOf { it.durationSeconds }
}

data class RoutineBlock(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val repeatCount: Int = 1,
    val steps: List<RoutineStep> = emptyList()
)

fun List<RoutineStep>.toBlocks(): List<RoutineBlock> {
    val blocks = mutableListOf<RoutineBlock>()
    var currentBlock: RoutineBlock? = null
    for (step in this) {
        val bId = step.blockId
        if (bId != null) {
            if (currentBlock != null && currentBlock.id == bId) {
                currentBlock = currentBlock.copy(steps = currentBlock.steps + step)
                blocks[blocks.lastIndex] = currentBlock
            } else {
                currentBlock = RoutineBlock(
                    id = bId,
                    title = step.blockTitle ?: "Bloque",
                    repeatCount = step.blockRepeatCount.coerceAtLeast(1),
                    steps = listOf(step)
                )
                blocks.add(currentBlock)
            }
        } else {
            currentBlock = RoutineBlock(
                id = "block_${step.id}",
                title = "",
                repeatCount = 1,
                steps = listOf(step)
            )
            blocks.add(currentBlock)
        }
    }
    return blocks
}

fun List<RoutineBlock>.toFlatSteps(): List<RoutineStep> {
    val result = mutableListOf<RoutineStep>()
    for (b in this) {
        val isRepeated = b.steps.size > 1 || b.title.isNotBlank() || b.repeatCount > 1
        val bId = if (isRepeated) b.id else null
        val bTitle = if (isRepeated) b.title.ifBlank { "Bloque" } else null
        val bRepeat = if (isRepeated) b.repeatCount.coerceAtLeast(1) else 1
        for (s in b.steps) {
            result.add(
                s.copy(
                    blockId = bId,
                    blockTitle = bTitle,
                    blockRepeatCount = bRepeat
                )
            )
        }
    }
    return result
}

fun List<RoutineStep>.unrollSteps(): List<RoutineStep> {
    val blocks = this.toBlocks()
    val unrolled = mutableListOf<RoutineStep>()
    for (b in blocks) {
        val repeats = b.repeatCount.coerceAtLeast(1)
        for (r in 0 until repeats) {
            for (s in b.steps) {
                val activeStages = s.stages.filter { it.isEnabled }
                if (activeStages.isEmpty()) continue

                unrolled.add(
                    s.copy(
                        id = "${s.id}_iter_${r}_${System.currentTimeMillis()}",
                        stages = activeStages,
                        blockId = if (b.steps.size > 1 || repeats > 1) b.id else null,
                        blockTitle = if (b.title.isNotBlank()) b.title else if (repeats > 1) "Bloque" else null,
                        blockRepeatCount = repeats,
                        iterationIndex = r,
                        totalIterations = repeats
                    )
                )
            }
        }
    }
    return unrolled
}

data class RoutineItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val steps: List<RoutineStep> = emptyList(),
    val isTemplate: Boolean = false,
    val tags: List<String> = emptyList(),
    val defaultRepeatCount: Int = 1,
    val defaultCycleBreakSeconds: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
) {
    val totalDurationSeconds: Long
        get() = steps.toBlocks().sumOf { b ->
            b.steps.sumOf { it.totalDurationSeconds } * b.repeatCount.coerceAtLeast(1)
        }

    val totalStagesCount: Int
        get() = steps.sumOf { it.stages.count { st -> st.isEnabled } }

    fun formattedTotalDuration(): String {
        val totalSecs = totalDurationSeconds
        val hours = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        return if (hours > 0) "${hours}h ${mins}m" else "${mins} min"
    }
}

data class ProjectRoutineItem(
    val id: String = UUID.randomUUID().toString(),
    val routineId: String,
    val title: String,
    val steps: List<RoutineStep> = emptyList(),
    val isCustomized: Boolean = false
)

data class ProjectItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val activityIds: List<String> = emptyList(),
    val routineIds: List<String> = emptyList(),
    val projectRoutines: List<ProjectRoutineItem> = emptyList(),
    val tags: List<String> = emptyList(),
    val defaultRepeatCount: Int = 1,
    val defaultCycleBreakSeconds: Long = 0L,
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
