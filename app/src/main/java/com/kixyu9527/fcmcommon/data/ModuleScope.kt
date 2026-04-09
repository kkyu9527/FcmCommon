package com.kixyu9527.fcmcommon.data

object ModuleScope {
    const val SystemServer = "system_server"
    const val PowerKeeper = "com.miui.powerkeeper"
    const val GooglePlayServices = "com.google.android.gms"

    val defaultScopes: Set<String> = linkedSetOf(
        SystemServer,
        PowerKeeper,
        GooglePlayServices,
    )
}
