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
import top.yukonga.miuix.kmp.basic.SmallTitle

@Composable
fun ModuleLogsPage(
    listState: LazyListState,
    uiState: HomeUiState,
) {
    PageList(
        state = listState,
        modifier = Modifier.testTag(TestTags.PageModuleLogs),
        bottomPadding = 28.dp,
    ) {
        item {
            Column {
                SmallTitle(text = "最近事件")
                Card(modifier = Modifier.testTag(TestTags.ModuleLogsCard)) {
                    if (uiState.recentConnectionEvents.isEmpty()) {
                        BasicComponent(
                            title = "暂无日志",
                            summary = "连接、断连和系统事件会显示在这里",
                        )
                    } else {
                        uiState.recentConnectionEvents.forEachIndexed { index, event ->
                            BasicComponent(
                                title = event.title,
                                summary = "${event.timestamp}\n${event.detail}",
                            )
                            if (index != uiState.recentConnectionEvents.lastIndex) {
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}
