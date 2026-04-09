package com.kixyu9527.fcmcommon.ui.home

import android.os.Build
import androidx.activity.BackEventCompat
import androidx.activity.ExperimentalActivityApi
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kixyu9527.fcmcommon.ui.theme.FcmCommonTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.delay

@OptIn(ExperimentalActivityApi::class)
@Composable
fun HomeRoute(
    viewModel: HomeViewModel = viewModel(),
    applySystemBarStyle: (Boolean) -> Unit = {},
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val darkTheme = uiState.value.themeMode.resolve(systemDark)
    val backGestureProgress = remember { Animatable(0f) }
    var backSwipeEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }

    SideEffect {
        applySystemBarStyle(darkTheme)
    }

    FcmCommonTheme(darkTheme = darkTheme) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PredictiveBackHandler(enabled = uiState.value.canNavigateBack) { progress ->
                try {
                    progress.collect { backEvent ->
                        backSwipeEdge = backEvent.swipeEdge
                        backGestureProgress.snapTo(backEvent.progress)
                    }
                    viewModel.navigateBack()
                    delay(220L)
                    backGestureProgress.snapTo(0f)
                } catch (error: CancellationException) {
                    backGestureProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 220),
                    )
                }
            }
        } else {
            BackHandler(enabled = uiState.value.canNavigateBack) {
                viewModel.navigateBack()
            }
        }

        HomeScaffold(
            uiState = uiState.value,
            backGestureProgress = backGestureProgress.value,
            backSwipeEdge = backSwipeEdge,
            onPageSelected = viewModel::selectPage,
            onNavigateBack = viewModel::navigateBack,
            onOpenSecondaryPage = viewModel::openSecondaryPage,
            onOpenAppDetails = viewModel::openAppDetails,
            onFeatureToggle = viewModel::setFeatureEnabled,
            onThemeModeChanged = viewModel::setThemeMode,
            onAutoRefreshAppsOnLaunchChanged = viewModel::setAutoRefreshAppsOnLaunch,
            onShowSystemAppsChanged = viewModel::setShowSystemApps,
            onShowPackageNameInListChanged = viewModel::setShowPackageNameInList,
            onShowDisabledAppsChanged = viewModel::setShowDisabledApps,
            onShowVersionNameInListChanged = viewModel::setShowVersionNameInList,
            onApplyRecommendedFeatures = viewModel::applyRecommendedFeatures,
            onSearchQueryChanged = viewModel::setSearchQuery,
            onOnlyPushAppsChanged = viewModel::setOnlyShowPushApps,
            onAppAllowedChanged = viewModel::toggleAppAllowed,
            onAllowPushCandidates = viewModel::allowAllPushCandidates,
            onClearAllowList = viewModel::clearAllowList,
            onRefreshAll = viewModel::refreshAll,
            onRefreshApps = viewModel::refreshApps,
        )
    }
}
