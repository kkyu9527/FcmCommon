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
            headline = "Waiting for Xposed service",
            detail = "The Compose console is ready. Binding will appear after LSPosed loads the module process.",
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
                            service.getRemotePreferences("config") != null
                        }.getOrDefault(false)

                        mutableState.value = XposedServiceState(
                            isConnected = true,
                            headline = "Xposed bridge connected",
                            detail = if (remotePreferencesReady) {
                                "Remote preferences are available. The UI can now evolve into a full module console."
                            } else {
                                "Bridge connected, but remote preferences are not mounted yet."
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
                headline = "Xposed service unavailable",
                detail = throwable.message
                    ?: "LibXposed service helper is not available in this process.",
                hasRemotePreferences = false,
                activeScopes = ModuleScope.defaultScopes,
            )
        }
    }
}
