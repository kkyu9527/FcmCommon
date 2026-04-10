package com.kixyu9527.fcmcommon.ui

import com.kixyu9527.fcmcommon.data.AppThemeMode
import com.kixyu9527.fcmcommon.data.FeatureKey

object TestTags {
    const val PageOverview = "page_overview"
    const val PageApps = "page_apps"
    const val PageSettings = "page_settings"
    const val PageAppPreferences = "page_app_preferences"
    const val PageFeatureSettings = "page_feature_settings"
    const val PageDiagnosticsDetail = "page_diagnostics_detail"
    const val PageModuleLogs = "page_module_logs"
    const val PageAppDetails = "page_app_details"
    const val Header = "header"
    const val ConnectionHeadline = "connection_headline"
    const val DiagnosticsCard = "diagnostics_card"
    const val ModuleLogsCard = "module_logs_card"
    const val ScopeRow = "scope_row"
    const val AppsList = "apps_list"
    const val AppsSearchField = "apps_search_field"
    const val AppsOnlyPushToggle = "apps_only_push_toggle"
    const val BottomBar = "bottom_bar"
    const val OverviewStatCard = "overview_stat_card"
    const val ActionApplyRecommended = "action_apply_recommended"
    const val ActionRefreshConfig = "action_refresh_config"
    const val ActionOpenPlayServicesDiagnostics = "action_open_play_services_diagnostics"
    const val TopBarBack = "top_bar_back"
    const val ThemeModeDropdown = "theme_mode_dropdown"
    const val SettingsAppsEntry = "settings_apps_entry"
    const val SettingsFeaturesEntry = "settings_features_entry"
    const val SettingsDiagnosticsEntry = "settings_diagnostics_entry"
    const val DiagnosticsLogsEntry = "diagnostics_logs_entry"

    fun featureToggle(featureKey: FeatureKey): String =
        "feature_toggle_${featureKey.name.lowercase()}"

    fun featureCard(featureKey: FeatureKey): String =
        "feature_card_${featureKey.name.lowercase()}"

    fun navItem(route: String): String =
        "nav_item_$route"

    fun themeMode(mode: AppThemeMode): String =
        "theme_mode_${mode.name.lowercase()}"

    fun appRow(packageName: String): String =
        "app_row_${packageName.replace('.', '_')}"
}
