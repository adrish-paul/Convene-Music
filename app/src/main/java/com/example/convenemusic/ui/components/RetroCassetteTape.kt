package com.example.convenemusic.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.convenemusic.ui.theme.UIColors
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable

@Composable
fun RetroCassetteTape(
    title: String,
    author: String,
    trackCount: Int,
    isPlaying: Boolean,
    uiColors: UIColors,
    modifier: Modifier = Modifier,
    showControls: Boolean = false,
    shuffleEnabled: Boolean = false,
    loopMode: Int = 0,
    onShuffleClick: (() -> Unit)? = null,
    onPreviousClick: (() -> Unit)? = null,
    onPlayClick: (() -> Unit)? = null,
    onNextClick: (() -> Unit)? = null,
    onLoopClick: (() -> Unit)? = null
) {
    // Spools rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "cassette_rotation")
    val rotationAngle by if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "spool_rotation"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.6f)
            .clip(RoundedCornerShape(16.dp))
            .background(uiColors.cardBackground.copy(alpha = 0.95f))
            .border(2.dp, uiColors.cardBorder, RoundedCornerShape(16.dp))
    ) {
        // Corner Screws
        CassetteScrew(uiColors = uiColors, modifier = Modifier.align(Alignment.TopStart).padding(top = 8.dp, start = 8.dp))
        CassetteScrew(uiColors = uiColors, modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp))
        CassetteScrew(uiColors = uiColors, modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 8.dp, start = 8.dp))
        CassetteScrew(uiColors = uiColors, modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 8.dp, end = 8.dp))

        // Top Label Block
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Main Playlist Title (with Marquee!)
                Text(
                    text = title.uppercase(),
                    color = uiColors.textPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .basicMarquee()
                )

                // Small frequency/meter dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(8) { index ->
                        Box(
                            modifier = Modifier
                                .size(width = 3.dp, height = 7.dp)
                                .background(uiColors.textSecondary.copy(alpha = 0.6f))
                        )
                    }
                }

                // ON/OFF markers
                Text(
                    text = "■ ON  □ OFF",
                    color = uiColors.textSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 7.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Progress/accent stripe line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(uiColors.progressAccent)
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Subtitle
            Text(
                text = "EXPENSIVE  APPEALING  BEAUTIFUL",
                color = uiColors.textSecondary.copy(alpha = 0.65f),
                fontWeight = FontWeight.Bold,
                fontSize = 6.sp,
                letterSpacing = 1.sp
            )
        }

        // Center Window Panel
        BoxWithConstraints(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.82f)
                .fillMaxHeight(0.44f)
                .border(2.dp, uiColors.cardBorder, RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.45f))
        ) {
            val width = maxWidth
            val height = maxHeight

            // Magnetic Tape Rolls background
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerY = size.height / 2f
                val leftSpoolX = size.width * 0.32f
                val rightSpoolX = size.width * 0.68f
                val leftRadius = size.height * 0.44f
                val rightRadius = size.height * 0.32f

                // Left Tape Roll
                drawCircle(
                    color = uiColors.progressAccent.copy(alpha = 0.45f),
                    radius = leftRadius,
                    center = Offset(leftSpoolX, centerY)
                )

                // Right Tape Roll
                drawCircle(
                    color = uiColors.progressAccent.copy(alpha = 0.45f),
                    radius = rightRadius,
                    center = Offset(rightSpoolX, centerY)
                )

                // Rotating Sheen/Light Effect on Left Tape Roll
                rotate(rotationAngle, pivot = Offset(leftSpoolX, centerY)) {
                    // Wedge reflection 1
                    drawArc(
                        color = Color.White.copy(alpha = 0.15f),
                        startAngle = -45f,
                        sweepAngle = 30f,
                        useCenter = true,
                        topLeft = Offset(leftSpoolX - leftRadius, centerY - leftRadius),
                        size = Size(leftRadius * 2, leftRadius * 2)
                    )
                    // Wedge reflection 2
                    drawArc(
                        color = Color.White.copy(alpha = 0.15f),
                        startAngle = 135f,
                        sweepAngle = 30f,
                        useCenter = true,
                        topLeft = Offset(leftSpoolX - leftRadius, centerY - leftRadius),
                        size = Size(leftRadius * 2, leftRadius * 2)
                    )
                    // Concentric dark grooves for tape texture
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.15f),
                        radius = leftRadius * 0.8f,
                        center = Offset(leftSpoolX, centerY),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.15f),
                        radius = leftRadius * 0.6f,
                        center = Offset(leftSpoolX, centerY),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // Rotating Sheen/Light Effect on Right Tape Roll
                rotate(rotationAngle, pivot = Offset(rightSpoolX, centerY)) {
                    // Wedge reflection 1
                    drawArc(
                        color = Color.White.copy(alpha = 0.15f),
                        startAngle = -45f,
                        sweepAngle = 30f,
                        useCenter = true,
                        topLeft = Offset(rightSpoolX - rightRadius, centerY - rightRadius),
                        size = Size(rightRadius * 2, rightRadius * 2)
                    )
                    // Wedge reflection 2
                    drawArc(
                        color = Color.White.copy(alpha = 0.15f),
                        startAngle = 135f,
                        sweepAngle = 30f,
                        useCenter = true,
                        topLeft = Offset(rightSpoolX - rightRadius, centerY - rightRadius),
                        size = Size(rightRadius * 2, rightRadius * 2)
                    )
                    // Concentric grooves
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.15f),
                        radius = rightRadius * 0.8f,
                        center = Offset(rightSpoolX, centerY),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.15f),
                        radius = rightRadius * 0.6f,
                        center = Offset(rightSpoolX, centerY),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }

            // Left Spool (aligned exactly over left reel center)
            Box(
                modifier = Modifier
                    .offset(x = width * 0.32f - 17.dp, y = height / 2f - 17.dp)
            ) {
                CassetteSpool(uiColors = uiColors, rotationAngle = rotationAngle)
            }

            // Right Spool (aligned exactly over right reel center)
            Box(
                modifier = Modifier
                    .offset(x = width * 0.68f - 17.dp, y = height / 2f - 17.dp)
            ) {
                CassetteSpool(uiColors = uiColors, rotationAngle = rotationAngle)
            }

            // Window Overlay labels: Track count on top right, author print on bottom right
            Text(
                text = String.format("%02d", trackCount),
                color = uiColors.accentRed,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 12.dp)
            )

            Text(
                text = author.uppercase(),
                color = uiColors.textPrimary.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 6.dp, end = 12.dp)
                    .fillMaxWidth(0.35f)
            )
        }

        // Bottom labels (above trapezoid)
        val hasButtons = onPlayClick != null
        val labelBottomPadding = if (hasButtons) 60.dp else 46.dp
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = labelBottomPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AN OLD TAPE",
                    color = uiColors.textSecondary.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 7.sp
                )
                Text(
                    text = "DISTORTION OUTDATED UNPLEASANT",
                    color = uiColors.textSecondary.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 6.sp
                )
                Text(
                    text = "ONCE IT WAS GOOD",
                    color = uiColors.textSecondary.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 6.sp
                )
            }
        }

        // Bottom Trapezoid Bracket
        val trapezoidHeight = if (hasButtons) 58.dp else 36.dp
        val trapezoidWidthFraction = if (showControls) 0.85f else 0.68f

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(trapezoidWidthFraction)
                .height(trapezoidHeight)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = Path().apply {
                    moveTo(size.width * 0.08f, 0f)
                    lineTo(size.width * 0.92f, 0f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }

                // Match card background for cohesive look
                drawPath(path = path, color = uiColors.cardBorder)
                drawPath(path = path, color = uiColors.cardBorder, style = Stroke(width = 2.dp.toPx()))

                // Hanger slot holes
                drawCircle(
                    color = uiColors.cardBackground,
                    radius = 3.dp.toPx(),
                    center = Offset(size.width * 0.18f, size.height * 0.45f)
                )
                drawCircle(
                    color = uiColors.cardBackground,
                    radius = 3.dp.toPx(),
                    center = Offset(size.width * 0.82f, size.height * 0.45f)
                )
            }

            if (showControls) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Loop Mode Button
                    val loopIcon = when (loopMode) {
                        1 -> Icons.Default.RepeatOne
                        2 -> Icons.Default.Repeat
                        else -> Icons.Default.Repeat
                    }
                    val loopTint = if (loopMode > 0) uiColors.progressAccent else uiColors.textSecondary.copy(alpha = 0.5f)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { onLoopClick?.invoke() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = loopIcon,
                            contentDescription = "Loop Mode",
                            tint = loopTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Previous Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { onPreviousClick?.invoke() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = uiColors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Play/Pause Button (Centered, larger)
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(4.dp, CircleShape)
                            .background(uiColors.progressAccent, shape = CircleShape)
                            .border(1.5.dp, uiColors.cardBorder, CircleShape)
                            .clickable { onPlayClick?.invoke() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Next Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { onNextClick?.invoke() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = uiColors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Shuffle Button
                    val shuffleTint = if (shuffleEnabled) uiColors.progressAccent else uiColors.textSecondary.copy(alpha = 0.5f)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { onShuffleClick?.invoke() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = shuffleTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                if (onPlayClick != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onPreviousClick != null) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable { onPreviousClick.invoke() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous",
                                    tint = uiColors.textPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .shadow(4.dp, CircleShape)
                                .background(uiColors.progressAccent, shape = CircleShape)
                                .border(1.5.dp, uiColors.cardBorder, CircleShape)
                                .clickable { onPlayClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        if (onNextClick != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable { onNextClick.invoke() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next",
                                    tint = uiColors.textPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Convene custom sticker in trapezoid center
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .border(1.dp, uiColors.cardBorder, RoundedCornerShape(2.dp))
                            .background(uiColors.cardBackground, RoundedCornerShape(2.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "#CONVENE",
                            color = uiColors.progressAccent,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 7.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CassetteScrew(uiColors: UIColors, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(8.dp)) {
        drawCircle(color = uiColors.cardBorder, radius = size.minDimension / 2f)
        drawLine(
            color = uiColors.cardBackground,
            start = Offset(size.width * 0.22f, size.height * 0.22f),
            end = Offset(size.width * 0.78f, size.height * 0.78f),
            strokeWidth = 1.5.dp.toPx()
        )
    }
}

@Composable
private fun CassetteSpool(
    uiColors: UIColors,
    rotationAngle: Float,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .size(34.dp)
            .rotate(rotationAngle)
    ) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Outer spool circle border
        drawCircle(
            color = uiColors.textPrimary.copy(alpha = 0.85f),
            radius = radius,
            center = center,
            style = Stroke(width = 3.dp.toPx())
        )

        // Draw 6 spools wedges/teeth
        val numTeeth = 6
        val toothAngle = 360f / numTeeth
        for (i in 0 until numTeeth) {
            val angleRad = Math.toRadians((i * toothAngle).toDouble())
            val startX = center.x + Math.cos(angleRad).toFloat() * (radius * 0.38f)
            val startY = center.y + Math.sin(angleRad).toFloat() * (radius * 0.38f)
            val endX = center.x + Math.cos(angleRad).toFloat() * (radius * 0.75f)
            val endY = center.y + Math.sin(angleRad).toFloat() * (radius * 0.75f)

            drawLine(
                color = uiColors.textPrimary.copy(alpha = 0.85f),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 3.dp.toPx()
            )
        }
    }
}
