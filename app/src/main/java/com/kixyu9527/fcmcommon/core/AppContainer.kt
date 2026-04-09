package com.kixyu9527.fcmcommon.core

import android.content.Context
import com.kixyu9527.fcmcommon.data.InstalledAppsRepository
import com.kixyu9527.fcmcommon.data.LocalModuleConfigRepository
import com.kixyu9527.fcmcommon.xposed.XposedServiceMonitor

class AppContainer(context: Context) {
    val configRepository = LocalModuleConfigRepository(context.applicationContext)
    val installedAppsRepository = InstalledAppsRepository(context.applicationContext)
    val xposedServiceMonitor = XposedServiceMonitor()
}
