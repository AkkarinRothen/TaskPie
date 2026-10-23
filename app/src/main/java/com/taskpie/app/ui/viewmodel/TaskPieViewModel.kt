package com.taskpie.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.taskpie.app.TaskPieApplication
import com.taskpie.app.data.local.ActivityEditorDraft
import com.taskpie.app.data.local.EditorDraftRead
import com.taskpie.app.data.local.EditorDraftStore
import com.taskpie.app.data.local.ProjectEditorDraft
import com.taskpie.app.data.local.RoutineEditorDraft
import com.taskpie.app.model.ActivityItem
import com.taskpie.app.model.FolderItem
import com.taskpie.app.model.ProjectItem
import com.taskpie.app.model.RoutineItem
import com.taskpie.app.model.SessionSummary
import com.taskpie.app.service.TimerController
import com.taskpie.app.service.TimerState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class TaskPieViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as TaskPieApplication).repository
    private val editorDraftStore = EditorDraftStore(application)

    internal fun editorDraftKey(type: String, itemId: String?): String = editorDraftStore.key(type, itemId)

    internal fun readActivityDraft(key: String) = editorDraftStore.read(key, ActivityEditorDraft::class.java)
    internal fun readRoutineDraft(key: String) = editorDraftStore.read(key, RoutineEditorDraft::class.java)
    internal fun readProjectDraft(key: String) = editorDraftStore.read(key, ProjectEditorDraft::class.java)

    internal fun saveEditorDraft(key: String, draft: Any) = editorDraftStore.save(key, draft)
    internal fun deleteEditorDraft(key: String) = editorDraftStore.delete(key)

    init {
        TimerController.initialize(application)
        viewModelScope.launch {
            repository.checkAndSeedFolders()
        }
    }

    val timerState: StateFlow<TimerState> = TimerController.timerState

    val folders: StateFlow<List<FolderItem>> = repository.allFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activities: StateFlow<List<ActivityItem>> = repository.allActivities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val routines: StateFlow<List<RoutineItem>> = repository.allRoutines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projects: StateFlow<List<ProjectItem>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessionHistory: StateFlow<List<SessionSummary>> = repository.sessionHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Folders CRUD ---
    fun saveFolder(folder: FolderItem) {
        viewModelScope.launch {
            repository.saveFolder(folder)
        }
    }

    fun deleteFolder(id: String) {
        viewModelScope.launch {
            repository.deleteFolder(id)
        }
    }

    // --- Activities CRUD ---
    fun saveActivity(activity: ActivityItem) {
        viewModelScope.launch {
            repository.saveActivity(activity)
        }
    }

    fun duplicateActivity(activity: ActivityItem) {
        viewModelScope.launch {
            val copy = activity.copy(
                id = UUID.randomUUID().toString(),
                title = "${activity.title} (Copia)",
                isTemplate = false,
                createdAt = System.currentTimeMillis()
            )
            repository.saveActivity(copy)
        }
    }

    fun deleteActivity(id: String) {
        viewModelScope.launch {
            repository.deleteActivity(id)
        }
    }

    // --- Routines CRUD ---
    fun saveRoutine(routine: RoutineItem) {
        viewModelScope.launch {
            repository.saveRoutine(routine)
        }
    }

    fun duplicateRoutine(routine: RoutineItem) {
        viewModelScope.launch {
            val copy = routine.copy(
                id = UUID.randomUUID().toString(),
                title = "${routine.title} (Copia)",
                isTemplate = false,
                createdAt = System.currentTimeMillis()
            )
            repository.saveRoutine(copy)
        }
    }

    fun deleteRoutine(id: String) {
        viewModelScope.launch {
            repository.deleteRoutine(id)
        }
    }

    // --- Projects CRUD ---
    fun saveProject(project: ProjectItem) {
        viewModelScope.launch {
            repository.saveProject(project)
        }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            repository.deleteProject(id)
        }
    }

    fun deleteSessionSummary(id: String) {
        viewModelScope.launch {
            repository.deleteSessionSummary(id)
        }
    }

    // --- Timer Controls ---
    fun getVoiceEnabledPreference(): Boolean {
        return TimerController.getVoiceEnabledPreference(getApplication())
    }

    fun toggleVoice() {
        TimerController.toggleVoice()
    }

    fun startActivitySession(
        activity: ActivityItem,
        sessionName: String? = null,
        voiceEnabled: Boolean? = null,
        repeatCount: Int = 1,
        cycleBreakSeconds: Long = 0L
    ) {
        TimerController.startSingleActivitySession(getApplication(), activity, sessionName, voiceEnabled, repeatCount, cycleBreakSeconds)
    }

    fun startRoutineSession(
        routine: RoutineItem,
        sessionName: String? = null,
        voiceEnabled: Boolean? = null,
        repeatCount: Int = 1,
        cycleBreakSeconds: Long = 0L
    ) {
        TimerController.startRoutineSession(getApplication(), routine, sessionName, voiceEnabled, repeatCount, cycleBreakSeconds)
    }

    fun startProjectSession(
        project: ProjectItem,
        sessionName: String? = null,
        voiceEnabled: Boolean? = null,
        repeatCount: Int = 1,
        cycleBreakSeconds: Long = 0L
    ) {
        TimerController.startProjectSession(
            getApplication(),
            project,
            activities.value,
            routines.value,
            sessionName,
            voiceEnabled,
            repeatCount,
            cycleBreakSeconds
        )
    }

    fun startProjectActivitySession(
        projectName: String,
        activity: ActivityItem,
        sessionName: String? = null,
        voiceEnabled: Boolean? = null,
        repeatCount: Int = 1,
        cycleBreakSeconds: Long = 0L
    ) {
        TimerController.startProjectActivitySession(getApplication(), projectName, activity, sessionName, voiceEnabled, repeatCount, cycleBreakSeconds)
    }

    fun pauseTimer() {
        TimerController.pause()
    }

    fun resumeTimer() {
        TimerController.resume(getApplication())
    }

    fun skipCurrentStage() {
        TimerController.skipCurrentStage()
    }

    fun advanceManualNextActivity() {
        TimerController.advanceManualNextActivity()
    }

    fun stopSession() {
        TimerController.stopSession(getApplication())
    }

    fun toggleMateMode() {
        TimerController.toggleMateMode()
    }

    fun setMateInterval(minutes: Int) {
        TimerController.setMateInterval(minutes)
    }

    fun resetMateCountdown() {
        TimerController.resetMateCountdown()
    }
}
