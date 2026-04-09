package com.kixyu9527.fcmcommon.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocalUiSettingsRepository(context: Context) {
    private val storageContext = context.applicationContext.createDeviceProtectedStorageContext()
    private val preferences: SharedPreferences =
        storageContext.getSharedPreferences(ConfigKeys.UiPrefsName, Context.MODE_PRIVATE)

    private val mutableSettings = MutableStateFlow(loadSettings())
    val settings: StateFlow<UiSettings> = mutableSettings.asStateFlow()

    fun setThemeMode(themeMode: AppThemeMode) {
        updateSettings(mutableSettings.value.copy(themeMode = themeMode))
    }

    fun setOnlyShowPushApps(enabled: Boolean) {
        updateSettings(mutableSettings.value.copy(onlyShowPushApps = enabled))
    }

    fun setAutoRefreshAppsOnLaunch(enabled: Boolean) {
        updateSettings(mutableSettings.value.copy(autoRefreshAppsOnLaunch = enabled))
    }

    fun setShowSystemApps(enabled: Boolean) {
        updateSettings(mutableSettings.value.copy(showSystemApps = enabled))
    }

    fun setShowPackageNameInList(enabled: Boolean) {
        updateSettings(mutableSettings.value.copy(showPackageNameInList = enabled))
    }

    fun setShowDisabledApps(enabled: Boolean) {
        updateSettings(mutableSettings.value.copy(showDisabledApps = enabled))
    }

    fun setShowVersionNameInList(enabled: Boolean) {
        updateSettings(mutableSettings.value.copy(showVersionNameInList = enabled))
    }

    private fun loadSettings(): UiSettings = UiSettings(
        themeMode = preferences.getString(ConfigKeys.KeyThemeMode, null)
            ?.let { saved -> AppThemeMode.entries.firstOrNull { it.name == saved } }
            ?: AppThemeMode.System,
        onlyShowPushApps = preferences.getBoolean(ConfigKeys.KeyOnlyShowPushApps, true),
        autoRefreshAppsOnLaunch = preferences.getBoolean(
            ConfigKeys.KeyAutoRefreshAppsOnLaunch,
            true,
        ),
        showSystemApps = preferences.getBoolean(ConfigKeys.KeyShowSystemApps, false),
        showPackageNameInList = preferences.getBoolean(
            ConfigKeys.KeyShowPackageNameInList,
            true,
        ),
        showDisabledApps = preferences.getBoolean(ConfigKeys.KeyShowDisabledApps, true),
        showVersionNameInList = preferences.getBoolean(
            ConfigKeys.KeyShowVersionNameInList,
            false,
        ),
    )

    private fun updateSettings(updated: UiSettings) {
        preferences.edit(commit = true) {
            putString(ConfigKeys.KeyThemeMode, updated.themeMode.name)
            putBoolean(ConfigKeys.KeyOnlyShowPushApps, updated.onlyShowPushApps)
            putBoolean(
                ConfigKeys.KeyAutoRefreshAppsOnLaunch,
                updated.autoRefreshAppsOnLaunch,
            )
            putBoolean(ConfigKeys.KeyShowSystemApps, updated.showSystemApps)
            putBoolean(
                ConfigKeys.KeyShowPackageNameInList,
                updated.showPackageNameInList,
            )
            putBoolean(ConfigKeys.KeyShowDisabledApps, updated.showDisabledApps)
            putBoolean(
                ConfigKeys.KeyShowVersionNameInList,
                updated.showVersionNameInList,
            )
        }
        mutableSettings.value = updated
    }
}
