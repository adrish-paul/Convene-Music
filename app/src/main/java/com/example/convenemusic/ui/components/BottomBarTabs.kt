package com.example.convenemusic.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


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
    CompositionLocalProvider(
        LocalTextStyle provides LocalTextStyle.current.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        ),
        LocalContentColor provides Color.White
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MusicTab.values().forEach { tab ->
                val isSelected = selectedTab == tab
                
                val alpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else .65f,
                    label = "alpha"
                )
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1f else .92f,
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
                        .pointerInput(Unit) {
                            detectTapGestures {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                onTabSelected(tab)
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = tab.icon, 
                        contentDescription = "${tab.title} Tab",
                        tint = iconColor,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}
