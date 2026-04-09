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
        summary = "解除 HyperOS 对推送广播的延迟和拦截。",
        scope = "系统服务",
        source = "HyperFCMLive + fcmfix",
        defaultEnabled = true,
    ),
    WakeStoppedApps(
        prefKey = "wake_stopped_apps",
        title = "唤醒已停止应用",
        summary = "让被杀后的目标应用仍能接收 FCM。",
        scope = "系统服务",
        source = "fcmfix",
        defaultEnabled = true,
    ),
    PowerKeeperBypass(
        prefKey = "powerkeeper_bypass",
        title = "PowerKeeper 旁路",
        summary = "放宽小米电量策略对 GMS 的限制。",
        scope = "电量策略",
        source = "HyperFCMLive + fcmfix",
        defaultEnabled = true,
    ),
    GmsReconnectTuning(
        prefKey = "gms_reconnect_tuning",
        title = "GMS 重连调优",
        summary = "保留 Google Play 服务的重连与心跳通道。",
        scope = "Google Play 服务",
        source = "HyperFCMLive + fcmfix",
        defaultEnabled = true,
    ),
    LocalNotificationBypass(
        prefKey = "local_notification_bypass",
        title = "本地通知放行",
        summary = "放行 HyperOS 下被拦截的本地通知。",
        scope = "系统服务",
        source = "fcmfix",
        defaultEnabled = true,
    ),
    KeepNotifications(
        prefKey = "keep_notifications",
        title = "保留通知",
        summary = "阻止系统在清理应用时顺带清空通知。",
        scope = "系统服务",
        source = "fcmfix",
        defaultEnabled = false,
    ),
    IceBoxWakeup(
        prefKey = "icebox_wakeup",
        title = "冰箱应用唤醒",
        summary = "尝试在广播到达前唤醒被冻结的应用。",
        scope = "系统服务",
        source = "fcmfix",
        defaultEnabled = false,
    ),
    ;

    companion object {
        val defaultEnabledSet: Set<FeatureKey> = entries.filter { it.defaultEnabled }.toSet()
    }
}
