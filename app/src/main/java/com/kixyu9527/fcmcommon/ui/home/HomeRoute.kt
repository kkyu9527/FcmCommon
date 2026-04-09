package com.kixyu9527.fcmcommon.ui.home

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeRoute(viewModel: HomeViewModel = viewModel()) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    HomeScaffold(
        uiState = uiState.value,
        onPageSelected = viewModel::selectPage,
        onFeatureToggle = viewModel::setFeatureEnabled,
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
