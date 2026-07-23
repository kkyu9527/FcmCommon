package com.kixyu9527.fcmcommon.ui.home

import com.kixyu9527.fcmcommon.data.FeatureKey
import com.kixyu9527.fcmcommon.data.InstalledAppInfo
import com.kixyu9527.fcmcommon.data.UiSettings
import com.kixyu9527.fcmcommon.xposed.XposedServiceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

internal data class HomeUiSeed(
    val page: AppPage,
    val secondaryPage: SecondaryPage?,
    val tertiaryPage: TertiaryPage?,
    val settings: UiSettings,
    val connection: XposedServiceState,
    val query: String,
    val enabledFeatures: Set<FeatureKey>,
    val allowList: Set<String>,
    val selectedAppPackage: String?,
)

internal data class HomeNavigationSeed(
    val page: AppPage,
    val secondaryPage: SecondaryPage?,
    val tertiaryPage: TertiaryPage?,
    val query: String,
    val selectedAppPackage: String?,
)

internal data class HomeRuntimeSeed(
    val apps: List<InstalledAppInfo>,
    val canReadInstalledApps: Boolean,
    val appsLoading: Boolean,
    val appsScanned: Boolean,
)

internal class HomeNavigationStateContainer {
    private val selectedPage = MutableStateFlow(AppPage.Overview)
    private val secondaryPage = MutableStateFlow<SecondaryPage?>(null)
    private val tertiaryPage = MutableStateFlow<TertiaryPage?>(null)
    private val selectedAppPackage = MutableStateFlow<String?>(null)
    private val searchQuery = MutableStateFlow("")

    val seed = combine(
        selectedPage,
        secondaryPage,
        tertiaryPage,
        searchQuery,
        selectedAppPackage,
    ) { page, secondary, tertiary, query, appPackage ->
        HomeNavigationSeed(
            page = page,
            secondaryPage = secondary,
            tertiaryPage = tertiary,
            query = query,
            selectedAppPackage = appPackage,
        )
    }

    fun selectPage(page: AppPage) {
        tertiaryPage.value = null
        secondaryPage.value = null
        selectedPage.value = page
    }

    fun openSecondaryPage(page: SecondaryPage) {
        tertiaryPage.value = null
        secondaryPage.value = page
    }

    fun openTertiaryPage(page: TertiaryPage) {
        if (secondaryPage.value != null) {
            tertiaryPage.value = page
        }
    }

    fun openAppDetails(packageName: String) {
        selectedAppPackage.value = packageName
        tertiaryPage.value = null
        secondaryPage.value = SecondaryPage.AppDetails(packageName)
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun syncNavigation(
        page: AppPage,
        secondary: SecondaryPage?,
        tertiary: TertiaryPage?,
        appPackage: String?,
    ) {
        selectedPage.value = page
        secondaryPage.value = secondary
        tertiaryPage.value = tertiary
        selectedAppPackage.value = appPackage
    }
}

internal class HomeRuntimeStateContainer(
    initialAppsPermissionGranted: Boolean,
    initialApps: List<InstalledAppInfo>,
    initialAppsScanned: Boolean,
    shouldBootstrapScan: Boolean,
) {
    private val appsPermissionGranted = MutableStateFlow(initialAppsPermissionGranted)
    private val installedApps = MutableStateFlow(initialApps)
    private val appsLoading = MutableStateFlow(shouldBootstrapScan && initialAppsPermissionGranted)
    private val appsScanned = MutableStateFlow(initialAppsScanned)
    val seed = combine(
        installedApps,
        appsPermissionGranted,
        appsLoading,
        appsScanned,
    ) { apps, canReadInstalledApps, loading, scanned ->
        HomeRuntimeSeed(
            apps = apps,
            canReadInstalledApps = canReadInstalledApps,
            appsLoading = loading,
            appsScanned = scanned,
        )
    }

    fun currentInstalledApps(): List<InstalledAppInfo> = installedApps.value

    fun canReadInstalledApps(): Boolean = appsPermissionGranted.value

    fun isAppsLoading(): Boolean = appsLoading.value

    fun isAppsScanned(): Boolean = appsScanned.value

    fun setAppsPermissionGranted(granted: Boolean) {
        appsPermissionGranted.value = granted
    }

    fun clearAppsState() {
        installedApps.value = emptyList()
        appsScanned.value = false
        appsLoading.value = false
    }

    fun setAppsLoading(loading: Boolean) {
        appsLoading.value = loading
    }

    fun setAppsScanned(scanned: Boolean) {
        appsScanned.value = scanned
    }

    fun setInstalledApps(apps: List<InstalledAppInfo>) {
        installedApps.value = apps
    }
}
