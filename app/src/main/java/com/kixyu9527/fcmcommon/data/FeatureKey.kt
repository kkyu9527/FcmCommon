package com.kixyu9527.fcmcommon.data

enum class FeatureKey(
    val prefKey: String,
    val title: String,
    val summary: String,
    val scope: String,
    val source: String,
    val defaultEnabled: Boolean,
) {
    HyperOsBroadcastShield(
        prefKey = "hyperos_broadcast_shield",
        title = "HyperOS broadcast shield",
        summary = "Lift HyperOS deferral and domestic-policy blocks on reconnect and push delivery broadcasts.",
        scope = ModuleScope.SystemServer,
        source = "HyperFCMLive + fcmfix",
        defaultEnabled = true,
    ),
    WakeStoppedApps(
        prefKey = "wake_stopped_apps",
        title = "Wake stopped apps",
        summary = "Inject stopped-package flags so target apps can receive FCM even after the process is gone.",
        scope = ModuleScope.SystemServer,
        source = "fcmfix",
        defaultEnabled = true,
    ),
    PowerKeeperBypass(
        prefKey = "powerkeeper_bypass",
        title = "PowerKeeper bypass",
        summary = "Keep GMS and downstream delivery out of Xiaomi power restrictions without double-module setup.",
        scope = ModuleScope.PowerKeeper,
        source = "HyperFCMLive + fcmfix",
        defaultEnabled = true,
    ),
    GmsReconnectTuning(
        prefKey = "gms_reconnect_tuning",
        title = "GMS reconnect tuning",
        summary = "Reserve a dedicated lane for GCM reconnect and heartbeat recovery inside Google Play services.",
        scope = ModuleScope.GooglePlayServices,
        source = "HyperFCMLive + fcmfix",
        defaultEnabled = true,
    ),
    KeepNotifications(
        prefKey = "keep_notifications",
        title = "Keep notifications",
        summary = "Prevent HyperOS from wiping notification history when the target app is killed or refreshed.",
        scope = ModuleScope.SystemServer,
        source = "fcmfix",
        defaultEnabled = false,
    ),
    ;

    companion object {
        val defaultEnabledSet: Set<FeatureKey> = entries.filter { it.defaultEnabled }.toSet()
    }
}
