package com.kixyu9527.fcmcommon.ui.home

import com.kixyu9527.fcmcommon.data.ConnectionEvent
import com.kixyu9527.fcmcommon.data.FcmDiagnosticsState
import com.kixyu9527.fcmcommon.data.FeatureKey
import com.kixyu9527.fcmcommon.data.UiSettings
import com.kixyu9527.fcmcommon.data.InstalledAppInfo
import com.kixyu9527.fcmcommon.data.ModuleScope
import com.kixyu9527.fcmcommon.xposed.XposedServiceState
import java.text.Collator
import java.text.SimpleDateFormat
import java.util.Date

internal class HomeUiStateAssembler(
    private val nameCollator: Collator,
    private val eventTimeFormatter: SimpleDateFormat,
    private val appTimeFormatter: SimpleDateFormat,
) {
    fun build(seed: HomeUiSeed, runtime: HomeRuntimeSeed): HomeUiState {
        val apps = runtime.apps
        val loading = runtime.appsLoading
        val scanned = runtime.appsScanned

        return HomeUiState(
            selectedPage = seed.page,
            secondaryPage = seed.secondaryPage,
            tertiaryPage = seed.tertiaryPage,
            navigationEpoch = seed.navigationEpoch,
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
            appRows = buildAppRows(
                apps = apps,
                query = seed.query,
                settings = seed.settings,
                allowList = seed.allowList,
            ),
            appSearchQuery = seed.query,
            onlyShowPushApps = seed.settings.onlyShowPushApps,
            showSystemApps = seed.settings.showSystemApps,
            showPackageNameInList = seed.settings.showPackageNameInList,
            showDisabledApps = seed.settings.showDisabledApps,
            canReadInstalledApps = runtime.canReadInstalledApps,
            appsLoading = loading,
            appsScanned = scanned,
            trackedAppsCount = seed.allowList.size,
            pushCandidateCount = apps.count { it.hasPushSupport },
            scopeStatuses = buildScopeStatuses(seed.connection),
            connectionDurationLabel = formatDurationFromElapsedRealtime(
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
            fcmDiagnosticsDurationLabel = formatDurationFromWallClock(
                connectedSinceMillis = runtime.diagnosticsState.connectedSinceMillis,
                nowWallClockMillis = runtime.nowWallClockMillis,
            ),
            fcmDiagnosticsSummary = buildFcmDiagnosticsSummary(runtime.diagnosticsState),
            selectedAppDetails = buildSelectedAppDetails(
                apps = apps,
                selectedPackageName = seed.selectedAppPackage,
                allowList = seed.allowList,
            ),
        )
    }

    fun buildInitialState(
        initialSettings: UiSettings,
        initialApps: List<InstalledAppInfo>,
        initialAppsPermissionGranted: Boolean,
        shouldBootstrapScan: Boolean,
        initialAppsScanned: Boolean,
    ): HomeUiState = HomeUiState(
        themeMode = initialSettings.themeMode,
        onlyShowPushApps = initialSettings.onlyShowPushApps,
        showSystemApps = initialSettings.showSystemApps,
        showPackageNameInList = initialSettings.showPackageNameInList,
        showDisabledApps = initialSettings.showDisabledApps,
        canReadInstalledApps = initialAppsPermissionGranted,
        overviewStats = buildOverviewStats(
            connection = XposedServiceState.bootstrap(),
            enabledFeatureCount = FeatureKey.defaultEnabledSet.size,
            trackedAppsCount = 0,
            pushCandidateCount = initialApps.count { it.hasPushSupport },
            appsLoading = shouldBootstrapScan && initialAppsPermissionGranted,
            appsScanned = initialAppsScanned,
        ),
        scopeStatuses = buildScopeStatuses(XposedServiceState.bootstrap()),
        appsLoading = shouldBootstrapScan && initialAppsPermissionGranted,
        appsScanned = initialAppsScanned,
        connectionDurationLabel = "未连接",
        connectionSummary = "暂无记录",
        fcmDiagnosticsDurationLabel = "未连接",
        fcmDiagnosticsSummary = "暂无记录",
    )

    private fun buildAppRows(
        apps: List<InstalledAppInfo>,
        query: String,
        settings: UiSettings,
        allowList: Set<String>,
    ): List<AppRowModel> = apps
        .map { installed ->
            AppRowModel(
                packageName = installed.packageName,
                label = installed.label,
                icon = installed.icon,
                hasPushSupport = installed.hasPushSupport,
                isSystemApp = installed.isSystemApp,
                isAllowed = installed.packageName in allowList,
                isEnabled = installed.isEnabled,
            )
        }
        .filter { item ->
            val matchesQuery = query.isBlank() ||
                item.label.contains(query, ignoreCase = true) ||
                item.packageName.contains(query, ignoreCase = true)
            val matchesPushFilter = !settings.onlyShowPushApps ||
                item.hasPushSupport ||
                item.isAllowed
            val matchesSystemFilter = settings.showSystemApps || !item.isSystemApp
            val matchesDisabledFilter = settings.showDisabledApps || item.isEnabled
            matchesQuery && matchesPushFilter && matchesSystemFilter && matchesDisabledFilter
        }
        .sortedWith { left, right ->
            when {
                left.isAllowed != right.isAllowed -> if (left.isAllowed) -1 else 1
                left.hasPushSupport != right.hasPushSupport -> if (left.hasPushSupport) -1 else 1
                else -> nameCollator.compare(left.label, right.label)
            }
        }

    private fun buildSelectedAppDetails(
        apps: List<InstalledAppInfo>,
        selectedPackageName: String?,
        allowList: Set<String>,
    ): AppDetailInfoModel? = selectedPackageName
        ?.let { packageName -> apps.firstOrNull { it.packageName == packageName } }
        ?.let { installed ->
            AppDetailInfoModel(
                packageName = installed.packageName,
                label = installed.label,
                icon = installed.icon,
                hasPushSupport = installed.hasPushSupport,
                isSystemApp = installed.isSystemApp,
                isAllowed = installed.packageName in allowList,
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
            label = "托管应用",
            value = trackedAppsCount.toString(),
            hint = "参与 FCM 修复链路的目标应用",
        ),
        OverviewStatModel(
            label = "推送候选",
            value = when {
                appsLoading -> "扫描中"
                !appsScanned -> "未扫描"
                else -> pushCandidateCount.toString()
            },
            hint = "基于最近一次扫描结果",
        ),
    )

    private fun buildScopeStatuses(connection: XposedServiceState): List<ScopeStatusModel> = listOf(
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

    private fun buildFcmDiagnosticsSummary(state: FcmDiagnosticsState): String {
        val eventTime = state.lastEventAtMillis?.let { eventTimeFormatter.format(Date(it)) } ?: return "暂无记录"
        return "${state.lastEventTitle} · $eventTime"
    }

    private fun formatDurationFromElapsedRealtime(
        connectedAtElapsedRealtime: Long?,
        nowElapsedRealtime: Long,
    ): String {
        if (connectedAtElapsedRealtime == null) return "未连接"
        return formatDuration((nowElapsedRealtime - connectedAtElapsedRealtime).coerceAtLeast(0L))
    }

    private fun formatDurationFromWallClock(
        connectedSinceMillis: Long?,
        nowWallClockMillis: Long,
    ): String {
        if (connectedSinceMillis == null) return "未连接"
        return formatDuration((nowWallClockMillis - connectedSinceMillis).coerceAtLeast(0L))
    }

    private fun formatDuration(durationMillis: Long): String {
        val totalSeconds = durationMillis / 1_000L
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L

        return when {
            hours > 0L -> "%d小时 %02d分 %02d秒".format(hours, minutes, seconds)
            minutes > 0L -> "%d分 %02d秒".format(minutes, seconds)
            else -> "%d秒".format(seconds)
        }
    }

    private fun Long.toReadableTime(formatter: SimpleDateFormat): String {
        if (this <= 0L) return "-"
        return formatter.format(Date(this))
    }
}
