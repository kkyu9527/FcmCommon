@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.kixyu9527.fcmcommon.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kixyu9527.fcmcommon.ui.TestTags

private val MiuixBottomBarEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

@Composable
fun PageHeader(
    title: String,
    canNavigateBack: Boolean,
    onNavigateBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background.copy(alpha = 0.98f),
                        MaterialTheme.colorScheme.background.copy(alpha = 0.90f),
                        Color.Transparent,
                    ),
                ),
            )
            .statusBarsPadding()
            .testTag(TestTags.Header),
    ) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = {
                if (canNavigateBack) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag(TestTags.TopBarBack),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            ),
        )
    }
}

@Composable
fun BoxScope.AnimatedFloatingCapsuleBottomBar(
    visible: Boolean,
    selectedPage: AppPage,
    onPageSelected: (AppPage) -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.align(Alignment.BottomCenter),
        enter = fadeIn(
            animationSpec = tween(durationMillis = 220, delayMillis = 40),
        ) + slideInVertically(
            animationSpec = tween(
                durationMillis = 320,
                easing = MiuixBottomBarEasing,
            ),
            initialOffsetY = { fullHeight -> fullHeight / 2 },
        ) + scaleIn(
            animationSpec = tween(
                durationMillis = 320,
                easing = MiuixBottomBarEasing,
            ),
            initialScale = 0.92f,
            transformOrigin = TransformOrigin(0.5f, 1f),
        ),
        exit = fadeOut(
            animationSpec = tween(durationMillis = 150),
        ) + slideOutVertically(
            animationSpec = tween(
                durationMillis = 220,
                easing = MiuixBottomBarEasing,
            ),
            targetOffsetY = { fullHeight -> fullHeight / 3 },
        ) + scaleOut(
            animationSpec = tween(
                durationMillis = 220,
                easing = MiuixBottomBarEasing,
            ),
            targetScale = 0.96f,
            transformOrigin = TransformOrigin(0.5f, 1f),
        ),
        label = "floating_bottom_bar_visibility",
    ) {
        FloatingCapsuleBottomBar(
            selectedPage = selectedPage,
            onPageSelected = onPageSelected,
        )
    }
}

@Composable
private fun FloatingCapsuleBottomBar(
    selectedPage: AppPage,
    onPageSelected: (AppPage) -> Unit,
) {
    val items = bottomBarItems()
    val selectedIndex = items.indexOfFirst { it.page == selectedPage }.coerceAtLeast(0)
    val shape = RoundedCornerShape(32.dp)
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp + bottomInset)
            .testTag(TestTags.BottomBar),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = shape,
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    spotColor = Color.Black.copy(alpha = 0.16f),
                ),
            shape = shape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(
                width = 0.8.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
            ),
            tonalElevation = 0.dp,
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                            ),
                        ),
                    )
                    .padding(4.dp),
            ) {
                val slotWidth = maxWidth / items.size
                val indicatorOffset by animateDpAsState(
                    targetValue = slotWidth * selectedIndex,
                    animationSpec = tween(
                        durationMillis = 360,
                        easing = MiuixBottomBarEasing,
                    ),
                    label = "miuix_indicator_offset",
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 62.dp),
                ) {
                    FloatingIndicator(
                        slotWidth = slotWidth,
                        indicatorOffset = indicatorOffset,
                    )

                    Row(modifier = Modifier.fillMaxWidth()) {
                        items.forEach { item ->
                            CapsuleNavItem(
                                item = item,
                                selected = selectedPage == item.page,
                                onClick = { onPageSelected(item.page) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingIndicator(
    slotWidth: Dp,
    indicatorOffset: Dp,
) {
    val indicatorShape = RoundedCornerShape(28.dp)

    Box(
        modifier = Modifier
            .offset(x = indicatorOffset)
            .width(slotWidth)
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = indicatorShape,
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    spotColor = Color.Black.copy(alpha = 0.12f),
                )
                .clip(indicatorShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        ),
                    ),
                )
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.56f),
                    indicatorShape,
                ),
        )
    }
}

@Composable
private fun RowScope.CapsuleNavItem(
    item: BottomBarItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val selectedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = MiuixBottomBarEasing),
        label = "nav_item_progress",
    )
    val containerColor by animateColorAsState(
        targetValue = Color.Transparent,
        animationSpec = tween(durationMillis = 320),
        label = "nav_item_color",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.86f)
        },
        animationSpec = tween(durationMillis = 320, easing = MiuixBottomBarEasing),
        label = "nav_content_color",
    )

    Surface(
        onClick = onClick,
        color = containerColor,
        contentColor = contentColor,
        shape = CircleShape,
        tonalElevation = 0.dp,
        modifier = Modifier
            .weight(1f)
            .testTag(TestTags.navItem(item.page.route)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 62.dp)
                .graphicsLayer {
                    val scale = 1f + (0.035f * selectedProgress)
                    scaleX = scale
                    scaleY = scale
                }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = contentColor,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

private data class BottomBarItem(
    val page: AppPage,
    val label: String,
    val icon: ImageVector,
)

private fun bottomBarItems(): List<BottomBarItem> = listOf(
    BottomBarItem(AppPage.Overview, "总览", Icons.Rounded.Home),
    BottomBarItem(AppPage.Apps, "应用", Icons.Rounded.Apps),
    BottomBarItem(AppPage.Settings, "设置", Icons.Rounded.Settings),
)
