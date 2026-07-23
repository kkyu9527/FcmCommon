package com.kixyu9527.fcmcommon.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.kixyu9527.fcmcommon.ui.TestTags
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.preference.SwitchPreference

@Composable
fun AppsPage(
    listState: LazyListState,
    uiState: HomeUiState,
    topPadding: androidx.compose.ui.unit.Dp = 0.dp,
    onAppAllowedChanged: (String, Boolean) -> Unit,
    onOpenAppDetails: (String) -> Unit,
    onOnlyPushAppsChanged: (Boolean) -> Unit,
) {
    PageList(
        state = listState,
        modifier = Modifier
            .testTag(TestTags.AppsList)
            .testTag(TestTags.PageApps),
        topPadding = topPadding,
    ) {
        item {
            AppsScopeCard(
                uiState = uiState,
                onOnlyPushAppsChanged = onOnlyPushAppsChanged,
            )
        }
        appRowsContent(
            uiState = uiState,
            onOpenAppDetails = onOpenAppDetails,
            onAppAllowedChanged = onAppAllowedChanged,
        )
    }
}

private fun LazyListScope.appRowsContent(
    uiState: HomeUiState,
    onOpenAppDetails: (String) -> Unit,
    onAppAllowedChanged: (String, Boolean) -> Unit,
) {
    if (uiState.appRows.isEmpty()) {
        item {
            Card {
                BasicComponent(
                    title = when {
                        uiState.appsLoading -> "正在建立应用缓存"
                        !uiState.appsScanned -> "等待首次扫描"
                        else -> "没有匹配结果"
                    },
                    summary = when {
                        uiState.appsLoading -> "正在识别可能使用 FCM 的应用，并保存本次扫描结果。"
                        !uiState.appsScanned -> "首次安装会自动扫描；后续只有点击“重新扫描”时才会更新应用列表。"
                        else -> "当前展示的是最近一次扫描结果，修改搜索条件或重新扫描后再试。"
                    },
                )
            }
        }
    } else {
        items(uiState.appRows, key = { it.packageName }) { app ->
            AppRow(
                app = app,
                showPackageName = uiState.showPackageNameInList,
                onClick = { onOpenAppDetails(app.packageName) },
                onAllowedChanged = { allowed ->
                    onAppAllowedChanged(app.packageName, allowed)
                },
            )
        }
    }
}

@Composable
private fun AppsScopeCard(
    uiState: HomeUiState,
    onOnlyPushAppsChanged: (Boolean) -> Unit,
) {
    val subtitle = buildString {
        append("托管 ${uiState.trackedAppsCount}")
        append(" · ")
        if (uiState.appsScanned || uiState.appsLoading) {
            append("候选 ${uiState.pushCandidateCount}")
        } else {
            append("待首扫")
        }
    }
    Column {
        SmallTitle(text = "显示范围")
        Card {
            BasicComponent(
                title = "应用概览",
                summary = subtitle,
            )
            HorizontalDivider()
            SwitchPreference(
                title = "显示所有应用",
                summary = if (uiState.onlyShowPushApps) {
                    "当前仅显示推送候选与已托管应用"
                } else {
                    "包含符合系统与停用筛选规则的全部应用"
                },
                checked = !uiState.onlyShowPushApps,
                modifier = Modifier.testTag(TestTags.AppsShowAllSwitch),
                onCheckedChange = { showAll -> onOnlyPushAppsChanged(!showAll) },
            )
        }
    }
}

@Composable
private fun AppRow(
    app: AppRowModel,
    showPackageName: Boolean,
    onClick: () -> Unit,
    onAllowedChanged: (Boolean) -> Unit,
) {
    val iconBitmap = remember(app.packageName, app.icon) {
        app.icon?.toBitmap(width = 96, height = 96)?.asImageBitmap()
    }
    val metadata = buildList {
        if (app.hasPushSupport) add("推送候选")
        if (app.isSystemApp) add("系统应用")
        if (!app.isEnabled) add("已停用")
        if (app.isAllowed) add("已托管")
    }.joinToString(" · ")

    Card(
        modifier = Modifier.testTag(TestTags.appRow(app.packageName)),
    ) {
        BasicComponent(
            title = app.label,
            summary = listOfNotNull(
                app.packageName.takeIf { showPackageName },
                metadata.takeIf(String::isNotBlank),
            ).joinToString("\n"),
            startAction = {
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                    )
                } else {
                    Icon(
                        imageVector = MiuixIcons.GridView,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            },
            endActions = {
                Switch(
                    checked = app.isAllowed,
                    onCheckedChange = onAllowedChanged,
                )
            },
            onClick = onClick,
        )
    }
}
