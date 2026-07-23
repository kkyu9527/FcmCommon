package com.kixyu9527.fcmcommon.ui.home

import android.graphics.drawable.Drawable
import com.kixyu9527.fcmcommon.data.AppThemeMode
import com.kixyu9527.fcmcommon.data.FeatureKey
import com.kixyu9527.fcmcommon.xposed.XposedServiceState

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
    val title: String,
) {
    data object AppPreferences : SecondaryPage(
        title = "应用偏好",
    )

    data object Features : SecondaryPage(
        title = "功能配置",
    )

    data object Diagnostics : SecondaryPage(
        title = "模块状态",
    )

    data class AppDetails(
        val packageName: String,
    ) : SecondaryPage(
        title = "应用详情",
    )
}

sealed class TertiaryPage(
    val title: String,
) {
    data object ModuleLogs : TertiaryPage(
        title = "模块日志",
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
    val tertiaryPage: TertiaryPage? = null,
    val themeMode: AppThemeMode = AppThemeMode.System,
    val connection: XposedServiceState = XposedServiceState.bootstrap(),
    val features: List<FeatureCardModel> = emptyList(),
    val overviewStats: List<OverviewStatModel> = emptyList(),
    val appRows: List<AppRowModel> = emptyList(),
    val appSearchQuery: String = "",
    val onlyShowPushApps: Boolean = false,
    val showSystemApps: Boolean = false,
    val showPackageNameInList: Boolean = true,
    val showDisabledApps: Boolean = true,
    val canReadInstalledApps: Boolean = true,
    val appsLoading: Boolean = false,
    val appsScanned: Boolean = false,
    val trackedAppsCount: Int = 0,
    val pushCandidateCount: Int = 0,
    val scopeStatuses: List<ScopeStatusModel> = emptyList(),
    val connectionSummary: String = "暂无记录",
    val recentConnectionEvents: List<ConnectionEventModel> = emptyList(),
    val selectedAppDetails: AppDetailInfoModel? = null,
) {
    val enabledFeatureCount: Int
        get() = features.count { it.enabled }
}
