package com.example.convenemusic.ui

import androidx.compose.animation.core.*
import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.saveable.rememberSaveable
import android.widget.Toast
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
import com.example.convenemusic.network.LyricLine
import com.example.convenemusic.network.LyricsData
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
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.foundation.verticalScroll
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
    isQueueEndless: Boolean = true,
    customPlaylists: List<CustomPlaylist> = emptyList(),
    onAddToPlaylist: (String, Song) -> Unit = { _, _ -> },
    onCreatePlaylist: (String) -> Unit = {},
    lyrics: LyricsData? = null,
    isLyricsLoading: Boolean = false,
    lyricsError: String? = null,
    onRetryLyrics: () -> Unit = {}
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
    var activeTab by remember { mutableStateOf(0) } // 0 = Queue, 1 = Lyrics

    // More Options & Equaliser state
    var showMoreMenu by remember { mutableStateOf(false) }
    var showEqualizerMenu by remember { mutableStateOf(false) }
    val prefs = remember(context) { context.getSharedPreferences("convenemusic_prefs", android.content.Context.MODE_PRIVATE) }
    var eqMode by remember {
        mutableStateOf(prefs.getString("eq_mode", "Default") ?: "Default")
    }
    var eqValues by remember {
        mutableStateOf(
            listOf(
                prefs.getFloat("eq_band_0", 0f),
                prefs.getFloat("eq_band_1", 0f),
                prefs.getFloat("eq_band_2", 0f),
                prefs.getFloat("eq_band_3", 0f),
                prefs.getFloat("eq_band_4", 0f)
            )
        )
    }
    var eqBassStrength by remember {
        mutableStateOf(prefs.getFloat("eq_bass_strength", 0f))
    }
    var eqSurroundStrength by remember {
        mutableStateOf(prefs.getFloat("eq_surround_strength", 0f))
    }
    var eqDefaultBassEnabled by remember {
        mutableStateOf(prefs.getBoolean("eq_default_bass_enabled", true))
    }
    var eqDefaultAudioBoostEnabled by remember {
        mutableStateOf(prefs.getBoolean("eq_default_audio_boost_enabled", true))
    }
    var eqDefaultSurroundEnabled by remember {
        mutableStateOf(prefs.getBoolean("eq_default_surround_enabled", false))
    }
    var eqAudioBoostStrength by remember {
        mutableStateOf(prefs.getFloat("eq_audio_boost_strength", 300f))
    }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val lyricsListState = rememberLazyListState()

    val nestedScrollConnection = remember(activeTab, listState, lyricsListState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val currentListState = if (activeTab == 0) listState else lyricsListState
                // When reverse scrolling (available.y > 0) at the top of the list
                if (available.y > 0f && currentListState.firstVisibleItemIndex == 0 && currentListState.firstVisibleItemScrollOffset == 0) {
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
        val screenWidth = maxWidth
        val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val collapsedHeight = 72.dp + navigationBarsPadding
        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val targetExpandedOffset = statusBarHeight + 8.dp
        val expandedHeight = screenHeight - targetExpandedOffset

        // Dynamic Sizing calculations based on device metrics (slightly reduced)
        val maxImageWidth = screenWidth - 48.dp
        val imageSize = (screenHeight * 0.30f).coerceIn(180.dp, 280.dp).coerceAtMost(maxImageWidth)
        val controlButtonSize = (screenHeight * 0.075f).coerceIn(48.dp, 64.dp)
        val playButtonSize = (screenHeight * 0.10f).coerceIn(64.dp, 84.dp)
        val controlIconSize = controlButtonSize * 0.5f
        val playIconSize = playButtonSize * 0.5f
        val controlCornerRadius = controlButtonSize * 0.3f

        val sheetOffsetY by animateDpAsState(
            targetValue = if (queueSheetExpanded) targetExpandedOffset else (screenHeight - collapsedHeight),
            animationSpec = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            ),
            label = "sheetOffsetY"
        )

        // 1. Main Player Content Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 110.dp)
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Add to Playlist Button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(uiColors.cardBackground, shape = CircleShape)
                                .border(1.dp, uiColors.cardBorder, CircleShape)
                                .clickable { showAddToPlaylistDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add to Playlist",
                                tint = uiColors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // More Options Button (replaces the Download button in the top bar)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(uiColors.cardBackground, shape = CircleShape)
                                .border(1.dp, uiColors.cardBorder, CircleShape)
                                .clickable { showMoreMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = uiColors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        // Add to Playlist Dialog
                        if (showAddToPlaylistDialog) {
                            AlertDialog(
                                onDismissRequest = { showAddToPlaylistDialog = false },
                                title = {
                                    Text(
                                        text = "Add to Playlist",
                                        color = uiColors.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                },
                                text = {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                showAddToPlaylistDialog = false
                                                showCreatePlaylistDialog = true
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = accentColor,
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Create New Playlist", fontWeight = FontWeight.Bold)
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        if (customPlaylists.isEmpty()) {
                                            Text(
                                                text = "No custom playlists yet.",
                                                color = uiColors.textSecondary,
                                                fontSize = 14.sp
                                            )
                                        } else {
                                            androidx.compose.foundation.lazy.LazyColumn(
                                                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                items(customPlaylists.size) { index ->
                                                    val pl = customPlaylists[index]
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                onAddToPlaylist(pl.name, song)
                                                                Toast.makeText(context, "Added to ${pl.name}", Toast.LENGTH_SHORT).show()
                                                                showAddToPlaylistDialog = false
                                                            }
                                                            .padding(vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.List,
                                                            contentDescription = null,
                                                            tint = accentColor,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Text(
                                                            text = pl.name,
                                                            color = uiColors.textPrimary,
                                                            fontSize = 15.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showAddToPlaylistDialog = false }) {
                                        Text("Cancel", color = accentColor)
                                    }
                                },
                                containerColor = uiColors.background,
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.border(1.dp, uiColors.cardBorder, RoundedCornerShape(24.dp))
                            )
                        }

                        // Create Playlist Dialog
                        if (showCreatePlaylistDialog) {
                            var playlistName by remember { mutableStateOf("") }
                            AlertDialog(
                                onDismissRequest = { showCreatePlaylistDialog = false },
                                title = {
                                    Text(
                                        text = "Create Playlist",
                                        color = uiColors.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                },
                                text = {
                                    OutlinedTextField(
                                        value = playlistName,
                                        onValueChange = { playlistName = it },
                                        label = { Text("Playlist Name", color = uiColors.textSecondary) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = accentColor,
                                            unfocusedBorderColor = uiColors.cardBorder,
                                            focusedTextColor = uiColors.textPrimary,
                                            unfocusedTextColor = uiColors.textPrimary,
                                            cursorColor = accentColor
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            if (playlistName.isNotBlank()) {
                                                onCreatePlaylist(playlistName.trim())
                                                onAddToPlaylist(playlistName.trim(), song)
                                                Toast.makeText(context, "Created & Added to ${playlistName.trim()}", Toast.LENGTH_SHORT).show()
                                                showCreatePlaylistDialog = false
                                            }
                                        }
                                    ) {
                                        Text("Done", color = accentColor, fontWeight = FontWeight.Bold)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showCreatePlaylistDialog = false }) {
                                        Text("Cancel", color = uiColors.textSecondary)
                                    }
                                },
                                containerColor = uiColors.background,
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.border(1.dp, uiColors.cardBorder, RoundedCornerShape(24.dp))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (song != null) {
                Spacer(modifier = Modifier.weight(1f))

                // Album Art Container (High Rounded Corner Card)
                Box(
                    modifier = Modifier
                        .size(imageSize)
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

                Spacer(modifier = Modifier.weight(1f))

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

                Spacer(modifier = Modifier.weight(0.8f))

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

                Spacer(modifier = Modifier.weight(1.2f))

                // Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val prevEnabled = true
                    Box(
                        modifier = Modifier
                            .size(controlButtonSize)
                            .clip(RoundedCornerShape(controlCornerRadius))
                            .background(uiColors.cardBackground, shape = RoundedCornerShape(controlCornerRadius))
                            .border(1.dp, uiColors.cardBorder, RoundedCornerShape(controlCornerRadius))
                            .clickable(enabled = prevEnabled) { onPreviousClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = if (prevEnabled) uiColors.textPrimary else uiColors.textSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(controlIconSize)
                        )
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                    val playPauseShapePercent by animateDpAsState(
                        targetValue = if (isPlaying) (playButtonSize * 0.475f) else (playButtonSize * 0.275f),
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "playPauseShape"
                    )
                    Box(
                        modifier = Modifier
                            .size(playButtonSize)
                            .clip(RoundedCornerShape(playPauseShapePercent))
                            .background(accentColor, shape = RoundedCornerShape(playPauseShapePercent))
                            .clickable { onPlayPauseClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(playIconSize)
                        )
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                    Box(
                        modifier = Modifier
                            .size(controlButtonSize)
                            .clip(RoundedCornerShape(controlCornerRadius))
                            .background(uiColors.cardBackground, shape = RoundedCornerShape(controlCornerRadius))
                            .border(1.dp, uiColors.cardBorder, RoundedCornerShape(controlCornerRadius))
                            .clickable { onNextClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = uiColors.textPrimary,
                            modifier = Modifier.size(controlIconSize)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(0.8f))

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
                        // Header Row (Queue and Lyrics buttons)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Queue Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (queueSheetExpanded && activeTab == 0) accentColor else uiColors.cardBackground)
                                    .border(
                                        width = 1.dp,
                                        color = if (queueSheetExpanded && activeTab == 0) Color.Transparent else uiColors.cardBorder,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { 
                                        if (!queueSheetExpanded) {
                                            activeTab = 0
                                            queueSheetExpanded = true
                                        } else {
                                            if (activeTab == 0) {
                                                queueSheetExpanded = false
                                            } else {
                                                activeTab = 0
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Queue",
                                    color = if (queueSheetExpanded && activeTab == 0) Color.White else uiColors.textPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Lyrics Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (queueSheetExpanded && activeTab == 1) accentColor else uiColors.cardBackground)
                                    .border(
                                        width = 1.dp,
                                        color = if (queueSheetExpanded && activeTab == 1) Color.Transparent else uiColors.cardBorder,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { 
                                        if (!queueSheetExpanded) {
                                            activeTab = 1
                                            queueSheetExpanded = true
                                        } else {
                                            if (activeTab == 1) {
                                                queueSheetExpanded = false
                                            } else {
                                                activeTab = 1
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Lyrics",
                                    color = if (queueSheetExpanded && activeTab == 1) Color.White else uiColors.textPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    if (queueSheetExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))

                        if (activeTab == 0) {
                            // Queue Songs List
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .nestedScroll(nestedScrollConnection),
                                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp + navigationBarsPadding),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // 1. Now Playing Section
                                val currentSongItem = queue.getOrNull(currentQueueIndex) ?: song
                                item {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "Now Playing",
                                            color = uiColors.textSecondary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                        QueueSongCard(
                                            song = currentSongItem,
                                            isCurrent = true,
                                            isPlaying = isPlaying,
                                            onPlayPauseClick = onPlayPauseClick,
                                            onClick = { /* Clicking current song does nothing */ },
                                            uiColors = uiColors,
                                            accentColor = accentColor
                                        )
                                    }
                                }

                                // 2. Next In Queue Section
                                val nextSongs = if (currentQueueIndex in queue.indices) {
                                    queue.subList(currentQueueIndex + 1, queue.size)
                                } else {
                                    emptyList()
                                }
                                if (nextSongs.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Next In Queue",
                                            color = uiColors.textSecondary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                                        )
                                    }
                                    itemsIndexed(nextSongs) { index, nextSong ->
                                        val actualQueueIndex = currentQueueIndex + 1 + index
                                        QueueSongCard(
                                            song = nextSong,
                                            isCurrent = false,
                                            isPlaying = false,
                                            onClick = { onQueueSongClick(actualQueueIndex) },
                                            uiColors = uiColors,
                                            accentColor = accentColor
                                        )
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
                        } else {
                            // Lyrics Page
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                when {
                                    isLyricsLoading -> {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(color = accentColor)
                                        }
                                    }
                                    lyricsError != null && lyrics == null -> {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = lyricsError ?: "Failed to load lyrics",
                                                color = uiColors.textSecondary,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Button(
                                                onClick = onRetryLyrics,
                                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                                            ) {
                                                Text("Retry", color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    lyrics == null || lyrics.lines.isEmpty() -> {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "No lyrics found",
                                                color = uiColors.textSecondary,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    else -> {
                                        val lyricsData = lyrics
                                        val lines = lyricsData.lines
                                        val activeLineIndex = remember(positionMs, lyricsData) {
                                            if (lines.isEmpty()) -1
                                            else if (lyricsData.isSynced) {
                                                lines.indexOfLast { it.timestampMs <= positionMs }
                                            } else {
                                                -1
                                            }
                                        }

                                        LaunchedEffect(activeLineIndex) {
                                            if (lyricsData.isSynced && lines.isNotEmpty() && activeLineIndex >= 0) {
                                                val targetScrollIndex = (activeLineIndex - 2).coerceAtLeast(0)
                                                lyricsListState.animateScrollToItem(targetScrollIndex)
                                            }
                                        }

                                        LazyColumn(
                                            state = lyricsListState,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .nestedScroll(nestedScrollConnection),
                                            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 48.dp + navigationBarsPadding),
                                            verticalArrangement = Arrangement.spacedBy(18.dp)
                                        ) {
                                            itemsIndexed(lines) { index, line ->
                                                val isActive = lyricsData.isSynced && index == activeLineIndex
                                                val fontColor = if (lyricsData.isSynced) {
                                                    if (isActive) Color.White else uiColors.textPrimary.copy(alpha = 0.45f)
                                                } else {
                                                    uiColors.textPrimary.copy(alpha = 0.75f)
                                                }
                                                val fontSize = if (lyricsData.isSynced) {
                                                    if (isActive) 24.sp else 20.sp
                                                } else {
                                                    18.sp
                                                }
                                                val fontWeight = if (lyricsData.isSynced) {
                                                    if (isActive) FontWeight.ExtraBold else FontWeight.Bold
                                                } else {
                                                    FontWeight.Medium
                                                }

                                                Text(
                                                    text = line.text.ifBlank { " " },
                                                    color = fontColor,
                                                    fontSize = fontSize,
                                                    fontWeight = fontWeight,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .let {
                                                            if (lyricsData.isSynced && line.text.isNotBlank() && durationMs > 0) {
                                                                it.clickable {
                                                                    onSeek(line.timestampMs)
                                                                }
                                                            } else {
                                                                it
                                                            }
                                                        }
                                                        .padding(vertical = 4.dp),
                                                    lineHeight = 32.sp
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

        // 3. More Options Menu Dialog
        if (showMoreMenu && song != null) {
            val isDownloaded = isSongDownloaded(song.id)
            val isDownloading = isSongDownloading(song.id)
            
            Dialog(onDismissRequest = { showMoreMenu = false }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = uiColors.background),
                    border = androidx.compose.foundation.BorderStroke(1.dp, uiColors.cardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Close button at top right
                        IconButton(
                            onClick = { showMoreMenu = false },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = uiColors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 28.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // "Save to Playlist" option
                            PopupOptionCard(
                                icon = Icons.Default.Add,
                                title = "Save to Playlist",
                                subtitle = "Add this song to your custom playlists",
                                onClick = {
                                    showMoreMenu = false
                                    showAddToPlaylistDialog = true
                                },
                                uiColors = uiColors,
                                accentColor = accentColor
                            )

                            // "Download/Cancel Download" option
                            val (downloadText, downloadSub, downloadIcon) = when {
                                isDownloading -> Triple("Cancel Download", "Stop downloading this song", Icons.Default.Close)
                                isDownloaded -> Triple("Delete Download", "Remove from offline storage", Icons.Default.Delete)
                                else -> Triple("Download Song", "Save offline for playback anywhere", Icons.Default.Download)
                            }
                            
                            PopupOptionCard(
                                icon = downloadIcon,
                                title = downloadText,
                                subtitle = downloadSub,
                                onClick = {
                                    showMoreMenu = false
                                    onDownloadClick(song)
                                },
                                uiColors = uiColors,
                                accentColor = accentColor
                            )

                            // "Equaliser" option
                            PopupOptionCard(
                                icon = Icons.Default.Equalizer,
                                title = "Equaliser",
                                subtitle = "Adjust frequencies and settings",
                                onClick = {
                                    showMoreMenu = false
                                    showEqualizerMenu = true
                                },
                                uiColors = uiColors,
                                accentColor = accentColor
                            )
                        }
                    }
                }
            }
        }

        // 4. Equaliser Dialog
        if (showEqualizerMenu && song != null) {
            Dialog(onDismissRequest = { showEqualizerMenu = false }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = uiColors.background),
                    border = androidx.compose.foundation.BorderStroke(1.dp, uiColors.cardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(530.dp) // Fixed height to prevent dialog window resize jitter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Header
                        Text(
                            text = "Equaliser",
                            color = uiColors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.TopStart)
                        )

                        // Close button at top right
                        IconButton(
                            onClick = { showEqualizerMenu = false },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = uiColors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 40.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Selector for Default and Custom
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Default Option Card
                                val isDefault = eqMode == "Default"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isDefault) accentColor else uiColors.cardBackground)
                                        .border(
                                            1.dp,
                                            if (isDefault) Color.Transparent else uiColors.cardBorder,
                                            RoundedCornerShape(14.dp)
                                        )
                                        .clickable {
                                            eqMode = "Default"
                                            prefs.edit().putString("eq_mode", "Default").apply()
                                            com.example.convenemusic.playback.PlaybackServiceConnector.onEqualizerChanged?.invoke()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Default",
                                        color = if (isDefault) Color.White else uiColors.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                // Custom Option Card
                                val isCustom = eqMode == "Custom"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isCustom) accentColor else uiColors.cardBackground)
                                        .border(
                                            1.dp,
                                            if (isCustom) Color.Transparent else uiColors.cardBorder,
                                            RoundedCornerShape(14.dp)
                                        )
                                        .clickable {
                                            eqMode = "Custom"
                                            prefs.edit().putString("eq_mode", "Custom").apply()
                                            com.example.convenemusic.playback.PlaybackServiceConnector.onEqualizerChanged?.invoke()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Custom",
                                        color = if (isCustom) Color.White else uiColors.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            // Content area with Crossfade for smooth switching
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                Crossfade(
                                    targetState = eqMode,
                                    animationSpec = tween(300),
                                    label = "EqualiserContentTransition"
                                ) { currentMode ->
                                    if (currentMode == "Default") {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState())
                                                .padding(vertical = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            // Glowing visualizer icon representation
                                            Box(
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .clip(CircleShape)
                                                    .background(accentColor.copy(alpha = 0.1f))
                                                    .border(2.dp, accentColor.copy(alpha = 0.3f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Equalizer,
                                                    contentDescription = null,
                                                    tint = accentColor,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                            
                                            Text(
                                                text = "Convene Signature Sound",
                                                color = uiColors.textPrimary,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            
                                            // On/Off Toggles
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                DefaultToggleRow(
                                                    title = "Bass Boost",
                                                    subtitle = "Signature low-end punch (100%)",
                                                    checked = eqDefaultBassEnabled,
                                                    onCheckedChange = { newValue ->
                                                        eqDefaultBassEnabled = newValue
                                                        prefs.edit().putBoolean("eq_default_bass_enabled", newValue).apply()
                                                        com.example.convenemusic.playback.PlaybackServiceConnector.onEqualizerChanged?.invoke()
                                                    },
                                                    uiColors = uiColors,
                                                    accentColor = accentColor
                                                )

                                                DefaultToggleRow(
                                                    title = "Audio Boost",
                                                    subtitle = "Enhanced clarity & loudness (+3 dB)",
                                                    checked = eqDefaultAudioBoostEnabled,
                                                    onCheckedChange = { newValue ->
                                                        eqDefaultAudioBoostEnabled = newValue
                                                        prefs.edit().putBoolean("eq_default_audio_boost_enabled", newValue).apply()
                                                        com.example.convenemusic.playback.PlaybackServiceConnector.onEqualizerChanged?.invoke()
                                                    },
                                                    uiColors = uiColors,
                                                    accentColor = accentColor
                                                )

                                                DefaultToggleRow(
                                                    title = "Surround Sound",
                                                    subtitle = "3D spatializer soundstage (100%)",
                                                    checked = eqDefaultSurroundEnabled,
                                                    onCheckedChange = { newValue ->
                                                        eqDefaultSurroundEnabled = newValue
                                                        prefs.edit().putBoolean("eq_default_surround_enabled", newValue).apply()
                                                        com.example.convenemusic.playback.PlaybackServiceConnector.onEqualizerChanged?.invoke()
                                                    },
                                                    uiColors = uiColors,
                                                    accentColor = accentColor
                                                )
                                            }
                                        }
                                    } else {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState()),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(
                                                text = "Custom Settings",
                                                color = uiColors.textSecondary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                            
                                            val bands = listOf(
                                                "60 Hz" to "Bass",
                                                "230 Hz" to "Mid-Bass",
                                                "910 Hz" to "Midrange",
                                                "4 kHz" to "High-Mid",
                                                "14 kHz" to "Treble"
                                            )
                                            
                                            bands.forEachIndexed { index, band ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.width(64.dp)) {
                                                        Text(
                                                            text = band.first,
                                                            color = uiColors.textPrimary,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = band.second,
                                                            color = uiColors.textSecondary,
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                    
                                                    Slider(
                                                        value = eqValues[index],
                                                        onValueChange = { newValue ->
                                                            val newList = eqValues.toMutableList()
                                                            newList[index] = newValue
                                                            eqValues = newList
                                                        },
                                                        onValueChangeFinished = {
                                                            eqValues.forEachIndexed { idx, valDb ->
                                                                prefs.edit().putFloat("eq_band_$idx", valDb).apply()
                                                            }
                                                            com.example.convenemusic.playback.PlaybackServiceConnector.onEqualizerChanged?.invoke()
                                                        },
                                                        valueRange = -15f..15f,
                                                        colors = SliderDefaults.colors(
                                                            thumbColor = accentColor,
                                                            activeTrackColor = accentColor,
                                                            inactiveTrackColor = uiColors.cardBorder,
                                                            activeTickColor = Color.Transparent,
                                                            inactiveTickColor = Color.Transparent
                                                        ),
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    
                                                    Text(
                                                        text = String.format("%+d dB", eqValues[index].toInt()),
                                                        color = uiColors.textPrimary,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.width(42.dp),
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                                                    )
                                                }
                                            }

                                            HorizontalDivider(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                color = uiColors.cardBorder
                                            )

                                            // Bass Boost Slider
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.width(64.dp)) {
                                                    Text(
                                                        text = "Bass Boost",
                                                        color = uiColors.textPrimary,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "Low-end",
                                                        color = uiColors.textSecondary,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                                
                                                Slider(
                                                    value = eqBassStrength,
                                                    onValueChange = { newValue ->
                                                        eqBassStrength = newValue
                                                    },
                                                    onValueChangeFinished = {
                                                        prefs.edit().putFloat("eq_bass_strength", eqBassStrength).apply()
                                                        com.example.convenemusic.playback.PlaybackServiceConnector.onEqualizerChanged?.invoke()
                                                    },
                                                    valueRange = 0f..1000f,
                                                    colors = SliderDefaults.colors(
                                                        thumbColor = accentColor,
                                                        activeTrackColor = accentColor,
                                                        inactiveTrackColor = uiColors.cardBorder,
                                                        activeTickColor = Color.Transparent,
                                                        inactiveTickColor = Color.Transparent
                                                    ),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                
                                                Text(
                                                    text = String.format("%d%%", (eqBassStrength / 10f).toInt()),
                                                    color = uiColors.textPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.width(42.dp),
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                                )
                                            }

                                            // Audio Boost Slider
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.width(64.dp)) {
                                                    Text(
                                                        text = "Audio Boost",
                                                        color = uiColors.textPrimary,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "Loudness",
                                                        color = uiColors.textSecondary,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                                
                                                Slider(
                                                    value = eqAudioBoostStrength,
                                                    onValueChange = { newValue ->
                                                        eqAudioBoostStrength = newValue
                                                    },
                                                    onValueChangeFinished = {
                                                        prefs.edit().putFloat("eq_audio_boost_strength", eqAudioBoostStrength).apply()
                                                        com.example.convenemusic.playback.PlaybackServiceConnector.onEqualizerChanged?.invoke()
                                                    },
                                                    valueRange = 0f..1000f,
                                                    colors = SliderDefaults.colors(
                                                        thumbColor = accentColor,
                                                        activeTrackColor = accentColor,
                                                        inactiveTrackColor = uiColors.cardBorder,
                                                        activeTickColor = Color.Transparent,
                                                        inactiveTickColor = Color.Transparent
                                                    ),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                
                                                Text(
                                                    text = String.format("%d%%", (eqAudioBoostStrength / 10f).toInt()),
                                                    color = uiColors.textPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.width(42.dp),
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                                )
                                            }

                                            // Surround Sound Slider
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.width(64.dp)) {
                                                    Text(
                                                        text = "Surround",
                                                        color = uiColors.textPrimary,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "3D Spatial",
                                                        color = uiColors.textSecondary,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                                
                                                Slider(
                                                    value = eqSurroundStrength,
                                                    onValueChange = { newValue ->
                                                        eqSurroundStrength = newValue
                                                    },
                                                    onValueChangeFinished = {
                                                        prefs.edit().putFloat("eq_surround_strength", eqSurroundStrength).apply()
                                                        com.example.convenemusic.playback.PlaybackServiceConnector.onEqualizerChanged?.invoke()
                                                    },
                                                    valueRange = 0f..1000f,
                                                    colors = SliderDefaults.colors(
                                                        thumbColor = accentColor,
                                                        activeTrackColor = accentColor,
                                                        inactiveTrackColor = uiColors.cardBorder,
                                                        activeTickColor = Color.Transparent,
                                                        inactiveTickColor = Color.Transparent
                                                    ),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                
                                                Text(
                                                    text = String.format("%d%%", (eqSurroundStrength / 10f).toInt()),
                                                    color = uiColors.textPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.width(42.dp),
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
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
}

@Composable
fun PopupOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    uiColors: UIColors,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(uiColors.cardBackground)
            .border(1.dp, uiColors.cardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = uiColors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = uiColors.textSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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

@Composable
fun QueueSongCard(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    uiColors: UIColors,
    accentColor: Color,
    onPlayPauseClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isCurrent) uiColors.cardBorder.copy(alpha = 0.2f) else uiColors.cardBackground,
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = if (isCurrent) accentColor else uiColors.cardBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
        ) {
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = "Thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (isCurrent && isPlaying) {
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
                text = song.title,
                color = if (isCurrent) accentColor else uiColors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (!song.album.isNullOrBlank())
                    "${song.artist} • ${song.album}"
                else
                    song.artist,
                color = uiColors.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isCurrent && onPlayPauseClick != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor, shape = CircleShape)
                    .clickable { onPlayPauseClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
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

@Composable
fun DefaultToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    uiColors: UIColors,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(uiColors.cardBackground)
            .border(1.dp, uiColors.cardBorder, RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = title,
                color = uiColors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = uiColors.textSecondary,
                fontSize = 10.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = uiColors.textSecondary,
                uncheckedTrackColor = uiColors.cardBorder,
                uncheckedBorderColor = Color.Transparent
            ),
            modifier = Modifier.graphicsLayer(scaleX = 0.8f, scaleY = 0.8f)
        )
    }
}

@Composable
fun SignatureBadge(text: String, uiColors: UIColors) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(uiColors.cardBackground)
            .border(1.dp, uiColors.cardBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = uiColors.textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
