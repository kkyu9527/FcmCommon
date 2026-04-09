package com.kixyu9527.fcmcommon.data

enum class AppThemeMode(
    val title: String,
    val summary: String,
) {
    System(
        title = "跟随系统",
        summary = "默认使用系统深浅色设置",
    ),
    Light(
        title = "浅色模式",
        summary = "始终使用浅色外观",
    ),
    Dark(
        title = "深色模式",
        summary = "始终使用深色外观",
    ),
    ;

    fun resolve(systemDark: Boolean): Boolean = when (this) {
        System -> systemDark
        Light -> false
        Dark -> true
    }
}
