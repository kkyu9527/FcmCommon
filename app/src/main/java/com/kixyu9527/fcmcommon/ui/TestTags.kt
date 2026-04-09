package com.kixyu9527.fcmcommon.ui

import com.kixyu9527.fcmcommon.data.FeatureKey

object TestTags {
    const val PageOverview = "page_overview"
    const val PageApps = "page_apps"
    const val PageFeatures = "page_features"
    const val PageDiagnostics = "page_diagnostics"
    const val Header = "header"
    const val ConnectionHeadline = "connection_headline"
    const val DiagnosticsCard = "diagnostics_card"
    const val ScopeRow = "scope_row"
    const val AppsList = "apps_list"
    const val AppsSearchField = "apps_search_field"
    const val AppsOnlyPushToggle = "apps_only_push_toggle"
    const val BottomBar = "bottom_bar"
    const val OverviewStatCard = "overview_stat_card"
    const val ActionApplyRecommended = "action_apply_recommended"
    const val ActionRefreshConfig = "action_refresh_config"

    fun featureToggle(featureKey: FeatureKey): String =
        "feature_toggle_${featureKey.name.lowercase()}"

    fun featureCard(featureKey: FeatureKey): String =
        "feature_card_${featureKey.name.lowercase()}"

    fun navItem(route: String): String =
        "nav_item_$route"
}
