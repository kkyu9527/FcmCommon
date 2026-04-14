package com.kixyu9527.fcmcommon.ui.home

import android.os.SystemClock
import com.kixyu9527.fcmcommon.data.FcmDiagnosticsState
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
    val navigationEpoch: Long,
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
    val navigationEpoch: Long,
    val query: String,
    val selectedAppPackage: String?,
)

internal data class HomeRuntimeSeed(
    val apps: List<InstalledAppInfo>,
    val canReadInstalledApps: Boolean,
    val appsLoading: Boolean,
    val appsScanned: Boolean,
    val nowElapsedRealtime: Long,
    val nowWallClockMillis: Long,
    val diagnosticsState: FcmDiagnosticsState,
)

private data class HomeAppRuntimeSeed(
    val apps: List<InstalledAppInfo>,
    val canReadInstalledApps: Boolean,
    val appsLoading: Boolean,
    val appsScanned: Boolean,
)

private data class HomeClockSeed(
    val nowElapsedRealtime: Long,
    val nowWallClockMillis: Long,
    val diagnosticsState: FcmDiagnosticsState,
)

private data class HomeNavigationRouteSeed(
    val page: AppPage,
    val secondaryPage: SecondaryPage?,
    val tertiaryPage: TertiaryPage?,
    val navigationEpoch: Long,
)

internal class HomeNavigationStateContainer {
    private val selectedPage = MutableStateFlow(AppPage.Overview)
    private val secondaryPage = MutableStateFlow<SecondaryPage?>(null)
    private val tertiaryPage = MutableStateFlow<TertiaryPage?>(null)
    private val selectedAppPackage = MutableStateFlow<String?>(null)
    private val searchQuery = MutableStateFlow("")
    private val navigationEpoch = MutableStateFlow(0L)

    val seed = combine(
        combine(
            selectedPage,
            secondaryPage,
            tertiaryPage,
            navigationEpoch,
        ) { page, secondary, tertiary, epoch ->
            HomeNavigationRouteSeed(
                page = page,
                secondaryPage = secondary,
                tertiaryPage = tertiary,
                navigationEpoch = epoch,
            )
        },
        searchQuery,
        selectedAppPackage,
    ) { route, query, appPackage ->
        HomeNavigationSeed(
            page = route.page,
            secondaryPage = route.secondaryPage,
            tertiaryPage = route.tertiaryPage,
            navigationEpoch = route.navigationEpoch,
            query = query,
            selectedAppPackage = appPackage,
        )
    }

    fun selectPage(page: AppPage) {
        tertiaryPage.value = null
        secondaryPage.value = null
        selectedPage.value = page
        bumpNavigationEpoch()
    }

    fun openSecondaryPage(page: SecondaryPage) {
        tertiaryPage.value = null
        secondaryPage.value = page
        bumpNavigationEpoch()
    }

    fun openTertiaryPage(page: TertiaryPage) {
        if (secondaryPage.value != null) {
            tertiaryPage.value = page
            bumpNavigationEpoch()
        }
    }

    fun openAppDetails(packageName: String) {
        selectedAppPackage.value = packageName
        tertiaryPage.value = null
        secondaryPage.value = SecondaryPage.AppDetails(packageName)
        bumpNavigationEpoch()
    }

    fun navigateBack() {
        if (tertiaryPage.value != null) {
            tertiaryPage.value = null
        } else {
            secondaryPage.value = null
        }
        bumpNavigationEpoch()
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    private fun bumpNavigationEpoch() {
        navigationEpoch.value += 1L
    }
}

internal class HomeRuntimeStateContainer(
    initialAppsPermissionGranted: Boolean,
    initialApps: List<InstalledAppInfo>,
    initialAppsScanned: Boolean,
    shouldBootstrapScan: Boolean,
    initialDiagnosticsState: FcmDiagnosticsState,
) {
    private val appsPermissionGranted = MutableStateFlow(initialAppsPermissionGranted)
    private val installedApps = MutableStateFlow(initialApps)
    private val appsLoading = MutableStateFlow(shouldBootstrapScan && initialAppsPermissionGranted)
    private val appsScanned = MutableStateFlow(initialAppsScanned)
    private val clockNow = MutableStateFlow(SystemClock.elapsedRealtime())
    private val wallClockNow = MutableStateFlow(System.currentTimeMillis())
    private val fcmDiagnosticsState = MutableStateFlow(initialDiagnosticsState)

    val seed = combine(
        combine(
            installedApps,
            appsPermissionGranted,
            appsLoading,
            appsScanned,
        ) { apps, canReadInstalledApps, loading, scanned ->
            HomeAppRuntimeSeed(
                apps = apps,
                canReadInstalledApps = canReadInstalledApps,
                appsLoading = loading,
                appsScanned = scanned,
            )
        },
        combine(
            clockNow,
            wallClockNow,
            fcmDiagnosticsState,
        ) { nowElapsedRealtime, nowWallClockMillis, diagnosticsState ->
            HomeClockSeed(
                nowElapsedRealtime = nowElapsedRealtime,
                nowWallClockMillis = nowWallClockMillis,
                diagnosticsState = diagnosticsState,
            )
        },
    ) { appRuntime, clock ->
        HomeRuntimeSeed(
            apps = appRuntime.apps,
            canReadInstalledApps = appRuntime.canReadInstalledApps,
            appsLoading = appRuntime.appsLoading,
            appsScanned = appRuntime.appsScanned,
            nowElapsedRealtime = clock.nowElapsedRealtime,
            nowWallClockMillis = clock.nowWallClockMillis,
            diagnosticsState = clock.diagnosticsState,
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

    fun tick(diagnosticsState: FcmDiagnosticsState) {
        clockNow.value = SystemClock.elapsedRealtime()
        wallClockNow.value = System.currentTimeMillis()
        fcmDiagnosticsState.value = diagnosticsState
    }
}
