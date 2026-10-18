package com.taskpie.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.taskpie.app.model.BuiltInTemplates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ActivityEntity::class,
        RoutineEntity::class,
        ProjectEntity::class,
        SessionSummaryEntity::class,
        FolderEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TaskPieDatabase : RoomDatabase() {

    abstract fun taskPieDao(): TaskPieDao

    companion object {
        @Volatile
        private var INSTANCE: TaskPieDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): TaskPieDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskPieDatabase::class.java,
                    "taskpie_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDatabase(database.taskPieDao())
                    }
                }
            }

            suspend fun populateDatabase(dao: TaskPieDao) {
                // Seed default folders
                val defaultFolders = listOf(
                    FolderEntity(id = "folder_escritura", name = "Escritura", colorHex = "#3B82F6", createdAt = 1),
                    FolderEntity(id = "folder_bienestar", name = "Bienestar", colorHex = "#10B981", createdAt = 2),
                    FolderEntity(id = "folder_hogar", name = "Hogar", colorHex = "#F97316", createdAt = 3),
                    FolderEntity(id = "folder_estudio", name = "Estudio", colorHex = "#6366F1", createdAt = 4),
                    FolderEntity(id = "folder_trabajo", name = "Trabajo", colorHex = "#8B5CF6", createdAt = 5)
                )
                dao.insertFolders(defaultFolders)

                // Seed default template activities
                val activityEntities = BuiltInTemplates.DEFAULT_ACTIVITIES.map {
                    ActivityEntity.fromDomain(it)
                }
                dao.insertActivities(activityEntities)

                // Seed default routine
                val routineEntities = BuiltInTemplates.DEFAULT_ROUTINES.map {
                    RoutineEntity.fromDomain(it)
                }
                dao.insertRoutines(routineEntities)

                // Seed default project
                dao.insertProject(ProjectEntity.fromDomain(BuiltInTemplates.DEFAULT_PROJECT))
            }
        }
    }
}
