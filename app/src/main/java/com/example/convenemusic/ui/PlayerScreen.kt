package com.example.convenemusic.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.example.convenemusic.network.Song
import com.example.convenemusic.ui.components.SquigglyProgressBar
import com.example.convenemusic.ui.theme.UIColors
import com.example.convenemusic.ui.theme.LocalUIColors
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.rotate
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.platform.LocalContext
import coil3.asDrawable
import kotlin.math.sin
import kotlin.math.cos

// New Imports
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Tv
import androidx.activity.compose.BackHandler
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

@Composable
fun PlayerScreen(
    song: Song?,
    isPlaying: Boolean,
    isStreamLoading: Boolean,
    positionMs: Long,
    durationMs: Long,
    onPlayPauseClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    queue: List<Song> = emptyList(),
    currentQueueIndex: Int = 0,
    hazeState: HazeState? = null,
    onQueueSongClick: (Int) -> Unit = {},
    onNextClick: () -> Unit = {},
    onPreviousClick: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    isSongDownloaded: (String) -> Boolean = { false },
    isSongDownloading: (String) -> Boolean = { false },
    onDownloadClick: (Song) -> Unit = {},
    isQueueLoadingMore: Boolean = false,
    isQueueEndless: Boolean = true
) {
    val uiColors = LocalUIColors.current
    val context = LocalContext.current
    var albumAccentColor by remember(song?.id) { mutableStateOf<Color?>(null) }
    val accentColor = albumAccentColor ?: uiColors.progressAccent
    val cleanArtist = remember(song?.artist) {
        song?.artist?.replace(Regex("\\s*•\\s*\\d+:\\d+\\s*$"), "") ?: ""
    }

    var verticalDragAmount by remember { mutableStateOf(0f) }
    var queueSheetExpanded by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // When reverse scrolling (available.y > 0) at the top of the list
                if (available.y > 0f && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                    if (queueSheetExpanded) {
                        queueSheetExpanded = false
                        // Return the available Offset to consume it
                        return Offset(x = 0f, y = available.y)
                    }
                }
                return super.onPreScroll(available, source)
            }
        }
    }

    LaunchedEffect(listState, isQueueEndless, isQueueLoadingMore) {
        snapshotFlow {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 3
        }
        .collect { nearEnd ->
            if (nearEnd && isQueueEndless && !isQueueLoadingMore) {
                onLoadMore()
            }
        }
    }

    // Intercept back button to collapse queue sheet first if expanded
    BackHandler(enabled = queueSheetExpanded) {
        queueSheetExpanded = false
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(uiColors.background)
    ) {
        val screenHeight = maxHeight
        val collapsedHeight = 72.dp
        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val targetExpandedOffset = statusBarHeight + 8.dp
        val expandedHeight = screenHeight - targetExpandedOffset

        val sheetOffsetY by animateDpAsState(
            targetValue = if (queueSheetExpanded) targetExpandedOffset else (screenHeight - collapsedHeight),
            animationSpec = tween(
                durationMillis = 600,
                easing = FastOutSlowInEasing
            ),
            label = "sheetOffsetY"
        )

        // 1. Main Player Content Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 80.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            verticalDragAmount = 0f
                        },
                        onDragEnd = {
                            if (verticalDragAmount > 150f && !queueSheetExpanded) { // swipe down threshold in pixels
                                onBack()
                            } else if (verticalDragAmount < -150f && !queueSheetExpanded) { // swipe up to open queue
                                queueSheetExpanded = true
                            }
                        },
                        onDragCancel = {
                            verticalDragAmount = 0f
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            verticalDragAmount += dragAmount
                        }
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Navigation Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(uiColors.cardBackground, shape = CircleShape)
                        .border(1.dp, uiColors.cardBorder, CircleShape)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Back",
                        tint = uiColors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (song != null) {
                    val isDownloaded = isSongDownloaded(song.id)
                    val isDownloading = isSongDownloading(song.id)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(uiColors.cardBackground, shape = CircleShape)
                            .border(1.dp, uiColors.cardBorder, CircleShape)
                            .clickable(enabled = !isDownloaded && !isDownloading) {
                                onDownloadClick(song)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = accentColor,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isDownloaded) Icons.Default.Check else Icons.Default.Download,
                                contentDescription = "Download",
                                tint = if (isDownloaded) accentColor else uiColors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (song != null) {
                // Album Art Container (High Rounded Corner Card)
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .dropShadow(
                            shape = RoundedCornerShape(32.dp),
                            shadow = Shadow(
                                radius = 30.dp,
                                color = accentColor
                            )
                        )
                        .clip(RoundedCornerShape(32.dp))
                        .border(1.dp, uiColors.cardBorder, RoundedCornerShape(32.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = song.thumbnailUrl,
                        contentDescription = "Album Art",
                        contentScale = ContentScale.Crop,
                        onSuccess = { state ->
                            val drawable = state.result.image.asDrawable(context.resources)
                            val bitmap = (drawable as? BitmapDrawable)?.bitmap
                            if (bitmap != null) {
                                albumAccentColor = extractAccentColor(bitmap, uiColors.progressAccent)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    if (isStreamLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center
                        ) {
                            SquigglyCircleProgressIndicator(
                                modifier = Modifier.size(72.dp),
                                color = accentColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Song Title and Artist Labels
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = song.title,
                        color = uiColors.textPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = cleanArtist,
                        color = uiColors.textSecondary,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Squiggly seekbar — tap OR drag to seek
                val progress = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
                var isDragging by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .pointerInput(durationMs) {
                            detectTapGestures { offset ->
                                val seekFraction = (offset.x / size.width).coerceIn(0f, 1f)
                                onSeek((seekFraction * durationMs).toLong())
                            }
                        }
                        .pointerInput(durationMs) {
                            detectHorizontalDragGestures(
                                onDragStart = { isDragging = true },
                                onDragEnd = { isDragging = false },
                                onDragCancel = { isDragging = false }
                            ) { change, _ ->
                                change.consume()
                                val seekFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                onSeek((seekFraction * durationMs).toLong())
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    SquigglyProgressBar(
                        progress = progress,
                        color = accentColor,
                        trackColor = uiColors.cardBorder,
                        strokeWidth = 6.dp,
                        amplitude = 6.dp,
                        wavelength = 56.dp,
                        showThumb = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(positionMs),
                        color = uiColors.textSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = formatTime(durationMs),
                        color = uiColors.textSecondary,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val prevEnabled = true
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(uiColors.cardBackground, shape = RoundedCornerShape(18.dp))
                            .border(1.dp, uiColors.cardBorder, RoundedCornerShape(18.dp))
                            .clickable(enabled = prevEnabled) { onPreviousClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = if (prevEnabled) uiColors.textPrimary else uiColors.textSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                    val playPauseShapePercent by animateDpAsState(
                        targetValue = if (isPlaying) 38.dp else 22.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "playPauseShape"
                    )
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(playPauseShapePercent))
                            .background(accentColor, shape = RoundedCornerShape(playPauseShapePercent))
                            .clickable { onPlayPauseClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(uiColors.cardBackground, shape = RoundedCornerShape(18.dp))
                            .border(1.dp, uiColors.cardBorder, RoundedCornerShape(18.dp))
                            .clickable { onNextClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = uiColors.textPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

            } else {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No track selected",
                        color = uiColors.textSecondary,
                        fontSize = 16.sp
                    )
                }
            }
        }

        // 2. Sliding Queue Bottom Sheet
        if (song != null) {
            Box(
                modifier = Modifier
                    .offset(y = sheetOffsetY)
                    .fillMaxWidth()
                    .height(expandedHeight)
                    .let { mod ->
                        if (hazeState != null) {
                            mod.hazeEffect(state = hazeState, style = HazeMaterials.ultraThin())
                        } else {
                            mod
                        }
                    }
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(
                        uiColors.bottomBarColor.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    )
                    .border(
                        1.5.dp,
                        uiColors.cardBorder.copy(alpha = 0.8f),
                        RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    )
                    .clickable(enabled = !queueSheetExpanded) {
                        queueSheetExpanded = true
                    }
                    .let { mod ->
                        if (!queueSheetExpanded) {
                            mod.pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragEnd = {},
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        if (dragAmount < -10f) {
                                            queueSheetExpanded = true
                                        }
                                    }
                                )
                            }
                        } else {
                            mod
                        }
                    }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .let { mod ->
                                if (queueSheetExpanded) {
                                    mod.pointerInput(Unit) {
                                        detectVerticalDragGestures(
                                            onDragEnd = {},
                                            onVerticalDrag = { change, dragAmount ->
                                                change.consume()
                                                if (dragAmount > 10f) {
                                                    queueSheetExpanded = false
                                                }
                                            }
                                        )
                                    }
                                } else {
                                    mod
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Drag Handle Line
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(uiColors.textSecondary.copy(alpha = 0.4f), shape = CircleShape)
                                .clickable {
                                    queueSheetExpanded = !queueSheetExpanded
                                }
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (!queueSheetExpanded) {
                            // Peek collapsed state
                            Text(
                                text = "Your queue",
                                color = uiColors.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        } else {
                            // Expanded State Content
                            
                            // Mini player bar at the top of the sheet
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = song.thumbnailUrl,
                                    contentDescription = "Mini Art",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = song.title,
                                        color = uiColors.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = cleanArtist,
                                        color = uiColors.textSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                val miniPlayPauseShapePercent by animateDpAsState(
                                    targetValue = if (isPlaying) 20.dp else 12.dp,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    ),
                                    label = "miniPlayPauseShape"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(miniPlayPauseShapePercent))
                                        .background(accentColor, shape = RoundedCornerShape(miniPlayPauseShapePercent))
                                        .clickable { onPlayPauseClick() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            // Divider (1.dp Box)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(uiColors.cardBorder)
                            )

                            // "Playing from" header (centered "Queue")
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Queue",
                                    color = uiColors.textPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    if (queueSheetExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))

                        // Queue Songs List
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .nestedScroll(nestedScrollConnection),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            itemsIndexed(queue) { index, queueSong ->
                                val isCurrent = index == currentQueueIndex
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onQueueSongClick(index) }
                                        .background(
                                            color = if (isCurrent) uiColors.cardBorder.copy(alpha = 0.2f) else Color.Transparent
                                        )
                                        .padding(horizontal = 20.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Thumbnail with playing soundwave indicator overlay
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.LightGray)
                                    ) {
                                        AsyncImage(
                                            model = queueSong.thumbnailUrl,
                                            contentDescription = "Track Thumbnail",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        if (isCurrent) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.5f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                SoundwaveIndicator(color = accentColor)
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = queueSong.title,
                                            color = if (isCurrent) accentColor else uiColors.textPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = queueSong.artist,
                                            color = uiColors.textSecondary,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    

                                }
                            }
                            // Load-more squiggly footer
                            if (isQueueEndless && isQueueLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "Loading more...",
                                                color = uiColors.textSecondary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(bottom = 10.dp)
                                            )
                                            SquigglyProgressBar(
                                                progress = null,
                                                color = accentColor,
                                                trackColor = uiColors.cardBorder,
                                                animated = true,
                                                animationSpeed = 700,
                                                modifier = Modifier
                                                    .fillMaxWidth(0.5f)
                                                    .height(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SoundwaveIndicator(color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(16.dp)
    ) {
        val transition = rememberInfiniteTransition(label = "soundwave")
        val h1 by transition.animateFloat(
            initialValue = 4f,
            targetValue = 16f,
            animationSpec = infiniteRepeatable(
                animation = tween(400, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "h1"
        )
        val h2 by transition.animateFloat(
            initialValue = 14f,
            targetValue = 6f,
            animationSpec = infiniteRepeatable(
                animation = tween(350, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "h2"
        )
        val h3 by transition.animateFloat(
            initialValue = 6f,
            targetValue = 12f,
            animationSpec = infiniteRepeatable(
                animation = tween(450, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "h3"
        )
        
        Box(modifier = Modifier.width(3.dp).height(h1.dp).background(color, RoundedCornerShape(1.5.dp)))
        Box(modifier = Modifier.width(3.dp).height(h2.dp).background(color, RoundedCornerShape(1.5.dp)))
        Box(modifier = Modifier.width(3.dp).height(h3.dp).background(color, RoundedCornerShape(1.5.dp)))
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

@Composable
fun SquigglyCircleProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF1D9F90),
    strokeWidth: Dp = 4.dp,
    amplitude: Dp = 4.dp,
    waveCount: Int = 12,
    animationSpeed: Int = 1400
) {
    val transition = rememberInfiniteTransition(label = "squiggly_circle_loader")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(animationSpeed, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(animationSpeed, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier.rotate(rotation)) {
        val sw = strokeWidth.toPx()
        val amp = amplitude.toPx()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val minDim = if (size.width < size.height) size.width else size.height
        val r = (minDim - sw - amp * 2f) / 2f

        if (r > 0f) {
            val path = Path()
            val steps = 180
            for (i in 0..steps) {
                val angleDeg = (i * 2).toFloat()
                val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
                val currentWiggle = amp * sin(waveCount * angleRad - phase)
                val currentR = r + currentWiggle
                val x = cx + currentR * cos(angleRad)
                val y = cy + currentR * sin(angleRad)
                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            path.close()

            val brush = Brush.sweepGradient(
                colors = listOf(
                    color.copy(alpha = 0.15f),
                    color,
                    color.copy(alpha = 0.15f)
                ),
                center = Offset(cx, cy)
            )

            drawPath(
                path = path,
                brush = brush,
                style = Stroke(
                    width = sw,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

fun extractAccentColor(bitmap: Bitmap, defaultColor: Color): Color {
    if (bitmap.isRecycled) return defaultColor
    
    // Copy hardware bitmap to software configuration if necessary
    val softwareBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
        bitmap.copy(Bitmap.Config.ARGB_8888, false)
    } else {
        bitmap
    } ?: return defaultColor
    
    val scaled = Bitmap.createScaledBitmap(softwareBitmap, 16, 16, false) ?: return defaultColor
    
    // Recycle temporary copy if it was created
    if (softwareBitmap != bitmap) {
        softwareBitmap.recycle()
    }
    
    var bestColor = defaultColor
    var maxSat = 0f
    val hsv = FloatArray(3)
    
    for (y in 0 until scaled.height) {
        for (x in 0 until scaled.width) {
            val pixel = scaled.getPixel(x, y)
            android.graphics.Color.colorToHSV(pixel, hsv)
            val sat = hsv[1]
            val value = hsv[2]
            
            // Focus on colorful, vibrant pixels (exclude near-black, near-white, or grayscale colors)
            if (sat > 0.25f && value > 0.2f && value < 0.85f) {
                if (sat > maxSat) {
                    maxSat = sat
                    bestColor = Color(pixel)
                }
            }
        }
    }
    scaled.recycle()
    
    // Ensure the color is sufficiently bright and vibrant for readability on dark backgrounds
    val hsvOut = FloatArray(3)
    android.graphics.Color.colorToHSV(bestColor.toArgb(), hsvOut)
    if (hsvOut[2] < 0.7f) {
        hsvOut[2] = 0.7f // Boost brightness/value to at least 70%
        bestColor = Color(android.graphics.Color.HSVToColor(hsvOut))
    }
    return bestColor
}
