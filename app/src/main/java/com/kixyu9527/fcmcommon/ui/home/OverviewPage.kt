@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.kixyu9527.fcmcommon.ui.home

import android.content.ComponentName
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kixyu9527.fcmcommon.ui.TestTags

@Composable
fun OverviewPage(
    listState: LazyListState,
    uiState: HomeUiState,
    onRefreshAll: () -> Unit,
    onApplyRecommendedFeatures: () -> Unit,
) {
    val context = LocalContext.current
    val enabledFeatures = remember(uiState.features) { uiState.features.filter { it.enabled } }
    val recentEvents = remember(uiState.recentConnectionEvents) { uiState.recentConnectionEvents.take(3) }

    PageList(
        state = listState,
        modifier = Modifier.testTag(TestTags.PageOverview),
    ) {
        item {
            PageCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(
                        title = "模块状态",
                        subtitle = uiState.connection.detail,
                    )
                    InfoLine(
                        title = uiState.connection.headline,
                        value = if (uiState.connection.isConnected) "正常" else "等待中",
                        secondary = if (uiState.connection.isConnected) {
                            "模块桥接已建立，可继续同步配置。"
                        } else {
                            null
                        },
                    )
                    InfoLine(
                        title = "FCM Diagnostics",
                        value = if (uiState.fcmDiagnosticsConnected) "已连接" else "未连接",
                        secondary = if (uiState.fcmDiagnosticsConnected) {
                            "已连接 ${uiState.fcmDiagnosticsDurationLabel}"
                        } else {
                            uiState.fcmDiagnosticsSummary
                        },
                    )
                    InfoLine(
                        title = "远程配置",
                        value = if (uiState.connection.hasRemotePreferences) "已就绪" else "未就绪",
                        secondary = "用于同步模块开关和白名单配置。",
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StatusPill(
                            label = "刷新配置",
                            background = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            foreground = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag(TestTags.ActionRefreshConfig),
                            onClick = onRefreshAll,
                        )
                        StatusPill(
                            label = "恢复推荐",
                            background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                            foreground = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag(TestTags.ActionApplyRecommended),
                            onClick = onApplyRecommendedFeatures,
                        )
                        StatusPill(
                            label = "Play 服务诊断",
                            background = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                            foreground = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.testTag(TestTags.ActionOpenPlayServicesDiagnostics),
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    setPackage("com.google.android.gms")
                                    component = ComponentName(
                                        "com.google.android.gms",
                                        "com.google.android.gms.gcm.GcmDiagnostics",
                                    )
                                }
                                runCatching {
                                    context.startActivity(intent)
                                }.onFailure {
                                    Toast.makeText(
                                        context,
                                        "当前设备未找到 Play 服务诊断页面",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            },
                        )
                    }
                }
            }
        }
        item {
            PageCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(
                        title = "概览指标",
                        subtitle = "首页直接展示常用状态，不用再跳到二级页查看。",
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        uiState.overviewStats.forEach { stat ->
                            MetricTile(stat = stat)
                        }
                    }
                }
            }
        }
        item {
            PageCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(title = "作用域状态")
                    uiState.scopeStatuses.forEachIndexed { index, scope ->
                        InfoLine(
                            title = scope.label,
                            value = if (scope.active) "已配置" else "未配置",
                            secondary = scope.packageName,
                        )
                        if (index != uiState.scopeStatuses.lastIndex) {
                            SectionDivider()
                        }
                    }
                }
            }
        }
        item {
            PageCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        title = "当前已启用修复",
                        subtitle = if (enabledFeatures.isEmpty()) {
                            "当前没有启用任何修复项。"
                        } else {
                            "已启用 ${enabledFeatures.size} 项，首页保留最常看的摘要。"
                        },
                    )
                    if (enabledFeatures.isEmpty()) {
                        InfoLine(
                            title = "暂无启用项",
                            value = "0",
                            secondary = "可以在设置中的功能配置里按需启用。",
                        )
                    } else {
                        enabledFeatures.take(4).forEachIndexed { index, feature ->
                            InfoLine(
                                title = feature.key.title,
                                value = feature.key.scope,
                                secondary = feature.key.summary,
                            )
                            if (index != minOf(enabledFeatures.lastIndex, 3)) {
                                SectionDivider()
                            }
                        }
                    }
                }
            }
        }
        item {
            PageCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(title = "最近事件")
                    if (recentEvents.isEmpty()) {
                        InfoLine(
                            title = "暂无记录",
                            value = "等待事件",
                            secondary = "模块连接、断连和系统开关机会记录在这里。",
                        )
                    } else {
                        recentEvents.forEachIndexed { index, event ->
                            InfoLine(
                                title = event.title,
                                value = event.timestamp,
                                secondary = event.detail,
                            )
                            if (index != recentEvents.lastIndex) {
                                SectionDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}
