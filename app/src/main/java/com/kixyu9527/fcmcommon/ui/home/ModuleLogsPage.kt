package com.kixyu9527.fcmcommon.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kixyu9527.fcmcommon.ui.TestTags

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
            PageCard(modifier = Modifier.testTag(TestTags.ModuleLogsCard)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        title = "事件列表",
                        subtitle = "这里保留最近的模块连接、断连和系统事件。",
                    )
                    if (uiState.recentConnectionEvents.isEmpty()) {
                        InfoLine(
                            title = "暂时没有日志",
                            value = "等待事件",
                            secondary = "模块一旦发生连接、断连或开关机事件，就会在这里显示。",
                        )
                    } else {
                        uiState.recentConnectionEvents.forEach { event ->
                            CuteLogCard(
                                title = event.title,
                                timestamp = event.timestamp,
                                detail = event.detail,
                            )
                        }
                    }
                }
            }
        }
    }
}
