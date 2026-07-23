package com.kixyu9527.fcmcommon.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh

@Composable
internal fun HomeContentHost(
    uiState: HomeUiState,
    pagerState: PagerState,
    listStates: HomePageListStates,
    actions: HomeScaffoldActions,
) {
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collectLatest { settledPage ->
                actions.onPageSelected(pageAtIndex(settledPage))
            }
    }

    TopLevelPageContent(
        uiState = uiState,
        pagerState = pagerState,
        listStates = listStates,
        actions = actions,
    )
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
    ) { page ->
        val pageModel = pageAtIndex(page)
        val isCurrentPage = page == pagerState.currentPage

        when (pageModel) {
            AppPage.Overview -> OverviewTopLevelPage(
                uiState = uiState,
                listState = listStates.overview,
                isCurrentPage = isCurrentPage,
            )
            AppPage.Apps -> AppsTopLevelPage(
                uiState = uiState,
                listState = listStates.apps,
                actions = actions,
                isCurrentPage = isCurrentPage,
            )
            AppPage.Settings -> SettingsTopLevelPage(
                uiState = uiState,
                listState = listStates.settings,
                actions = actions,
                isCurrentPage = isCurrentPage,
            )
        }
    }
}

@Composable
private fun OverviewTopLevelPage(
    uiState: HomeUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    isCurrentPage: Boolean,
) {
    ProvideHomeScrollBehavior { scrollBehavior ->
        Scaffold(
            topBar = {
                PageHeader(
                    title = AppPage.Overview.title,
                    canNavigateBack = false,
                    onNavigateBack = {},
                    scrollBehavior = scrollBehavior,
                    testTagsEnabled = isCurrentPage,
                )
            },
            popupHost = { },
            contentWindowInsets = WindowInsets.systemBars
                .add(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Horizontal),
        ) { innerPadding ->
            OverviewPage(
                listState = listState,
                uiState = uiState,
                topPadding = innerPadding.calculateTopPadding(),
            )
        }
    }
}

@Composable
private fun SettingsTopLevelPage(
    uiState: HomeUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    actions: HomeScaffoldActions,
    isCurrentPage: Boolean,
) {
    ProvideHomeScrollBehavior { scrollBehavior ->
        Scaffold(
            topBar = {
                PageHeader(
                    title = AppPage.Settings.title,
                    canNavigateBack = false,
                    onNavigateBack = {},
                    scrollBehavior = scrollBehavior,
                    testTagsEnabled = isCurrentPage,
                )
            },
            popupHost = { },
            contentWindowInsets = WindowInsets.systemBars
                .add(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Horizontal),
        ) { innerPadding ->
            SettingsPage(
                listState = listState,
                uiState = uiState,
                topPadding = innerPadding.calculateTopPadding(),
                onThemeModeChanged = actions.onThemeModeChanged,
                onOpenAppPreferences = { actions.onOpenSecondaryPage(SecondaryPage.AppPreferences) },
                onOpenFeatures = { actions.onOpenSecondaryPage(SecondaryPage.Features) },
                onOpenDiagnostics = { actions.onOpenSecondaryPage(SecondaryPage.Diagnostics) },
            )
        }
    }
}

@Composable
private fun AppsTopLevelPage(
    uiState: HomeUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    actions: HomeScaffoldActions,
    isCurrentPage: Boolean,
) {
    ProvideHomeScrollBehavior { scrollBehavior ->
        Scaffold(
            topBar = {
                PageHeader(
                    title = AppPage.Apps.title,
                    canNavigateBack = false,
                    onNavigateBack = {},
                    scrollBehavior = scrollBehavior,
                    testTagsEnabled = isCurrentPage,
                    actions = {
                        IconButton(onClick = actions.onRefreshApps) {
                            Icon(
                                imageVector = MiuixIcons.Refresh,
                                contentDescription = "重新扫描应用",
                            )
                        }
                    },
                    bottomContent = {
                        InputField(
                            query = uiState.appSearchQuery,
                            onQueryChange = actions.onSearchQueryChanged,
                            onSearch = { _ -> },
                            expanded = false,
                            onExpandedChange = { _ -> },
                            label = "搜索应用",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    },
                )
            },
            popupHost = { },
            contentWindowInsets = WindowInsets.systemBars
                .add(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Horizontal),
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                AppsPage(
                    listState = listState,
                    uiState = uiState,
                    topPadding = innerPadding.calculateTopPadding(),
                    onAppAllowedChanged = actions.onAppAllowedChanged,
                    onOpenAppDetails = actions.onOpenAppDetails,
                    onOnlyPushAppsChanged = actions.onOnlyPushAppsChanged,
                )
            }
        }
    }
}

@Composable
internal fun HomePageLayer(
    title: String,
    canNavigateBack: Boolean,
    onNavigateBack: () -> Unit,
    testTagsEnabled: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    ProvideHomeScrollBehavior { scrollBehavior ->
        Column(modifier = Modifier.fillMaxSize()) {
            PageHeader(
                title = title,
                canNavigateBack = canNavigateBack,
                onNavigateBack = onNavigateBack,
                scrollBehavior = scrollBehavior,
                testTagsEnabled = testTagsEnabled,
                actions = actions,
                bottomContent = bottomContent,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                content()
            }
        }
    }
}
