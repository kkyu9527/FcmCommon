package com.kixyu9527.fcmcommon.ui.home

import androidx.activity.BackEventCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.kixyu9527.fcmcommon.data.AppThemeMode
import com.kixyu9527.fcmcommon.data.FeatureKey
import kotlinx.coroutines.launch

private val PredictiveBackEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val SecondaryPageEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

@Composable
fun HomeScaffold(
    uiState: HomeUiState,
    backGestureProgress: Float,
    backSwipeEdge: Int,
    onPageSelected: (AppPage) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenSecondaryPage: (SecondaryPage) -> Unit,
    onOpenAppDetails: (String) -> Unit,
    onFeatureToggle: (FeatureKey, Boolean) -> Unit,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    onAutoRefreshAppsOnLaunchChanged: (Boolean) -> Unit,
    onShowSystemAppsChanged: (Boolean) -> Unit,
    onShowPackageNameInListChanged: (Boolean) -> Unit,
    onShowDisabledAppsChanged: (Boolean) -> Unit,
    onShowVersionNameInListChanged: (Boolean) -> Unit,
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
    val overviewListState = rememberLazyListState()
    val appsListState = rememberLazyListState()
    val settingsListState = rememberLazyListState()
    val appPreferencesListState = rememberLazyListState()
    val featureSettingsListState = rememberLazyListState()
    val diagnosticsListState = rememberLazyListState()
    val appDetailsListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val activeListState = when (val secondary = uiState.secondaryPage) {
        SecondaryPage.AppPreferences -> appPreferencesListState
        SecondaryPage.Features -> featureSettingsListState
        SecondaryPage.Diagnostics -> diagnosticsListState
        is SecondaryPage.AppDetails -> appDetailsListState
        null -> when (uiState.selectedPage) {
            AppPage.Overview -> overviewListState
            AppPage.Apps -> appsListState
            AppPage.Settings -> settingsListState
        }
    }

    LaunchedEffect(uiState.secondaryPage) {
        if (uiState.secondaryPage is SecondaryPage.AppDetails) {
            appDetailsListState.scrollToItem(0)
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
                onDoubleTapHeader = {
                    coroutineScope.launch {
                        if (activeListState.firstVisibleItemIndex > 0 ||
                            activeListState.firstVisibleItemScrollOffset > 0
                        ) {
                            activeListState.animateScrollToItem(0)
                        }
                    }
                },
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
            ContentHost(
                uiState = uiState,
                backGestureProgress = backGestureProgress,
                backSwipeEdge = backSwipeEdge,
                overviewListState = overviewListState,
                appsListState = appsListState,
                settingsListState = settingsListState,
                appPreferencesListState = appPreferencesListState,
                featureSettingsListState = featureSettingsListState,
                diagnosticsListState = diagnosticsListState,
                appDetailsListState = appDetailsListState,
                onOpenSecondaryPage = onOpenSecondaryPage,
                onOpenAppDetails = onOpenAppDetails,
                onFeatureToggle = onFeatureToggle,
                onThemeModeChanged = onThemeModeChanged,
                onAutoRefreshAppsOnLaunchChanged = onAutoRefreshAppsOnLaunchChanged,
                onShowSystemAppsChanged = onShowSystemAppsChanged,
                onShowPackageNameInListChanged = onShowPackageNameInListChanged,
                onShowDisabledAppsChanged = onShowDisabledAppsChanged,
                onShowVersionNameInListChanged = onShowVersionNameInListChanged,
                onApplyRecommendedFeatures = onApplyRecommendedFeatures,
                onSearchQueryChanged = onSearchQueryChanged,
                onOnlyPushAppsChanged = onOnlyPushAppsChanged,
                onAppAllowedChanged = onAppAllowedChanged,
                onAllowPushCandidates = onAllowPushCandidates,
                onClearAllowList = onClearAllowList,
                onRefreshAll = onRefreshAll,
                onRefreshApps = onRefreshApps,
            )
            if (uiState.showsBottomBar) {
                FloatingCapsuleBottomBar(
                    selectedPage = uiState.selectedPage,
                    onPageSelected = onPageSelected,
                )
            }
        }
    }
}

