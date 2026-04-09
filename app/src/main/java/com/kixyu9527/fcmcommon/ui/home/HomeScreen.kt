@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.kixyu9527.fcmcommon.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kixyu9527.fcmcommon.data.FeatureKey
import com.kixyu9527.fcmcommon.ui.TestTags
import com.kixyu9527.fcmcommon.ui.theme.FcmCommonTheme

@Composable
fun HomeRoute(viewModel: HomeViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onFeatureToggle = viewModel::setFeatureEnabled,
        onRefresh = viewModel::refreshConfig,
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onFeatureToggle: (FeatureKey, Boolean) -> Unit,
    onRefresh: () -> Unit,
) {
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.background,
        ),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "FcmCommon",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
            )
        },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(innerPadding)
                .testTag(TestTags.HomeList),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                HeroCard(uiState = uiState)
            }
            item {
                RuntimeCard(uiState = uiState)
            }
            item {
                SectionHeader(
                    title = "Feature stack",
                    subtitle = "These switches already persist locally and are wired for E2E verification.",
                )
            }
            items(uiState.features, key = { it.key.name }) { feature ->
                FeatureCard(
                    feature = feature,
                    onFeatureToggle = onFeatureToggle,
                )
            }
            item {
                SectionHeader(
                    title = "Migration board",
                    subtitle = "First-pass mapping from the two legacy modules into one Kotlin-first codebase.",
                )
            }
            items(uiState.migrationTracks, key = { it.title }) { track ->
                MigrationTrackCard(track = track)
            }
            item {
                DiagnosticsCard(
                    uiState = uiState,
                    onRefresh = onRefresh,
                )
            }
        }
    }
}

@Composable
private fun HeroCard(uiState: HomeUiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary,
                    ),
                ),
            )
            .padding(24.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "HyperOS FCM workbench",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Text(
                text = "A Compose-first control surface for the unified HyperOS push repair module.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
            )
            StatusPill(
                label = if (uiState.connection.isConnected) {
                    "Bridge live"
                } else {
                    "Foundation mode"
                },
                background = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
                foreground = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun RuntimeCard(uiState: HomeUiState) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Runtime",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    AnimatedContent(
                        targetState = uiState.connection.headline,
                        label = "connection_headline",
                    ) { headline ->
                        Text(
                            text = headline,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.testTag(TestTags.ConnectionHeadline),
                        )
                    }
                }
                StatusPill(
                    label = if (uiState.connection.hasRemotePreferences) {
                        "Remote prefs"
                    } else {
                        "Local draft"
                    },
                    background = MaterialTheme.colorScheme.tertiary,
                    foreground = MaterialTheme.colorScheme.onTertiary,
                )
            }
            Text(
                text = uiState.connection.detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                modifier = Modifier.testTag(TestTags.ScopeRow),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                uiState.connection.activeScopes.forEach { scope ->
                    ScopePill(scope = scope)
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(
    feature: FeatureCardModel,
    onFeatureToggle: (FeatureKey, Boolean) -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = if (feature.enabled) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "feature_card_color",
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TestTags.featureCard(feature.key))
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = feature.key.title,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = feature.key.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = feature.enabled,
                    onCheckedChange = { enabled ->
                        onFeatureToggle(feature.key, enabled)
                    },
                    modifier = Modifier.testTag(TestTags.featureToggle(feature.key)),
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatusPill(
                    label = feature.key.scope,
                    background = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    foreground = MaterialTheme.colorScheme.primary,
                )
                StatusPill(
                    label = feature.key.source,
                    background = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                    foreground = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun MigrationTrackCard(track: MigrationTrackModel) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = track.detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StatusPill(
                label = track.source,
                background = MaterialTheme.colorScheme.tertiary,
                foreground = MaterialTheme.colorScheme.onTertiary,
            )
        }
    }
}

@Composable
private fun DiagnosticsCard(
    uiState: HomeUiState,
    onRefresh: () -> Unit,
) {
    Card(
        modifier = Modifier.testTag(TestTags.DiagnosticsCard),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Diagnostics",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Enabled features: ${uiState.enabledFeatureCount}/${uiState.features.size}\nTracked apps: ${uiState.trackedAppsCount}\nE2E status: baseline flow ready",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Refresh note",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "The refresh action currently reloads local config. Remote preference sync lands in the next milestone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            StatusPill(
                label = "Refresh local draft",
                background = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                foreground = MaterialTheme.colorScheme.primary,
                onClick = onRefresh,
            )
        }
    }
}

@Composable
private fun ScopePill(scope: String) {
    StatusPill(
        label = scope,
        background = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        foreground = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun StatusPill(
    label: String,
    background: Color,
    foreground: Color,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        color = background,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
            color = foreground,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    FcmCommonTheme {
        HomeScreen(
            uiState = HomeUiState(
                features = FeatureKey.entries.mapIndexed { index, featureKey ->
                    FeatureCardModel(
                        key = featureKey,
                        enabled = index != FeatureKey.KeepNotifications.ordinal,
                    )
                },
                migrationTracks = listOf(
                    MigrationTrackModel(
                        title = "System broadcast lane",
                        detail = "Merge stopped-app delivery with HyperOS broadcast exemption.",
                        source = "Hooker + BroadcastFix",
                    ),
                ),
            ),
            onFeatureToggle = { _, _ -> },
            onRefresh = {},
        )
    }
}
