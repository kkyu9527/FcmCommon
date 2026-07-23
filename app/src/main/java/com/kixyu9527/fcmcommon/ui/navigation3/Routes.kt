package com.kixyu9527.fcmcommon.ui.navigation3

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Main : Route

    @Serializable
    data object AppPreferences : Route

    @Serializable
    data object Features : Route

    @Serializable
    data object Diagnostics : Route

    @Serializable
    data object ModuleLogs : Route

    @Serializable
    data class AppDetails(
        val packageName: String,
    ) : Route
}
