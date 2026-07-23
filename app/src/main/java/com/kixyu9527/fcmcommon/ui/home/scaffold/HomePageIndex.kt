package com.kixyu9527.fcmcommon.ui.home

internal fun pageIndex(page: AppPage): Int = when (page) {
    AppPage.Overview -> 0
    AppPage.Apps -> 1
    AppPage.Settings -> 2
}

internal fun pageAtIndex(index: Int): AppPage = when (index) {
    0 -> AppPage.Overview
    1 -> AppPage.Apps
    else -> AppPage.Settings
}
