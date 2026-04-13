package com.kixyu9527.fcmcommon.ui.home

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlin.math.abs

internal fun pageIndex(page: AppPage): Int = when (page) {
    AppPage.Overview -> 0
    AppPage.Apps -> 1
    AppPage.Settings -> 2
}

internal fun pageAtIndex(index: Int): AppPage = when (index) {
    0 -> AppPage.Overview
    1 -> AppPage.Apps
    else -> AppPage.Settings
}

internal class HomePagerNavigator(
    private val pagerState: PagerState,
    private val coroutineScope: CoroutineScope,
) {
    var selectedPage by mutableIntStateOf(pagerState.currentPage)
        private set

    var isNavigating by mutableStateOf(false)
        private set

    private var navJob: Job? = null

    fun animateToPage(targetIndex: Int) {
        if (targetIndex == selectedPage) return

        navJob?.cancel()
        selectedPage = targetIndex
        isNavigating = true

        val distance = abs(targetIndex - pagerState.currentPage).coerceAtLeast(1)
        val durationMillis = 120 * distance + 140
        val pageSize = pagerState.layoutInfo.pageSize + pagerState.layoutInfo.pageSpacing
        val pagesDelta = targetIndex - pagerState.currentPage - pagerState.currentPageOffsetFraction
        val scrollPixels = pagesDelta * pageSize

        navJob = coroutineScope.launch {
            val currentJob = coroutineContext.job
            try {
                pagerState.animateScrollBy(
                    value = scrollPixels,
                    animationSpec = tween(
                        durationMillis = durationMillis,
                        easing = EaseInOut,
                    ),
                )
            } finally {
                if (navJob == currentJob) {
                    isNavigating = false
                    selectedPage = pagerState.currentPage
                }
            }
        }
    }

    suspend fun snapToPage(targetIndex: Int) {
        navJob?.cancel()
        isNavigating = false
        pagerState.scrollToPage(targetIndex)
        selectedPage = targetIndex
    }

    fun syncPage() {
        if (!isNavigating && selectedPage != pagerState.currentPage) {
            selectedPage = pagerState.currentPage
        }
    }
}

@Composable
internal fun rememberHomePagerNavigator(
    pagerState: PagerState,
    coroutineScope: CoroutineScope,
): HomePagerNavigator = remember(pagerState, coroutineScope) {
    HomePagerNavigator(
        pagerState = pagerState,
        coroutineScope = coroutineScope,
    )
}
