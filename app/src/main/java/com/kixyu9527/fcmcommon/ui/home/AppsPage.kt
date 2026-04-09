@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.kixyu9527.fcmcommon.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
    uiState: HomeUiState,
    onSearchQueryChanged: (String) -> Unit,
    onOnlyPushAppsChanged: (Boolean) -> Unit,
    onAppAllowedChanged: (String, Boolean) -> Unit,
    onAllowPushCandidates: () -> Unit,
    onClearAllowList: () -> Unit,
    onRefreshApps: () -> Unit,
) {
    PageList(
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
                    title = if (uiState.appsLoading) "正在扫描应用" else "没有匹配结果",
                    body = if (uiState.appsLoading) {
                        "正在识别可能使用 FCM 的应用。"
                    } else {
                        "修改搜索条件或重新扫描后再试。"
                    },
                )
            }
        } else {
            items(uiState.appRows, key = { it.packageName }) { app ->
                AppRow(
                    app = app,
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
    PageCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeader(
                title = "应用白名单",
                subtitle = "白名单 ${uiState.trackedAppsCount} · 候选 ${uiState.pushCandidateCount}",
            )
            OutlinedTextField(
                value = uiState.appSearchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.AppsSearchField),
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
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
                ),
            )
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
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
                    label = "加入候选",
                    background = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    foreground = MaterialTheme.colorScheme.primary,
                    onClick = onAllowPushCandidates,
                )
                StatusPill(
                    label = "清空",
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
    onAllowedChanged: (Boolean) -> Unit,
) {
    val iconBitmap = remember(app.packageName, app.icon) {
        app.icon?.toBitmap(width = 96, height = 96)?.asImageBitmap()
    }
    val metadata = buildList {
        if (app.hasPushSupport) add("推送候选")
        if (app.isSystemApp) add("系统应用")
        if (app.isAllowed) add("已加入")
    }.joinToString(" · ")

    PageCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
                    tonalElevation = 0.dp,
                ) {
                    if (iconBitmap != null) {
                        Image(
                            bitmap = iconBitmap,
                            contentDescription = null,
                            modifier = Modifier.padding(7.dp),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Apps,
                            contentDescription = null,
                            modifier = Modifier.padding(10.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
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
