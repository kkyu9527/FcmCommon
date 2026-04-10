package com.kixyu9527.fcmcommon.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kixyu9527.fcmcommon.ui.theme.FcmCommonTheme

@Composable
fun HomeRoute(
    viewModel: HomeViewModel = viewModel(),
    applySystemBarStyle: (Boolean) -> Unit = {},
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val darkTheme = uiState.value.themeMode.resolve(systemDark)
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
        BackHandler(enabled = uiState.value.canNavigateBack) {
            viewModel.navigateBack()
        }

        HomeScaffold(
            uiState = uiState.value,
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
