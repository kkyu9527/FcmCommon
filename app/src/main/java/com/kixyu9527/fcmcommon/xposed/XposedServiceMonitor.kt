package com.kixyu9527.fcmcommon.xposed

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
) {
    companion object {
        fun bootstrap(): XposedServiceState = XposedServiceState(
            isConnected = false,
            headline = "等待模块连接",
            detail = "请确认已在 LSPosed 启用 FcmCommon 并勾选作用域。",
            hasRemotePreferences = false,
            activeScopes = ModuleScope.defaultScopes,
        )
    }
}

class XposedServiceMonitor {
    private val mutableState = MutableStateFlow(XposedServiceState.bootstrap())
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
                        )
                    }

                    override fun onServiceDied(service: XposedService) {
                        mutableState.value = XposedServiceState.bootstrap()
                    }
                },
            )
        }.onFailure { throwable ->
            mutableState.value = XposedServiceState(
                isConnected = false,
                headline = "无法连接模块服务",
                detail = throwable.message
                    ?: "当前进程里没有可用的 LibXposed 服务接口。",
                hasRemotePreferences = false,
                activeScopes = ModuleScope.defaultScopes,
            )
        }
    }
}
