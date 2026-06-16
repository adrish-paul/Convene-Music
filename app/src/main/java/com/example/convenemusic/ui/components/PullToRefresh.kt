package com.example.convenemusic.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun PullToRefreshContainer(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    accentColor: androidx.compose.ui.graphics.Color,
    backgroundColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val triggerDistance = with(density) { 80.dp.toPx() }
    
    var pullOffset by remember { mutableStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()
    
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // If user is pulling up, reduce pullOffset
                return if (available.y < 0 && pullOffset > 0) {
                    val prevOffset = pullOffset
                    pullOffset = (pullOffset + available.y).coerceAtLeast(0f)
                    Offset(0f, pullOffset - prevOffset)
                } else {
                    Offset.Zero
                }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // If user is pulling down at the top of list, increase pullOffset
                return if (available.y > 0) {
                    val prevOffset = pullOffset
                    pullOffset += available.y * 0.5f // Resistance factor
                    Offset(0f, pullOffset - prevOffset)
                } else {
                    Offset.Zero
                }
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullOffset >= triggerDistance) {
                    onRefresh()
                }
                // Animate pullOffset back to 0
                coroutineScope.launch {
                    val steps = 10
                    val delta = pullOffset / steps
                    for (i in 1..steps) {
                        pullOffset = (pullOffset - delta).coerceAtLeast(0f)
                        delay(10)
                    }
                    pullOffset = 0f
                }
                return Velocity.Zero
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        // Content container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, pullOffset.roundToInt()) }
        ) {
            content()
        }
    }
}
