package com.kixyu9527.fcmcommon

import android.app.Application
import android.content.Context
import com.kixyu9527.fcmcommon.core.AppContainer

class FcmCommonApp : Application() {
    val container: AppContainer by lazy {
        AppContainer(this)
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as FcmCommonApp).container
