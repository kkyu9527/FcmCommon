package com.kixyu9527.fcmcommon

import android.graphics.Color
import android.os.Bundle
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import com.kixyu9527.fcmcommon.ui.home.HomeRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemBarStyle(
            darkTheme = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES,
        )
        window.isNavigationBarContrastEnforced = false
        applyPreferredRefreshRate()
        setContent {
            HomeRoute(applySystemBarStyle = ::applySystemBarStyle)
        }
    }

    override fun onResume() {
        super.onResume()
        applyPreferredRefreshRate()
    }

    private fun applySystemBarStyle(darkTheme: Boolean) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT,
            ) { darkTheme },
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT,
            ) { darkTheme },
        )
    }

    private fun applyPreferredRefreshRate() {
        val attributes = window.attributes
        if (attributes.preferredRefreshRate < 120f) {
            attributes.preferredRefreshRate = 120f
            window.attributes = attributes
        }
    }
}
