@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.kixyu9527.fcmcommon.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.kixyu9527.fcmcommon.ui.TestTags

@Composable
fun AppsPage(
    listState: LazyListState,
    uiState: HomeUiState,
    onSearchQueryChanged: (String) -> Unit,
    onOnlyPushAppsChanged: (Boolean) -> Unit,
    onAppAllowedChanged: (String, Boolean) -> Unit,
    onOpenAppDetails: (String) -> Unit,
    onAllowPushCandidates: () -> Unit,
    onClearAllowList: () -> Unit,
    onRefreshApps: () -> Unit,
) {
    PageList(
        state = listState,
        modifier = Modifier
            .testTag(TestTags.AppsList)
            .testTag(TestTags.PageApps),
    ) {
        item {
            AppsToolbarCard(
                uiState = uiState,
                onSearchQueryChanged = onSearchQueryChanged,
                onOnlyPushAppsChanged = onOnlyPushAppsChanged,
                onAllowPushCandidates = onAllowPushCandidates,
                onClearAllowList = onClearAllowList,
                onRefreshApps = onRefreshApps,
            )
        }

        if (uiState.appRows.isEmpty()) {
            item {
                EmptyStateCard(
                    title = when {
                        uiState.appsLoading -> "正在建立应用缓存"
                        !uiState.appsScanned -> "等待首次扫描"
                        else -> "没有匹配结果"
                    },
                    body = when {
                        uiState.appsLoading -> "正在识别可能使用 FCM 的应用，并保存本次扫描结果。"
                        !uiState.appsScanned -> "首次安装会自动扫描；后续只有点击“重新扫描”时才会更新应用列表。"
                        else -> "当前展示的是最近一次扫描结果，修改搜索条件或重新扫描后再试。"
                    },
                )
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
}

@Composable
private fun AppsToolbarCard(
    uiState: HomeUiState,
    onSearchQueryChanged: (String) -> Unit,
    onOnlyPushAppsChanged: (Boolean) -> Unit,
    onAllowPushCandidates: () -> Unit,
    onClearAllowList: () -> Unit,
    onRefreshApps: () -> Unit,
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

    PageCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(
                title = "托管应用",
                subtitle = subtitle,
            )
            OutlinedTextField(
                value = uiState.appSearchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.AppsSearchField),
                singleLine = true,
                shape = RoundedCornerShape(22.dp),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                    )
                },
                label = { Text("搜索应用") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
            )
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 0.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "仅显示推送候选",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "优先显示检测到 FCM 组件的应用",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = uiState.onlyShowPushApps,
                        onCheckedChange = onOnlyPushAppsChanged,
                        modifier = Modifier.testTag(TestTags.AppsOnlyPushToggle),
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatusPill(
                    label = "重新扫描",
                    background = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                    foreground = MaterialTheme.colorScheme.secondary,
                    onClick = onRefreshApps,
                )
                StatusPill(
                    label = "纳入候选",
                    background = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    foreground = MaterialTheme.colorScheme.primary,
                    onClick = onAllowPushCandidates,
                )
                StatusPill(
                    label = "清空托管列表",
                    background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                    foreground = MaterialTheme.colorScheme.onSurface,
                    onClick = onClearAllowList,
                )
            }
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
    val interactionSource = remember(app.packageName) { MutableInteractionSource() }
    val metadata = buildList {
        if (app.hasPushSupport) add("推送候选")
        if (app.isSystemApp) add("系统应用")
        if (!app.isEnabled) add("已停用")
        if (app.isAllowed) add("已托管")
    }.joinToString(" · ")

    PageCard(
        modifier = Modifier.testTag(TestTags.appRow(app.packageName)),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 0.dp,
                ) {
                    if (iconBitmap != null) {
                        Image(
                            bitmap = iconBitmap,
                            contentDescription = null,
                            modifier = Modifier.padding(6.dp),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Apps,
                            contentDescription = null,
                            modifier = Modifier.padding(9.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (showPackageName) {
                        Text(
                            text = app.packageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (metadata.isNotBlank()) {
                        Text(
                            text = metadata,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Switch(
                checked = app.isAllowed,
                onCheckedChange = onAllowedChanged,
            )
        }
    }
}
