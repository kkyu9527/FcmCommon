package com.kixyu9527.fcmcommon.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kixyu9527.fcmcommon.data.FeatureKey
import com.kixyu9527.fcmcommon.ui.TestTags
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.SwitchPreference

@Composable
fun FeatureSettingsPage(
    listState: LazyListState,
    uiState: HomeUiState,
    onFeatureToggle: (FeatureKey, Boolean) -> Unit,
) {
    val groupedFeatures = remember(uiState.features) {
        uiState.features.groupBy { it.key.scope }
    }

    PageList(
        state = listState,
        modifier = Modifier.testTag(TestTags.PageFeatureSettings),
        bottomPadding = 28.dp,
    ) {
        groupedFeatures.forEach { (scope, itemsForScope) ->
            item {
                Column {
                    SmallTitle(text = scope)
                    Card {
                        itemsForScope.forEachIndexed { index, feature ->
                            SwitchPreference(
                                title = feature.key.title,
                                summary = feature.key.summary,
                                checked = feature.enabled,
                                onCheckedChange = { enabled -> onFeatureToggle(feature.key, enabled) },
                                modifier = Modifier.testTag(TestTags.featureToggle(feature.key)),
                            )
                            if (index != itemsForScope.lastIndex) {
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}
