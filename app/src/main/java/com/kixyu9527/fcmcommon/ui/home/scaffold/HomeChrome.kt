package com.kixyu9527.fcmcommon.ui.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.kixyu9527.fcmcommon.ui.TestTags
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back

@Composable
fun PageHeader(
    title: String,
    canNavigateBack: Boolean,
    onNavigateBack: () -> Unit,
    scrollBehavior: ScrollBehavior,
    testTagsEnabled: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: (@Composable () -> Unit)? = null,
) {
    TopAppBar(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTagsEnabled) Modifier.testTag(TestTags.Header) else Modifier),
        title = title,
        actions = actions,
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(
                    modifier = if (testTagsEnabled) {
                        Modifier.testTag(TestTags.TopBarBack)
                    } else {
                        Modifier
                    },
                    onClick = onNavigateBack,
                ) {
                    Icon(
                        imageVector = MiuixIcons.Back,
                        contentDescription = "返回",
                    )
                }
            }
        },
        bottomContent = {
            bottomContent?.invoke()
        },
    )
}
