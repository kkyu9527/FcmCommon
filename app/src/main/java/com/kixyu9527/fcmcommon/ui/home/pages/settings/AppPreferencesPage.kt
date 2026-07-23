package com.kixyu9527.fcmcommon.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kixyu9527.fcmcommon.ui.TestTags
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.SwitchPreference

@Composable
fun AppPreferencesPage(
    listState: LazyListState,
    uiState: HomeUiState,
    onOnlyPushAppsChanged: (Boolean) -> Unit,
    onShowSystemAppsChanged: (Boolean) -> Unit,
    onShowPackageNameInListChanged: (Boolean) -> Unit,
    onShowDisabledAppsChanged: (Boolean) -> Unit,
    onAllowPushCandidates: () -> Unit,
    onClearAllowList: () -> Unit,
) {
    PageList(
        state = listState,
        modifier = Modifier.testTag(TestTags.PageAppPreferences),
        bottomPadding = 28.dp,
    ) {
        item {
            Column {
                SmallTitle(text = "应用列表")
                Card {
                    SwitchPreference(
                        title = "显示所有应用",
                        summary = "关闭后仅显示推送候选与已托管应用",
                        checked = !uiState.onlyShowPushApps,
                        onCheckedChange = { showAll -> onOnlyPushAppsChanged(!showAll) },
                    )
                    HorizontalDivider()
                    BasicComponent(
                        title = "扫描策略",
                        summary = "首次安装或缓存缺失时自动扫描，后续只在点击“重新扫描”时更新列表。",
                        endActions = {
                            Text(if (uiState.appsScanned) "使用缓存" else "等待首扫")
                        },
                    )
                    HorizontalDivider()
                    SwitchPreference(
                        title = "显示系统应用",
                        summary = "在应用列表中显示系统预装应用",
                        checked = uiState.showSystemApps,
                        onCheckedChange = onShowSystemAppsChanged,
                    )
                    HorizontalDivider()
                    SwitchPreference(
                        title = "列表显示包名",
                        summary = "在应用列表中直接显示包名，方便识别目标应用",
                        checked = uiState.showPackageNameInList,
                        onCheckedChange = onShowPackageNameInListChanged,
                    )
                    HorizontalDivider()
                    SwitchPreference(
                        title = "显示停用应用",
                        summary = "关闭后会隐藏已经停用或冻结的应用",
                        checked = uiState.showDisabledApps,
                        onCheckedChange = onShowDisabledAppsChanged,
                    )
                }
            }
        }
        item {
            Column {
                SmallTitle(text = "列表管理")
                Card {
                    BasicComponent(
                        title = "纳入全部推送候选",
                        summary = "将最近一次扫描识别到的候选应用加入托管",
                        onClick = onAllowPushCandidates,
                    )
                    HorizontalDivider()
                    BasicComponent(
                        title = "清空托管列表",
                        summary = "移除当前全部托管应用",
                        onClick = onClearAllowList,
                    )
                }
            }
        }
    }
}
