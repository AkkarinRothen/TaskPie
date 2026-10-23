package com.taskpie.app.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** A short-lived editor history. Drafts save content separately from this in-memory history. */
class EditorUndoHistory<T>(initial: T) {
    private val undoEntries = mutableListOf<T>()
    private val redoEntries = mutableListOf<T>()

    var current by mutableStateOf(initial)
        private set
    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    fun reset(snapshot: T) {
        undoEntries.clear()
        redoEntries.clear()
        current = snapshot
        updateAvailability()
    }

    fun record(snapshot: T) {
        if (snapshot == current) return
        undoEntries += current
        if (undoEntries.size > 100) undoEntries.removeAt(0)
        current = snapshot
        redoEntries.clear()
        updateAvailability()
    }

    fun undo(): T? {
        if (undoEntries.isEmpty()) return null
        redoEntries += current
        if (redoEntries.size > 100) redoEntries.removeAt(0)
        current = undoEntries.removeAt(undoEntries.lastIndex)
        updateAvailability()
        return current
    }

    fun redo(): T? {
        if (redoEntries.isEmpty()) return null
        undoEntries += current
        if (undoEntries.size > 100) undoEntries.removeAt(0)
        current = redoEntries.removeAt(redoEntries.lastIndex)
        updateAvailability()
        return current
    }

    private fun updateAvailability() {
        canUndo = undoEntries.isNotEmpty()
        canRedo = redoEntries.isNotEmpty()
    }
}

@Composable
fun DragReorderHandle(
    index: Int,
    itemCount: Int,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentIndex by rememberUpdatedState(index)
    val currentOnMove by rememberUpdatedState(onMove)
    val dragThreshold = with(LocalDensity.current) { 48.dp.toPx() }
    Icon(
        imageVector = Icons.Default.DragHandle,
        contentDescription = "Arrastrar para ordenar",
        tint = Color.Gray,
        modifier = modifier.pointerInput(itemCount) {
            var accumulated = 0f
            var movingIndex = index
            detectDragGestures(
                onDragStart = { movingIndex = currentIndex; accumulated = 0f },
                onDragEnd = { accumulated = 0f },
                onDragCancel = { accumulated = 0f }
            ) { change, dragAmount ->
                change.consume()
                accumulated += dragAmount.y
                if (accumulated >= dragThreshold && movingIndex < itemCount - 1) {
                    currentOnMove(movingIndex, movingIndex + 1)
                    movingIndex += 1
                    accumulated -= dragThreshold
                } else if (accumulated <= -dragThreshold && movingIndex > 0) {
                    currentOnMove(movingIndex, movingIndex - 1)
                    movingIndex -= 1
                    accumulated += dragThreshold
                }
            }
        }
    )
}
