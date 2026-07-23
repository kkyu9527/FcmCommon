package com.kixyu9527.fcmcommon.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.kixyu9527.fcmcommon.ui.navigation3.Route
import com.kixyu9527.fcmcommon.ui.theme.FcmCommonTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun HomeRoute(
    viewModel: HomeViewModel = viewModel(),
    applySystemBarStyle: (Boolean) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val darkTheme = uiState.themeMode.resolve(systemDark)
    val lifecycleOwner = LocalLifecycleOwner.current
    val backStack = rememberNavBackStack(Route.Main)

    LaunchedEffect(darkTheme) {
        applySystemBarStyle(darkTheme)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onHostResumed()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    FcmCommonTheme(darkTheme = darkTheme) {
        val listStates = rememberHomePageListStates()
        val onNavigateBack = {
            if (backStack.size > 1) {
                backStack.removeAt(backStack.lastIndex)
            }
        }
        val actions = rememberHomeScaffoldActions(
            onPageSelected = viewModel::selectPage,
            onOpenSecondaryPage = { page ->
                viewModel.openSecondaryPage(page)
                backStack.add(routeFor(page))
            },
            onOpenTertiaryPage = { page ->
                viewModel.openTertiaryPage(page)
                backStack.add(routeFor(page))
            },
            onOpenAppDetails = { packageName ->
                viewModel.openAppDetails(packageName)
                backStack.add(Route.AppDetails(packageName))
            },
            onFeatureToggle = viewModel::setFeatureEnabled,
            onThemeModeChanged = viewModel::setThemeMode,
            onShowSystemAppsChanged = viewModel::setShowSystemApps,
            onShowPackageNameInListChanged = viewModel::setShowPackageNameInList,
            onShowDisabledAppsChanged = viewModel::setShowDisabledApps,
            onSearchQueryChanged = viewModel::setSearchQuery,
            onOnlyPushAppsChanged = viewModel::setOnlyShowPushApps,
            onAppAllowedChanged = viewModel::toggleAppAllowed,
            onAllowPushCandidates = viewModel::allowAllPushCandidates,
            onClearAllowList = viewModel::clearAllowList,
            onRefreshApps = viewModel::refreshApps,
        )

        LaunchedEffect(backStack.toList(), uiState.selectedPage) {
            val syncState = navigationStateForRoute(
                route = backStack.lastOrNull(),
                selectedPage = uiState.selectedPage,
            )
            viewModel.syncNavigation(
                page = syncState.page,
                secondaryPage = syncState.secondaryPage,
                tertiaryPage = syncState.tertiaryPage,
                selectedAppPackage = syncState.selectedAppPackage,
            )
        }

        NavDisplay(
                backStack = backStack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                onBack = {
                    if (backStack.lastOrNull() != Route.Main) {
                        onNavigateBack()
                    }
                },
                entryProvider = entryProvider {
                    entry<Route.Main> {
                        HomeScaffold(
                            uiState = uiState,
                            listStates = listStates,
                            actions = actions,
                        )
                    }
                    entry<Route.AppPreferences> {
                        HomeSecondaryDestination(
                            title = SecondaryPage.AppPreferences.title,
                            onNavigateBack = onNavigateBack,
                        ) {
                            AppPreferencesPage(
                                listState = listStates.appPreferences,
                                uiState = uiState,
                                onOnlyPushAppsChanged = actions.onOnlyPushAppsChanged,
                                onShowSystemAppsChanged = actions.onShowSystemAppsChanged,
                                onShowPackageNameInListChanged = actions.onShowPackageNameInListChanged,
                                onShowDisabledAppsChanged = actions.onShowDisabledAppsChanged,
                                onAllowPushCandidates = actions.onAllowPushCandidates,
                                onClearAllowList = actions.onClearAllowList,
                            )
                        }
                    }
                    entry<Route.Features> {
                        HomeSecondaryDestination(
                            title = SecondaryPage.Features.title,
                            onNavigateBack = onNavigateBack,
                        ) {
                            FeatureSettingsPage(
                                listState = listStates.featureSettings,
                                uiState = uiState,
                                onFeatureToggle = actions.onFeatureToggle,
                            )
                        }
                    }
                    entry<Route.Diagnostics> {
                        HomeSecondaryDestination(
                            title = SecondaryPage.Diagnostics.title,
                            onNavigateBack = onNavigateBack,
                        ) {
                            DiagnosticsPage(
                                listState = listStates.diagnostics,
                                uiState = uiState,
                                onOpenModuleLogs = { actions.onOpenTertiaryPage(TertiaryPage.ModuleLogs) },
                            )
                        }
                    }
                    entry<Route.ModuleLogs> {
                        LaunchedEffect(Unit) {
                            listStates.moduleLogs.scrollToItem(0)
                        }
                        HomeSecondaryDestination(
                            title = TertiaryPage.ModuleLogs.title,
                            onNavigateBack = onNavigateBack,
                        ) {
                            ModuleLogsPage(
                                listState = listStates.moduleLogs,
                                uiState = uiState,
                            )
                        }
                    }
                    entry<Route.AppDetails> { route ->
                        LaunchedEffect(route.packageName) {
                            listStates.appDetails.scrollToItem(0)
                        }
                        HomeSecondaryDestination(
                            title = uiState.selectedAppDetails?.label ?: SecondaryPage.AppDetails(route.packageName).title,
                            onNavigateBack = onNavigateBack,
                        ) {
                            AppDetailsPage(
                                listState = listStates.appDetails,
                                uiState = uiState,
                                onAppAllowedChanged = actions.onAppAllowedChanged,
                            )
                        }
                    }
                },
            )
    }
}

@Composable
private fun HomeSecondaryDestination(
    title: String,
    onNavigateBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    HomePageLayer(
        title = title,
        canNavigateBack = true,
        onNavigateBack = onNavigateBack,
        testTagsEnabled = true,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background),
        ) {
            content()
        }
    }
}

