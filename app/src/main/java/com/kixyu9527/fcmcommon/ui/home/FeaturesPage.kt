package com.kixyu9527.fcmcommon.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.kixyu9527.fcmcommon.data.FeatureKey
import com.kixyu9527.fcmcommon.ui.TestTags

@Composable
fun FeatureSettingsPage(
    listState: LazyListState,
    uiState: HomeUiState,
    onFeatureToggle: (FeatureKey, Boolean) -> Unit,
    onApplyRecommendedFeatures: () -> Unit,
) {
    val groupedFeatures = remember(uiState.features) {
        uiState.features.groupBy { it.key.scope }
    }

    PageList(
        state = listState,
        modifier = Modifier.testTag(TestTags.PageFeatureSettings),
        bottomPadding = 28.dp,
    ) {
        item {
            CompactRecommendedCard(
                enabledFeatureCount = uiState.enabledFeatureCount,
                totalFeatureCount = uiState.features.size,
                onApplyRecommendedFeatures = onApplyRecommendedFeatures,
            )
        }

        groupedFeatures.forEach { (scope, itemsForScope) ->
            item {
                SectionHeader(title = scope)
            }
            item {
                PageCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

@Composable
private fun CompactRecommendedCard(
    enabledFeatureCount: Int,
    totalFeatureCount: Int,
    onApplyRecommendedFeatures: () -> Unit,
) {
    PageCard {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "默认推荐",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "已启用 $enabledFeatureCount / $totalFeatureCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusPill(
                label = "一键恢复",
                background = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                foreground = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag(TestTags.ActionApplyRecommended),
                onClick = onApplyRecommendedFeatures,
            )
        }
    }
}
