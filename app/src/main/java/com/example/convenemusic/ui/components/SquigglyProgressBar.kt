package com.example.convenemusic.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.ui.draw.drawWithCache

/**
 * A squiggly progress bar with two modes:
 *
 * - [animated] = true  → scrolling animated wave (used for loading states)
 * - [animated] = false → static sine wave (used for the player seekbar)
 *
 * Layout:
 * - Played portion  → squiggle (animated or static)
 * - Remaining portion → flat straight line
 * - Thumb at boundary → small thick vertical bar (when [showThumb] = true)
 *
 * @param progress       0f..1f fraction (null = indeterminate full-width squiggle)
 * @param color          played wave / squiggle color
 * @param trackColor     unplayed flat line color
 * @param strokeWidth    line thickness
 * @param amplitude      wave peak height
 * @param wavelength     wave period — higher = fewer, gentler waves
 * @param animated       if true the wave scrolls (loading); if false it is static (seekbar)
 * @param animationSpeed full wave-cycle duration in ms (only used when animated = true)
 * @param showThumb      draw the vertical bar handle at the progress tip
 */
@Composable
fun SquigglyProgressBar(
    progress: Float? = null,
    color: Color = Color(0xFF1D9F90),
    trackColor: Color = Color(0xFFE5E5EA),
    strokeWidth: Dp = 3.dp,
    amplitude: Dp = 4.dp,
    wavelength: Dp = 60.dp,
    animated: Boolean = false,
    animationSpeed: Int = 700,
    showThumb: Boolean = false,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(16.dp)
) {
    // Phase offset — only driven when animated = true
    val phaseAnim = rememberInfiniteTransition(label = "squiggle_phase")
    val animatedPhase by phaseAnim.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = animationSpeed, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "squiggle_phase_value"
    )
    // When not animated, phase is always 0 (static)
    val phaseOffset = if (animated) animatedPhase else 0f

    val path = remember { Path() }

    Spacer(
        modifier = modifier.drawWithCache {
            onDrawBehind {
                val sw = strokeWidth.toPx()
                val amp = amplitude.toPx()
                val wl = wavelength.toPx()
                val midY = size.height / 2f
                val fillWidth = if (progress != null) size.width * progress.coerceIn(0f, 1f) else size.width

                // Tiny gap on each side of the thumb bar for a seamless clean look
                val gap = if (showThumb) 2.5f else 0f

                // ── Unplayed track: flat straight line ──────────────────────────────
                if (progress != null && fillWidth < size.width) {
                    drawLine(
                        color = trackColor,
                        start = Offset((fillWidth + gap).coerceAtMost(size.width), midY),
                        end = Offset(size.width, midY),
                        strokeWidth = sw * 0.85f,
                        cap = StrokeCap.Round
                    )
                }

                // ── Squiggle (played portion or full width if indeterminate) ─────────
                if (fillWidth > 0f) {
                    path.reset()
                    val startX = 0f
                    val endX = (fillWidth - gap).coerceAtLeast(0f)
                    val step = 5f // 5 pixels step is extremely smooth and saves 2.5x computation!
                    var x = startX
                    var firstPoint = true

                    while (x <= endX) {
                        val phase = (2.0f * PI.toFloat() * (x / wl - phaseOffset))
                        val y = midY + amp * kotlin.math.sin(phase)
                        if (firstPoint) {
                            path.moveTo(x, y)
                            firstPoint = false
                        } else {
                            path.lineTo(x, y)
                        }
                        x += step
                    }
                    // If we didn't end exactly at endX, draw a final point to endX to make sure it reaches the thumb seamlessly.
                    if (x - step < endX) {
                        val phase = (2.0f * PI.toFloat() * (endX / wl - phaseOffset))
                        val y = midY + amp * kotlin.math.sin(phase)
                        path.lineTo(endX, y)
                    }

                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(
                            width = sw,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // ── Thumb: small thick vertical bar at the junction ─────────────────
                if (showThumb && progress != null) {
                    val thumbX = fillWidth.coerceIn(sw, size.width - sw)
                    val barHalfHeight = amp * 1.6f
                    val barWidth = sw * 1.8f
                    drawLine(
                        color = color,
                        start = Offset(thumbX, midY - barHalfHeight),
                        end = Offset(thumbX, midY + barHalfHeight),
                        strokeWidth = barWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    )
}
