package com.kixyu9527.fcmcommon.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.kixyu9527.fcmcommon.ui.TestTags
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AppDetailsPage(
    listState: LazyListState,
    uiState: HomeUiState,
    onAppAllowedChanged: (String, Boolean) -> Unit,
) {
    val app = uiState.selectedAppDetails
    val requestedPackageName = (uiState.secondaryPage as? SecondaryPage.AppDetails)?.packageName
    val context = LocalContext.current
    val clipboardManager = remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    val copyValue: (String, String) -> Unit = { label, value ->
        clipboardManager.setPrimaryClip(ClipData.newPlainText(label, value))
        Toast.makeText(context, "已复制$label", Toast.LENGTH_SHORT).show()
    }

    PageList(
        state = listState,
        modifier = Modifier.testTag(TestTags.PageAppDetails),
        bottomPadding = 28.dp,
    ) {
        if (app == null) {
            item {
                Column {
                    SmallTitle(text = "状态")
                    Card {
                        BasicComponent(
                            title = "应用信息暂未就绪",
                            summary = "返回应用页重新扫描后再试",
                        )
                        requestedPackageName?.takeIf(String::isNotBlank)?.let { packageName ->
                            HorizontalDivider()
                            BasicComponent(
                                title = "请求的包名",
                                summary = packageName,
                                onClick = { copyValue("包名", packageName) },
                            )
                        }
                    }
                }
            }
        } else {
            item {
                val iconBitmap = remember(app.packageName, app.icon) {
                    app.icon?.toBitmap(width = 128, height = 128)?.asImageBitmap()
                }
                Card {
                    BasicComponent(
                        title = app.label,
                        summary = buildList {
                            add(app.packageName)
                            add(
                                listOf(
                                    if (app.isAllowed) "已纳入托管" else "未纳入托管",
                                    if (app.hasPushSupport) "推送候选" else "未识别推送",
                                    if (app.isEnabled) "已启用" else "已停用",
                                ).joinToString(" · "),
                            )
                        }.joinToString("\n"),
                        startAction = {
                            if (iconBitmap != null) {
                                Image(
                                    bitmap = iconBitmap,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                )
                            } else {
                                Icon(
                                    imageVector = MiuixIcons.GridView,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                            }
                        },
                    )
                }
            }
            item {
                Column {
                    SmallTitle(text = "状态与开关")
                    Card {
                        SwitchPreference(
                            title = "纳入托管",
                            summary = "托管后会参与 FCM 修复链路。",
                            checked = app.isAllowed,
                            onCheckedChange = { onAppAllowedChanged(app.packageName, it) },
                        )
                        HorizontalDivider()
                        BasicComponent(
                            title = "推送候选",
                            summary = if (app.hasPushSupport) {
                                "检测到了 FCM 相关组件"
                            } else {
                                "暂未检测到 FCM 相关组件"
                            },
                            endActions = { Text(if (app.hasPushSupport) "是" else "否") },
                        )
                        HorizontalDivider()
                        BasicComponent(
                            title = "启用状态",
                            summary = if (app.isSystemApp) "系统应用" else "普通应用",
                            endActions = { Text(if (app.isEnabled) "已启用" else "已停用") },
                        )
                    }
                }
            }
            item {
                val values = listOf(
                    "应用名" to app.label,
                    "包名" to app.packageName,
                    "版本名称" to app.versionName,
                    "版本号" to app.versionCode.toString(),
                )
                Column {
                    SmallTitle(text = "基础信息")
                    Card {
                        values.forEachIndexed { index, (label, value) ->
                            BasicComponent(
                                title = label,
                                summary = value,
                                onClick = { copyValue(label, value) },
                            )
                            if (index != values.lastIndex) HorizontalDivider()
                        }
                    }
                }
            }
            item {
                val values = listOf(
                    "targetSdk" to app.targetSdkVersion.toString(),
                    "minSdk" to (app.minSdkVersion?.toString() ?: "-"),
                    "安装来源" to app.installerPackageName,
                    "进程名" to (app.processName ?: "-"),
                    "UID" to app.uid.toString(),
                )
                Column {
                    SmallTitle(text = "SDK 与安装来源")
                    Card {
                        values.forEachIndexed { index, (label, value) ->
                            BasicComponent(
                                title = label,
                                summary = value,
                                onClick = { copyValue(label, value) },
                            )
                            if (index != values.lastIndex) HorizontalDivider()
                        }
                    }
                }
            }
            item {
                val values = listOf(
                    "首次安装" to app.firstInstallLabel,
                    "最近更新" to app.lastUpdateLabel,
                )
                Column {
                    SmallTitle(text = "安装时间")
                    Card {
                        values.forEachIndexed { index, (label, value) ->
                            BasicComponent(
                                title = label,
                                summary = value,
                                onClick = { copyValue(label, value) },
                            )
                            if (index != values.lastIndex) HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
