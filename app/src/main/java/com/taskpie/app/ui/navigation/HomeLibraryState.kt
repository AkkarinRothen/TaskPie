package com.taskpie.app.ui.navigation

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState

internal class HomeLibraryState(
    val selectedTab: MutableIntState,
    val categoryFilter: MutableState<String>,
    val searchActive: MutableState<Boolean>,
    val searchQuery: MutableState<String>,
    val selectedActivityId: MutableState<String?>,
    val selectedRoutineId: MutableState<String?>,
    val selectedProjectId: MutableState<String?>,
    val activityListState: LazyListState,
    val routineListState: LazyListState,
    val projectListState: LazyListState,
    val historyListState: LazyListState
)
