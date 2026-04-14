package com.kixyu9527.fcmcommon.ui.home

import androidx.activity.BackEventCompat
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.preferredFrameRate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged

private val OverlayBackdropEasing = CubicBezierEasing(0.2f, 0.9f, 0.2f, 1f)
private val SecondaryPageEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
private const val OverlayOpenOffsetFraction = 0.14f
private const val OverlayCloseTravelMultiplier = 1.08f
private const val OverlayBaseParallaxFraction = 0.03f
private const val PreferredHighRefreshRate = 120f
private const val OverlayDismissThreshold = 0.001f
private const val OverlayAnimationDurationMillis = 300

@Composable
internal fun HomeContentHost(
    uiState: HomeUiState,
    pagerState: PagerState,
    pagerNavigator: HomePagerNavigator,
    predictiveBackState: HomePredictiveBackState,
    listStates: HomePageListStates,
    actions: HomeScaffoldActions,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val pageWidth = with(LocalDensity.current) { maxWidth.toPx() }
        val offscreenOffset = with(LocalDensity.current) { 36.dp.toPx() }
        val closeTravelDistance = (pageWidth * OverlayCloseTravelMultiplier) + offscreenOffset
        var renderedSecondaryPage by remember { mutableStateOf(uiState.secondaryPage) }
        var renderedTertiaryPage by remember { mutableStateOf(uiState.tertiaryPage) }
        val secondaryVisible = uiState.secondaryPage != null
        val tertiaryVisible = uiState.tertiaryPage != null
        val activeSecondaryPage = uiState.secondaryPage ?: renderedSecondaryPage
        val activeTertiaryPage = uiState.tertiaryPage ?: renderedTertiaryPage
        val latestSelectedPage by rememberUpdatedState(uiState.selectedPage)
        val latestSecondaryPage by rememberUpdatedState(uiState.secondaryPage)
        val latestTertiaryPage by rememberUpdatedState(uiState.tertiaryPage)
        val usesSecondaryPredictiveBack = when (predictiveBackState.target) {
            PredictiveBackTarget.Secondary -> {
                predictiveBackState.isGestureInProgress ||
                    (predictiveBackState.isSettlingDismiss && !secondaryVisible)
            }

            else -> false
        }
        val usesTertiaryPredictiveBack = when (predictiveBackState.target) {
            PredictiveBackTarget.Tertiary -> {
                predictiveBackState.isGestureInProgress ||
                    (predictiveBackState.isSettlingDismiss && !tertiaryVisible)
            }

            else -> false
        }
        val predictiveDirection = if (predictiveBackState.swipeEdge == BackEventCompat.EDGE_RIGHT) {
            -1f
        } else {
            1f
        }
        val secondaryOverlayProgress by animateFloatAsState(
            targetValue = if (secondaryVisible) 1f else 0f,
            animationSpec = tween(
                durationMillis = OverlayAnimationDurationMillis,
                easing = SecondaryPageEasing,
            ),
            label = "secondary_page_overlay",
        )
        val tertiaryOverlayProgress by animateFloatAsState(
            targetValue = if (tertiaryVisible) 1f else 0f,
            animationSpec = tween(
                durationMillis = OverlayAnimationDurationMillis,
                easing = SecondaryPageEasing,
            ),
            label = "tertiary_page_overlay",
        )

        SideEffect {
            uiState.secondaryPage?.let { secondaryPage ->
                renderedSecondaryPage = secondaryPage
            }
            uiState.tertiaryPage?.let { tertiaryPage ->
                renderedTertiaryPage = tertiaryPage
            }
            if (
                !secondaryVisible &&
                renderedSecondaryPage != null &&
                secondaryOverlayProgress <= OverlayDismissThreshold
            ) {
                renderedSecondaryPage = null
            }
            if (
                !tertiaryVisible &&
                renderedTertiaryPage != null &&
                tertiaryOverlayProgress <= OverlayDismissThreshold
            ) {
                renderedTertiaryPage = null
            }
        }

        LaunchedEffect(
            predictiveBackState.committed,
            predictiveBackState.target,
            secondaryVisible,
            tertiaryVisible,
            activeSecondaryPage,
            activeTertiaryPage,
        ) {
            if (!predictiveBackState.committed) return@LaunchedEffect

            when (predictiveBackState.target) {
                PredictiveBackTarget.Secondary -> {
                    if (secondaryVisible || activeSecondaryPage == null) {
                        predictiveBackState.reset()
                    }
                }

                PredictiveBackTarget.Tertiary -> {
                    if (tertiaryVisible || activeTertiaryPage == null) {
                        predictiveBackState.reset()
                    }
                }

                null -> Unit
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
                            actions.onPageSelected(targetPage)
                        }
                    }
                }
        }

        val secondaryOverlayTranslationX = if (usesSecondaryPredictiveBack) {
            closeTravelDistance * predictiveBackState.progress * predictiveDirection
        } else {
            overlayTranslationX(
                overlayVisible = secondaryVisible,
                overlayProgress = secondaryOverlayProgress,
                pageWidth = pageWidth,
                offscreenOffset = offscreenOffset,
            )
        }
        val tertiaryOverlayTranslationX = if (usesTertiaryPredictiveBack) {
            closeTravelDistance * predictiveBackState.progress * predictiveDirection
        } else {
            overlayTranslationX(
                overlayVisible = tertiaryVisible,
                overlayProgress = tertiaryOverlayProgress,
                pageWidth = pageWidth,
                offscreenOffset = offscreenOffset,
            )
        }
        val baseLayerProgress = when {
            usesSecondaryPredictiveBack -> {
                (1f - predictiveBackState.progress).coerceIn(0f, 1f)
            }

            usesTertiaryPredictiveBack -> {
                if (activeSecondaryPage != null || secondaryVisible) 1f else 0f
            }

            activeTertiaryPage != null || tertiaryVisible -> tertiaryOverlayProgress
            activeSecondaryPage != null || secondaryVisible -> secondaryOverlayProgress
            else -> 0f
        }.coerceIn(0f, 1f)
        val easedBaseLayerProgress = OverlayBackdropEasing.transform(baseLayerProgress)
        val baseLayerTranslationX = -pageWidth *
            OverlayBaseParallaxFraction *
            easedBaseLayerProgress
        val prefersHighRefreshRate =
            pagerNavigator.isNavigating ||
                usesSecondaryPredictiveBack ||
                usesTertiaryPredictiveBack ||
                (secondaryVisible && secondaryOverlayProgress < 1f) ||
                (!secondaryVisible && secondaryOverlayProgress > 0f) ||
                (tertiaryVisible && tertiaryOverlayProgress < 1f) ||
                (!tertiaryVisible && tertiaryOverlayProgress > 0f)
        val contentModifier = if (prefersHighRefreshRate) {
            Modifier
                .fillMaxSize()
                .clipToBounds()
                .preferredFrameRate(PreferredHighRefreshRate)
        } else {
            Modifier
                .fillMaxSize()
                .clipToBounds()
        }

        Box(modifier = contentModifier) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = baseLayerTranslationX
                    },
            ) {
                TopLevelPageContent(
                    uiState = uiState,
                    pagerState = pagerState,
                    listStates = listStates,
                    actions = actions,
                )
            }

            if (activeSecondaryPage != null || secondaryOverlayProgress > 0f) {
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
                        activeSecondaryPage?.let { secondaryPage ->
                            SecondaryPageContent(
                                page = secondaryPage,
                                uiState = uiState,
                                listStates = listStates,
                                actions = actions,
                            )
                        }
                    }
                }
            }

            if (activeTertiaryPage != null || tertiaryOverlayProgress > 0f) {
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
                        activeTertiaryPage?.let { tertiaryPage ->
                            TertiaryPageContent(
                                page = tertiaryPage,
                                uiState = uiState,
                                listState = listStates.moduleLogs,
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
    pagerState: PagerState,
    listStates: HomePageListStates,
    actions: HomeScaffoldActions,
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 1,
        userScrollEnabled = uiState.secondaryPage == null,
    ) { page ->
        when (pageAtIndex(page)) {
            AppPage.Overview -> OverviewPage(
                listState = listStates.overview,
                uiState = uiState,
                onRefreshAll = actions.onRefreshAll,
                onApplyRecommendedFeatures = actions.onApplyRecommendedFeatures,
            )

            AppPage.Apps -> AppsPage(
                listState = listStates.apps,
                uiState = uiState,
                onSearchQueryChanged = actions.onSearchQueryChanged,
                onOnlyPushAppsChanged = actions.onOnlyPushAppsChanged,
                onAppAllowedChanged = actions.onAppAllowedChanged,
                onOpenAppDetails = actions.onOpenAppDetails,
                onAllowPushCandidates = actions.onAllowPushCandidates,
                onClearAllowList = actions.onClearAllowList,
                onRefreshApps = actions.onRefreshApps,
            )

            AppPage.Settings -> SettingsPage(
                listState = listStates.settings,
                uiState = uiState,
                onThemeModeChanged = actions.onThemeModeChanged,
                onOpenAppPreferences = { actions.onOpenSecondaryPage(SecondaryPage.AppPreferences) },
                onOpenFeatures = { actions.onOpenSecondaryPage(SecondaryPage.Features) },
                onOpenDiagnostics = { actions.onOpenSecondaryPage(SecondaryPage.Diagnostics) },
            )
        }
    }
}

@Composable
private fun SecondaryPageContent(
    page: SecondaryPage,
    uiState: HomeUiState,
    listStates: HomePageListStates,
    actions: HomeScaffoldActions,
) {
    when (page) {
        SecondaryPage.AppPreferences -> AppPreferencesPage(
            listState = listStates.appPreferences,
            uiState = uiState,
            onOnlyPushAppsChanged = actions.onOnlyPushAppsChanged,
            onShowSystemAppsChanged = actions.onShowSystemAppsChanged,
            onShowPackageNameInListChanged = actions.onShowPackageNameInListChanged,
            onShowDisabledAppsChanged = actions.onShowDisabledAppsChanged,
        )

        SecondaryPage.Features -> FeatureSettingsPage(
            listState = listStates.featureSettings,
            uiState = uiState,
            onFeatureToggle = actions.onFeatureToggle,
            onApplyRecommendedFeatures = actions.onApplyRecommendedFeatures,
        )

        SecondaryPage.Diagnostics -> DiagnosticsPage(
            listState = listStates.diagnostics,
            uiState = uiState,
            onOpenModuleLogs = { actions.onOpenTertiaryPage(TertiaryPage.ModuleLogs) },
            onRefreshAll = actions.onRefreshAll,
            onRefreshApps = actions.onRefreshApps,
        )

        is SecondaryPage.AppDetails -> AppDetailsPage(
            listState = listStates.appDetails,
            uiState = uiState,
            onAppAllowedChanged = actions.onAppAllowedChanged,
        )
    }
}

@Composable
private fun TertiaryPageContent(
    page: TertiaryPage,
    uiState: HomeUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
) {
    when (page) {
        TertiaryPage.ModuleLogs -> ModuleLogsPage(
            listState = listState,
            uiState = uiState,
        )
    }
}

private fun overlayTranslationX(
    overlayVisible: Boolean,
    overlayProgress: Float,
    pageWidth: Float,
    offscreenOffset: Float,
): Float {
    val openingOffset = if (overlayVisible) {
        (1f - overlayProgress) * pageWidth * OverlayOpenOffsetFraction
    } else {
        0f
    }
    val closingOffset = if (!overlayVisible) {
        ((pageWidth * OverlayCloseTravelMultiplier) + offscreenOffset) * (1f - overlayProgress)
    } else {
        0f
    }

    return if (overlayVisible) openingOffset else closingOffset
}
