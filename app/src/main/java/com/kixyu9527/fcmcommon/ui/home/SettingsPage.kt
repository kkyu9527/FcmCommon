@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.kixyu9527.fcmcommon.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.kixyu9527.fcmcommon.data.AppThemeMode
import com.kixyu9527.fcmcommon.ui.TestTags

@Composable
fun SettingsPage(
    listState: LazyListState,
    uiState: HomeUiState,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    onOpenAppPreferences: () -> Unit,
    onOpenFeatures: () -> Unit,
    onOpenDiagnostics: () -> Unit,
) {
    PageList(
        state = listState,
        modifier = Modifier.testTag(TestTags.PageSettings),
    ) {
        item {
            ThemeModeCard(
                selectedMode = uiState.themeMode,
                onThemeModeChanged = onThemeModeChanged,
            )
        }
        item {
            PageCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(title = "更多设置")
                    PreferenceNavigationLine(
                        title = "应用偏好",
                        summary = "筛选规则与扫描行为",
                        value = buildAppPreferenceSummary(uiState),
                        onClick = onOpenAppPreferences,
                        modifier = Modifier.testTag(TestTags.SettingsAppsEntry),
                    )
                    SectionDivider()
                    PreferenceNavigationLine(
                        title = "功能配置",
                        summary = "推送修复与兼容开关",
                        value = "${uiState.enabledFeatureCount}/${uiState.features.size}",
                        onClick = onOpenFeatures,
                        modifier = Modifier.testTag(TestTags.SettingsFeaturesEntry),
                    )
                    SectionDivider()
                    PreferenceNavigationLine(
                        title = "模块状态",
                        summary = "查看作用域与当前配置详情",
                        value = "进入查看",
                        onClick = onOpenDiagnostics,
                        modifier = Modifier.testTag(TestTags.SettingsDiagnosticsEntry),
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeModeCard(
    selectedMode: AppThemeMode,
    onThemeModeChanged: (AppThemeMode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var anchorWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    PageCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(title = "外观")
            Box(modifier = Modifier.fillMaxWidth()) {
                PreferenceDropdownLine(
                    title = "亮暗色模式",
                    summary = "默认跟随系统",
                    value = selectedMode.title,
                    expanded = expanded,
                    onClick = { expanded = !expanded },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            anchorWidth = coordinates.size.width
                        }
                        .testTag(TestTags.ThemeModeDropdown),
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    offset = DpOffset(x = 0.dp, y = 8.dp),
                    properties = PopupProperties(focusable = true),
                    modifier = if (anchorWidth > 0) {
                        Modifier.width(with(density) { anchorWidth.toDp() })
                    } else {
                        Modifier
                    },
                ) {
                    AppThemeMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = mode.title,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = mode.summary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                            trailingIcon = if (selectedMode == mode) {
                                {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            } else {
                                null
                            },
                            onClick = {
                                expanded = false
                                onThemeModeChanged(mode)
                            },
                            modifier = Modifier.testTag(TestTags.themeMode(mode)),
                        )
                    }
                }
            } 
        }
    }
}

private fun buildAppPreferenceSummary(uiState: HomeUiState): String {
    val parts = mutableListOf<String>()
    parts += if (uiState.onlyShowPushApps) "候选优先" else "显示全部"
    parts += if (uiState.autoRefreshAppsOnLaunch) "自动扫描" else "手动扫描"
    parts += if (uiState.showSystemApps) "包含系统应用" else "隐藏系统应用"
    parts += if (uiState.showPackageNameInList) "显示包名" else "隐藏包名"
    parts += if (uiState.showDisabledApps) "显示停用应用" else "隐藏停用应用"
    parts += if (uiState.showVersionNameInList) "显示版本" else "隐藏版本"
    return parts.joinToString(" · ")
}
