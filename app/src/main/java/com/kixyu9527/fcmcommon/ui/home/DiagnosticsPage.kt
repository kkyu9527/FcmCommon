package com.kixyu9527.fcmcommon.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.kixyu9527.fcmcommon.ui.TestTags

@Composable
fun DiagnosticsPage(
    uiState: HomeUiState,
    onRefreshAll: () -> Unit,
    onRefreshApps: () -> Unit,
) {
    PageList(modifier = Modifier.testTag(TestTags.PageDiagnostics)) {
        item {
            PageCard(modifier = Modifier.testTag(TestTags.DiagnosticsCard)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(title = "连接状态")
                    InfoLine(
                        title = uiState.connection.headline,
                        value = if (uiState.connection.isConnected) "已连接" else "未连接",
                        secondary = uiState.connection.detail,
                    )
                    InfoLine(
                        title = "远程配置",
                        value = if (uiState.connection.hasRemotePreferences) "已就绪" else "未就绪",
                    )
                }
            }
        }
        item {
            PageCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SectionHeader(title = "当前配置")
                    InfoLine(
                        title = "功能开关",
                        value = "${uiState.enabledFeatureCount}/${uiState.features.size}",
                    )
                    InfoLine(
                        title = "白名单应用",
                        value = uiState.trackedAppsCount.toString(),
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
                            label = "重扫应用",
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
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(title = "作用域")
                    Column(
                        modifier = Modifier.testTag(TestTags.ScopeRow),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        uiState.scopeStatuses.forEach { scope ->
                            InfoLine(
                                title = scope.label,
                                value = scope.packageName,
                            )
                        }
                    }
                }
            }
        }
    }
}
