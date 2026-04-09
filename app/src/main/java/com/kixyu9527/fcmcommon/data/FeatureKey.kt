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
        title = "澎湃广播解限",
        summary = "解除 HyperOS 对重连广播和推送投递广播的延迟与拦截限制。",
        scope = "系统服务",
        source = "HyperFCMLive + fcmfix",
        defaultEnabled = true,
    ),
    WakeStoppedApps(
        prefKey = "wake_stopped_apps",
        title = "唤醒已停止应用",
        summary = "为目标应用补上 stopped-package 标记，让被杀后的应用也能收到 FCM。",
        scope = "系统服务",
        source = "fcmfix",
        defaultEnabled = true,
    ),
    PowerKeeperBypass(
        prefKey = "powerkeeper_bypass",
        title = "PowerKeeper 旁路",
        summary = "统一处理 GMS 在小米电量策略下的网络、闹钟与白名单限制。",
        scope = "电量策略",
        source = "HyperFCMLive + fcmfix",
        defaultEnabled = true,
    ),
    GmsReconnectTuning(
        prefKey = "gms_reconnect_tuning",
        title = "GMS 重连调优",
        summary = "为 Google Play 服务保留 GCM 重连与心跳恢复通道。",
        scope = "Google Play 服务",
        source = "HyperFCMLive + fcmfix",
        defaultEnabled = true,
    ),
    LocalNotificationBypass(
        prefKey = "local_notification_bypass",
        title = "本地通知放行",
        summary = "放行目标应用在 HyperOS 下被本地通知策略拦截的通知。",
        scope = "系统服务",
        source = "fcmfix",
        defaultEnabled = true,
    ),
    KeepNotifications(
        prefKey = "keep_notifications",
        title = "保留通知",
        summary = "阻止 HyperOS 在应用被清理或刷新时自动清空对应通知。",
        scope = "系统服务",
        source = "fcmfix",
        defaultEnabled = false,
    ),
    IceBoxWakeup(
        prefKey = "icebox_wakeup",
        title = "冰箱应用唤醒",
        summary = "允许在广播到达前尝试唤醒被 Ice Box 冻结的目标应用。",
        scope = "系统服务",
        source = "fcmfix",
        defaultEnabled = false,
    ),
    ;

    companion object {
        val defaultEnabledSet: Set<FeatureKey> = entries.filter { it.defaultEnabled }.toSet()
    }
}
