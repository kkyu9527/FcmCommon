package com.kixyu9527.fcmcommon.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ScrollBehavior

internal val LocalHomeBottomPadding = staticCompositionLocalOf { 0.dp }
internal val LocalHomeScrollBehavior = staticCompositionLocalOf<ScrollBehavior?> { null }

@Composable
fun PageList(
    state: LazyListState,
    modifier: Modifier = Modifier,
    topPadding: Dp = 0.dp,
    bottomPadding: Dp = 0.dp,
    content: LazyListScope.() -> Unit,
) {
    val resolvedBottomPadding = bottomPadding + LocalHomeBottomPadding.current
    val scrollBehavior = LocalHomeScrollBehavior.current
    LazyColumn(
        state = state,
        modifier = modifier
            .fillMaxHeight()
            .then(
                if (scrollBehavior != null) {
                    Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(
            top = 12.dp + topPadding,
            bottom = 12.dp + resolvedBottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
internal fun ProvideHomeScrollBehavior(
    content: @Composable (ScrollBehavior) -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    CompositionLocalProvider(LocalHomeScrollBehavior provides scrollBehavior) {
        content(scrollBehavior)
    }
}
