package com.taskpie.app.service

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.taskpie.app.TaskPieApplication
import com.taskpie.app.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object TimerController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tickerJob: Job? = null

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var appContext: Context? = null
    private var repositoryScope: CoroutineScope? = null
    private var voiceManager: VoiceAnnouncementManager? = null

    private const val PREFS_NAME = "taskpie_settings"
    private const val PREF_VOICE_ENABLED = "voice_announcements_enabled"
    private const val PREF_MATE_ENABLED = "mate_reminder_enabled"
    private const val PREF_MATE_INTERVAL = "mate_interval_minutes"

    private val matePhrases = listOf(
        "¡Che, tomate el mate que se enfría!",
        "¡Momento de mate!",
        "¡Cuidado que se te enfría el mate!",
        "¡Cebate otro para seguir!",
        "¡Pausa para un matecito!"
    )

    fun getVoiceEnabledPreference(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_VOICE_ENABLED, true)
    }

    fun setVoiceEnabledPreference(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_VOICE_ENABLED, enabled).apply()
    }

    fun getMateEnabledPreference(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_MATE_ENABLED, false)
    }

    fun setMateEnabledPreference(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_MATE_ENABLED, enabled).apply()
    }

    fun getMateIntervalPreference(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(PREF_MATE_INTERVAL, 8)
    }

    fun setMateIntervalPreference(context: Context, minutes: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(PREF_MATE_INTERVAL, minutes).apply()
    }

    fun toggleMateMode() {
        val current = _timerState.value
        val newEnabled = !current.isMateModeEnabled
        val interval = current.mateIntervalMinutes
        _timerState.update {
            it.copy(
                isMateModeEnabled = newEnabled,
                mateRemainingSeconds = if (newEnabled && it.mateRemainingSeconds <= 0) interval * 60L else it.mateRemainingSeconds
            )
        }
        appContext?.let { setMateEnabledPreference(it, newEnabled) }
        if (newEnabled && current.isVoiceEnabled) {
            voiceManager?.speak("Compañero de mate activado, cada $interval minutos")
        }
    }

    fun setMateInterval(minutes: Int) {
        val clamped = minutes.coerceIn(1, 60)
        _timerState.update {
            it.copy(
                mateIntervalMinutes = clamped,
                mateRemainingSeconds = clamped * 60L
            )
        }
        appContext?.let { setMateIntervalPreference(it, clamped) }
    }

    fun resetMateCountdown() {
        val interval = _timerState.value.mateIntervalMinutes
        _timerState.update {
            it.copy(mateRemainingSeconds = interval * 60L)
        }
    }

    fun toggleVoice() {
        val newVoiceState = !_timerState.value.isVoiceEnabled
        _timerState.update { it.copy(isVoiceEnabled = newVoiceState) }
        appContext?.let { setVoiceEnabledPreference(it, newVoiceState) }
        if (!newVoiceState) {
            voiceManager?.stop()
        }
    }

    fun initialize(context: Context) {
        appContext = context.applicationContext
        if (voiceManager == null) {
            voiceManager = VoiceAnnouncementManager(context.applicationContext)
        }
        val defaultVoice = getVoiceEnabledPreference(context)
        val defaultMateEnabled = getMateEnabledPreference(context)
        val defaultMateInterval = getMateIntervalPreference(context)
        _timerState.update {
            it.copy(
                isVoiceEnabled = defaultVoice,
                isMateModeEnabled = defaultMateEnabled,
                mateIntervalMinutes = defaultMateInterval,
                mateRemainingSeconds = defaultMateInterval * 60L
            )
        }
        repositoryScope = (context.applicationContext as? TaskPieApplication)?.let {
            CoroutineScope(SupervisorJob() + Dispatchers.IO)
        }
    }

    fun startSingleActivitySession(
        context: Context,
        activity: ActivityItem,
        customSessionName: String? = null,
        voiceEnabled: Boolean? = null,
        repeatCount: Int = 1,
        cycleBreakSeconds: Long = 0L
    ) {
        val activeStages = activity.stages.filter { it.isEnabled }
        if (activeStages.isEmpty()) return
        val routineStep = RoutineStep(
            activityId = activity.id,
            activityTitle = activity.title,
            stages = activeStages,
            autoAdvance = true
        )
        startSession(
            context = context,
            title = customSessionName?.takeIf { it.isNotBlank() } ?: activity.title,
            contextType = "ACTIVITY",
            parentTitle = null,
            steps = listOf(routineStep),
            voiceEnabled = voiceEnabled,
            repeatCount = repeatCount,
            cycleBreakSeconds = cycleBreakSeconds
        )
    }

    fun startRoutineSession(
        context: Context,
        routine: RoutineItem,
        customSessionName: String? = null,
        voiceEnabled: Boolean? = null,
        repeatCount: Int = routine.defaultRepeatCount.coerceAtLeast(1),
        cycleBreakSeconds: Long = routine.defaultCycleBreakSeconds
    ) {
        startSession(
            context = context,
            title = customSessionName?.takeIf { it.isNotBlank() } ?: routine.title,
            contextType = "ROUTINE",
            parentTitle = routine.title,
            steps = routine.steps.unrollSteps(),
            voiceEnabled = voiceEnabled,
            repeatCount = repeatCount,
            cycleBreakSeconds = cycleBreakSeconds
        )
    }

    fun startProjectSession(
        context: Context,
        project: ProjectItem,
        activities: List<ActivityItem>,
        routines: List<RoutineItem>,
        customSessionName: String? = null,
        voiceEnabled: Boolean? = null,
        repeatCount: Int = project.defaultRepeatCount.coerceAtLeast(1),
        cycleBreakSeconds: Long = project.defaultCycleBreakSeconds
    ) {
        val steps = mutableListOf<RoutineStep>()

        // Actividades del proyecto
        project.activityIds.forEach { actId ->
            val act = activities.find { it.id == actId }
            if (act != null) {
                val activeStages = act.stages.filter { it.isEnabled }
                if (activeStages.isNotEmpty()) {
                    steps.add(
                        RoutineStep(
                            activityId = act.id,
                            activityTitle = act.title,
                            stages = activeStages,
                            autoAdvance = true
                        )
                    )
                }
            }
        }

        // Rutinas del proyecto: si tiene projectRoutines (snapshots desacoplados con exclusiones granulares)
        if (project.projectRoutines.isNotEmpty()) {
            project.projectRoutines.forEach { pr ->
                steps.addAll(pr.steps.unrollSteps())
            }
        } else {
            // Compatibilidad con proyectos legados
            project.routineIds.forEach { rId ->
                val rout = routines.find { it.id == rId }
                if (rout != null) {
                    steps.addAll(rout.steps.unrollSteps())
                }
            }
        }

        startSession(
            context = context,
            title = customSessionName?.takeIf { it.isNotBlank() } ?: project.title,
            contextType = "PROJECT",
            parentTitle = project.title,
            steps = steps,
            voiceEnabled = voiceEnabled,
            repeatCount = repeatCount,
            cycleBreakSeconds = cycleBreakSeconds
        )
    }

    fun startProjectActivitySession(
        context: Context,
        projectName: String,
        activity: ActivityItem,
        customSessionName: String? = null,
        voiceEnabled: Boolean? = null,
        repeatCount: Int = 1,
        cycleBreakSeconds: Long = 0L
    ) {
        val routineStep = RoutineStep(
            activityId = activity.id,
            activityTitle = activity.title,
            stages = activity.stages,
            autoAdvance = true
        )
        startSession(
            context = context,
            title = customSessionName?.takeIf { it.isNotBlank() } ?: activity.title,
            contextType = "PROJECT",
            parentTitle = projectName,
            steps = listOf(routineStep),
            voiceEnabled = voiceEnabled,
            repeatCount = repeatCount,
            cycleBreakSeconds = cycleBreakSeconds
        )
    }

    fun startSession(
        context: Context,
        title: String,
        contextType: String,
        parentTitle: String?,
        steps: List<RoutineStep>,
        voiceEnabled: Boolean? = null,
        repeatCount: Int = 1,
        cycleBreakSeconds: Long = 0L
    ) {
        if (steps.isEmpty() || steps.all { it.stages.isEmpty() }) return

        val isVoice = voiceEnabled ?: getVoiceEnabledPreference(context)
        setVoiceEnabledPreference(context, isVoice)

        if (voiceManager == null) {
            voiceManager = VoiceAnnouncementManager(context.applicationContext)
        }

        val singleCyclePlanned = steps.sumOf { it.totalDurationSeconds }
        val totalPlannedSeconds = singleCyclePlanned * repeatCount + (cycleBreakSeconds * (repeatCount - 1).coerceAtLeast(0))
        val firstStep = steps[0]
        val firstStage = firstStep.stages.firstOrNull() ?: return

        val defaultMateEnabled = getMateEnabledPreference(context)
        val defaultMateInterval = getMateIntervalPreference(context)

        _timerState.value = TimerState(
            status = TimerStatus.RUNNING,
            sessionTitle = title,
            contextType = contextType,
            parentTitle = parentTitle,
            routineSteps = steps,
            currentActivityIndex = 0,
            currentStageIndex = 0,
            remainingSeconds = firstStage.durationSeconds,
            elapsedSecondsInCurrentStage = 0,
            totalElapsedSecondsSession = 0,
            totalPlannedSecondsSession = totalPlannedSeconds,
            records = emptyList(),
            completedSummary = null,
            isVoiceEnabled = isVoice,
            isMateModeEnabled = defaultMateEnabled,
            mateIntervalMinutes = defaultMateInterval,
            mateRemainingSeconds = defaultMateInterval * 60L,
            totalCycles = repeatCount.coerceAtLeast(1),
            currentCycle = 1,
            cycleBreakSeconds = cycleBreakSeconds,
            isInCycleBreak = false,
            cycleBreakRemainingSeconds = 0L
        )

        if (isVoice) {
            if (repeatCount > 1) {
                voiceManager?.speak("Iniciando Ciclo 1 de $repeatCount. ${firstStage.name}")
            } else {
                voiceManager?.speak("Iniciando: ${firstStage.name}")
            }
        }

        startForegroundService(context)
        startTicker()
    }

    fun pause() {
        if (_timerState.value.status == TimerStatus.RUNNING) {
            _timerState.update { it.copy(status = TimerStatus.PAUSED) }
            stopTicker()
            voiceManager?.stop()
            updateNotification()
        }
    }

    fun resume(context: Context) {
        if (_timerState.value.status == TimerStatus.PAUSED) {
            _timerState.update { it.copy(status = TimerStatus.RUNNING) }
            startTicker()
            updateNotification()
        }
    }

    fun skipCurrentStage() {
        val state = _timerState.value
        val stage = state.currentStage ?: return
        val activity = state.currentActivity ?: return

        voiceManager?.stop()

        val record = StageExecutionRecord(
            stageId = stage.id,
            stageName = stage.name,
            activityName = activity.activityTitle,
            plannedSeconds = stage.durationSeconds,
            actualSeconds = state.elapsedSecondsInCurrentStage,
            isSkipped = true,
            isCompleted = false
        )

        _timerState.update {
            it.copy(records = it.records + record)
        }

        advanceToNextStage(fromSkip = true)
    }

    fun advanceManualNextActivity() {
        if (_timerState.value.status == TimerStatus.WAITING_MANUAL_ADVANCE) {
            val state = _timerState.value
            val nextActivityIdx = state.currentActivityIndex + 1
            if (nextActivityIdx < state.routineSteps.size) {
                val nextActivity = state.routineSteps[nextActivityIdx]
                val firstStage = nextActivity.stages.firstOrNull()
                if (firstStage != null) {
                    _timerState.update {
                        it.copy(
                            status = TimerStatus.RUNNING,
                            currentActivityIndex = nextActivityIdx,
                            currentStageIndex = 0,
                            remainingSeconds = firstStage.durationSeconds,
                            elapsedSecondsInCurrentStage = 0
                        )
                    }
                    if (_timerState.value.isVoiceEnabled) {
                        voiceManager?.speak("Iniciando: ${firstStage.name}")
                    }
                    startTicker()
                    updateNotification()
                }
            }
        }
    }

    fun stopSession(context: Context) {
        stopTicker()
        voiceManager?.stop()
        _timerState.update { it.copy(status = TimerStatus.IDLE) }
        stopForegroundService(context)
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive && _timerState.value.status == TimerStatus.RUNNING) {
                delay(1000L)
                tick()
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun tick() {
        val currentState = _timerState.value
        if (currentState.status != TimerStatus.RUNNING) return

        // Handle cycle break if active
        if (currentState.isInCycleBreak) {
            val breakRemaining = currentState.cycleBreakRemainingSeconds - 1
            val totalElapsed = currentState.totalElapsedSecondsSession + 1

            if (breakRemaining <= 0) {
                // Break ended! Transition to next cycle
                val nextCycle = currentState.currentCycle + 1
                val firstActivity = currentState.routineSteps.firstOrNull()
                val firstStage = firstActivity?.stages?.firstOrNull()

                if (firstStage != null) {
                    _timerState.update {
                        it.copy(
                            isInCycleBreak = false,
                            cycleBreakRemainingSeconds = 0,
                            currentCycle = nextCycle,
                            currentActivityIndex = 0,
                            currentStageIndex = 0,
                            remainingSeconds = firstStage.durationSeconds,
                            elapsedSecondsInCurrentStage = 0,
                            totalElapsedSecondsSession = totalElapsed
                        )
                    }
                    triggerAlert()
                    if (_timerState.value.isVoiceEnabled) {
                        scope.launch {
                            delay(600L)
                            voiceManager?.speak("Descanso finalizado. Iniciando Ciclo $nextCycle de ${currentState.totalCycles}")
                        }
                    }
                    updateNotification()
                } else {
                    finishSession()
                }
            } else {
                _timerState.update {
                    it.copy(
                        cycleBreakRemainingSeconds = breakRemaining,
                        totalElapsedSecondsSession = totalElapsed
                    )
                }
                updateNotification()
            }
            return
        }

        val remaining = currentState.remainingSeconds - 1
        val elapsedStage = currentState.elapsedSecondsInCurrentStage + 1
        val totalElapsed = currentState.totalElapsedSecondsSession + 1

        var newMateRemaining = currentState.mateRemainingSeconds
        var newLastMateAlertTime = currentState.lastMateAlertTimeMillis

        if (currentState.isMateModeEnabled) {
            newMateRemaining -= 1
            if (newMateRemaining <= 0) {
                triggerMateAlert()
                newMateRemaining = currentState.mateIntervalMinutes * 60L
                newLastMateAlertTime = System.currentTimeMillis()
            }
        }

        if (remaining <= 0) {
            // Stage completed!
            val currentStage = currentState.currentStage
            val currentActivity = currentState.currentActivity
            if (currentStage != null && currentActivity != null) {
                val record = StageExecutionRecord(
                    stageId = currentStage.id,
                    stageName = currentStage.name,
                    activityName = currentActivity.activityTitle,
                    plannedSeconds = currentStage.durationSeconds,
                    actualSeconds = elapsedStage,
                    isSkipped = false,
                    isCompleted = true
                )
                _timerState.update {
                    it.copy(
                        records = it.records + record,
                        totalElapsedSecondsSession = totalElapsed,
                        mateRemainingSeconds = newMateRemaining,
                        lastMateAlertTimeMillis = newLastMateAlertTime
                    )
                }
            }

            triggerAlert()
            advanceToNextStage(fromSkip = false)
        } else {
            _timerState.update {
                it.copy(
                    remainingSeconds = remaining,
                    elapsedSecondsInCurrentStage = elapsedStage,
                    totalElapsedSecondsSession = totalElapsed,
                    mateRemainingSeconds = newMateRemaining,
                    lastMateAlertTimeMillis = newLastMateAlertTime
                )
            }
            checkVoiceAnnouncements(currentState.currentStage, remaining)
            updateNotification()
        }
    }

    private fun checkVoiceAnnouncements(stage: Stage?, remainingSeconds: Long) {
        if (!_timerState.value.isVoiceEnabled || stage == null) return
        val totalSeconds = stage.durationSeconds
        val halfTime = totalSeconds / 2

        when {
            // Countdown last 10 seconds
            totalSeconds > 15 && remainingSeconds == 10L -> {
                voiceManager?.speak("10 segundos")
            }
            totalSeconds > 15 && remainingSeconds in 1L..5L -> {
                voiceManager?.speak(remainingSeconds.toString())
            }

            // 1 minute remaining
            remainingSeconds == 60L && totalSeconds > 60L -> {
                if (halfTime == 60L) {
                    voiceManager?.speak("Último minuto, mitad del tiempo")
                } else {
                    voiceManager?.speak("Último minuto")
                }
            }

            // 5 minutes remaining
            remainingSeconds == 300L && totalSeconds > 300L -> {
                if (halfTime == 300L) {
                    voiceManager?.speak("Quedan 5 minutos, mitad del tiempo")
                } else {
                    voiceManager?.speak("Quedan 5 minutos")
                }
            }

            // Half time (only if total duration >= 120s and not coinciding with 60s or 300s)
            totalSeconds >= 120L && remainingSeconds == halfTime && remainingSeconds != 60L && remainingSeconds != 300L -> {
                voiceManager?.speak("Mitad del tiempo")
            }
        }
    }

    private fun advanceToNextStage(fromSkip: Boolean) {
        val state = _timerState.value
        val currentActivity = state.currentActivity ?: return
        val currentStageIdx = state.currentStageIndex

        if (currentStageIdx + 1 < currentActivity.stages.size) {
            // Next stage in same activity
            val nextStage = currentActivity.stages[currentStageIdx + 1]
            _timerState.update {
                it.copy(
                    currentStageIndex = currentStageIdx + 1,
                    remainingSeconds = nextStage.durationSeconds,
                    elapsedSecondsInCurrentStage = 0
                )
            }
            if (_timerState.value.isVoiceEnabled) {
                if (fromSkip) {
                    voiceManager?.speak("Iniciando: ${nextStage.name}")
                } else {
                    scope.launch {
                        delay(800L) // Wait for vibration & tone
                        voiceManager?.speak("Iniciando: ${nextStage.name}")
                    }
                }
            }
            updateNotification()
        } else {
            // End of current activity! Check if there's a next activity in the routine
            val nextActivityIdx = state.currentActivityIndex + 1
            if (nextActivityIdx < state.routineSteps.size) {
                val nextActivity = state.routineSteps[nextActivityIdx]
                val firstStage = nextActivity.stages.firstOrNull()

                if (firstStage != null) {
                    if (currentActivity.autoAdvance) {
                        // Auto-advance directly
                        _timerState.update {
                            it.copy(
                                currentActivityIndex = nextActivityIdx,
                                currentStageIndex = 0,
                                remainingSeconds = firstStage.durationSeconds,
                                elapsedSecondsInCurrentStage = 0
                            )
                        }
                        if (_timerState.value.isVoiceEnabled) {
                            scope.launch {
                                delay(800L)
                                voiceManager?.speak("Iniciando: ${firstStage.name}")
                            }
                        }
                        updateNotification()
                    } else {
                        // Requires manual confirmation!
                        stopTicker()
                        _timerState.update {
                            it.copy(
                                status = TimerStatus.WAITING_MANUAL_ADVANCE
                            )
                        }
                        updateNotification()
                    }
                } else {
                    finishSession()
                }
            } else {
                // Whole cycle sequence completed!
                if (state.currentCycle < state.totalCycles) {
                    val nextCycle = state.currentCycle + 1
                    if (state.cycleBreakSeconds > 0) {
                        _timerState.update {
                            it.copy(
                                isInCycleBreak = true,
                                cycleBreakRemainingSeconds = state.cycleBreakSeconds
                            )
                        }
                        if (_timerState.value.isVoiceEnabled) {
                            scope.launch {
                                delay(800L)
                                val breakMinutes = state.cycleBreakSeconds / 60
                                val breakText = if (breakMinutes > 0) "$breakMinutes minutos" else "${state.cycleBreakSeconds} segundos"
                                voiceManager?.speak("Ciclo ${state.currentCycle} completado. Descanso de $breakText.")
                            }
                        }
                        updateNotification()
                    } else {
                        // Direct next cycle with no break
                        val firstActivity = state.routineSteps.firstOrNull()
                        val firstStage = firstActivity?.stages?.firstOrNull()
                        if (firstStage != null) {
                            _timerState.update {
                                it.copy(
                                    currentCycle = nextCycle,
                                    currentActivityIndex = 0,
                                    currentStageIndex = 0,
                                    remainingSeconds = firstStage.durationSeconds,
                                    elapsedSecondsInCurrentStage = 0
                                )
                            }
                            if (_timerState.value.isVoiceEnabled) {
                                scope.launch {
                                    delay(800L)
                                    voiceManager?.speak("Iniciando Ciclo $nextCycle de ${state.totalCycles}")
                                }
                            }
                            updateNotification()
                        } else {
                            finishSession()
                        }
                    }
                } else {
                    // All cycles finished!
                    finishSession()
                }
            }
        }
    }

    private fun finishSession() {
        stopTicker()
        val state = _timerState.value
        val completedCount = state.records.count { it.isCompleted && !it.isSkipped }
        val skippedCount = state.records.count { it.isSkipped }

        if (_timerState.value.isVoiceEnabled) {
            scope.launch {
                delay(700L)
                if (state.totalCycles > 1) {
                    voiceManager?.speak("¡Sesión completada! Has terminado todos los ${state.totalCycles} ciclos.")
                } else {
                    voiceManager?.speak("Sesión completada")
                }
            }
        }

        val summary = SessionSummary(
            title = state.sessionTitle,
            contextType = state.contextType,
            parentTitle = state.parentTitle,
            totalPlannedSeconds = state.totalPlannedSecondsSession,
            totalActualSeconds = state.totalElapsedSecondsSession,
            completedStagesCount = completedCount,
            skippedStagesCount = skippedCount,
            stageRecords = state.records
        )

        _timerState.update {
            it.copy(
                status = TimerStatus.COMPLETED,
                completedSummary = summary
            )
        }

        // Save session summary to DB
        appContext?.let { ctx ->
            (ctx.applicationContext as? TaskPieApplication)?.let { app ->
                CoroutineScope(Dispatchers.IO).launch {
                    app.repository.saveSessionSummary(summary)
                }
            }
            stopForegroundService(ctx)
        }
    }

    private fun triggerMateAlert() {
        appContext?.let { ctx ->
            // Subtle distinctive vibration for mate (2 short pulses)
            try {
                val pattern = longArrayOf(0, 160, 120, 160)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    val vibrator = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(pattern, -1)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Subtle notification tone
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 250)
            } catch (t: Exception) {
                t.printStackTrace()
            }
        }

        // Voice announcement if voice is enabled
        if (_timerState.value.isVoiceEnabled) {
            val phrase = matePhrases.random()
            voiceManager?.speak(phrase)
        }
    }

    private fun triggerAlert() {
        appContext?.let { ctx ->
            // Vibrate
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    val vibrator = vibratorManager?.defaultVibrator
                    val pattern = longArrayOf(0, 300, 150, 300, 150, 400)
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    val vibrator = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    val pattern = longArrayOf(0, 300, 150, 300, 150, 400)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(pattern, -1)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Play notification tone
            try {
                val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(ctx, notification)
                ringtone?.play()
            } catch (e: Exception) {
                // Fallback to ToneGenerator
                try {
                    val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
                } catch (t: Exception) {
                    t.printStackTrace()
                }
            }
        }
    }

    private fun startForegroundService(context: Context) {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun updateNotification() {
        appContext?.let { ctx ->
            val intent = Intent(ctx, TimerService::class.java).apply {
                action = TimerService.ACTION_UPDATE_NOTIFICATION
            }
            ctx.startService(intent)
        }
    }

    private fun stopForegroundService(context: Context) {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_STOP
        }
        context.startService(intent)
    }
}
