package com.kixyu9527.fcmcommon.ui.home

import android.app.Application
import android.os.SystemClock
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kixyu9527.fcmcommon.appContainer
import com.kixyu9527.fcmcommon.data.AppThemeMode
import com.kixyu9527.fcmcommon.data.ConnectionEvent
import com.kixyu9527.fcmcommon.data.FcmDiagnosticsState
import com.kixyu9527.fcmcommon.data.FeatureKey
import com.kixyu9527.fcmcommon.data.InstalledAppInfo
import com.kixyu9527.fcmcommon.data.ModuleScope
import com.kixyu9527.fcmcommon.data.UiSettings
import com.kixyu9527.fcmcommon.xposed.XposedServiceState
import java.text.SimpleDateFormat
import java.text.Collator
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppPage(
    val route: String,
    val title: String,
) {
    Overview(
        route = "overview",
        title = "总览",
    ),
    Apps(
        route = "apps",
        title = "应用",
    ),
    Settings(
        route = "settings",
        title = "设置",
    ),
}

sealed class SecondaryPage(
    val route: String,
    val title: String,
) {
    data object AppPreferences : SecondaryPage(
        route = "app_preferences",
        title = "应用偏好",
    )

    data object Features : SecondaryPage(
        route = "features",
        title = "功能配置",
    )

    data object Diagnostics : SecondaryPage(
        route = "diagnostics",
        title = "模块状态",
    )

    data class AppDetails(
        val packageName: String,
    ) : SecondaryPage(
        route = "app_details",
        title = "应用详情",
    )
}

data class FeatureCardModel(
    val key: FeatureKey,
    val enabled: Boolean,
)

data class OverviewStatModel(
    val label: String,
    val value: String,
    val hint: String,
)

data class AppRowModel(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val hasPushSupport: Boolean,
    val isSystemApp: Boolean,
    val isAllowed: Boolean,
    val isEnabled: Boolean,
    val versionName: String,
)

data class AppDetailInfoModel(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val hasPushSupport: Boolean,
    val isSystemApp: Boolean,
    val isAllowed: Boolean,
    val isEnabled: Boolean,
    val versionName: String,
    val versionCode: Long,
    val targetSdkVersion: Int,
    val minSdkVersion: Int?,
    val installerPackageName: String,
    val processName: String?,
    val uid: Int,
    val firstInstallLabel: String,
    val lastUpdateLabel: String,
)

data class ScopeStatusModel(
    val label: String,
    val packageName: String,
    val active: Boolean,
)

data class ConnectionEventModel(
    val title: String,
    val timestamp: String,
    val detail: String,
)

data class HomeUiState(
    val selectedPage: AppPage = AppPage.Overview,
    val secondaryPage: SecondaryPage? = null,
    val themeMode: AppThemeMode = AppThemeMode.System,
    val connection: XposedServiceState = XposedServiceState.bootstrap(),
    val features: List<FeatureCardModel> = emptyList(),
    val overviewStats: List<OverviewStatModel> = emptyList(),
    val appRows: List<AppRowModel> = emptyList(),
    val appSearchQuery: String = "",
    val onlyShowPushApps: Boolean = true,
    val autoRefreshAppsOnLaunch: Boolean = true,
    val showSystemApps: Boolean = false,
    val showPackageNameInList: Boolean = true,
    val showDisabledApps: Boolean = true,
    val showVersionNameInList: Boolean = false,
    val appsLoading: Boolean = false,
    val appsScanned: Boolean = false,
    val trackedAppsCount: Int = 0,
    val pushCandidateCount: Int = 0,
    val scopeStatuses: List<ScopeStatusModel> = emptyList(),
    val connectionDurationLabel: String = "未连接",
    val connectionSummary: String = "暂无记录",
    val recentConnectionEvents: List<ConnectionEventModel> = emptyList(),
    val fcmDiagnosticsConnected: Boolean = false,
    val fcmDiagnosticsDurationLabel: String = "未连接",
    val fcmDiagnosticsSummary: String = "暂无记录",
    val selectedAppDetails: AppDetailInfoModel? = null,
) {
    val enabledFeatureCount: Int
        get() = features.count { it.enabled }

    val headerTitle: String
        get() = when (secondaryPage) {
            is SecondaryPage.AppDetails -> selectedAppDetails?.label ?: secondaryPage.title
            null -> selectedPage.title
            else -> secondaryPage.title
        }

    val showsBottomBar: Boolean
        get() = secondaryPage == null

    val canNavigateBack: Boolean
        get() = secondaryPage != null
}

