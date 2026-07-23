package com.kixyu9527.fcmcommon.ui.component.bottombar

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import com.kixyu9527.fcmcommon.ui.TestTags
import com.kixyu9527.fcmcommon.ui.home.AppPage
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.All
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.Settings

@Composable
fun BoxScope.HomeBottomBarMiuix(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val items = HomeBottomBarDestination.entries.map { destination ->
        NavigationItem(label = destination.label, icon = destination.icon)
    }

    NavigationBar(modifier = modifier.testTag(TestTags.BottomBar)) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                modifier = Modifier
                    .weight(1f)
                    .testTag(TestTags.navItem(HomeBottomBarDestination.entries[index].page.route)),
                icon = item.icon,
                label = item.label,
                selected = pagerState.targetPage == index,
                onClick = {
                    if (pagerState.targetPage != index) {
                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                    }
                },
            )
        }
    }
}

private enum class HomeBottomBarDestination(
    val page: AppPage,
    val label: String,
    val icon: ImageVector,
) {
    Overview(AppPage.Overview, "总览", MiuixIcons.All),
    Apps(AppPage.Apps, "应用", MiuixIcons.GridView),
    Settings(AppPage.Settings, "设置", MiuixIcons.Settings),
}
