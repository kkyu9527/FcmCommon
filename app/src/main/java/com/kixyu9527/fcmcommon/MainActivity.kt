package com.kixyu9527.fcmcommon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kixyu9527.fcmcommon.ui.home.HomeRoute
import com.kixyu9527.fcmcommon.ui.theme.FcmCommonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FcmCommonTheme {
                HomeRoute()
            }
        }
    }
}
