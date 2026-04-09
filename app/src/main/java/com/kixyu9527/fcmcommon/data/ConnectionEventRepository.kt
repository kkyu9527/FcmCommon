package com.kixyu9527.fcmcommon.data

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

enum class ConnectionEventType(
    val title: String,
) {
    Connected("模块已连接"),
    Disconnected("模块已断连"),
    Boot("系统开机"),
    LockedBoot("直启开机"),
    Shutdown("系统关机"),
    Failure("连接失败"),
}

data class ConnectionEvent(
    val recordedAtMillis: Long,
    val type: ConnectionEventType,
    val detail: String,
)

class ConnectionEventRepository(context: Context) {
    private val storageContext = context.applicationContext.createDeviceProtectedStorageContext()
    private val preferences: SharedPreferences =
        storageContext.getSharedPreferences(ConfigKeys.ConnectionPrefsName, Context.MODE_PRIVATE)

    fun loadEvents(): List<ConnectionEvent> {
        val raw = preferences.getString(ConfigKeys.KeyConnectionEvents, null).orEmpty()
        if (raw.isBlank()) return emptyList()

        return runCatching {
            val jsonArray = JSONArray(raw)
            buildList {
                for (index in 0 until jsonArray.length()) {
                    val item = jsonArray.optJSONObject(index) ?: continue
                    val type = item.optString("type")
                        .let { saved -> ConnectionEventType.entries.firstOrNull { it.name == saved } }
                        ?: continue
                    add(
                        ConnectionEvent(
                            recordedAtMillis = item.optLong("recordedAtMillis"),
                            type = type,
                            detail = item.optString("detail"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun recordConnected(remotePreferencesReady: Boolean): List<ConnectionEvent> = appendEvent(
        ConnectionEvent(
            recordedAtMillis = System.currentTimeMillis(),
            type = ConnectionEventType.Connected,
            detail = if (remotePreferencesReady) {
                "远程配置桥接已就绪。"
            } else {
                "服务已连接，但远程配置桥接暂未就绪。"
            },
        ),
    )

    fun recordDisconnected(reason: String): List<ConnectionEvent> = appendEvent(
        ConnectionEvent(
            recordedAtMillis = System.currentTimeMillis(),
            type = ConnectionEventType.Disconnected,
            detail = reason,
        ),
    )

    fun recordFailure(reason: String): List<ConnectionEvent> = appendEvent(
        ConnectionEvent(
            recordedAtMillis = System.currentTimeMillis(),
            type = ConnectionEventType.Failure,
            detail = reason,
        ),
    )

    fun recordBoot(action: String?): List<ConnectionEvent> = appendEvent(
        ConnectionEvent(
            recordedAtMillis = System.currentTimeMillis(),
            type = when (action) {
                Intent.ACTION_LOCKED_BOOT_COMPLETED -> ConnectionEventType.LockedBoot
                Intent.ACTION_SHUTDOWN -> ConnectionEventType.Shutdown
                else -> ConnectionEventType.Boot
            },
            detail = when (action) {
                Intent.ACTION_LOCKED_BOOT_COMPLETED ->
                    "收到 LOCKED_BOOT_COMPLETED，系统正在直启阶段恢复配置。"

                Intent.ACTION_BOOT_COMPLETED ->
                    "收到 BOOT_COMPLETED，系统已开始同步模块配置。"

                Intent.ACTION_SHUTDOWN ->
                    "收到 ACTION_SHUTDOWN，系统正在关机。"

                else -> "系统触发了开机后的模块同步。"
            },
        ),
    )

    private fun appendEvent(event: ConnectionEvent): List<ConnectionEvent> {
        val updated = buildList {
            add(event)
            addAll(loadEvents())
        }.take(20)

        preferences.edit(commit = true) {
            putString(
                ConfigKeys.KeyConnectionEvents,
                JSONArray().apply {
                    updated.forEach { connectionEvent ->
                        put(
                            JSONObject().apply {
                                put("recordedAtMillis", connectionEvent.recordedAtMillis)
                                put("type", connectionEvent.type.name)
                                put("detail", connectionEvent.detail)
                            },
                        )
                    }
                }.toString(),
            )
        }
        return updated
    }
}
