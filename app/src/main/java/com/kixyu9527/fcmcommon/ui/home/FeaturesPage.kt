package com.kixyu9527.fcmcommon.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.kixyu9527.fcmcommon.data.FeatureKey
import com.kixyu9527.fcmcommon.ui.TestTags

@Composable
fun FeaturesPage(
    uiState: HomeUiState,
    onFeatureToggle: (FeatureKey, Boolean) -> Unit,
    onApplyRecommendedFeatures: () -> Unit,
) {
    val groupedFeatures = remember(uiState.features) {
        uiState.features.groupBy { it.key.scope }
    }

    PageList(modifier = Modifier.testTag(TestTags.PageFeatures)) {
        item {
            PageCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SectionHeader(
                        title = "推荐配置",
                        subtitle = "已启用 ${uiState.enabledFeatureCount}/${uiState.features.size}",
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StatusPill(
                            label = "恢复推荐配置",
                            background = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            foreground = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag(TestTags.ActionApplyRecommended),
                            onClick = onApplyRecommendedFeatures,
                        )
                    }
                }
            }
        }

        groupedFeatures.forEach { (scope, itemsForScope) ->
            item {
                SectionHeader(title = scope)
            }
            item {
                PageCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsForScope.forEachIndexed { index, feature ->
                            FeatureLine(
                                feature = feature,
                                onFeatureToggle = onFeatureToggle,
                            )
                            if (index != itemsForScope.lastIndex) {
                                SectionDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}
