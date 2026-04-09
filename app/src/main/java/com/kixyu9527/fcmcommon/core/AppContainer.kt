package com.kixyu9527.fcmcommon.core

import android.content.Context
import com.kixyu9527.fcmcommon.data.ConnectionEventRepository
import com.kixyu9527.fcmcommon.data.FcmDiagnosticsStateRepository
import com.kixyu9527.fcmcommon.data.InstalledAppsRepository
import com.kixyu9527.fcmcommon.data.LocalModuleConfigRepository
import com.kixyu9527.fcmcommon.data.LocalUiSettingsRepository
import com.kixyu9527.fcmcommon.xposed.XposedServiceMonitor

class AppContainer(context: Context) {
    val configRepository = LocalModuleConfigRepository(context.applicationContext)
    val uiSettingsRepository = LocalUiSettingsRepository(context.applicationContext)
    val installedAppsRepository = InstalledAppsRepository(context.applicationContext)
    val connectionEventRepository = ConnectionEventRepository(context.applicationContext)
    val fcmDiagnosticsStateRepository = FcmDiagnosticsStateRepository(context.applicationContext)
    val xposedServiceMonitor = XposedServiceMonitor(connectionEventRepository)
}
