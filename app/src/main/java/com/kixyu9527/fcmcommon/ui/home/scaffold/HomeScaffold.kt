package com.kixyu9527.fcmcommon.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.kixyu9527.fcmcommon.data.AppThemeMode
import com.kixyu9527.fcmcommon.data.FeatureKey
import com.kixyu9527.fcmcommon.ui.component.bottombar.HomeBottomBarMiuix
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Scaffold

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
    val onSearchQueryChanged: (String) -> Unit,
    val onOnlyPushAppsChanged: (Boolean) -> Unit,
    val onAppAllowedChanged: (String, Boolean) -> Unit,
    val onAllowPushCandidates: () -> Unit,
    val onClearAllowList: () -> Unit,
    val onRefreshApps: () -> Unit,
)

@Composable
internal fun HomeScaffold(
    uiState: HomeUiState,
    listStates: HomePageListStates,
    actions: HomeScaffoldActions,
    isBackHandlerEnabled: Boolean,
) {
    val pagerState = rememberPagerState(
        initialPage = pageIndex(uiState.selectedPage),
        pageCount = { 3 },
    )
    val coroutineScope = rememberCoroutineScope()
    val navEventState = rememberNavigationEventState(NavigationEventInfo.None)

    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = isBackHandlerEnabled && uiState.selectedPage != AppPage.Overview,
        onBackCompleted = {
            coroutineScope.launch {
                pagerState.animateScrollToPage(pageIndex(AppPage.Overview))
            }
        },
    )

    Scaffold(
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth()) {
                HomeBottomBarMiuix(
                    pagerState = pagerState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        },
        contentWindowInsets = WindowInsets.systemBars
            .add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        CompositionLocalProvider(
            LocalHomeBottomPadding provides innerPadding.calculateBottomPadding(),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                HomeContentHost(
                    uiState = uiState,
                    pagerState = pagerState,
                    listStates = listStates,
                    actions = actions,
                )
            }
        }
    }
}

@Composable
internal fun rememberHomePageListStates(): HomePageListStates = HomePageListStates(
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
internal fun rememberHomeScaffoldActions(
    onPageSelected: (AppPage) -> Unit,
    onOpenSecondaryPage: (SecondaryPage) -> Unit,
    onOpenTertiaryPage: (TertiaryPage) -> Unit,
    onOpenAppDetails: (String) -> Unit,
    onFeatureToggle: (FeatureKey, Boolean) -> Unit,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    onShowSystemAppsChanged: (Boolean) -> Unit,
    onShowPackageNameInListChanged: (Boolean) -> Unit,
    onShowDisabledAppsChanged: (Boolean) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onOnlyPushAppsChanged: (Boolean) -> Unit,
    onAppAllowedChanged: (String, Boolean) -> Unit,
    onAllowPushCandidates: () -> Unit,
    onClearAllowList: () -> Unit,
    onRefreshApps: () -> Unit,
): HomeScaffoldActions = remember(
    onPageSelected,
    onOpenSecondaryPage,
    onOpenTertiaryPage,
    onOpenAppDetails,
    onFeatureToggle,
    onThemeModeChanged,
    onShowSystemAppsChanged,
    onShowPackageNameInListChanged,
    onShowDisabledAppsChanged,
    onSearchQueryChanged,
    onOnlyPushAppsChanged,
    onAppAllowedChanged,
    onAllowPushCandidates,
    onClearAllowList,
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
        onSearchQueryChanged = onSearchQueryChanged,
        onOnlyPushAppsChanged = onOnlyPushAppsChanged,
        onAppAllowedChanged = onAppAllowedChanged,
        onAllowPushCandidates = onAllowPushCandidates,
        onClearAllowList = onClearAllowList,
        onRefreshApps = onRefreshApps,
    )
}