@Composable
private fun ContentHost(
    uiState: HomeUiState,
    backGestureProgress: Float,
    backSwipeEdge: Int,
    overviewListState: androidx.compose.foundation.lazy.LazyListState,
    appsListState: androidx.compose.foundation.lazy.LazyListState,
    settingsListState: androidx.compose.foundation.lazy.LazyListState,
    appPreferencesListState: androidx.compose.foundation.lazy.LazyListState,
    featureSettingsListState: androidx.compose.foundation.lazy.LazyListState,
    diagnosticsListState: androidx.compose.foundation.lazy.LazyListState,
    appDetailsListState: androidx.compose.foundation.lazy.LazyListState,
    onOpenSecondaryPage: (SecondaryPage) -> Unit,
    onOpenAppDetails: (String) -> Unit,
    onFeatureToggle: (FeatureKey, Boolean) -> Unit,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    onAutoRefreshAppsOnLaunchChanged: (Boolean) -> Unit,
    onShowSystemAppsChanged: (Boolean) -> Unit,
    onShowPackageNameInListChanged: (Boolean) -> Unit,
    onShowDisabledAppsChanged: (Boolean) -> Unit,
    onShowVersionNameInListChanged: (Boolean) -> Unit,
    onApplyRecommendedFeatures: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onOnlyPushAppsChanged: (Boolean) -> Unit,
    onAppAllowedChanged: (String, Boolean) -> Unit,
    onAllowPushCandidates: () -> Unit,
    onClearAllowList: () -> Unit,
    onRefreshAll: () -> Unit,
    onRefreshApps: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val pageWidth = with(LocalDensity.current) { maxWidth.toPx() }
        val predictiveProgress = backGestureProgress.coerceIn(0f, 1f)
        val easedProgress = PredictiveBackEasing.transform(predictiveProgress)
        val edgeDirection = if (backSwipeEdge == BackEventCompat.EDGE_RIGHT) -1f else 1f
        var renderedSecondaryPage by remember { mutableStateOf(uiState.secondaryPage) }
        val overlayVisible = uiState.secondaryPage != null
        val overlayProgress by animateFloatAsState(
            targetValue = if (overlayVisible) 1f else 0f,
            animationSpec = tween(
                durationMillis = if (overlayVisible) 320 else 220,
                easing = SecondaryPageEasing,
            ),
            finishedListener = { animatedValue ->
                if (animatedValue == 0f && !overlayVisible) {
                    renderedSecondaryPage = null
                }
            },
            label = "secondary_page_overlay",
        )

        LaunchedEffect(uiState.secondaryPage) {
            if (uiState.secondaryPage != null) {
                renderedSecondaryPage = uiState.secondaryPage
            }
        }

        val openingOffset = if (overlayVisible) {
            (1f - overlayProgress) * pageWidth * 0.08f
        } else {
            0f
        }
        val closingOffset = if (!overlayVisible && predictiveProgress == 0f) {
            edgeDirection * pageWidth * (1f - overlayProgress) * 0.08f
        } else {
            0f
        }
        val overlayTranslationX = when {
            predictiveProgress > 0f -> edgeDirection * pageWidth * 0.08f * easedProgress
            overlayVisible -> openingOffset
            else -> closingOffset
        }
        val overlayScale = if (predictiveProgress > 0f) {
            1f - (0.08f * easedProgress)
        } else {
            1f
        }

        TopLevelPageContent(
            page = uiState.selectedPage,
            uiState = uiState,
            overviewListState = overviewListState,
            appsListState = appsListState,
            settingsListState = settingsListState,
            onOpenSecondaryPage = onOpenSecondaryPage,
            onOpenAppDetails = onOpenAppDetails,
            onThemeModeChanged = onThemeModeChanged,
            onApplyRecommendedFeatures = onApplyRecommendedFeatures,
            onSearchQueryChanged = onSearchQueryChanged,
            onOnlyPushAppsChanged = onOnlyPushAppsChanged,
            onAppAllowedChanged = onAppAllowedChanged,
            onAllowPushCandidates = onAllowPushCandidates,
            onClearAllowList = onClearAllowList,
            onRefreshAll = onRefreshAll,
            onRefreshApps = onRefreshApps,
        )

        if (renderedSecondaryPage != null || overlayProgress > 0f) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
                    .graphicsLayer {
                        alpha = if (predictiveProgress > 0f) 1f else overlayProgress
                        translationX = overlayTranslationX
                        scaleX = overlayScale
                        scaleY = overlayScale
                        shape = RoundedCornerShape(28.dp)
                        clip = predictiveProgress > 0f
                    },
                color = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
                tonalElevation = 0.dp,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.background,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                                ),
                            ),
                        ),
                ) {
                    renderedSecondaryPage?.let { secondaryPage ->
                        SecondaryPageContent(
                            page = secondaryPage,
                            uiState = uiState,
                            appPreferencesListState = appPreferencesListState,
                            featureSettingsListState = featureSettingsListState,
                            diagnosticsListState = diagnosticsListState,
                            appDetailsListState = appDetailsListState,
                            onFeatureToggle = onFeatureToggle,
                            onAutoRefreshAppsOnLaunchChanged = onAutoRefreshAppsOnLaunchChanged,
                            onShowSystemAppsChanged = onShowSystemAppsChanged,
                            onShowPackageNameInListChanged = onShowPackageNameInListChanged,
                            onShowDisabledAppsChanged = onShowDisabledAppsChanged,
                            onShowVersionNameInListChanged = onShowVersionNameInListChanged,
                            onApplyRecommendedFeatures = onApplyRecommendedFeatures,
                            onOnlyPushAppsChanged = onOnlyPushAppsChanged,
                            onAppAllowedChanged = onAppAllowedChanged,
                            onRefreshAll = onRefreshAll,
                            onRefreshApps = onRefreshApps,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopLevelPageContent(
    page: AppPage,
    uiState: HomeUiState,
    overviewListState: androidx.compose.foundation.lazy.LazyListState,
    appsListState: androidx.compose.foundation.lazy.LazyListState,
    settingsListState: androidx.compose.foundation.lazy.LazyListState,
    onOpenSecondaryPage: (SecondaryPage) -> Unit,
    onOpenAppDetails: (String) -> Unit,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    onApplyRecommendedFeatures: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onOnlyPushAppsChanged: (Boolean) -> Unit,
    onAppAllowedChanged: (String, Boolean) -> Unit,
    onAllowPushCandidates: () -> Unit,
    onClearAllowList: () -> Unit,
    onRefreshAll: () -> Unit,
    onRefreshApps: () -> Unit,
) {
    AnimatedContent(
        targetState = page,
        transitionSpec = {
            val isForward = pageIndex(targetState) > pageIndex(initialState)
            (
                slideInHorizontally(
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                    initialOffsetX = { fullWidth ->
                        if (isForward) fullWidth else -fullWidth
                    },
                ) togetherWith
                    slideOutHorizontally(
                        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                        targetOffsetX = { fullWidth ->
                            if (isForward) -fullWidth else fullWidth
                        },
                    )
                ).using(SizeTransform(clip = false))
        },
        modifier = Modifier.fillMaxSize(),
        label = "top_level_pages",
    ) { currentPage ->
        when (currentPage) {
            AppPage.Overview -> OverviewPage(
                listState = overviewListState,
                uiState = uiState,
                onRefreshAll = onRefreshAll,
                onApplyRecommendedFeatures = onApplyRecommendedFeatures,
            )

            AppPage.Apps -> AppsPage(
                listState = appsListState,
                uiState = uiState,
                onSearchQueryChanged = onSearchQueryChanged,
                onOnlyPushAppsChanged = onOnlyPushAppsChanged,
                onAppAllowedChanged = onAppAllowedChanged,
                onOpenAppDetails = onOpenAppDetails,
                onAllowPushCandidates = onAllowPushCandidates,
                onClearAllowList = onClearAllowList,
                onRefreshApps = onRefreshApps,
            )

            AppPage.Settings -> SettingsPage(
                listState = settingsListState,
                uiState = uiState,
                onThemeModeChanged = onThemeModeChanged,
                onOpenAppPreferences = { onOpenSecondaryPage(SecondaryPage.AppPreferences) },
                onOpenFeatures = { onOpenSecondaryPage(SecondaryPage.Features) },
                onOpenDiagnostics = { onOpenSecondaryPage(SecondaryPage.Diagnostics) },
            )
        }
    }
}

@Composable
private fun SecondaryPageContent(
    page: SecondaryPage,
    uiState: HomeUiState,
    appPreferencesListState: androidx.compose.foundation.lazy.LazyListState,
    featureSettingsListState: androidx.compose.foundation.lazy.LazyListState,
    diagnosticsListState: androidx.compose.foundation.lazy.LazyListState,
    appDetailsListState: androidx.compose.foundation.lazy.LazyListState,
    onFeatureToggle: (FeatureKey, Boolean) -> Unit,
    onAutoRefreshAppsOnLaunchChanged: (Boolean) -> Unit,
    onShowSystemAppsChanged: (Boolean) -> Unit,
    onShowPackageNameInListChanged: (Boolean) -> Unit,
    onShowDisabledAppsChanged: (Boolean) -> Unit,
    onShowVersionNameInListChanged: (Boolean) -> Unit,
    onApplyRecommendedFeatures: () -> Unit,
    onOnlyPushAppsChanged: (Boolean) -> Unit,
    onAppAllowedChanged: (String, Boolean) -> Unit,
    onRefreshAll: () -> Unit,
    onRefreshApps: () -> Unit,
) {
    when (page) {
        SecondaryPage.AppPreferences -> AppPreferencesPage(
            listState = appPreferencesListState,
            uiState = uiState,
            onOnlyPushAppsChanged = onOnlyPushAppsChanged,
            onAutoRefreshAppsOnLaunchChanged = onAutoRefreshAppsOnLaunchChanged,
            onShowSystemAppsChanged = onShowSystemAppsChanged,
            onShowPackageNameInListChanged = onShowPackageNameInListChanged,
            onShowDisabledAppsChanged = onShowDisabledAppsChanged,
            onShowVersionNameInListChanged = onShowVersionNameInListChanged,
        )

        SecondaryPage.Features -> FeatureSettingsPage(
            listState = featureSettingsListState,
            uiState = uiState,
            onFeatureToggle = onFeatureToggle,
            onApplyRecommendedFeatures = onApplyRecommendedFeatures,
        )

        SecondaryPage.Diagnostics -> DiagnosticsPage(
            listState = diagnosticsListState,
            uiState = uiState,
            onRefreshAll = onRefreshAll,
            onRefreshApps = onRefreshApps,
        )

        is SecondaryPage.AppDetails -> AppDetailsPage(
            listState = appDetailsListState,
            uiState = uiState,
            onAppAllowedChanged = onAppAllowedChanged,
        )
    }
}

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

private fun pageIndex(page: AppPage): Int = when (page) {
    AppPage.Overview -> 0
    AppPage.Apps -> 1
    AppPage.Settings -> 2
}