private data class HomeRouteNavigationState(
    val page: AppPage,
    val secondaryPage: SecondaryPage?,
    val tertiaryPage: TertiaryPage?,
    val selectedAppPackage: String?,
)

private fun routeFor(page: SecondaryPage): Route = when (page) {
    SecondaryPage.AppPreferences -> Route.AppPreferences
    SecondaryPage.Features -> Route.Features
    SecondaryPage.Diagnostics -> Route.Diagnostics
    is SecondaryPage.AppDetails -> Route.AppDetails(page.packageName)
}

private fun routeFor(page: TertiaryPage): Route = when (page) {
    TertiaryPage.ModuleLogs -> Route.ModuleLogs
}

private fun navigationStateForRoute(
    route: androidx.navigation3.runtime.NavKey?,
    selectedPage: AppPage,
): HomeRouteNavigationState = when (route) {
    Route.Main,
    null -> HomeRouteNavigationState(
        page = selectedPage,
        secondaryPage = null,
        tertiaryPage = null,
        selectedAppPackage = null,
    )

    Route.AppPreferences -> HomeRouteNavigationState(
        page = AppPage.Settings,
        secondaryPage = SecondaryPage.AppPreferences,
        tertiaryPage = null,
        selectedAppPackage = null,
    )

    Route.Features -> HomeRouteNavigationState(
        page = AppPage.Settings,
        secondaryPage = SecondaryPage.Features,
        tertiaryPage = null,
        selectedAppPackage = null,
    )

    Route.Diagnostics -> HomeRouteNavigationState(
        page = AppPage.Settings,
        secondaryPage = SecondaryPage.Diagnostics,
        tertiaryPage = null,
        selectedAppPackage = null,
    )

    Route.ModuleLogs -> HomeRouteNavigationState(
        page = AppPage.Settings,
        secondaryPage = SecondaryPage.Diagnostics,
        tertiaryPage = TertiaryPage.ModuleLogs,
        selectedAppPackage = null,
    )

    is Route.AppDetails -> HomeRouteNavigationState(
        page = AppPage.Apps,
        secondaryPage = SecondaryPage.AppDetails(route.packageName),
        tertiaryPage = null,
        selectedAppPackage = route.packageName,
    )

    else -> HomeRouteNavigationState(
        page = selectedPage,
        secondaryPage = null,
        tertiaryPage = null,
        selectedAppPackage = null,
    )
}
