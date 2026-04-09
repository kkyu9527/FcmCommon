@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.kixyu9527.fcmcommon.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.kixyu9527.fcmcommon.data.FeatureKey
import com.kixyu9527.fcmcommon.ui.TestTags

@Composable
fun OverviewPage(
    uiState: HomeUiState,
    onRefreshAll: () -> Unit,
    onApplyRecommendedFeatures: () -> Unit,
) {
    PageList(modifier = Modifier.testTag(TestTags.PageOverview)) {
        item {
            PageCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SectionHeader(
                        title = "模块状态",
                        subtitle = uiState.connection.detail,
                    )
                    InfoLine(
                        title = uiState.connection.headline,
                        value = if (uiState.connection.isConnected) "正常" else "等待中",
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        StatusPill(
                            label = "刷新配置",
                            background = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            foreground = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag(TestTags.ActionRefreshConfig),
                            onClick = onRefreshAll,
                        )
                        StatusPill(
                            label = "恢复推荐配置",
                            background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                            foreground = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag(TestTags.ActionApplyRecommended),
                            onClick = onApplyRecommendedFeatures,
                        )
                    }
                }
            }
        }
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                uiState.overviewStats.forEach { stat ->
                    MetricTile(stat = stat)
                }
            }
        }
        item {
            PageCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(
                        title = "核心保护",
                        subtitle = "影响推送送达的关键链路",
                    )
                    ProtectionLine(
                        title = "广播解限",
                        enabled = uiState.features.isFeatureEnabled(FeatureKey.HyperOsBroadcastShield),
                    )
                    ProtectionLine(
                        title = "已停止应用唤醒",
                        enabled = uiState.features.isFeatureEnabled(FeatureKey.WakeStoppedApps),
                    )
                    ProtectionLine(
                        title = "PowerKeeper 旁路",
                        enabled = uiState.features.isFeatureEnabled(FeatureKey.PowerKeeperBypass),
                    )
                    ProtectionLine(
                        title = "GMS 重连调优",
                        enabled = uiState.features.isFeatureEnabled(FeatureKey.GmsReconnectTuning),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProtectionLine(
    title: String,
    enabled: Boolean,
) {
    InfoLine(
        title = title,
        value = if (enabled) "已开启" else "已关闭",
    )
}

private fun List<FeatureCardModel>.isFeatureEnabled(featureKey: FeatureKey): Boolean =
    any { it.key == featureKey && it.enabled }
