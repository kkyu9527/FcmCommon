package com.kixyu9527.fcmcommon.ui.home

import android.content.ComponentName
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
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
fun OverviewPage(
    listState: LazyListState,
    uiState: HomeUiState,
    topPadding: Dp = 0.dp,
) {
    val context = LocalContext.current
    val moduleReady = uiState.connection.isConnected && uiState.connection.hasRemotePreferences

    PageList(
        state = listState,
        modifier = Modifier.testTag(TestTags.PageOverview),
        topPadding = topPadding,
    ) {
        item {
            Card {
                BasicComponent(
                    title = if (moduleReady) "模块已生效" else "模块尚未就绪",
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
            }
        }
        item {
            Column {
                SmallTitle(text = "诊断")
                Card {
                    ArrowPreference(
                        title = "Google Play 服务诊断",
                        summary = "打开系统提供的 GCM 诊断页面",
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
                            runCatching { context.startActivity(intent) }
                                .onFailure {
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
        item {
            Column {
                SmallTitle(text = "数据概览")
                Card {
                    uiState.overviewStats.forEachIndexed { index, stat ->
                        BasicComponent(
                            title = stat.label,
                            summary = stat.hint,
                            endActions = { Text(stat.value) },
                        )
                        if (index != uiState.overviewStats.lastIndex) HorizontalDivider()
                    }
                }
            }
        }
        item {
            Column {
                SmallTitle(text = "作用域")
                Card {
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
    }
}
