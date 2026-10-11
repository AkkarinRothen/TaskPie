package com.taskpie.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.taskpie.app.MainActivity
import com.taskpie.app.TaskPieApplication

class TimerService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForegroundWithNotification()
            }
            ACTION_PAUSE -> {
                TimerController.pause()
            }
            ACTION_RESUME -> {
                TimerController.resume(this)
            }
            ACTION_SKIP_STAGE -> {
                TimerController.skipCurrentStage()
            }
            ACTION_NEXT_MANUAL -> {
                TimerController.advanceManualNextActivity()
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_UPDATE_NOTIFICATION -> {
                updateNotification()
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        val state = TimerController.timerState.value
        if (state.status == TimerStatus.IDLE || state.status == TimerStatus.COMPLETED) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val state = TimerController.timerState.value
        val stageName = state.currentStage?.name ?: "Etapa"
        val activityName = state.currentActivity?.activityTitle ?: "Actividad"
        val timeFormatted = state.formattedRemainingTime()

        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, TaskPieApplication.CHANNEL_TIMER_ID)
            .setContentTitle("$timeFormatted • $stageName")
            .setContentText("$activityName (${state.currentStageIndex + 1}/${state.currentActivity?.stages?.size ?: 1})")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(contentPendingIntent)
            .setOngoing(state.status == TimerStatus.RUNNING)
            .setOnlyAlertOnce(true)

        // Actions
        if (state.status == TimerStatus.RUNNING) {
            val pauseIntent = Intent(this, TimerService::class.java).apply { action = ACTION_PAUSE }
            val pausePending = PendingIntent.getService(
                this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_pause, "Pausar", pausePending)

            val skipIntent = Intent(this, TimerService::class.java).apply { action = ACTION_SKIP_STAGE }
            val skipPending = PendingIntent.getService(
                this, 2, skipIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_next, "Saltar", skipPending)
        } else if (state.status == TimerStatus.PAUSED) {
            val resumeIntent = Intent(this, TimerService::class.java).apply { action = ACTION_RESUME }
            val resumePending = PendingIntent.getService(
                this, 3, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_play, "Reanudar", resumePending)
        } else if (state.status == TimerStatus.WAITING_MANUAL_ADVANCE) {
            val nextIntent = Intent(this, TimerService::class.java).apply { action = ACTION_NEXT_MANUAL }
            val nextPending = PendingIntent.getService(
                this, 4, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_play, "Iniciar Siguiente", nextPending)
        }

        return builder.build()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.taskpie.app.service.ACTION_START"
        const val ACTION_PAUSE = "com.taskpie.app.service.ACTION_PAUSE"
        const val ACTION_RESUME = "com.taskpie.app.service.ACTION_RESUME"
        const val ACTION_SKIP_STAGE = "com.taskpie.app.service.ACTION_SKIP_STAGE"
        const val ACTION_NEXT_MANUAL = "com.taskpie.app.service.ACTION_NEXT_MANUAL"
        const val ACTION_STOP = "com.taskpie.app.service.ACTION_STOP"
        const val ACTION_UPDATE_NOTIFICATION = "com.taskpie.app.service.ACTION_UPDATE_NOTIFICATION"
    }
}
