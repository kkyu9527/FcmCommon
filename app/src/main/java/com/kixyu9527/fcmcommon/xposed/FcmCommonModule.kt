package com.kixyu9527.fcmcommon.xposed

import android.util.Log
import com.kixyu9527.fcmcommon.data.ModuleScope
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface

class FcmCommonModule : XposedModule() {
    override fun onSystemServerStarting(param: XposedModuleInterface.SystemServerStartingParam) {
        log(
            Log.INFO,
            TAG,
            "Preparing hook bundle for ${ModuleScope.SystemServer}",
            null,
        )
    }

    override fun onPackageReady(param: XposedModuleInterface.PackageReadyParam) {
        if (!param.isFirstPackage()) {
            return
        }

        when (param.packageName) {
            ModuleScope.PowerKeeper -> log(
                Log.INFO,
                TAG,
                "Preparing hook bundle for ${ModuleScope.PowerKeeper}",
                null,
            )

            ModuleScope.GooglePlayServices -> log(
                Log.INFO,
                TAG,
                "Preparing hook bundle for ${ModuleScope.GooglePlayServices}",
                null,
            )
        }
    }

    private companion object {
        private const val TAG = "FcmCommonModule"
    }
}
