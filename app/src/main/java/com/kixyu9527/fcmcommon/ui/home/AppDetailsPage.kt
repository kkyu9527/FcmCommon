@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.kixyu9527.fcmcommon.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.kixyu9527.fcmcommon.ui.TestTags

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

    fun copyValue(label: String, value: String) {
        clipboardManager.setPrimaryClip(ClipData.newPlainText(label, value))
        Toast.makeText(
            context,
            "已复制$label",
            Toast.LENGTH_SHORT,
        ).show()
    }

    PageList(
        state = listState,
        modifier = Modifier.testTag(TestTags.PageAppDetails),
        bottomPadding = 28.dp,
    ) {
        if (app == null) {
            item {
                PageCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionHeader(
                            title = "应用信息暂未就绪",
                            subtitle = "可能是应用列表刚刷新，或者当前详情尚未重新载入。",
                        )
                        if (!requestedPackageName.isNullOrBlank()) {
                            CopyableInfoLine(
                                title = "请求的包名",
                                value = requestedPackageName,
                                onCopy = { copyValue("包名", requestedPackageName) },
                            )
                            SectionDivider()
                        }
                        InfoLine(
                            title = "当前状态",
                            value = "等待重新载入",
                            secondary = "返回应用页后重新扫描一次，再进入详情会更稳定。",
                        )
                    }
                }
            }
        } else {
            item {
                AppSummaryCard(app = app)
            }
            item {
                PageCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionHeader(
                            title = "状态与开关",
                            subtitle = "这里保留运行状态，详细字段点按即可复制。",
                        )
                        PreferenceToggleLine(
                            title = "加入白名单",
                            summary = "加入后会作为重点保活目标参与相关修复链路",
                            checked = app.isAllowed,
                            onCheckedChange = { onAppAllowedChanged(app.packageName, it) },
                        )
                        SectionDivider()
                        InfoLine(
                            title = "推送候选",
                            value = if (app.hasPushSupport) "是" else "否",
                            secondary = if (app.hasPushSupport) {
                                "检测到了 FCM 相关组件"
                            } else {
                                "暂未检测到 FCM 相关组件"
                            },
                        )
                        SectionDivider()
                        InfoLine(
                            title = "启用状态",
                            value = if (app.isEnabled) "已启用" else "已停用",
                            secondary = if (app.isSystemApp) "系统应用" else "普通应用",
                        )
                    }
                }
            }
            item {
                PageCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionHeader(
                            title = "基础信息",
                            subtitle = "点按条目可复制对应内容。",
                        )
                        CopyableInfoLine(
                            title = "应用名",
                            value = app.label,
                            onCopy = { copyValue("应用名", app.label) },
                        )
                        SectionDivider()
                        CopyableInfoLine(
                            title = "包名",
                            value = app.packageName,
                            onCopy = { copyValue("包名", app.packageName) },
                        )
                        SectionDivider()
                        CopyableInfoLine(
                            title = "版本名称",
                            value = app.versionName,
                            onCopy = { copyValue("版本名称", app.versionName) },
                        )
                        SectionDivider()
                        CopyableInfoLine(
                            title = "版本号",
                            value = app.versionCode.toString(),
                            onCopy = { copyValue("版本号", app.versionCode.toString()) },
                        )
                    }
                }
            }
            item {
                PageCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionHeader(title = "SDK 与安装来源")
                        CopyableInfoLine(
                            title = "targetSdk",
                            value = app.targetSdkVersion.toString(),
                            onCopy = { copyValue("targetSdk", app.targetSdkVersion.toString()) },
                        )
                        SectionDivider()
                        CopyableInfoLine(
                            title = "minSdk",
                            value = app.minSdkVersion?.toString() ?: "-",
                            onCopy = { copyValue("minSdk", app.minSdkVersion?.toString() ?: "-") },
                        )
                        SectionDivider()
                        CopyableInfoLine(
                            title = "安装来源",
                            value = app.installerPackageName,
                            onCopy = { copyValue("安装来源", app.installerPackageName) },
                        )
                        SectionDivider()
                        CopyableInfoLine(
                            title = "进程名",
                            value = app.processName ?: "-",
                            onCopy = { copyValue("进程名", app.processName ?: "-") },
                        )
                        SectionDivider()
                        CopyableInfoLine(
                            title = "UID",
                            value = app.uid.toString(),
                            onCopy = { copyValue("UID", app.uid.toString()) },
                        )
                    }
                }
            }
            item {
                PageCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionHeader(title = "安装时间")
                        CopyableInfoLine(
                            title = "首次安装",
                            value = app.firstInstallLabel,
                            onCopy = { copyValue("首次安装时间", app.firstInstallLabel) },
                        )
                        SectionDivider()
                        CopyableInfoLine(
                            title = "最近更新",
                            value = app.lastUpdateLabel,
                            onCopy = { copyValue("最近更新时间", app.lastUpdateLabel) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppSummaryCard(
    app: AppDetailInfoModel,
) {
    val iconBitmap = remember(app.packageName, app.icon) {
        app.icon?.toBitmap(width = 128, height = 128)?.asImageBitmap()
    }

    PageCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 0.dp,
            ) {
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Apps,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatusPill(
                    label = if (app.isAllowed) "已加入白名单" else "未加入白名单",
                    background = if (app.isAllowed) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                    },
                    foreground = if (app.isAllowed) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                StatusPill(
                    label = if (app.hasPushSupport) "推送候选" else "未识别推送",
                    background = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                    foreground = MaterialTheme.colorScheme.secondary,
                )
                StatusPill(
                    label = if (app.isEnabled) "已启用" else "已停用",
                    background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                    foreground = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun CopyableInfoLine(
    title: String,
    value: String,
    onCopy: () -> Unit,
) {
    InfoLine(
        title = title,
        value = value,
        secondary = "点按复制",
        onClick = onCopy,
    )
}
