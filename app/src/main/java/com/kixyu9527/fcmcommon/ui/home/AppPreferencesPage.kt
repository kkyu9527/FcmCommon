package com.kixyu9527.fcmcommon.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kixyu9527.fcmcommon.ui.TestTags

@Composable
fun AppPreferencesPage(
    listState: LazyListState,
    uiState: HomeUiState,
    onOnlyPushAppsChanged: (Boolean) -> Unit,
    onAutoRefreshAppsOnLaunchChanged: (Boolean) -> Unit,
    onShowSystemAppsChanged: (Boolean) -> Unit,
    onShowPackageNameInListChanged: (Boolean) -> Unit,
    onShowDisabledAppsChanged: (Boolean) -> Unit,
) {
    PageList(
        state = listState,
        modifier = Modifier.testTag(TestTags.PageAppPreferences),
        bottomPadding = 28.dp,
    ) {
        item {
            PageCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(title = "应用列表")
                    PreferenceToggleLine(
                        title = "优先显示推送候选",
                        summary = "应用页默认优先显示检测到 FCM 的应用",
                        checked = uiState.onlyShowPushApps,
                        onCheckedChange = onOnlyPushAppsChanged,
                    )
                    SectionDivider()
                    PreferenceToggleLine(
                        title = "启动时自动扫描应用",
                        summary = "进入 FcmCommon 时自动刷新应用列表",
                        checked = uiState.autoRefreshAppsOnLaunch,
                        onCheckedChange = onAutoRefreshAppsOnLaunchChanged,
                    )
                    SectionDivider()
                    PreferenceToggleLine(
                        title = "显示系统应用",
                        summary = "在应用列表中显示系统预装应用",
                        checked = uiState.showSystemApps,
                        onCheckedChange = onShowSystemAppsChanged,
                    )
                    SectionDivider()
                    PreferenceToggleLine(
                        title = "列表显示包名",
                        summary = "在应用列表中直接显示包名，方便识别目标应用",
                        checked = uiState.showPackageNameInList,
                        onCheckedChange = onShowPackageNameInListChanged,
                    )
                    SectionDivider()
                    PreferenceToggleLine(
                        title = "显示停用应用",
                        summary = "关闭后会隐藏已经停用或冻结的应用",
                        checked = uiState.showDisabledApps,
                        onCheckedChange = onShowDisabledAppsChanged,
                    )
                }
            }
        }
    }
}
