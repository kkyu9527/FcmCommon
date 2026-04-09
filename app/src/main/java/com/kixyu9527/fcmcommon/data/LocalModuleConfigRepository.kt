package com.kixyu9527.fcmcommon.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocalModuleConfigRepository(context: Context) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val mutableConfig = MutableStateFlow(loadConfig())
    val config: StateFlow<ModuleConfig> = mutableConfig.asStateFlow()

    fun refresh() {
        mutableConfig.value = loadConfig()
    }

    fun setFeatureEnabled(featureKey: FeatureKey, enabled: Boolean) {
        val updated = mutableConfig.value.withFeature(featureKey, enabled)
        preferences.edit(commit = true) {
            putStringSet(
                KEY_ENABLED_FEATURES,
                updated.enabledFeatures.mapTo(linkedSetOf()) { it.name },
            )
        }
        mutableConfig.value = updated
    }

    private fun loadConfig(): ModuleConfig {
        val savedFeatures = preferences.getStringSet(KEY_ENABLED_FEATURES, null)
            ?.mapNotNull { raw -> FeatureKey.entries.firstOrNull { it.name == raw } }
            ?.toSet()
            ?: FeatureKey.defaultEnabledSet

        val savedAllowList = preferences.getStringSet(KEY_ALLOW_LIST, emptySet()).orEmpty()

        return ModuleConfig(
            enabledFeatures = savedFeatures,
            allowList = savedAllowList,
        )
    }

    private companion object {
        const val PREFS_NAME = "fcmcommon.config"
        const val KEY_ENABLED_FEATURES = "enabled_features"
        const val KEY_ALLOW_LIST = "allow_list"
    }
}
