package com.kixyu9527.fcmcommon.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.kixyu9527.fcmcommon.data.FeatureKey

@Composable
fun HomeScaffold(
    uiState: HomeUiState,
    onPageSelected: (AppPage) -> Unit,
    onFeatureToggle: (FeatureKey, Boolean) -> Unit,
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            PageHeader(page = uiState.selectedPage)
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
            Crossfade(
                targetState = uiState.selectedPage,
                label = "page_crossfade",
            ) { page ->
                when (page) {
                    AppPage.Overview -> OverviewPage(
                        uiState = uiState,
                        onRefreshAll = onRefreshAll,
                        onApplyRecommendedFeatures = onApplyRecommendedFeatures,
                    )
                    AppPage.Apps -> AppsPage(
                        uiState = uiState,
                        onSearchQueryChanged = onSearchQueryChanged,
                        onOnlyPushAppsChanged = onOnlyPushAppsChanged,
                        onAppAllowedChanged = onAppAllowedChanged,
                        onAllowPushCandidates = onAllowPushCandidates,
                        onClearAllowList = onClearAllowList,
                        onRefreshApps = onRefreshApps,
                    )
                    AppPage.Features -> FeaturesPage(
                        uiState = uiState,
                        onFeatureToggle = onFeatureToggle,
                        onApplyRecommendedFeatures = onApplyRecommendedFeatures,
                    )
                    AppPage.Diagnostics -> DiagnosticsPage(
                        uiState = uiState,
                        onRefreshAll = onRefreshAll,
                        onRefreshApps = onRefreshApps,
                    )
                }
            }
            FloatingCapsuleBottomBar(
                selectedPage = uiState.selectedPage,
                onPageSelected = onPageSelected,
            )
        }
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
