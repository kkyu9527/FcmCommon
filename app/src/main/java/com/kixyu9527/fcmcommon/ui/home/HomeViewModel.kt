package com.kixyu9527.fcmcommon.ui.home

import android.app.Application
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kixyu9527.fcmcommon.appContainer
import com.kixyu9527.fcmcommon.data.FeatureKey
import com.kixyu9527.fcmcommon.data.InstalledAppInfo
import com.kixyu9527.fcmcommon.xposed.XposedServiceState
import java.text.Collator
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
    val subtitle: String,
) {
    Overview(
        route = "overview",
        title = "总览",
        subtitle = "FcmCommon",
    ),
    Apps(
        route = "apps",
        title = "应用",
        subtitle = "白名单与推送候选",
    ),
    Features(
        route = "features",
        title = "功能",
        subtitle = "修复策略",
    ),
    Diagnostics(
        route = "diagnostics",
        title = "状态",
        subtitle = "模块与作用域",
    ),
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
)

data class ScopeStatusModel(
    val label: String,
    val packageName: String,
)

data class HomeUiState(
    val selectedPage: AppPage = AppPage.Overview,
    val connection: XposedServiceState = XposedServiceState.bootstrap(),
    val features: List<FeatureCardModel> = emptyList(),
    val overviewStats: List<OverviewStatModel> = emptyList(),
    val appRows: List<AppRowModel> = emptyList(),
    val appSearchQuery: String = "",
    val onlyShowPushApps: Boolean = true,
    val appsLoading: Boolean = false,
    val trackedAppsCount: Int = 0,
    val pushCandidateCount: Int = 0,
    val scopeStatuses: List<ScopeStatusModel> = emptyList(),
) {
    val enabledFeatureCount: Int
        get() = features.count { it.enabled }
}

private data class UiSeed(
    val page: AppPage,
    val connection: XposedServiceState,
    val query: String,
    val onlyPush: Boolean,
    val enabledFeatures: Set<FeatureKey>,
    val allowList: Set<String>,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val container = application.appContainer
    private val configRepository = container.configRepository
    private val appsRepository = container.installedAppsRepository
    private val serviceMonitor = container.xposedServiceMonitor
    private val nameCollator = Collator.getInstance(Locale.CHINA)

    private val selectedPage = MutableStateFlow(AppPage.Overview)
    private val searchQuery = MutableStateFlow("")
    private val onlyShowPushApps = MutableStateFlow(true)
    private val installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    private val appsLoading = MutableStateFlow(false)

    private val uiSeed = combine(
        configRepository.config,
        serviceMonitor.state,
        selectedPage,
        searchQuery,
        onlyShowPushApps,
    ) { config, connection, page, query, onlyPush ->
        UiSeed(
            page = page,
            connection = connection,
            query = query,
            onlyPush = onlyPush,
            enabledFeatures = config.enabledFeatures,
            allowList = config.allowList,
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        uiSeed,
        installedApps,
        appsLoading,
    ) { seed, apps, loading ->
        val appRows = apps
            .map { installed ->
                AppRowModel(
                    packageName = installed.packageName,
                    label = installed.label,
                    icon = installed.icon,
                    hasPushSupport = installed.hasPushSupport,
                    isSystemApp = installed.isSystemApp,
                    isAllowed = installed.packageName in seed.allowList,
                )
            }
            .filter { item ->
                val matchesQuery = seed.query.isBlank() ||
                    item.label.contains(seed.query, ignoreCase = true) ||
                    item.packageName.contains(seed.query, ignoreCase = true)
                val matchesPushFilter = !seed.onlyPush || item.hasPushSupport || item.isAllowed
                matchesQuery && matchesPushFilter
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

        HomeUiState(
            selectedPage = seed.page,
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
            ),
            appRows = appRows,
            appSearchQuery = seed.query,
            onlyShowPushApps = seed.onlyPush,
            appsLoading = loading,
            trackedAppsCount = seed.allowList.size,
            pushCandidateCount = apps.count { it.hasPushSupport },
            scopeStatuses = scopeStatuses(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(
            overviewStats = buildOverviewStats(
                connection = XposedServiceState.bootstrap(),
                enabledFeatureCount = FeatureKey.defaultEnabledSet.size,
                trackedAppsCount = 0,
                pushCandidateCount = 0,
                appsLoading = true,
            ),
            scopeStatuses = scopeStatuses(),
            appsLoading = true,
        ),
    )

    init {
        refreshAll()
    }

    fun selectPage(page: AppPage) {
        selectedPage.value = page
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
        onlyShowPushApps.value = enabled
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
            refreshApps()
        }
    }

    fun refreshApps() {
        viewModelScope.launch {
            appsLoading.value = true
            installedApps.value = runCatching {
                appsRepository.loadInstalledApps()
            }.getOrElse {
                emptyList()
            }
            appsLoading.value = false
        }
    }

    private fun buildOverviewStats(
        connection: XposedServiceState,
        enabledFeatureCount: Int,
        trackedAppsCount: Int,
        pushCandidateCount: Int,
        appsLoading: Boolean,
    ): List<OverviewStatModel> = listOf(
        OverviewStatModel(
            label = "模块连接",
            value = if (connection.isConnected) "已连接" else "未连接",
            hint = if (connection.hasRemotePreferences) "配置已同步" else "等待配置桥接",
        ),
        OverviewStatModel(
            label = "功能开关",
            value = "$enabledFeatureCount / ${FeatureKey.entries.size}",
            hint = "修复功能总览",
        ),
        OverviewStatModel(
            label = "白名单应用",
            value = trackedAppsCount.toString(),
            hint = "重点保活目标",
        ),
        OverviewStatModel(
            label = "推送候选",
            value = if (appsLoading) "扫描中" else pushCandidateCount.toString(),
            hint = "检测到 FCM 组件",
        ),
    )

    private fun scopeStatuses(): List<ScopeStatusModel> = listOf(
        ScopeStatusModel(
            label = "系统服务",
            packageName = "system",
        ),
        ScopeStatusModel(
            label = "电量策略",
            packageName = "com.miui.powerkeeper",
        ),
        ScopeStatusModel(
            label = "Google Play 服务",
            packageName = "com.google.android.gms",
        ),
    )

}