private data class UiSeed(
    val page: AppPage,
    val secondaryPage: SecondaryPage?,
    val settings: UiSettings,
    val connection: XposedServiceState,
    val query: String,
    val enabledFeatures: Set<FeatureKey>,
    val allowList: Set<String>,
    val selectedAppPackage: String?,
)

private data class NavigationSeed(
    val page: AppPage,
    val secondaryPage: SecondaryPage?,
    val query: String,
    val selectedAppPackage: String?,
)

private data class RuntimeSeed(
    val apps: List<InstalledAppInfo>,
    val appsLoading: Boolean,
    val appsScanned: Boolean,
    val nowElapsedRealtime: Long,
    val nowWallClockMillis: Long,
    val diagnosticsState: FcmDiagnosticsState,
)

private data class AppRuntimeSeed(
    val apps: List<InstalledAppInfo>,
    val appsLoading: Boolean,
    val appsScanned: Boolean,
)

private data class ClockSeed(
    val nowElapsedRealtime: Long,
    val nowWallClockMillis: Long,
    val diagnosticsState: FcmDiagnosticsState,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val container = application.appContainer
    private val configRepository = container.configRepository
    private val uiSettingsRepository = container.uiSettingsRepository
    private val appsRepository = container.installedAppsRepository
    private val serviceMonitor = container.xposedServiceMonitor
    private val fcmDiagnosticsRepository = container.fcmDiagnosticsStateRepository
    private val nameCollator = Collator.getInstance(Locale.CHINA)
    private val eventTimeFormatter = SimpleDateFormat("MM-dd HH:mm:ss", Locale.CHINA)
    private val appTimeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
    private val initialSettings = uiSettingsRepository.settings.value

    private val selectedPage = MutableStateFlow(AppPage.Overview)
    private val secondaryPage = MutableStateFlow<SecondaryPage?>(null)
    private val selectedAppPackage = MutableStateFlow<String?>(null)
    private val searchQuery = MutableStateFlow("")
    private val installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    private val appsLoading = MutableStateFlow(initialSettings.autoRefreshAppsOnLaunch)
    private val appsScanned = MutableStateFlow(false)
    private val clockNow = MutableStateFlow(SystemClock.elapsedRealtime())
    private val wallClockNow = MutableStateFlow(System.currentTimeMillis())
    private val fcmDiagnosticsState = MutableStateFlow(fcmDiagnosticsRepository.loadState())

    private val navigationSeed = combine(
        selectedPage,
        secondaryPage,
        searchQuery,
        selectedAppPackage,
    ) { page, detailsPage, query, appPackage ->
        NavigationSeed(
            page = page,
            secondaryPage = detailsPage,
            query = query,
            selectedAppPackage = appPackage,
        )
    }

    private val uiSeed = combine(
        configRepository.config,
        uiSettingsRepository.settings,
        serviceMonitor.state,
        navigationSeed,
    ) { config, settings, connection, navigation ->
        UiSeed(
            page = navigation.page,
            secondaryPage = navigation.secondaryPage,
            settings = settings,
            connection = connection,
            query = navigation.query,
            enabledFeatures = config.enabledFeatures,
            allowList = config.allowList,
            selectedAppPackage = navigation.selectedAppPackage,
        )
    }

    private val appRuntimeSeed = combine(
        installedApps,
        appsLoading,
        appsScanned,
    ) { apps, loading, scanned ->
        AppRuntimeSeed(
            apps = apps,
            appsLoading = loading,
            appsScanned = scanned,
        )
    }

    private val clockSeed = combine(
        clockNow,
        wallClockNow,
        fcmDiagnosticsState,
    ) { nowElapsedRealtime, nowWallClockMillis, diagnosticsState ->
        ClockSeed(
            nowElapsedRealtime = nowElapsedRealtime,
            nowWallClockMillis = nowWallClockMillis,
            diagnosticsState = diagnosticsState,
        )
    }

    private val runtimeSeed = combine(
        appRuntimeSeed,
        clockSeed,
    ) { appRuntime, clock ->
        RuntimeSeed(
            apps = appRuntime.apps,
            appsLoading = appRuntime.appsLoading,
            appsScanned = appRuntime.appsScanned,
            nowElapsedRealtime = clock.nowElapsedRealtime,
            nowWallClockMillis = clock.nowWallClockMillis,
            diagnosticsState = clock.diagnosticsState,
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        uiSeed,
        runtimeSeed,
    ) { seed, runtime ->
        val apps = runtime.apps
        val loading = runtime.appsLoading
        val scanned = runtime.appsScanned
        val appRows = apps
            .map { installed ->
                AppRowModel(
                    packageName = installed.packageName,
                    label = installed.label,
                    icon = installed.icon,
                    hasPushSupport = installed.hasPushSupport,
                    isSystemApp = installed.isSystemApp,
                    isAllowed = installed.packageName in seed.allowList,
                    isEnabled = installed.isEnabled,
                    versionName = installed.versionName,
                )
            }
            .filter { item ->
                val matchesQuery = seed.query.isBlank() ||
                    item.label.contains(seed.query, ignoreCase = true) ||
                    item.packageName.contains(seed.query, ignoreCase = true)
                val matchesPushFilter = !seed.settings.onlyShowPushApps ||
                    item.hasPushSupport ||
                    item.isAllowed
                val matchesSystemFilter = seed.settings.showSystemApps || !item.isSystemApp
                val matchesDisabledFilter = seed.settings.showDisabledApps || item.isEnabled
                matchesQuery && matchesPushFilter && matchesSystemFilter && matchesDisabledFilter
            }
            .sortedWith { left, right ->
                when {
                    left.isAllowed != right.isAllowed -> {
                        if (left.isAllowed) -1 else 1
                    }

                    left.hasPushSupport != right.hasPushSupport -> {
                        if (left.hasPushSupport) -1 else 1
                    }

                    else -> nameCollator.compare(left.label, right.label)
                }
            }

        val selectedAppDetails = seed.selectedAppPackage
            ?.let { packageName ->
                apps.firstOrNull { it.packageName == packageName }
            }
            ?.let { installed ->
                AppDetailInfoModel(
                    packageName = installed.packageName,
                    label = installed.label,
                    icon = installed.icon,
                    hasPushSupport = installed.hasPushSupport,
                    isSystemApp = installed.isSystemApp,
                    isAllowed = installed.packageName in seed.allowList,
                    isEnabled = installed.isEnabled,
                    versionName = installed.versionName,
                    versionCode = installed.versionCode,
                    targetSdkVersion = installed.targetSdkVersion,
                    minSdkVersion = installed.minSdkVersion,
                    installerPackageName = installed.installerPackageName,
                    processName = installed.processName,
                    uid = installed.uid,
                    firstInstallLabel = installed.firstInstallTime.toReadableTime(appTimeFormatter),
                    lastUpdateLabel = installed.lastUpdateTime.toReadableTime(appTimeFormatter),
                )
            }

        HomeUiState(
            selectedPage = seed.page,
            secondaryPage = seed.secondaryPage,
            themeMode = seed.settings.themeMode,
            connection = seed.connection,
            features = FeatureKey.entries.map { featureKey ->
                FeatureCardModel(
                    key = featureKey,
                    enabled = featureKey in seed.enabledFeatures,
                )
            },
            overviewStats = buildOverviewStats(
                connection = seed.connection,
                enabledFeatureCount = seed.enabledFeatures.size,
                trackedAppsCount = seed.allowList.size,
                pushCandidateCount = apps.count { it.hasPushSupport },
                appsLoading = loading,
                appsScanned = scanned,
            ),
            appRows = appRows,
            appSearchQuery = seed.query,
            onlyShowPushApps = seed.settings.onlyShowPushApps,
            autoRefreshAppsOnLaunch = seed.settings.autoRefreshAppsOnLaunch,
            showSystemApps = seed.settings.showSystemApps,
            showPackageNameInList = seed.settings.showPackageNameInList,
            showDisabledApps = seed.settings.showDisabledApps,
            showVersionNameInList = seed.settings.showVersionNameInList,
            appsLoading = loading,
            appsScanned = scanned,
            trackedAppsCount = seed.allowList.size,
            pushCandidateCount = apps.count { it.hasPushSupport },
            scopeStatuses = scopeStatuses(seed.connection),
            connectionDurationLabel = formatConnectionDuration(
                connectedAtElapsedRealtime = seed.connection.connectedAtElapsedRealtime,
                nowElapsedRealtime = runtime.nowElapsedRealtime,
            ),
            connectionSummary = buildConnectionSummary(seed.connection.connectionEvents.firstOrNull()),
            recentConnectionEvents = seed.connection.connectionEvents
                .take(8)
                .map(::toConnectionEventModel),
            fcmDiagnosticsConnected = runtime.diagnosticsState.isConnected ||
                (runtime.diagnosticsState.connectedSinceMillis != null &&
                    runtime.diagnosticsState.lastEventTitle == "FCM 已连接"),
            fcmDiagnosticsDurationLabel = formatFcmDiagnosticsDuration(
                connectedSinceMillis = runtime.diagnosticsState.connectedSinceMillis,
                nowWallClockMillis = runtime.nowWallClockMillis,
            ),
            fcmDiagnosticsSummary = buildFcmDiagnosticsSummary(runtime.diagnosticsState),
            selectedAppDetails = selectedAppDetails,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(
            themeMode = initialSettings.themeMode,
            onlyShowPushApps = initialSettings.onlyShowPushApps,
            autoRefreshAppsOnLaunch = initialSettings.autoRefreshAppsOnLaunch,
            showSystemApps = initialSettings.showSystemApps,
            showPackageNameInList = initialSettings.showPackageNameInList,
            showDisabledApps = initialSettings.showDisabledApps,
            showVersionNameInList = initialSettings.showVersionNameInList,
            overviewStats = buildOverviewStats(
                connection = XposedServiceState.bootstrap(),
                enabledFeatureCount = FeatureKey.defaultEnabledSet.size,
                trackedAppsCount = 0,
                pushCandidateCount = 0,
                appsLoading = initialSettings.autoRefreshAppsOnLaunch,
                appsScanned = false,
            ),
            scopeStatuses = scopeStatuses(XposedServiceState.bootstrap()),
            appsLoading = initialSettings.autoRefreshAppsOnLaunch,
            connectionDurationLabel = "未连接",
            connectionSummary = "暂无记录",
            fcmDiagnosticsDurationLabel = "未连接",
            fcmDiagnosticsSummary = "暂无记录",
        ),
    )

    init {
        startClockTicker()
        bootstrapRefresh()
    }

    fun selectPage(page: AppPage) {
        secondaryPage.value = null
        selectedPage.value = page
    }

    fun openSecondaryPage(page: SecondaryPage) {
        secondaryPage.value = page
    }

    fun openAppDetails(packageName: String) {
        selectedAppPackage.value = packageName
        secondaryPage.value = SecondaryPage.AppDetails(packageName)
    }

    fun navigateBack() {
        secondaryPage.value = null
    }

    fun setFeatureEnabled(featureKey: FeatureKey, enabled: Boolean) {
        viewModelScope.launch {
            configRepository.setFeatureEnabled(featureKey, enabled)
        }
    }

    fun applyRecommendedFeatures() {
        viewModelScope.launch {
            configRepository.applyRecommendedFeatures()
        }
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setOnlyShowPushApps(enabled: Boolean) {
        viewModelScope.launch {
            uiSettingsRepository.setOnlyShowPushApps(enabled)
        }
    }

    fun setThemeMode(themeMode: AppThemeMode) {
        viewModelScope.launch {
            uiSettingsRepository.setThemeMode(themeMode)
        }
    }

    fun setAutoRefreshAppsOnLaunch(enabled: Boolean) {
        viewModelScope.launch {
            uiSettingsRepository.setAutoRefreshAppsOnLaunch(enabled)
            if (enabled && !appsScanned.value && !appsLoading.value) {
                refreshAppsInternal()
            }
        }
    }

    fun setShowSystemApps(enabled: Boolean) {
        viewModelScope.launch {
            uiSettingsRepository.setShowSystemApps(enabled)
        }
    }

    fun setShowPackageNameInList(enabled: Boolean) {
        viewModelScope.launch {
            uiSettingsRepository.setShowPackageNameInList(enabled)
        }
    }

    fun setShowDisabledApps(enabled: Boolean) {
        viewModelScope.launch {
            uiSettingsRepository.setShowDisabledApps(enabled)
        }
    }

    fun setShowVersionNameInList(enabled: Boolean) {
        viewModelScope.launch {
            uiSettingsRepository.setShowVersionNameInList(enabled)
        }
    }

    fun toggleAppAllowed(packageName: String, allowed: Boolean) {
        viewModelScope.launch {
            configRepository.setAppAllowed(packageName, allowed)
        }
    }

    fun allowAllPushCandidates() {
        viewModelScope.launch {
            val packages = installedApps.value
                .filter { it.hasPushSupport }
                .mapTo(linkedSetOf()) { it.packageName }
            configRepository.replaceAllowList(packages)
        }
    }

    fun clearAllowList() {
        viewModelScope.launch {
            configRepository.clearAllowList()
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            configRepository.refresh()
            refreshAppsInternal()
        }
    }

    fun refreshApps() {
        viewModelScope.launch {
            refreshAppsInternal()
        }
    }

    private fun bootstrapRefresh() {
        viewModelScope.launch {
            configRepository.refresh()
            if (initialSettings.autoRefreshAppsOnLaunch) {
                refreshAppsInternal()
            } else {
                appsLoading.value = false
            }
        }
    }

    private fun startClockTicker() {
        viewModelScope.launch {
            while (true) {
                clockNow.value = SystemClock.elapsedRealtime()
                wallClockNow.value = System.currentTimeMillis()
                fcmDiagnosticsState.value = fcmDiagnosticsRepository.loadState()
                kotlinx.coroutines.delay(1_000L)
            }
        }
    }

    private suspend fun refreshAppsInternal() {
        appsLoading.value = true
        installedApps.value = runCatching {
            appsRepository.loadInstalledApps()
        }.getOrElse {
            emptyList()
        }
        appsScanned.value = true
        appsLoading.value = false
    }

    private fun buildOverviewStats(
        connection: XposedServiceState,
        enabledFeatureCount: Int,
        trackedAppsCount: Int,
        pushCandidateCount: Int,
        appsLoading: Boolean,
        appsScanned: Boolean,
    ): List<OverviewStatModel> = listOf(
        OverviewStatModel(
            label = "模块连接",
            value = if (connection.isConnected) "已连接" else "未连接",
            hint = if (connection.hasRemotePreferences) "配置已同步" else "等待配置桥接",
        ),
        OverviewStatModel(
            label = "功能开关",
            value = "$enabledFeatureCount / ${FeatureKey.entries.size}",
            hint = "当前已启用的修复项",
        ),
        OverviewStatModel(
            label = "白名单应用",
            value = trackedAppsCount.toString(),
            hint = "重点保活目标",
        ),
        OverviewStatModel(
            label = "推送候选",
            value = when {
                appsLoading -> "扫描中"
                !appsScanned -> "未扫描"
                else -> pushCandidateCount.toString()
            },
            hint = "检测到 FCM 组件",
        ),
    )

    private fun scopeStatuses(connection: XposedServiceState): List<ScopeStatusModel> = listOf(
        ScopeStatusModel(
            label = "系统服务",
            packageName = ModuleScope.SystemServer,
            active = ModuleScope.SystemServer in connection.activeScopes,
        ),
        ScopeStatusModel(
            label = "电量策略",
            packageName = ModuleScope.PowerKeeper,
            active = ModuleScope.PowerKeeper in connection.activeScopes,
        ),
        ScopeStatusModel(
            label = "Google Play 服务",
            packageName = ModuleScope.GooglePlayServices,
            active = ModuleScope.GooglePlayServices in connection.activeScopes,
        ),
    )

    private fun toConnectionEventModel(event: ConnectionEvent): ConnectionEventModel = ConnectionEventModel(
        title = event.type.title,
        timestamp = eventTimeFormatter.format(Date(event.recordedAtMillis)),
        detail = event.detail,
    )

    private fun buildConnectionSummary(event: ConnectionEvent?): String {
        if (event == null) return "暂无记录"
        return "${event.type.title} · ${eventTimeFormatter.format(Date(event.recordedAtMillis))}"
    }

    private fun formatConnectionDuration(
        connectedAtElapsedRealtime: Long?,
        nowElapsedRealtime: Long,
    ): String {
        if (connectedAtElapsedRealtime == null) return "未连接"

        val totalSeconds = ((nowElapsedRealtime - connectedAtElapsedRealtime).coerceAtLeast(0L) / 1_000L)
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L

        return when {
            hours > 0L -> "%d小时 %02d分 %02d秒".format(hours, minutes, seconds)
            minutes > 0L -> "%d分 %02d秒".format(minutes, seconds)
            else -> "%d秒".format(seconds)
        }
    }

    private fun formatFcmDiagnosticsDuration(
        connectedSinceMillis: Long?,
        nowWallClockMillis: Long,
    ): String {
        if (connectedSinceMillis == null) return "未连接"

        val totalSeconds = ((nowWallClockMillis - connectedSinceMillis).coerceAtLeast(0L) / 1_000L)
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L

        return when {
            hours > 0L -> "%d小时 %02d分 %02d秒".format(hours, minutes, seconds)
            minutes > 0L -> "%d分 %02d秒".format(minutes, seconds)
            else -> "%d秒".format(seconds)
        }
    }

    private fun buildFcmDiagnosticsSummary(state: FcmDiagnosticsState): String {
        val eventTime = state.lastEventAtMillis?.let { eventTimeFormatter.format(Date(it)) } ?: return "暂无记录"
        return "${state.lastEventTitle} · $eventTime"
    }

    private fun Long.toReadableTime(formatter: SimpleDateFormat): String {
        if (this <= 0L) return "-"
        return formatter.format(Date(this))
    }
}
