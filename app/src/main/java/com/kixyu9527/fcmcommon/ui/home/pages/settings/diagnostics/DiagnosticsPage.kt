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
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun DiagnosticsPage(
    listState: LazyListState,
    uiState: HomeUiState,
    onOpenModuleLogs: () -> Unit,
) {
    val enabledFeatures = uiState.features.filter { it.enabled }
    val moduleReady = uiState.connection.isConnected && uiState.connection.hasRemotePreferences

    PageList(
        state = listState,
        modifier = Modifier.testTag(TestTags.PageDiagnosticsDetail),
        bottomPadding = 28.dp,
    ) {
        item {
            Column {
                SmallTitle(text = "运行状态")
                Card(modifier = Modifier.testTag(TestTags.DiagnosticsCard)) {
                    BasicComponent(
                        title = if (moduleReady) "模块运行正常" else "模块尚未就绪",
                        summary = if (moduleReady) {
                            "服务连接和配置同步均正常"
                        } else {
                            "请检查模块开关与作用域配置"
                        },
                        startAction = {
                            Icon(
                                imageVector = if (moduleReady) MiuixIcons.Ok else MiuixIcons.Refresh,
                                contentDescription = null,
                                tint = if (moduleReady) {
                                    MiuixTheme.colorScheme.primary
                                } else {
                                    MiuixTheme.colorScheme.onSurfaceVariantSummary
                                },
                            )
                        },
                    )
                    HorizontalDivider()
                    BasicComponent(
                        title = "模块服务",
                        summary = "LibXposed 服务连接",
                        endActions = {
                            Text(if (uiState.connection.isConnected) "已连接" else "未连接")
                        },
                    )
                    HorizontalDivider()
                    BasicComponent(
                        title = "配置同步",
                        summary = "远程配置桥接",
                        endActions = {
                            Text(if (uiState.connection.hasRemotePreferences) "已就绪" else "未就绪")
                        },
                    )
                    HorizontalDivider()
                    ArrowPreference(
                        title = "模块日志",
                        summary = if (uiState.recentConnectionEvents.isEmpty()) {
                            "暂无连接事件"
                        } else {
                            uiState.connectionSummary
                        },
                        onClick = onOpenModuleLogs,
                        modifier = Modifier.testTag(TestTags.DiagnosticsLogsEntry),
                    )
                }
            }
        }
        item {
            Column {
                SmallTitle(text = "作用域")
                Card(modifier = Modifier.testTag(TestTags.ScopeRow)) {
                    uiState.scopeStatuses.forEachIndexed { index, scope ->
                        BasicComponent(
                            title = scope.label,
                            summary = scope.packageName,
                            endActions = {
                                Text(
                                    text = if (scope.active) "已配置" else "未配置",
                                    color = if (scope.active) {
                                        MiuixTheme.colorScheme.primary
                                    } else {
                                        MiuixTheme.colorScheme.onSurfaceVariantSummary
                                    },
                                )
                            },
                        )
                        if (index != uiState.scopeStatuses.lastIndex) HorizontalDivider()
                    }
                }
            }
        }
        item {
            Column {
                SmallTitle(text = "已启用修复 · ${enabledFeatures.size}")
                Card {
                    if (enabledFeatures.isEmpty()) {
                        BasicComponent(
                            title = "暂无启用项",
                            summary = "可在功能配置中开启需要的修复项",
                        )
                    } else {
                        enabledFeatures.forEachIndexed { index, feature ->
                            BasicComponent(
                                title = feature.key.title,
                                summary = "${feature.key.scope} · ${feature.key.summary}",
                            )
                            if (index != enabledFeatures.lastIndex) HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
