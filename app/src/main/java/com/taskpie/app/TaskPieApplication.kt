package com.taskpie.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.taskpie.app.data.local.TaskPieDatabase
import com.taskpie.app.data.repository.TaskPieRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TaskPieApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { TaskPieDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { TaskPieRepository(database.taskPieDao()) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        
        // Ensure initial templates are seeded
        applicationScope.launch(Dispatchers.IO) {
            repository.checkAndSeedDefaults()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_TIMER_ID,
                "Temporizador de Etapas",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Muestra el progreso y tiempo restante de la etapa activa"
                setShowBadge(false)
            }

            val alertChannel = NotificationChannel(
                CHANNEL_ALERT_ID,
                "Alertas de Transición",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Sonidos y notificaciones cuando termina una etapa o actividad"
                enableVibration(true)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
            manager.createNotificationChannel(alertChannel)
        }
    }

    companion object {
        const val CHANNEL_TIMER_ID = "taskpie_timer_channel"
        const val CHANNEL_ALERT_ID = "taskpie_alert_channel"
    }
}
