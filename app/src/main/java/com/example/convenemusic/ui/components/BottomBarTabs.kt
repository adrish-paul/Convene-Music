package com.example.convenemusic.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

enum class MusicTab(
    val title: String,
    val icon: ImageVector,
    val activeColor: Color
) {
    Home("Home", Icons.Default.Home, Color(0xFF33B5A5)),
    Search("Search", Icons.Default.Search, Color(0xFFECC844)),
    Library("Library", Icons.Default.List, Color(0xFF2B5BE5))
}

@Composable
fun BottomBarTabs(
    selectedTab: MusicTab,
    onTabSelected: (MusicTab) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val currentOnTabSelected by rememberUpdatedState(onTabSelected)

    CompositionLocalProvider(
        LocalTextStyle provides LocalTextStyle.current.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        ),
        LocalContentColor provides Color.White
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterStart
        ) {
            val density = LocalDensity.current
            val totalWidthPx = with(density) { maxWidth.toPx() }
            val tabs = MusicTab.entries
            val tabsCount = tabs.size
            val tabWidthPx = totalWidthPx / tabsCount
            val tabWidthDp = maxWidth / tabsCount

            fun getTabCenterPx(index: Int): Float {
                return tabWidthPx * index + tabWidthPx / 2f
            }

            // Simple float state for casing position
            var casingCenterX by remember(totalWidthPx) {
                mutableStateOf(getTabCenterPx(selectedTab.ordinal))
            }
            var isDragging by remember { mutableStateOf(false) }
            var lastClosestTabOrdinal by remember { mutableStateOf(selectedTab.ordinal) }

            // Smoothly animate casing position to target tab when not dragging
            LaunchedEffect(selectedTab, isDragging, totalWidthPx) {
                if (!isDragging) {
                    animate(
                        initialValue = casingCenterX,
                        targetValue = getTabCenterPx(selectedTab.ordinal),
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) { value, _ ->
                        casingCenterX = value
                    }
                }
            }

            // Drag State: delta is processed synchronously for 120 FPS swiping with no coroutine lags!
            val dragState = rememberDraggableState { delta ->
                val newTarget = (casingCenterX + delta).coerceIn(
                    getTabCenterPx(0),
                    getTabCenterPx(tabsCount - 1)
                )
                casingCenterX = newTarget

                val closest = (newTarget / tabWidthPx).toInt().coerceIn(0, tabsCount - 1)
                if (closest != lastClosestTabOrdinal) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    lastClosestTabOrdinal = closest
                }
            }

            val draggableModifier = Modifier.draggable(
                state = dragState,
                orientation = Orientation.Horizontal,
                onDragStarted = {
                    isDragging = true
                },
                onDragStopped = {
                    isDragging = false
                    val targetTabOrdinal = (casingCenterX / tabWidthPx).toInt().coerceIn(0, tabsCount - 1)
                    currentOnTabSelected(tabs[targetTabOrdinal])
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(draggableModifier)
            ) {
                // Draggable Glass Casing (pill shape)
                val casingWidth = tabWidthDp - 16.dp
                val casingHeight = 52.dp

                // Calculate which tab's activeColor to use dynamically based on the current position of the casing
                val progress = if (totalWidthPx > 0f) (casingCenterX / totalWidthPx).coerceIn(0f, 1f) else 0f
                val activeColor = remember(progress) {
                    val segment = progress * (tabsCount - 1)
                    val index = segment.toInt().coerceIn(0, tabsCount - 2)
                    val fraction = (segment - index).coerceIn(0f, 1f)
                    val c1 = tabs[index].activeColor
                    val c2 = tabs[index + 1].activeColor

                    Color(
                        red = c1.red + (c2.red - c1.red) * fraction,
                        green = c1.green + (c2.green - c1.green) * fraction,
                        blue = c1.blue + (c2.blue - c1.blue) * fraction,
                        alpha = c1.alpha + (c2.alpha - c1.alpha) * fraction
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset {
                            val widthPx = with(density) { casingWidth.toPx() }
                            IntOffset(
                                x = (casingCenterX - widthPx / 2f).roundToInt(),
                                y = 0
                            )
                        }
                        .width(casingWidth)
                        .height(casingHeight)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.12f),
                                    Color.White.copy(alpha = 0.04f)
                                )
                            ),
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0.05f)
                                )
                            ),
                            shape = CircleShape
                        )
                        .border(
                            width = 2.dp,
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    activeColor.copy(alpha = 0.4f),
                                    Color.Transparent
                                ),
                                radius = with(density) { casingWidth.toPx() }
                            ),
                            shape = CircleShape
                        )
                ) {
                    // Inner glow / liquid dot or highlight inside casing
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 4.dp)
                            .size(width = 24.dp, height = 3.dp)
                            .background(activeColor, shape = CircleShape)
                    )
                }

                // Tab Items Row
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { tab ->
                        val isSelected = selectedTab == tab

                        val alpha by animateFloatAsState(
                            targetValue = if (isSelected) 1f else .65f,
                            label = "alpha"
                        )
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.05f else .92f,
                            visibilityThreshold = .000001f,
                            animationSpec = spring(
                                stiffness = Spring.StiffnessLow,
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                            ),
                            label = "scale"
                        )
                        val iconColor by animateColorAsState(
                            targetValue = if (isSelected) tab.activeColor else Color(0xFF8E8E93),
                            label = "iconColor"
                        )

                        Column(
                            modifier = Modifier
                                .scale(scale)
                                .alpha(alpha)
                                .fillMaxHeight()
                                .weight(1f)
                                .pointerInput(tab) {
                                    detectTapGestures {
                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                        currentOnTabSelected(tab)
                                    }
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = "${tab.title} Tab",
                                tint = iconColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
