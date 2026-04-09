package com.kixyu9527.fcmcommon.xposed

import android.util.Log
import com.kixyu9527.fcmcommon.data.ModuleScope
import com.kixyu9527.fcmcommon.xposed.compat.XposedBridge
import com.kixyu9527.fcmcommon.xposed.legacy.AutoStartFix
import com.kixyu9527.fcmcommon.xposed.legacy.BroadcastFix
import com.kixyu9527.fcmcommon.xposed.legacy.HyperOsPowerKeeperFix
import com.kixyu9527.fcmcommon.xposed.legacy.HyperOsSystemFix
import com.kixyu9527.fcmcommon.xposed.legacy.GmsReconnectFix
import com.kixyu9527.fcmcommon.xposed.legacy.KeepNotification
import com.kixyu9527.fcmcommon.xposed.legacy.LegacyHookModule
import com.kixyu9527.fcmcommon.xposed.legacy.MiuiLocalNotificationFix
import com.kixyu9527.fcmcommon.xposed.legacy.OplusProxyFix
import com.kixyu9527.fcmcommon.xposed.legacy.PowerkeeperFix
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface

class FcmCommonModule : XposedModule() {
    override fun onSystemServerStarting(param: XposedModuleInterface.SystemServerStartingParam) {
        XposedBridge.init(this)
        LegacyHookModule.setSelfPackageName("android")

        installHook("BroadcastFix") { BroadcastFix(param.classLoader) }
        installHook("MiuiLocalNotificationFix") { MiuiLocalNotificationFix(param.classLoader) }
        installHook("AutoStartFix") { AutoStartFix(param.classLoader) }
        installHook("KeepNotification") { KeepNotification(param.classLoader) }
        installHook("HyperOsSystemFix") { HyperOsSystemFix(param.classLoader) }
        installHook("OplusProxyFix") { OplusProxyFix(param.classLoader) }
    }

    override fun onPackageReady(param: XposedModuleInterface.PackageReadyParam) {
        if (!param.isFirstPackage()) {
            return
        }

        XposedBridge.init(this)

        when (param.packageName) {
            ModuleScope.PowerKeeper -> {
                LegacyHookModule.setSelfPackageName(ModuleScope.PowerKeeper)
                installHook("PowerkeeperFix") { PowerkeeperFix(param.classLoader) }
                installHook("HyperOsPowerKeeperFix") { HyperOsPowerKeeperFix(param.classLoader) }
            }

            ModuleScope.GooglePlayServices -> {
                LegacyHookModule.setSelfPackageName(ModuleScope.GooglePlayServices)
                installHook("GmsReconnectFix") { GmsReconnectFix(param.classLoader) }
            }
        }
    }

    private inline fun installHook(name: String, install: () -> Unit) {
        runCatching(install)
            .onSuccess {
                log(Log.INFO, TAG, "Hook ready: $name", null)
            }
            .onFailure { throwable ->
                log(Log.ERROR, TAG, "Hook failed: $name", throwable)
            }
    }

    private companion object {
        private const val TAG = "FcmCommonModule"
    }
}
