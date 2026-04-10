package com.kixyu9527.fcmcommon.ui.home

import android.os.Build
import androidx.activity.BackEventCompat
import androidx.activity.ExperimentalActivityApi
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kixyu9527.fcmcommon.ui.theme.FcmCommonTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect

private const val PredictiveBackCommitDurationMillis = 420
private const val PredictiveBackCancelDurationMillis = 460
private val PredictiveBackCommitEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val PredictiveBackCancelEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

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
    val lifecycleOwner = LocalLifecycleOwner.current

    SideEffect {
        applySystemBarStyle(darkTheme)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.onHostResumed()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    FcmCommonTheme(darkTheme = darkTheme) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PredictiveBackHandler(enabled = uiState.value.canNavigateBack) { progress ->
                try {
                    backGestureProgress.stop()
                    backGestureProgress.snapTo(0f)
                    progress.collect { backEvent ->
                        backSwipeEdge = backEvent.swipeEdge
                        backGestureProgress.snapTo(backEvent.progress)
                    }
                    viewModel.navigateBack()
                    backGestureProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = PredictiveBackCommitDurationMillis,
                            easing = PredictiveBackCommitEasing,
                        ),
                    )
                    backGestureProgress.snapTo(0f)
                } catch (error: CancellationException) {
                    backGestureProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = PredictiveBackCancelDurationMillis,
                            easing = PredictiveBackCancelEasing,
                        ),
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
            onOpenTertiaryPage = viewModel::openTertiaryPage,
            onOpenAppDetails = viewModel::openAppDetails,
            onFeatureToggle = viewModel::setFeatureEnabled,
            onThemeModeChanged = viewModel::setThemeMode,
            onShowSystemAppsChanged = viewModel::setShowSystemApps,
            onShowPackageNameInListChanged = viewModel::setShowPackageNameInList,
            onShowDisabledAppsChanged = viewModel::setShowDisabledApps,
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
