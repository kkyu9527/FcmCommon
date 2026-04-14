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
    val route: String,
    val title: String,
    val parentPage: AppPage,
) {
    data object AppPreferences : SecondaryPage(
        route = "app_preferences",
        title = "应用偏好",
        parentPage = AppPage.Settings,
    )

    data object Features : SecondaryPage(
        route = "features",
        title = "功能配置",
        parentPage = AppPage.Settings,
    )

    data object Diagnostics : SecondaryPage(
        route = "diagnostics",
        title = "模块状态",
        parentPage = AppPage.Settings,
    )

    data class AppDetails(
        val packageName: String,
    ) : SecondaryPage(
        route = "app_details",
        title = "应用详情",
        parentPage = AppPage.Apps,
    )
}

sealed class TertiaryPage(
    val route: String,
    val title: String,
    val parentPage: AppPage,
    val parentSecondaryPage: SecondaryPage,
) {
    data object ModuleLogs : TertiaryPage(
        route = "module_logs",
        title = "模块日志",
        parentPage = AppPage.Settings,
        parentSecondaryPage = SecondaryPage.Diagnostics,
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
    val navigationEpoch: Long = 0L,
    val themeMode: AppThemeMode = AppThemeMode.System,
    val connection: XposedServiceState = XposedServiceState.bootstrap(),
    val features: List<FeatureCardModel> = emptyList(),
    val overviewStats: List<OverviewStatModel> = emptyList(),
    val appRows: List<AppRowModel> = emptyList(),
    val appSearchQuery: String = "",
    val onlyShowPushApps: Boolean = true,
    val showSystemApps: Boolean = false,
    val showPackageNameInList: Boolean = true,
    val showDisabledApps: Boolean = true,
    val canReadInstalledApps: Boolean = true,
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

    val currentParentPage: AppPage
        get() = tertiaryPage?.parentPage ?: secondaryPage?.parentPage ?: selectedPage

    val currentParentSecondaryPage: SecondaryPage?
        get() = tertiaryPage?.parentSecondaryPage ?: secondaryPage

    val headerTitle: String
        get() = tertiaryPage?.title ?: when (secondaryPage) {
            is SecondaryPage.AppDetails -> selectedAppDetails?.label ?: secondaryPage.title
            null -> selectedPage.title
            else -> secondaryPage.title
        }

    val showsBottomBar: Boolean
        get() = secondaryPage == null && tertiaryPage == null

    val canNavigateBack: Boolean
        get() = secondaryPage != null || tertiaryPage != null
}
