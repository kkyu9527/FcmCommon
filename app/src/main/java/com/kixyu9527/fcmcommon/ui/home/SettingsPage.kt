package com.kixyu9527.fcmcommon.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
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
        Box {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader(title = "外观")
                PreferenceDropdownLine(
                    title = "亮暗色模式",
                    summary = selectedMode.summary,
                    value = selectedMode.title,
                    expanded = expanded,
                    onClick = { expanded = !expanded },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { anchorWidth = it.width }
                        .testTag(TestTags.ThemeModeDropdown),
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = if (anchorWidth > 0) {
                    Modifier.width(with(density) { anchorWidth.toDp() })
                } else {
                    Modifier
                },
                offset = DpOffset(x = 0.dp, y = 8.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f),
                ),
            ) {
                AppThemeMode.entries.forEachIndexed { index, mode ->
                    DropdownMenuItem(
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = mode.title,
                                    style = MaterialTheme.typography.titleMedium,
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
                        trailingIcon = {
                            if (selectedMode == mode) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                        onClick = {
                            expanded = false
                            onThemeModeChanged(mode)
                        },
                        modifier = Modifier.testTag(TestTags.themeMode(mode)),
                    )
                    if (index != AppThemeMode.entries.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                        )
                    }
                }
            }
        }
    }
}

private fun buildAppPreferenceSummary(uiState: HomeUiState): String {
    var enabledCount = 0
    if (uiState.onlyShowPushApps) enabledCount++
    if (uiState.showSystemApps) enabledCount++
    if (uiState.showPackageNameInList) enabledCount++
    if (uiState.showDisabledApps) enabledCount++
    return "$enabledCount 项已开"
}
