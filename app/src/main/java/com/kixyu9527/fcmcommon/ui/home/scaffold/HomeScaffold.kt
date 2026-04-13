package com.kixyu9527.fcmcommon.ui.home

import androidx.activity.ExperimentalActivityApi
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.kixyu9527.fcmcommon.data.AppThemeMode
import com.kixyu9527.fcmcommon.data.FeatureKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect

internal data class HomePageListStates(
    val overview: LazyListState,
    val apps: LazyListState,
    val settings: LazyListState,
    val appPreferences: LazyListState,
    val featureSettings: LazyListState,
    val diagnostics: LazyListState,
    val moduleLogs: LazyListState,
    val appDetails: LazyListState,
)

internal data class HomeScaffoldActions(
    val onPageSelected: (AppPage) -> Unit,
    val onOpenSecondaryPage: (SecondaryPage) -> Unit,
    val onOpenTertiaryPage: (TertiaryPage) -> Unit,
    val onOpenAppDetails: (String) -> Unit,
    val onFeatureToggle: (FeatureKey, Boolean) -> Unit,
    val onThemeModeChanged: (AppThemeMode) -> Unit,
    val onShowSystemAppsChanged: (Boolean) -> Unit,
    val onShowPackageNameInListChanged: (Boolean) -> Unit,
    val onShowDisabledAppsChanged: (Boolean) -> Unit,
    val onApplyRecommendedFeatures: () -> Unit,
    val onSearchQueryChanged: (String) -> Unit,
    val onOnlyPushAppsChanged: (Boolean) -> Unit,
    val onAppAllowedChanged: (String, Boolean) -> Unit,
    val onAllowPushCandidates: () -> Unit,
    val onClearAllowList: () -> Unit,
    val onRefreshAll: () -> Unit,
    val onRefreshApps: () -> Unit,
)

private val PredictiveBackSettleEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

