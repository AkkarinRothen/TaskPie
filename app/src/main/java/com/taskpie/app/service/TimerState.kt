package com.taskpie.app.service

import com.taskpie.app.model.RoutineStep
import com.taskpie.app.model.SessionSummary
import com.taskpie.app.model.Stage
import com.taskpie.app.model.StageExecutionRecord

enum class TimerStatus {
    IDLE,
    RUNNING,
    PAUSED,
    WAITING_MANUAL_ADVANCE, // When activity finishes and autoAdvance == false
    COMPLETED
}

data class TimerState(
    val status: TimerStatus = TimerStatus.IDLE,
    val sessionTitle: String = "",
    val contextType: String = "ACTIVITY", // "ACTIVITY", "ROUTINE", "PROJECT"
    val parentTitle: String? = null,
    val routineSteps: List<RoutineStep> = emptyList(),
    val currentActivityIndex: Int = 0,
    val currentStageIndex: Int = 0,
    val remainingSeconds: Long = 0,
    val elapsedSecondsInCurrentStage: Long = 0,
    val totalElapsedSecondsSession: Long = 0,
    val totalPlannedSecondsSession: Long = 0,
    val records: List<StageExecutionRecord> = emptyList(),
    val completedSummary: SessionSummary? = null,
    val isVoiceEnabled: Boolean = true,
    val isMateModeEnabled: Boolean = false,
    val mateIntervalMinutes: Int = 8,
    val mateRemainingSeconds: Long = 8 * 60L,
    val lastMateAlertTimeMillis: Long = 0L,
    val totalCycles: Int = 1,
    val currentCycle: Int = 1,
    val cycleBreakSeconds: Long = 0L,
    val isInCycleBreak: Boolean = false,
    val cycleBreakRemainingSeconds: Long = 0L
) {
    val currentActivity: RoutineStep?
        get() = routineSteps.getOrNull(currentActivityIndex)

    val currentStage: Stage?
        get() = currentActivity?.stages?.getOrNull(currentStageIndex)

    val currentStageTotalSeconds: Long
        get() = currentStage?.durationSeconds ?: 1L

    val currentStageProgress: Float
        get() {
            val total = currentStageTotalSeconds
            if (total <= 0) return 1f
            return (elapsedSecondsInCurrentStage.toFloat() / total).coerceIn(0f, 1f)
        }

    val totalSessionProgress: Float
        get() {
            if (totalPlannedSecondsSession <= 0) return 0f
            return (totalElapsedSecondsSession.toFloat() / totalPlannedSecondsSession).coerceIn(0f, 1f)
        }

    val isLastStageInActivity: Boolean
        get() = currentActivity?.let { currentStageIndex >= it.stages.size - 1 } ?: true

    val isLastActivityInRoutine: Boolean
        get() = currentActivityIndex >= routineSteps.size - 1

    fun formattedRemainingTime(): String {
        val mins = remainingSeconds / 60
        val secs = remainingSeconds % 60
        return String.format("%02d:%02d", mins, secs)
    }

    fun formattedTotalElapsedTime(): String {
        val hours = totalElapsedSecondsSession / 3600
        val mins = (totalElapsedSecondsSession % 3600) / 60
        val secs = totalElapsedSecondsSession % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, mins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }
    }

    fun formattedMateRemainingTime(): String {
        val mins = mateRemainingSeconds / 60
        val secs = mateRemainingSeconds % 60
        return String.format("%02d:%02d", mins, secs)
    }
}
