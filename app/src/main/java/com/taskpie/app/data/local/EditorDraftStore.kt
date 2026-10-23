package com.taskpie.app.data.local

import android.content.Context
import com.google.gson.Gson
import com.taskpie.app.model.ProjectRoutineItem
import com.taskpie.app.model.RoutineBlock
import com.taskpie.app.model.Stage

internal data class ActivityEditorDraft(
    val title: String = "",
    val description: String = "",
    val category: String = "General",
    val isTemplate: Boolean = false,
    val stages: List<Stage> = emptyList(),
    val hasStartedSetup: Boolean = false,
    val creationStep: Int = 0
)

internal data class RoutineEditorDraft(
    val title: String = "",
    val description: String = "",
    val isTemplate: Boolean = false,
    val blocks: List<RoutineBlock> = emptyList(),
    val creationStep: Int = 0
)

internal data class ProjectEditorDraft(
    val title: String = "",
    val description: String = "",
    val selectedActivityIds: List<String> = emptyList(),
    val projectRoutines: List<ProjectRoutineItem> = emptyList(),
    val creationStep: Int = 0
)

internal sealed interface EditorDraftRead<out T> {
    data object Missing : EditorDraftRead<Nothing>
    data class Found<T>(val value: T) : EditorDraftRead<T>
    data object Corrupt : EditorDraftRead<Nothing>
}

internal class EditorDraftStore(context: Context) {
    private val preferences = context.getSharedPreferences("editor_drafts", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun key(type: String, itemId: String?): String =
        if (itemId == null) "$type:create" else "$type:edit:$itemId"

    fun <T : Any> read(key: String, type: Class<T>): EditorDraftRead<T> {
        val json = preferences.getString(key, null) ?: return EditorDraftRead.Missing
        return try {
            gson.fromJson(json, type)?.let(EditorDraftRead<T>::Found) ?: EditorDraftRead.Corrupt
        } catch (_: Exception) {
            EditorDraftRead.Corrupt
        }
    }

    fun save(key: String, draft: Any) {
        preferences.edit().putString(key, gson.toJson(draft)).apply()
    }

    fun delete(key: String) {
        preferences.edit().remove(key).apply()
    }
}
