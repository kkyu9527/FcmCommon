package com.kixyu9527.fcmcommon.data

data class ModuleConfig(
    val enabledFeatures: Set<FeatureKey> = FeatureKey.defaultEnabledSet,
    val allowList: Set<String> = emptySet(),
) {
    fun isEnabled(featureKey: FeatureKey): Boolean = featureKey in enabledFeatures

    fun hasFeature(prefKey: String): Boolean =
        enabledFeatures.any { it.prefKey == prefKey }

    fun withEnabledFeatures(featureKeys: Set<FeatureKey>): ModuleConfig =
        copy(enabledFeatures = featureKeys.toSet())

    fun withAllowList(packageNames: Set<String>): ModuleConfig =
        copy(allowList = packageNames.toSet())

    fun withFeature(featureKey: FeatureKey, enabled: Boolean): ModuleConfig {
        val updated = enabledFeatures.toMutableSet()
        if (enabled) {
            updated += featureKey
        } else {
            updated -= featureKey
        }
        return copy(enabledFeatures = updated)
    }

    fun withAllowedPackage(packageName: String, allowed: Boolean): ModuleConfig {
        val updated = allowList.toMutableSet()
        if (allowed) {
            updated += packageName
        } else {
            updated -= packageName
        }
        return copy(allowList = updated)
    }
}
