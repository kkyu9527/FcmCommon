package com.kixyu9527.fcmcommon.ui.home

import androidx.activity.BackEventCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

internal enum class PredictiveBackTarget {
    Secondary,
    Tertiary,
}

@Stable
internal class HomePredictiveBackState {
    var target by mutableStateOf<PredictiveBackTarget?>(null)
        private set

    var progress by mutableFloatStateOf(0f)
        private set

    var swipeEdge by mutableIntStateOf(BackEventCompat.EDGE_LEFT)
        private set

    var committed by mutableStateOf(false)
        private set

    val isActive: Boolean
        get() = target != null

    fun update(target: PredictiveBackTarget, event: BackEventCompat) {
        this.target = target
        swipeEdge = event.swipeEdge
        progress = event.progress.coerceIn(0f, 1f)
        committed = false
    }

    fun updateProgress(value: Float) {
        progress = value.coerceIn(0f, 1f)
    }

    fun markCommitted() {
        committed = true
        progress = 1f
    }

    fun reset() {
        target = null
        progress = 0f
        swipeEdge = BackEventCompat.EDGE_LEFT
        committed = false
    }
}

@Composable
internal fun rememberHomePredictiveBackState(): HomePredictiveBackState = remember {
    HomePredictiveBackState()
}
