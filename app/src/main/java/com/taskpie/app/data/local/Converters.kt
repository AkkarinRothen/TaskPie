package com.taskpie.app.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.taskpie.app.model.RoutineStep
import com.taskpie.app.model.Stage
import com.taskpie.app.model.StageExecutionRecord

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStageList(stages: List<Stage>?): String {
        return gson.toJson(stages ?: emptyList<Stage>())
    }

    @TypeConverter
    fun toStageList(json: String?): List<Stage> {
        if (json.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<Stage>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    @TypeConverter
    fun fromRoutineStepList(steps: List<RoutineStep>?): String {
        return gson.toJson(steps ?: emptyList<RoutineStep>())
    }

    @TypeConverter
    fun toRoutineStepList(json: String?): List<RoutineStep> {
        if (json.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<RoutineStep>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        return gson.toJson(list ?: emptyList<String>())
    }

    @TypeConverter
    fun toStringList(json: String?): List<String> {
        if (json.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    @TypeConverter
    fun fromStageRecordList(records: List<StageExecutionRecord>?): String {
        return gson.toJson(records ?: emptyList<StageExecutionRecord>())
    }

    @TypeConverter
    fun toStageRecordList(json: String?): List<StageExecutionRecord> {
        if (json.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<StageExecutionRecord>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    @TypeConverter
    fun fromProjectRoutineList(routines: List<com.taskpie.app.model.ProjectRoutineItem>?): String {
        return gson.toJson(routines ?: emptyList<com.taskpie.app.model.ProjectRoutineItem>())
    }

    @TypeConverter
    fun toProjectRoutineList(json: String?): List<com.taskpie.app.model.ProjectRoutineItem> {
        if (json.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<com.taskpie.app.model.ProjectRoutineItem>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
}