@OptIn(ExperimentalActivityApi::class)
@Composable
fun HomeScaffold(
    uiState: HomeUiState,
    onPageSelected: (AppPage) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenSecondaryPage: (SecondaryPage) -> Unit,
    onOpenTertiaryPage: (TertiaryPage) -> Unit,
    onOpenAppDetails: (String) -> Unit,
    onFeatureToggle: (FeatureKey, Boolean) -> Unit,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    onShowSystemAppsChanged: (Boolean) -> Unit,
    onShowPackageNameInListChanged: (Boolean) -> Unit,
    onShowDisabledAppsChanged: (Boolean) -> Unit,
    onApplyRecommendedFeatures: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onOnlyPushAppsChanged: (Boolean) -> Unit,
    onAppAllowedChanged: (String, Boolean) -> Unit,
    onAllowPushCandidates: () -> Unit,
    onClearAllowList: () -> Unit,
    onRefreshAll: () -> Unit,
    onRefreshApps: () -> Unit,
) {
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            MaterialTheme.colorScheme.background,
        ),
    )
    val listStates = rememberHomePageListStates()
    val pagerState = rememberPagerState(
        initialPage = pageIndex(uiState.selectedPage),
        pageCount = { 3 },
    )
    val pagerNavigator = rememberHomePagerNavigator(
        pagerState = pagerState,
        coroutineScope = rememberCoroutineScope(),
    )
    val predictiveBackState = rememberHomePredictiveBackState()
    val actions = remember(
        onPageSelected,
        onOpenSecondaryPage,
        onOpenTertiaryPage,
        onOpenAppDetails,
        onFeatureToggle,
        onThemeModeChanged,
        onShowSystemAppsChanged,
        onShowPackageNameInListChanged,
        onShowDisabledAppsChanged,
        onApplyRecommendedFeatures,
        onSearchQueryChanged,
        onOnlyPushAppsChanged,
        onAppAllowedChanged,
        onAllowPushCandidates,
        onClearAllowList,
        onRefreshAll,
        onRefreshApps,
    ) {
        HomeScaffoldActions(
            onPageSelected = onPageSelected,
            onOpenSecondaryPage = onOpenSecondaryPage,
            onOpenTertiaryPage = onOpenTertiaryPage,
            onOpenAppDetails = onOpenAppDetails,
            onFeatureToggle = onFeatureToggle,
            onThemeModeChanged = onThemeModeChanged,
            onShowSystemAppsChanged = onShowSystemAppsChanged,
            onShowPackageNameInListChanged = onShowPackageNameInListChanged,
            onShowDisabledAppsChanged = onShowDisabledAppsChanged,
            onApplyRecommendedFeatures = onApplyRecommendedFeatures,
            onSearchQueryChanged = onSearchQueryChanged,
            onOnlyPushAppsChanged = onOnlyPushAppsChanged,
            onAppAllowedChanged = onAppAllowedChanged,
            onAllowPushCandidates = onAllowPushCandidates,
            onClearAllowList = onClearAllowList,
            onRefreshAll = onRefreshAll,
            onRefreshApps = onRefreshApps,
        )
    }

    PredictiveBackHandler(enabled = uiState.canNavigateBack) { progress ->
        val target = if (uiState.tertiaryPage != null) {
            PredictiveBackTarget.Tertiary
        } else {
            PredictiveBackTarget.Secondary
        }

        try {
            progress.collect { backEvent ->
                predictiveBackState.update(target, backEvent)
            }

            if (predictiveBackState.isActive) {
                animate(
                    initialValue = predictiveBackState.progress,
                    targetValue = 1f,
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 120,
                        easing = PredictiveBackSettleEasing,
                    ),
                ) { value, _ ->
                    predictiveBackState.updateProgress(value)
                }
                predictiveBackState.markCommitted()
            }

            onNavigateBack()
        } catch (_: CancellationException) {
            if (predictiveBackState.isActive) {
                animate(
                    initialValue = predictiveBackState.progress,
                    targetValue = 0f,
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 240,
                        easing = PredictiveBackSettleEasing,
                    ),
                ) { value, _ ->
                    predictiveBackState.updateProgress(value)
                }
            }
        } finally {
            if (!predictiveBackState.committed) {
                predictiveBackState.reset()
            }
        }
    }

    LaunchedEffect(uiState.secondaryPage) {
        if (uiState.secondaryPage is SecondaryPage.AppDetails) {
            listStates.appDetails.scrollToItem(0)
        }
    }

    LaunchedEffect(uiState.tertiaryPage) {
        if (uiState.tertiaryPage == TertiaryPage.ModuleLogs) {
            listStates.moduleLogs.scrollToItem(0)
        }
    }

    LaunchedEffect(uiState.selectedPage, uiState.secondaryPage) {
        if (uiState.secondaryPage != null) return@LaunchedEffect
        val targetPage = pageIndex(uiState.selectedPage)
        if (!pagerNavigator.isNavigating && pagerState.currentPage != targetPage) {
            pagerNavigator.snapToPage(targetPage)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            PageHeader(
                title = uiState.headerTitle,
                canNavigateBack = uiState.canNavigateBack,
                onNavigateBack = onNavigateBack,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            HomeBackdrop()
            HomeContentHost(
                uiState = uiState,
                pagerState = pagerState,
                pagerNavigator = pagerNavigator,
                predictiveBackState = predictiveBackState,
                listStates = listStates,
                actions = actions,
            )
            if (uiState.showsBottomBar) {
                FloatingCapsuleBottomBar(
                    selectedPage = pageAtIndex(pagerNavigator.selectedPage),
                    onPageSelected = { page ->
                        if (pageIndex(page) != pagerNavigator.selectedPage) {
                            pagerNavigator.animateToPage(pageIndex(page))
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun rememberHomePageListStates(): HomePageListStates = HomePageListStates(
    overview = rememberLazyListState(),
    apps = rememberLazyListState(),
    settings = rememberLazyListState(),
    appPreferences = rememberLazyListState(),
    featureSettings = rememberLazyListState(),
    diagnostics = rememberLazyListState(),
    moduleLogs = rememberLazyListState(),
    appDetails = rememberLazyListState(),
)

@Composable
private fun HomeBackdrop() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.09f),
                        Color.Transparent,
                    ),
                    radius = 900f,
                ),
            ),
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.04f),
                        Color.Transparent,
                    ),
                ),
            ),
    )
}
