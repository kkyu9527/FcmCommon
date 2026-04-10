@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.kixyu9527.fcmcommon.ui.home

import android.content.ComponentName
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
    val moduleReady = uiState.connection.isConnected && uiState.connection.hasRemotePreferences
    val working = moduleReady && uiState.fcmDiagnosticsConnected
    val statusTitle = when {
        working -> "工作中"
        moduleReady -> "模块已生效"
        else -> "等待生效"
    }

    PageList(
        state = listState,
        modifier = Modifier.testTag(TestTags.PageOverview),
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
                color = if (working) {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.88f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                },
                contentColor = if (working) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                tonalElevation = 0.dp,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = if (working || moduleReady) {
                                    Icons.Outlined.CheckCircle
                                } else {
                                    Icons.Outlined.SyncProblem
                                },
                                contentDescription = null,
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = statusTitle,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        StatusPill(
                            label = if (uiState.connection.hasRemotePreferences) "已同步" else "待同步",
                            background = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            foreground = MaterialTheme.colorScheme.primary,
                        )
                    }
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
                            background = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
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
                    )
                    uiState.overviewStats.forEachIndexed { index, stat ->
                        InfoLine(
                            title = stat.label,
                            value = stat.value,
                            secondary = stat.hint,
                        )
                        if (index != uiState.overviewStats.lastIndex) {
                            SectionDivider()
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
    }
}
