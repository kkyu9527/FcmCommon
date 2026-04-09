package com.kixyu9527.fcmcommon.data

data class UiSettings(
    val themeMode: AppThemeMode = AppThemeMode.System,
    val onlyShowPushApps: Boolean = true,
    val autoRefreshAppsOnLaunch: Boolean = true,
    val showSystemApps: Boolean = false,
    val showPackageNameInList: Boolean = true,
    val showDisabledApps: Boolean = true,
    val hasCompletedInitialScan: Boolean = false,
)
