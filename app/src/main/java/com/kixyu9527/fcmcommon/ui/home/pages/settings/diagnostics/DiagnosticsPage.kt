@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.kixyu9527.fcmcommon.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kixyu9527.fcmcommon.ui.TestTags

@Composable
fun DiagnosticsPage(
    listState: LazyListState,
    uiState: HomeUiState,
    onOpenModuleLogs: () -> Unit,
    onRefreshAll: () -> Unit,
    onRefreshApps: () -> Unit,
) {
    val enabledFeatures = uiState.features.filter { it.enabled }

    PageList(
        state = listState,
        modifier = Modifier.testTag(TestTags.PageDiagnosticsDetail),
        bottomPadding = 28.dp,
    ) {
        item {
            PageCard(modifier = Modifier.testTag(TestTags.DiagnosticsCard)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(title = "当前配置")
                    InfoLine(
                        title = "功能开关",
                        value = "${uiState.enabledFeatureCount}/${uiState.features.size}",
                        secondary = "当前启用的修复项数量",
                    )
                    InfoLine(
                        title = "托管应用",
                        value = uiState.trackedAppsCount.toString(),
                        secondary = "已纳入模块托管的应用数量",
                    )
                    InfoLine(
                        title = "推送候选",
                        value = when {
                            uiState.appsLoading -> "扫描中"
                            !uiState.appsScanned -> "未扫描"
                            else -> uiState.pushCandidateCount.toString()
                        },
                        secondary = "基于最近一次手动或首次扫描得到的候选数量",
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StatusPill(
                            label = "刷新配置",
                            background = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            foreground = MaterialTheme.colorScheme.primary,
                            onClick = onRefreshAll,
                        )
                        StatusPill(
                            label = "重新扫描",
                            background = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                            foreground = MaterialTheme.colorScheme.secondary,
                            onClick = onRefreshApps,
                        )
                    }
                }
            }
        }
        item {
            PageCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        title = "模块日志",
                    )
                    PreferenceNavigationLine(
                        title = "查看模块日志",
                        summary = if (uiState.recentConnectionEvents.isEmpty()) {
                            "模块一旦发生连接、断连或开关机事件，就会在这里显示。"
                        } else {
                            uiState.connectionSummary
                        },
                        value = "进入查看",
                        onClick = onOpenModuleLogs,
                        modifier = Modifier.testTag(TestTags.DiagnosticsLogsEntry),
                    )
                }
            }
        }
        item {
            PageCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(title = "作用域")
                    Column(
                        modifier = Modifier.testTag(TestTags.ScopeRow),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        uiState.scopeStatuses.forEach { scope ->
                            InfoLine(
                                title = scope.label,
                                value = if (scope.active) "已配置" else "未配置",
                                secondary = scope.packageName,
                            )
                        }
                    }
                }
            }
        }
        item {
            PageCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        title = "已启用修复",
                        subtitle = "这里只保留当前生效的功能，不再重复展示连接状态。",
                    )
                    if (enabledFeatures.isEmpty()) {
                        InfoLine(
                            title = "暂无启用项",
                            value = "0",
                            secondary = "可以回到功能配置中开启需要的修复项。",
                        )
                    } else {
                        enabledFeatures.forEachIndexed { index, feature ->
                            InfoLine(
                                title = feature.key.title,
                                value = feature.key.scope,
                                secondary = feature.key.summary,
                            )
                            if (index != enabledFeatures.lastIndex) {
                                SectionDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}
