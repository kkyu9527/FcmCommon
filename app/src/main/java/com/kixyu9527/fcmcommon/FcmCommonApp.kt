package com.kixyu9527.fcmcommon

import android.app.Application
import android.content.Context
import com.kixyu9527.fcmcommon.data.ConfigKeys
import com.kixyu9527.fcmcommon.core.AppContainer

class FcmCommonApp : Application() {
    val container: AppContainer by lazy {
        AppContainer(this)
    }

    override fun onCreate() {
        super.onCreate()
        val deviceContext = createDeviceProtectedStorageContext()
        deviceContext.moveSharedPreferencesFrom(this, ConfigKeys.LocalPrefsName)
        deviceContext.moveSharedPreferencesFrom(this, ConfigKeys.UiPrefsName)
        container
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as FcmCommonApp).container
