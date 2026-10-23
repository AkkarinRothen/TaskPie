package com.taskpie.app

import android.Manifest
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.taskpie.app.service.TimerController
import com.taskpie.app.service.TimerStatus
import com.taskpie.app.ui.components.ApkUpdatePrompt
import com.taskpie.app.ui.components.PipTimerView
import com.taskpie.app.ui.navigation.Screen
import com.taskpie.app.ui.navigation.TaskPieNavHost
import com.taskpie.app.ui.navigation.HomeLibraryState
import com.taskpie.app.ui.theme.TaskPieTheme
import com.taskpie.app.ui.viewmodel.TaskPieViewModel

private const val ACTION_PIP_PLAY_PAUSE = "com.taskpie.app.PIP_PLAY_PAUSE"
private const val ACTION_PIP_SKIP = "com.taskpie.app.PIP_SKIP"

class MainActivity : ComponentActivity() {

    private val viewModel: TaskPieViewModel by viewModels()
    private var isInPipMode by mutableStateOf(false)
    private var shouldNavigateToActiveSessionOnRestore = false

    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PIP_PLAY_PAUSE -> {
                    val status = viewModel.timerState.value.status
                    if (status == TimerStatus.RUNNING) {
                        viewModel.pauseTimer()
                    } else if (status == TimerStatus.PAUSED) {
                        viewModel.resumeTimer()
                    } else if (status == TimerStatus.WAITING_MANUAL_ADVANCE) {
                        viewModel.advanceManualNextActivity()
                    }
                    updatePipParams()
                }
                ACTION_PIP_SKIP -> {
                    viewModel.skipCurrentStage()
                    updatePipParams()
                }
            }
        }
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        askNotificationPermission()
        registerPipReceiver()

        setContent {
            TaskPieTheme {
                val timerState by viewModel.timerState.collectAsState()
                val navController = rememberNavController()
                val selectedTabState = rememberSaveable { mutableIntStateOf(0) }
                val categoryFilterState = rememberSaveable { mutableStateOf("Todas") }
                val searchActiveState = rememberSaveable { mutableStateOf(false) }
                val searchQueryState = rememberSaveable { mutableStateOf("") }
                val selectedActivityIdState = rememberSaveable { mutableStateOf<String?>(null) }
                val selectedRoutineIdState = rememberSaveable { mutableStateOf<String?>(null) }
                val selectedProjectIdState = rememberSaveable { mutableStateOf<String?>(null) }
                val activityListState = rememberLazyListState()
                val routineListState = rememberLazyListState()
                val projectListState = rememberLazyListState()
                val historyListState = rememberLazyListState()
                val libraryState = remember {
                    HomeLibraryState(
                        selectedTab = selectedTabState,
                        categoryFilter = categoryFilterState,
                        searchActive = searchActiveState,
                        searchQuery = searchQueryState,
                        selectedActivityId = selectedActivityIdState,
                        selectedRoutineId = selectedRoutineIdState,
                        selectedProjectId = selectedProjectIdState,
                        activityListState = activityListState,
                        routineListState = routineListState,
                        projectListState = projectListState,
                        historyListState = historyListState
                    )
                }

                // Keep PiP actions and auto-enter params updated with timer state
                LaunchedEffect(timerState.status) {
                    updatePipParams()
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    TaskPieNavHost(
                        navController = navController,
                        viewModel = viewModel,
                        libraryState = libraryState
                    )

                    if (isInPipMode) {
                        PipTimerView(timerState = timerState)
                    }

                    if (timerState.status == TimerStatus.IDLE || timerState.status == TimerStatus.COMPLETED) {
                        ApkUpdatePrompt()
                    }
                }

                // Navigate to ActiveSession if returning from PiP
                LaunchedEffect(isInPipMode) {
                    if (!isInPipMode && shouldNavigateToActiveSessionOnRestore) {
                        shouldNavigateToActiveSessionOnRestore = false
                        val status = timerState.status
                        if (status == TimerStatus.RUNNING || status == TimerStatus.PAUSED || status == TimerStatus.WAITING_MANUAL_ADVANCE) {
                            navController.navigate(Screen.ActiveSession.route) {
                                launchSingleTop = true
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val status = viewModel.timerState.value.status
        val isTimerActive = status == TimerStatus.RUNNING || status == TimerStatus.PAUSED || status == TimerStatus.WAITING_MANUAL_ADVANCE
        if (isTimerActive) {
            shouldNavigateToActiveSessionOnRestore = true
            enterPictureInPictureMode(buildPipParams())
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
    }

    private fun registerPipReceiver() {
        val filter = IntentFilter().apply {
            addAction(ACTION_PIP_PLAY_PAUSE)
            addAction(ACTION_PIP_SKIP)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(pipReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(pipReceiver, filter)
        }
    }

    private fun buildPipParams(): PictureInPictureParams {
        val status = viewModel.timerState.value.status
        val isTimerActive = status == TimerStatus.RUNNING || status == TimerStatus.PAUSED || status == TimerStatus.WAITING_MANUAL_ADVANCE

        val playPauseIcon = if (status == TimerStatus.RUNNING) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }
        val playPauseLabel = if (status == TimerStatus.RUNNING) "Pausar" else "Reanudar"

        val playPauseIntent = PendingIntent.getBroadcast(
            this,
            101,
            Intent(ACTION_PIP_PLAY_PAUSE).setPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val playPauseAction = RemoteAction(
            Icon.createWithResource(this, playPauseIcon),
            playPauseLabel,
            playPauseLabel,
            playPauseIntent
        )

        val skipIntent = PendingIntent.getBroadcast(
            this,
            102,
            Intent(ACTION_PIP_SKIP).setPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val skipAction = RemoteAction(
            Icon.createWithResource(this, android.R.drawable.ic_media_next),
            "Saltar etapa",
            "Saltar etapa",
            skipIntent
        )

        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setActions(listOf(playPauseAction, skipAction))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(isTimerActive)
            builder.setSeamlessResizeEnabled(true)
        }

        return builder.build()
    }

    private fun updatePipParams() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setPictureInPictureParams(buildPipParams())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(pipReceiver)
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
