package com.kixyu9527.fcmcommon.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kixyu9527.fcmcommon.data.AppThemeMode
import com.kixyu9527.fcmcommon.ui.TestTags
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsPage(
    listState: LazyListState,
    uiState: HomeUiState,
    topPadding: Dp = 0.dp,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    onOpenAppPreferences: () -> Unit,
    onOpenFeatures: () -> Unit,
    onOpenDiagnostics: () -> Unit,
) {
    val moduleReady = uiState.connection.isConnected && uiState.connection.hasRemotePreferences

    PageList(
        state = listState,
        modifier = Modifier.testTag(TestTags.PageSettings),
        topPadding = topPadding,
    ) {
        item {
            Column {
                SmallTitle(text = "显示")
                Card {
                    ThemeModePreference(
                        selectedMode = uiState.themeMode,
                        onThemeModeChanged = onThemeModeChanged,
                    )
                }
            }
        }
        item {
            Column {
                SmallTitle(text = "设置")
                Card {
                    ArrowPreference(
                        title = "应用偏好",
                        summary = "筛选规则与扫描行为",
                        endActions = { Text(buildAppPreferenceSummary(uiState)) },
                        onClick = onOpenAppPreferences,
                        modifier = Modifier.testTag(TestTags.SettingsAppsEntry),
                    )
                    HorizontalDivider()
                    ArrowPreference(
                        title = "功能配置",
                        summary = "推送修复与兼容开关",
                        endActions = {
                            Text(
                                "${uiState.enabledFeatureCount}/${uiState.features.size}",
                            )
                        },
                        onClick = onOpenFeatures,
                        modifier = Modifier.testTag(TestTags.SettingsFeaturesEntry),
                    )
                    HorizontalDivider()
                    ArrowPreference(
                        title = "模块状态",
                        summary = if (moduleReady) {
                            "服务连接与配置同步正常"
                        } else {
                            "模块尚未就绪，进入查看详情"
                        },
                        endActions = {
                            Text(
                                text = if (moduleReady) "正常" else "待检查",
                                color = if (moduleReady) {
                                    MiuixTheme.colorScheme.primary
                                } else {
                                    MiuixTheme.colorScheme.onSurfaceVariantSummary
                                },
                            )
                        },
                        onClick = onOpenDiagnostics,
                        modifier = Modifier.testTag(TestTags.SettingsDiagnosticsEntry),
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeModePreference(
    selectedMode: AppThemeMode,
    onThemeModeChanged: (AppThemeMode) -> Unit,
) {
    val items = AppThemeMode.entries
    OverlayDropdownPreference(
        items = items.map(AppThemeMode::title),
        selectedIndex = items.indexOf(selectedMode),
        title = "主题",
        modifier = Modifier.testTag(TestTags.ThemeModeDropdown),
        onSelectedIndexChange = { index -> onThemeModeChanged(items[index]) },
    )
}

private fun buildAppPreferenceSummary(uiState: HomeUiState): String {
    var enabledCount = 0
    if (uiState.onlyShowPushApps) enabledCount++
    if (uiState.showSystemApps) enabledCount++
    if (uiState.showPackageNameInList) enabledCount++
    if (uiState.showDisabledApps) enabledCount++
    return "$enabledCount 项已开"
}
