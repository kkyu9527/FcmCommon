package com.kixyu9527.fcmcommon.data

data class ModuleConfig(
    val enabledFeatures: Set<FeatureKey> = FeatureKey.defaultEnabledSet,
    val allowList: Set<String> = emptySet(),
) {
    fun isEnabled(featureKey: FeatureKey): Boolean = featureKey in enabledFeatures

    fun withFeature(featureKey: FeatureKey, enabled: Boolean): ModuleConfig {
        val updated = enabledFeatures.toMutableSet()
        if (enabled) {
            updated += featureKey
        } else {
            updated -= featureKey
        }
        return copy(enabledFeatures = updated)
    }
}
