package com.kixyu9527.fcmcommon.xposed

import android.os.SystemClock
import com.kixyu9527.fcmcommon.data.ConnectionEvent
import com.kixyu9527.fcmcommon.data.ConnectionEventRepository
import com.kixyu9527.fcmcommon.data.ModuleScope
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class XposedServiceState(
    val isConnected: Boolean,
    val headline: String,
    val detail: String,
    val hasRemotePreferences: Boolean,
    val activeScopes: Set<String>,
    val connectedAtElapsedRealtime: Long?,
    val connectionEvents: List<ConnectionEvent>,
) {
    companion object {
        fun bootstrap(connectionEvents: List<ConnectionEvent> = emptyList()): XposedServiceState =
            XposedServiceState(
                isConnected = false,
                headline = "等待模块连接",
                detail = "请确认已在 LSPosed 启用 FcmCommon 并勾选作用域。",
                hasRemotePreferences = false,
                activeScopes = ModuleScope.defaultScopes,
                connectedAtElapsedRealtime = null,
                connectionEvents = connectionEvents,
            )
    }
}

class XposedServiceMonitor(
    private val connectionEventRepository: ConnectionEventRepository,
) {
    private val mutableState = MutableStateFlow(
        XposedServiceState.bootstrap(connectionEventRepository.loadEvents()),
    )
    val state: StateFlow<XposedServiceState> = mutableState.asStateFlow()

    init {
        runCatching {
            XposedServiceHelper.registerListener(
                object : XposedServiceHelper.OnServiceListener {
                    override fun onServiceBind(service: XposedService) {
                        val remotePreferencesReady = runCatching {
                            service.getRemotePreferences("config")
                            true
                        }.getOrDefault(false)
                        val events = connectionEventRepository.recordConnected(remotePreferencesReady)

                        mutableState.value = XposedServiceState(
                            isConnected = true,
                            headline = "模块已连接",
                            detail = if (remotePreferencesReady) {
                                "当前配置已同步到模块作用域。"
                            } else {
                                "模块已连接，但配置桥接暂未就绪。"
                            },
                            hasRemotePreferences = remotePreferencesReady,
                            activeScopes = ModuleScope.defaultScopes,
                            connectedAtElapsedRealtime = SystemClock.elapsedRealtime(),
                            connectionEvents = events,
                        )
                    }

                    override fun onServiceDied(service: XposedService) {
                        val reason = "模块服务进程已断开，可能是系统重启、LSPosed 重载或服务进程被回收。"
                        val events = connectionEventRepository.recordDisconnected(reason)
                        mutableState.value = XposedServiceState(
                            isConnected = false,
                            headline = "模块已断连",
                            detail = reason,
                            hasRemotePreferences = false,
                            activeScopes = ModuleScope.defaultScopes,
                            connectedAtElapsedRealtime = null,
                            connectionEvents = events,
                        )
                    }
                },
            )
        }.onFailure { throwable ->
            val reason = throwable.message
                ?: "当前进程里没有可用的 LibXposed 服务接口。"
            val events = connectionEventRepository.recordFailure(reason)
            mutableState.value = XposedServiceState(
                isConnected = false,
                headline = "无法连接模块服务",
                detail = reason,
                hasRemotePreferences = false,
                activeScopes = ModuleScope.defaultScopes,
                connectedAtElapsedRealtime = null,
                connectionEvents = events,
            )
        }
    }
}
