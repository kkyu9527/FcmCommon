package com.kixyu9527.fcmcommon.ui.home

import androidx.activity.BackEventCompat
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.kixyu9527.fcmcommon.data.AppThemeMode
import com.kixyu9527.fcmcommon.data.FeatureKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

private val PredictiveBackEasing = CubicBezierEasing(0.18f, 0.96f, 0.22f, 1f)
private val PredictiveBackBackdropEasing = CubicBezierEasing(0.22f, 0.94f, 0.26f, 1f)
private val SecondaryPageEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
private const val OverlayOpenOffsetFraction = 0.14f
private const val OverlayCloseTravelMultiplier = 1.14f
private const val BaseLayerParallaxFraction = 0.045f
private const val BaseLayerScaleFraction = 0.018f
private const val BaseLayerScaleYFraction = 0.012f

@Composable
fun HomeScaffold(
    uiState: HomeUiState,
    backGestureProgress: Float,
    backSwipeEdge: Int,
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
    val overviewListState = rememberLazyListState()
    val appsListState = rememberLazyListState()
    val settingsListState = rememberLazyListState()
    val appPreferencesListState = rememberLazyListState()
    val featureSettingsListState = rememberLazyListState()
    val diagnosticsListState = rememberLazyListState()
    val moduleLogsListState = rememberLazyListState()
    val appDetailsListState = rememberLazyListState()
    val pagerState = rememberPagerState(
        initialPage = pageIndex(uiState.selectedPage),
        pageCount = { 3 },
    )
    val scope = rememberCoroutineScope()
    val pagerNavigator = rememberHomePagerNavigator(
        pagerState = pagerState,
        coroutineScope = scope,
    )

    LaunchedEffect(uiState.secondaryPage) {
        if (uiState.secondaryPage is SecondaryPage.AppDetails) {
            appDetailsListState.scrollToItem(0)
        }
    }

    LaunchedEffect(uiState.tertiaryPage) {
        if (uiState.tertiaryPage == TertiaryPage.ModuleLogs) {
            moduleLogsListState.scrollToItem(0)
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
            ContentHost(
                uiState = uiState,
                pagerState = pagerState,
                pagerNavigator = pagerNavigator,
                backGestureProgress = backGestureProgress,
                backSwipeEdge = backSwipeEdge,
                onPageSelected = onPageSelected,
                overviewListState = overviewListState,
                appsListState = appsListState,
                settingsListState = settingsListState,
                appPreferencesListState = appPreferencesListState,
                featureSettingsListState = featureSettingsListState,
                diagnosticsListState = diagnosticsListState,
                moduleLogsListState = moduleLogsListState,
                appDetailsListState = appDetailsListState,
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
private fun ContentHost(
    uiState: HomeUiState,
    pagerState: PagerState,
    pagerNavigator: HomePagerNavigator,
    backGestureProgress: Float,
    backSwipeEdge: Int,
    onPageSelected: (AppPage) -> Unit,
    overviewListState: androidx.compose.foundation.lazy.LazyListState,
    appsListState: androidx.compose.foundation.lazy.LazyListState,
    settingsListState: androidx.compose.foundation.lazy.LazyListState,
    appPreferencesListState: androidx.compose.foundation.lazy.LazyListState,
    featureSettingsListState: androidx.compose.foundation.lazy.LazyListState,
    diagnosticsListState: androidx.compose.foundation.lazy.LazyListState,
    moduleLogsListState: androidx.compose.foundation.lazy.LazyListState,
    appDetailsListState: androidx.compose.foundation.lazy.LazyListState,
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
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val contentModifier = Modifier
            .fillMaxSize()
            .clipToBounds()
        val pageWidth = with(LocalDensity.current) { maxWidth.toPx() }
        val offscreenOffset = with(LocalDensity.current) { 36.dp.toPx() }
        val predictiveProgress = backGestureProgress.coerceIn(0f, 1f)
        val edgeDirection = if (backSwipeEdge == BackEventCompat.EDGE_RIGHT) -1f else 1f
        var renderedSecondaryPage by remember { mutableStateOf(uiState.secondaryPage) }
        var renderedTertiaryPage by remember { mutableStateOf(uiState.tertiaryPage) }
        val secondaryVisible = uiState.secondaryPage != null
        val tertiaryVisible = uiState.tertiaryPage != null
        val latestSelectedPage by rememberUpdatedState(uiState.selectedPage)
        val latestSecondaryPage by rememberUpdatedState(uiState.secondaryPage)
        val latestTertiaryPage by rememberUpdatedState(uiState.tertiaryPage)
        val secondaryOverlayProgress by animateFloatAsState(
            targetValue = if (secondaryVisible) 1f else 0f,
            animationSpec = tween(
                durationMillis = if (secondaryVisible) 360 else 420,
                easing = SecondaryPageEasing,
            ),
            finishedListener = { animatedValue ->
                if (animatedValue == 0f && !secondaryVisible) {
                    renderedSecondaryPage = null
                }
            },
            label = "secondary_page_overlay",
        )
        val tertiaryOverlayProgress by animateFloatAsState(
            targetValue = if (tertiaryVisible) 1f else 0f,
            animationSpec = tween(
                durationMillis = if (tertiaryVisible) 360 else 420,
                easing = SecondaryPageEasing,
            ),
            finishedListener = { animatedValue ->
                if (animatedValue == 0f && !tertiaryVisible) {
                    renderedTertiaryPage = null
                }
            },
            label = "tertiary_page_overlay",
        )

        LaunchedEffect(uiState.secondaryPage) {
            if (uiState.secondaryPage != null) {
                renderedSecondaryPage = uiState.secondaryPage
            }
        }

        LaunchedEffect(uiState.tertiaryPage) {
            if (uiState.tertiaryPage != null) {
                renderedTertiaryPage = uiState.tertiaryPage
            }
        }

        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.settledPage }
                .distinctUntilChanged()
                .collect { settledPage ->
                    pagerNavigator.syncPage()
                    if (latestSecondaryPage == null && latestTertiaryPage == null) {
                        val targetPage = pageAtIndex(settledPage)
                        if (targetPage != latestSelectedPage) {
                            onPageSelected(targetPage)
                        }
                    }
                }
        }

        val secondaryOverlayTranslationX = overlayTranslationX(
            overlayVisible = secondaryVisible,
            overlayProgress = secondaryOverlayProgress,
            predictiveProgress = if (tertiaryVisible) 0f else predictiveProgress,
            edgeDirection = edgeDirection,
            pageWidth = pageWidth,
            offscreenOffset = offscreenOffset,
        )
        val tertiaryOverlayTranslationX = overlayTranslationX(
            overlayVisible = tertiaryVisible,
            overlayProgress = tertiaryOverlayProgress,
            predictiveProgress = if (tertiaryVisible) predictiveProgress else 0f,
            edgeDirection = edgeDirection,
            pageWidth = pageWidth,
            offscreenOffset = offscreenOffset,
        )
        val baseLayerProgress = when {
            renderedTertiaryPage != null || tertiaryVisible -> {
                max(tertiaryOverlayProgress, predictiveProgress)
            }

            renderedSecondaryPage != null || secondaryVisible -> {
                max(secondaryOverlayProgress, predictiveProgress)
            }

            else -> 0f
        }.coerceIn(0f, 1f)
        val easedBaseLayerProgress = PredictiveBackBackdropEasing.transform(baseLayerProgress)
        val baseLayerTranslationX = -edgeDirection * pageWidth *
            BaseLayerParallaxFraction *
            easedBaseLayerProgress
        val baseLayerScaleX = 1f - (BaseLayerScaleFraction * easedBaseLayerProgress)
        val baseLayerScaleY = 1f - (BaseLayerScaleYFraction * easedBaseLayerProgress)

        Box(modifier = contentModifier) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = baseLayerTranslationX
                        scaleX = baseLayerScaleX
                        scaleY = baseLayerScaleY
                    },
            ) {
                TopLevelPageContent(
                    uiState = uiState,
                    pagerState = pagerState,
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
            }

            if (renderedSecondaryPage != null || secondaryOverlayProgress > 0f) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1f)
                        .graphicsLayer {
                            alpha = 1f
                            translationX = secondaryOverlayTranslationX
                        },
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    tonalElevation = 0.dp,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                    ) {
                        renderedSecondaryPage?.let { secondaryPage ->
                            SecondaryPageContent(
                                page = secondaryPage,
                                uiState = uiState,
                                appPreferencesListState = appPreferencesListState,
                                featureSettingsListState = featureSettingsListState,
                                diagnosticsListState = diagnosticsListState,
                                onOpenTertiaryPage = onOpenTertiaryPage,
                                appDetailsListState = appDetailsListState,
                                onFeatureToggle = onFeatureToggle,
                                onShowSystemAppsChanged = onShowSystemAppsChanged,
                                onShowPackageNameInListChanged = onShowPackageNameInListChanged,
                                onShowDisabledAppsChanged = onShowDisabledAppsChanged,
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

            if (renderedTertiaryPage != null || tertiaryOverlayProgress > 0f) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(2f)
                        .graphicsLayer {
                            alpha = 1f
                            translationX = tertiaryOverlayTranslationX
                        },
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    tonalElevation = 0.dp,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                    ) {
                        renderedTertiaryPage?.let { tertiaryPage ->
                            TertiaryPageContent(
                                page = tertiaryPage,
                                uiState = uiState,
                                moduleLogsListState = moduleLogsListState,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopLevelPageContent(
    uiState: HomeUiState,
    pagerState: androidx.compose.foundation.pager.PagerState,
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
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 1,
        userScrollEnabled = uiState.secondaryPage == null,
    ) { page ->
        when (pageAtIndex(page)) {
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
    onOpenTertiaryPage: (TertiaryPage) -> Unit,
    appDetailsListState: androidx.compose.foundation.lazy.LazyListState,
    onFeatureToggle: (FeatureKey, Boolean) -> Unit,
    onShowSystemAppsChanged: (Boolean) -> Unit,
    onShowPackageNameInListChanged: (Boolean) -> Unit,
    onShowDisabledAppsChanged: (Boolean) -> Unit,
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
            onShowSystemAppsChanged = onShowSystemAppsChanged,
            onShowPackageNameInListChanged = onShowPackageNameInListChanged,
            onShowDisabledAppsChanged = onShowDisabledAppsChanged,
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
            onOpenModuleLogs = { onOpenTertiaryPage(TertiaryPage.ModuleLogs) },
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
private fun TertiaryPageContent(
    page: TertiaryPage,
    uiState: HomeUiState,
    moduleLogsListState: androidx.compose.foundation.lazy.LazyListState,
) {
    when (page) {
        TertiaryPage.ModuleLogs -> ModuleLogsPage(
            listState = moduleLogsListState,
            uiState = uiState,
        )
    }
}

private fun overlayTranslationX(
    overlayVisible: Boolean,
    overlayProgress: Float,
    predictiveProgress: Float,
    edgeDirection: Float,
    pageWidth: Float,
    offscreenOffset: Float,
): Float {
    val clampedPredictiveProgress = predictiveProgress.coerceIn(0f, 1f)
    val openingOffset = if (overlayVisible) {
        (1f - overlayProgress) * pageWidth * OverlayOpenOffsetFraction
    } else {
        0f
    }
    val closingOffset = if (!overlayVisible && clampedPredictiveProgress == 0f) {
        edgeDirection * ((pageWidth * OverlayCloseTravelMultiplier) + offscreenOffset) * (1f - overlayProgress)
    } else {
        0f
    }

    return when {
        clampedPredictiveProgress > 0f -> {
            val easedProgress = PredictiveBackEasing.transform(clampedPredictiveProgress)
            edgeDirection * ((pageWidth * OverlayCloseTravelMultiplier) + offscreenOffset) * easedProgress
        }

        overlayVisible -> openingOffset
        else -> closingOffset
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

private fun pageAtIndex(index: Int): AppPage = when (index) {
    0 -> AppPage.Overview
    1 -> AppPage.Apps
    else -> AppPage.Settings
}

private class HomePagerNavigator(
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
private fun rememberHomePagerNavigator(
    pagerState: PagerState,
    coroutineScope: CoroutineScope,
): HomePagerNavigator = remember(pagerState, coroutineScope) {
    HomePagerNavigator(
        pagerState = pagerState,
        coroutineScope = coroutineScope,
    )
}
