package com.kixyu9527.fcmcommon.data

import android.content.Context
import android.content.SharedPreferences

data class FcmDiagnosticsState(
    val isConnected: Boolean = false,
    val connectedSinceMillis: Long? = null,
    val lastEventAtMillis: Long? = null,
    val lastEventTitle: String = "暂无记录",
    val lastEventDetail: String = "等待 Google Play 服务上报连接状态。",
) {
    companion object {
        fun bootstrap(): FcmDiagnosticsState = FcmDiagnosticsState()
    }
}

class FcmDiagnosticsStateRepository(context: Context) {
    private val storageContext = context.applicationContext.createDeviceProtectedStorageContext()
    private val preferences: SharedPreferences =
        storageContext.getSharedPreferences(ConfigKeys.ConnectionPrefsName, Context.MODE_PRIVATE)

    fun loadState(): FcmDiagnosticsState = FcmDiagnosticsState(
        isConnected = preferences.getBoolean(ConfigKeys.KeyFcmDiagnosticsConnected, false),
        connectedSinceMillis = preferences.getLong(ConfigKeys.KeyFcmDiagnosticsConnectedSince, 0L)
            .takeIf { it > 0L },
        lastEventAtMillis = preferences.getLong(ConfigKeys.KeyFcmDiagnosticsLastEventAt, 0L)
            .takeIf { it > 0L },
        lastEventTitle = preferences.getString(
            ConfigKeys.KeyFcmDiagnosticsLastEventTitle,
            "暂无记录",
        ).orEmpty().ifBlank { "暂无记录" },
        lastEventDetail = preferences.getString(
            ConfigKeys.KeyFcmDiagnosticsLastEventDetail,
            "等待 Google Play 服务上报连接状态。",
        ).orEmpty().ifBlank { "等待 Google Play 服务上报连接状态。" },
    )
}
