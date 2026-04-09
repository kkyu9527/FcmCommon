package com.kixyu9527.fcmcommon.ui

import com.kixyu9527.fcmcommon.data.FeatureKey

object TestTags {
    const val HomeList = "home_list"
    const val ConnectionHeadline = "connection_headline"
    const val DiagnosticsCard = "diagnostics_card"
    const val ScopeRow = "scope_row"

    fun featureToggle(featureKey: FeatureKey): String =
        "feature_toggle_${featureKey.name.lowercase()}"

    fun featureCard(featureKey: FeatureKey): String =
        "feature_card_${featureKey.name.lowercase()}"
}
