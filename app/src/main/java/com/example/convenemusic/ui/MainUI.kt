package com.example.convenemusic.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Movie
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.VideoView
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import com.example.convenemusic.ui.components.PullToRefreshContainer
import com.example.convenemusic.ui.Movie
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import coil3.asDrawable
import com.example.convenemusic.ui.components.BottomBarTabs
import com.example.convenemusic.ui.components.MusicTab
import com.example.convenemusic.ui.theme.UIColors
import com.example.convenemusic.ui.theme.LocalUIColors
import com.example.convenemusic.network.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import dev.chrisbanes.haze.rememberHazeState
import com.example.convenemusic.network.Playlist
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

@OptIn(ExperimentalHazeMaterialsApi::class, ExperimentalHazeApi::class)
@Composable
fun MainUI(
    viewModel: MusicViewModel
) {
    val uiColors = LocalUIColors.current
    val focusManager = LocalFocusManager.current
    val hazeState = rememberHazeState()

    // Hoisted search state to keep it alive across tab switches
    var searchQuery by remember { mutableStateOf("") }
    var searchSegment by remember { mutableStateOf("Songs") }

    val currentTab = NavigationController.currentTab
    val activePlaylist = NavigationController.activePlaylist
    val isPlayerOpen = NavigationController.isPlayerOpen
    val isVideoPlayerOpen = NavigationController.isVideoPlayerOpen

    fun performBack() {
        focusManager.clearFocus()
        if (NavigationController.history.size > 1) {
            NavigationController.goBack()
        } else if (searchQuery.isNotEmpty()) {
            searchQuery = ""
        }
    }

    // Intercept back presses dynamically to pop history
    BackHandler(enabled = NavigationController.history.size > 1 || searchQuery.isNotEmpty()) {
        performBack()
    }

    val searchResults by viewModel.searchResults.collectAsState()
    val playlistResults by viewModel.playlistResults.collectAsState()
    val playlistTracks by viewModel.playlistTracks.collectAsState()
    val artistTracks by viewModel.artistTracks.collectAsState()
    val isArtistLoading by viewModel.isArtistLoading.collectAsState()
    val genreTracks by viewModel.genreTracks.collectAsState()
    val isGenreLoading by viewModel.isGenreLoading.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isPlaylistLoading by viewModel.isPlaylistLoading.collectAsState()
    val isLoadMoreLoading by viewModel.isLoadMoreLoading.collectAsState()
    val isStreamLoading by viewModel.isStreamLoading.collectAsState()

    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val positionMs by viewModel.currentPosition.collectAsState()
    val durationMs by viewModel.duration.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val currentQueueIndex by viewModel.currentQueueIndex.collectAsState()
    val historySongs by viewModel.historyList.collectAsState()
    val downloadedSongs by viewModel.downloadedList.collectAsState()
    val artistThumbnails by viewModel.artistThumbnails.collectAsState()
    val downloadingSongs by viewModel.downloadingSongs.collectAsState()
    val downloadingQueue by viewModel.downloadingQueue.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val localTracks by viewModel.localTracks.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val isQueueLoadingMore by viewModel.isQueueLoadingMore.collectAsState()
    val isQueueEndless by viewModel.isQueueEndless.collectAsState()
    val localVideos by viewModel.localVideos.collectAsState()
    val isVideoScanning by viewModel.isVideoScanning.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .hazeSource(hazeState)
            .background(uiColors.background)
    ) {
            val nonPlayerDest = NavigationController.history.lastOrNull { it !is Destination.Player && it !is Destination.VideoPlayer } ?: Destination.Home
            when (nonPlayerDest) {
                is Destination.Home -> {
                    HomeScreen(
                        song = currentSong,
                        isPlaying = isPlaying,
                        positionMs = positionMs,
                        durationMs = durationMs,
                        onPlayPauseClick = { viewModel.togglePlayPause() },
                        onNextClick = { viewModel.playNextSong() },
                        onPreviousClick = { viewModel.playPreviousSong() },
                        onVinylClick = { NavigationController.navigateTo(Destination.Player) },
                        uiColors = uiColors
                    )
                }

                is Destination.SearchList,
                is Destination.PlaylistDetail -> {
                    SearchScreen(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        selectedSegment = searchSegment,
                        onSegmentChange = { searchSegment = it },
                        activePlaylist = activePlaylist,
                        onActivePlaylistChange = { playlist ->
                            if (playlist != null) {
                                NavigationController.navigateTo(Destination.PlaylistDetail(playlist))
                            } else {
                                NavigationController.navigateTo(Destination.SearchList)
                            }
                        },
                        onSongSelect = { song ->
                            val isPremade = activePlaylist != null
                            val initialQueue = if (isPremade) {
                                playlistTracks
                            } else if (artistTracks.isNotEmpty() && artistTracks.any { it.id == song.id }) {
                                artistTracks
                            } else if (genreTracks.isNotEmpty() && genreTracks.any { it.id == song.id }) {
                                genreTracks
                            } else {
                                emptyList()
                            }
                            viewModel.playSong(song, initialQueue, isPremadePlaylist = isPremade)
                            NavigationController.navigateTo(Destination.Player)
                        },
                        onPlayPlaylist = { playlist ->
                            viewModel.playPlaylist(playlist)
                            NavigationController.navigateTo(Destination.Player)
                        },
                        searchSongs = searchResults,
                        playlistResults = playlistResults,
                        playlistTracks = playlistTracks,
                        artistTracks = artistTracks,
                        isArtistLoading = isArtistLoading,
                        historySongs = historySongs,
                        downloadedSongs = downloadedSongs,
                        onPerformSearch = { viewModel.search(it) },
                        onPerformPlaylistSearch = { viewModel.searchPlaylists(it) },
                        onLoadPlaylistTracks = { viewModel.loadPlaylistTracks(it) },
                        onClearPlaylistTracks = { viewModel.clearPlaylistTracks() },
                        onLoadArtistTracks = { viewModel.loadArtistTracks(it) },
                        onClearArtistTracks = { viewModel.clearArtistTracks() },
                        onLoadMoreArtistTracks = { viewModel.loadMoreArtistTracks(it) },
                        onLoadMore = {
                            if (searchSegment == "Songs") {
                                viewModel.loadMoreSongs()
                            } else {
                                viewModel.loadMorePlaylists()
                            }
                        },
                        isLoading = isLoading,
                        isPlaylistLoading = isPlaylistLoading,
                        isLoadMoreLoading = isLoadMoreLoading,
                        isPlayerOpen = isPlayerOpen,
                        artistThumbnails = artistThumbnails,
                        genreTracks = genreTracks,
                        isGenreLoading = isGenreLoading,
                        onLoadGenreTracks = { viewModel.loadGenreTracks(it) },
                        onClearGenreTracks = { viewModel.clearGenreTracks() },
                        onLoadMoreGenreTracks = { viewModel.loadMoreGenreTracks(it) },
                    )
                }

                is Destination.Library -> {
                    LibraryScreen(
                        historySongs = historySongs,
                        downloadedSongs = downloadedSongs,
                        downloadingQueue = downloadingQueue,
                        downloadProgress = downloadProgress,
                        localTracks = localTracks,
                        isScanning = isScanning,
                        localVideos = localVideos,
                        isVideoScanning = isVideoScanning,
                        onScanLocalFiles = { viewModel.scanLocalFiles() },
                        onScanLocalVideos = { viewModel.scanLocalVideos() },
                        onClearLocalTracks = { viewModel.clearLocalTracks() },
                        onClearLocalVideos = { viewModel.clearLocalVideos() },
                        onSongSelect = { song ->
                            viewModel.playSong(song)
                            NavigationController.navigateTo(Destination.Player)
                        },
                        onPauseBackgroundMusic = { viewModel.playbackManager.pause() },
                        onPlayMovie = { movie ->
                            NavigationController.navigateTo(Destination.VideoPlayer(movie))
                        },
                        uiColors = uiColors,
                        isPlayerOpen = isPlayerOpen
                    )
                }

                else -> {}
            }
            AnimatedVisibility(
                visible = isPlayerOpen,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
                ),
                modifier = Modifier.fillMaxSize()
            ) {
                PlayerScreen(
                    song = currentSong,
                    isPlaying = isPlaying,
                    isStreamLoading = isStreamLoading,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    onPlayPauseClick = {
                        viewModel.togglePlayPause()
                    },
                    onSeek = {
                        viewModel.seekTo(it)
                    },
                    onBack = {
                        performBack()
                    },
                    queue = queue,
                    currentQueueIndex = currentQueueIndex,
                    hazeState = hazeState,
                    onQueueSongClick = { viewModel.playQueueSong(it) },
                    onNextClick = { viewModel.playNextSong() },
                    onPreviousClick = { viewModel.playPreviousSong() },
                    onLoadMore = { viewModel.generateAutoplaySongs() },
                    isSongDownloaded = { viewModel.isSongDownloaded(it) },
                    isSongDownloading = { viewModel.isSongDownloading(it) },
                    onDownloadClick = { viewModel.downloadSong(it) },
                    isQueueLoadingMore = isQueueLoadingMore,
                    isQueueEndless = isQueueEndless
                )
            }

            AnimatedVisibility(
                visible = isVideoPlayerOpen,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
                ),
                modifier = Modifier.fillMaxSize()
            ) {
                var activeMovie by remember { mutableStateOf<Movie?>(null) }
                val currentDest = NavigationController.currentDestination
                if (currentDest is Destination.VideoPlayer) {
                    activeMovie = currentDest.movie
                }
                activeMovie?.let { movie ->
                    VideoPlayerScreen(
                        movie = movie,
                        onClose = { NavigationController.goBack() },
                        uiColors = uiColors
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 28.dp
                    )
                    .fillMaxWidth()
            ) {
                AnimatedVisibility(
                    visible = currentSong != null && !isPlayerOpen && !isVideoPlayerOpen && nonPlayerDest != Destination.Home,
                    enter = slideInVertically(
                        initialOffsetY = { it * 4 },
                        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { it * 4 },
                        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp)
                ) {
                    val song = currentSong
                    if (song != null) {
                        var rotationAngle by remember { mutableStateOf(0f) }
                        LaunchedEffect(isPlaying) {
                            if (isPlaying) {
                                val startTime = System.currentTimeMillis()
                                val startAngle = rotationAngle
                                while (true) {
                                    val elapsed = System.currentTimeMillis() - startTime
                                    rotationAngle = (startAngle + (elapsed.toFloat() / 6000f) * 360f) % 360f
                                    kotlinx.coroutines.delay(16)
                                }
                            }
                        }

                        val cleanTitle = remember(song.title) {
                            song.title.replace(Regex("\\s*[\\(\\[].*?[\\)\\]]"), "").trim()
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth(0.65f)
                                .height(46.dp)
                                .hazeEffect(
                                    state = hazeState,
                                    style = HazeMaterials.ultraThin()
                                ) {
                                    blurRadius = 4.dp
                                    alpha = 0.8f
                                }
                                .background(
                                    uiColors.cardBackground.copy(alpha = 0.75f),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .border(1.dp, uiColors.cardBorder, RoundedCornerShape(24.dp))
                                .clip(RoundedCornerShape(24.dp))
                                .clickable {
                                    NavigationController.navigateTo(Destination.Player)
                                }
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = song.thumbnailUrl,
                                contentDescription = "Mini Art",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(32.dp)
                                    .rotate(rotationAngle)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = cleanTitle,
                                color = uiColors.textPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.togglePlayPause() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = uiColors.textPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Glassmorphic bar shell containing the canvas radial indicator
                AnimatedVisibility(
                    visible = !isPlayerOpen && !isVideoPlayerOpen,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(durationMillis = 300)
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(durationMillis = 300)
                    ),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(68.dp)
                            .hazeEffect(
                                state = hazeState,
                                style = HazeMaterials.ultraThin()
                            ) {
                                blurRadius = 4.dp
                                noiseFactor = 0.01f
                                inputScale = HazeInputScale.Auto
                                alpha = 0.85f
                            }
                            .clip(CircleShape)
                            .background(
                                uiColors.bottomBarColor
                            )
                            .border(
                                width = Dp.Hairline,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        uiColors.cardBorder.copy(alpha = 0.6f),
                                        uiColors.cardBorder.copy(alpha = 0.15f),
                                    )
                                ),
                                shape = CircleShape
                            )
                    ) {
                        // Custom active tab border outline and radial glow canvas matching KITO

                        val animatedSelectedTabIndex by animateFloatAsState(
                            targetValue = currentTab.ordinal.toFloat(),
                            label = "animatedSelectedTabIndex",
                            animationSpec = spring(
                                stiffness = Spring.StiffnessLow,
                                dampingRatio = Spring.DampingRatioLowBouncy,
                            )
                        )
                        val animatedColor by animateColorAsState(
                            targetValue = currentTab.activeColor,
                            label = "animatedColor",
                            animationSpec = spring(stiffness = Spring.StiffnessLow)
                        )

                        Canvas(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val tabsCount = MusicTab.entries.size
                            val tabWidth = size.width / tabsCount
                            val centerOffset = tabWidth * animatedSelectedTabIndex + tabWidth / 2

                            // 1. Radial active background glow
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        animatedColor.copy(alpha = 0.2f),
                                        Color.Transparent
                                    ),
                                    center = Offset(centerOffset, size.height * 0.5f),
                                    radius = tabWidth * 0.7f
                                ),
                                radius = tabWidth * 0.7f,
                                center = Offset(centerOffset, size.height * 0.5f)
                            )

                            // 2. Linear highlighted border path matching active tab
                            val path = Path().apply {
                                addRoundRect(
                                    RoundRect(
                                        size.toRect(),
                                        CornerRadius(size.height / 2f)
                                    )
                                )
                            }
                            drawPath(
                                path = path,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        animatedColor.copy(alpha = 0.4f),
                                        animatedColor,
                                        animatedColor.copy(alpha = 0.4f),
                                        Color.Transparent,
                                    ),
                                    startX = centerOffset - (tabWidth * 0.6f),
                                    endX = centerOffset + (tabWidth * 0.6f),
                                ),
                                style = Stroke(width = 2f)
                            )
                        }

                        BottomBarTabs(
                            selectedTab = currentTab,
                            onTabSelected = { tab ->
                                if (
                                    tab == MusicTab.Search &&
                                    NavigationController.history.lastOrNull() is Destination.PlaylistDetail
                                ) {
                                    NavigationController.navigateTo(Destination.SearchList)
                                } else {
                                    val dest = when (tab) {
                                        MusicTab.Home -> Destination.Home

                                        MusicTab.Search -> {
                                            val activePl = NavigationController.activePlaylist
                                            if (activePl != null)
                                                Destination.PlaylistDetail(activePl)
                                            else
                                                Destination.SearchList
                                        }

                                        MusicTab.Library -> Destination.Library
                                    }
                                    NavigationController.navigateTo(dest)
                                }
                            }
                        )
                    }
                }
            }
        }
    }


@Composable
fun LibraryScreen(
    historySongs: List<Song>,
    downloadedSongs: List<Song>,
    downloadingQueue: List<Song>,
    downloadProgress: Map<String, Int>,
    localTracks: List<Song>,
    isScanning: Boolean,
    localVideos: List<Movie>,
    isVideoScanning: Boolean,
    onScanLocalFiles: () -> Unit,
    onScanLocalVideos: () -> Unit,
    onClearLocalTracks: () -> Unit,
    onClearLocalVideos: () -> Unit,
    onSongSelect: (Song) -> Unit,
    onPauseBackgroundMusic: () -> Unit,
    onPlayMovie: (Movie) -> Unit,
    uiColors: UIColors,
    isPlayerOpen: Boolean,
    modifier: Modifier = Modifier
) {
    var librarySection by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(librarySection) {
        if (librarySection != "Local") {
            onClearLocalTracks()
        }
        if (librarySection != "Movies") {
            onClearLocalVideos()
        }
    }

    val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val transitionProgress = remember { Animatable(0f) }
    var activeSection by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(librarySection) {
        if (librarySection != null) {
            activeSection = librarySection
            transitionProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = EmphasizedEasing)
            )
        } else {
            transitionProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 400, easing = EmphasizedEasing)
            )
            activeSection = null
        }
    }

    PredictiveBackHandler(enabled = librarySection != null) { progressFlow ->
        try {
            progressFlow.collect { backEvent ->
                transitionProgress.snapTo(1f - backEvent.progress)
            }
            transitionProgress.animateTo(
                targetValue = 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            )
            librarySection = null
            activeSection = null
        } catch (e: CancellationException) {
            transitionProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            )
        }
    }

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val density = LocalDensity.current

    Box(modifier = modifier.fillMaxSize()) {
        if (transitionProgress.value < 1f || activeSection == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .offset {
                        IntOffset(
                            x = (-(screenWidth.value * density.density * 0.3f) * transitionProgress.value).toInt(),
                            y = 0
                        )
                    }
                    .background(uiColors.background)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Your Library",
                    color = uiColors.textPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. History Card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(uiColors.cardBackground, shape = RoundedCornerShape(20.dp))
                            .border(1.dp, uiColors.cardBorder, RoundedCornerShape(20.dp))
                            .clickable { librarySection = "History" }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(uiColors.cardBorder.copy(alpha = 0.4f), shape = RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "History",
                                tint = uiColors.progressAccent,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "History",
                                color = uiColors.textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${historySongs.size} songs listened",
                                color = uiColors.textSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // 2. Downloads Card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(uiColors.cardBackground, shape = RoundedCornerShape(20.dp))
                            .border(1.dp, uiColors.cardBorder, RoundedCornerShape(20.dp))
                            .clickable { librarySection = "Downloads" }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(uiColors.cardBorder.copy(alpha = 0.4f), shape = RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Downloads",
                                tint = uiColors.progressAccent,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Downloads",
                                color = uiColors.textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val subText = if (downloadingQueue.isNotEmpty()) {
                                "${downloadedSongs.size} songs offline • ${downloadingQueue.size} downloading"
                            } else {
                                "${downloadedSongs.size} songs offline"
                            }
                            Text(
                                text = subText,
                                color = uiColors.textSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // 3. Local Card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(uiColors.cardBackground, shape = RoundedCornerShape(20.dp))
                            .border(1.dp, uiColors.cardBorder, RoundedCornerShape(20.dp))
                            .clickable { librarySection = "Local" }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(uiColors.cardBorder.copy(alpha = 0.4f), shape = RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Local",
                                tint = uiColors.progressAccent,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Local",
                                color = uiColors.textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (localTracks.isNotEmpty()) "${localTracks.size} files found" else "Scan your device",
                                color = uiColors.textSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // 4. Movies Card (Local video scanner)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(uiColors.cardBackground, shape = RoundedCornerShape(20.dp))
                            .border(1.dp, uiColors.cardBorder, RoundedCornerShape(20.dp))
                            .clickable { librarySection = "Movies" }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(uiColors.cardBorder.copy(alpha = 0.4f), shape = RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = "Movies",
                                tint = uiColors.progressAccent,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Movies",
                                color = uiColors.textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (localVideos.isNotEmpty()) "${localVideos.size} videos found" else "Scan local videos",
                                color = uiColors.textSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        if (activeSection != null) {
            val currentSection = activeSection!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .offset {
                        IntOffset(
                            x = (screenWidth.value * density.density * (1f - transitionProgress.value)).toInt(),
                            y = 0
                        )
                    }
                    .shadow(8.dp, clip = false)
                    .background(uiColors.background)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp)
            ) {
                // Details Sub-Panel for History, Downloads, Local, or Movies
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(uiColors.cardBackground, shape = CircleShape)
                            .border(1.dp, uiColors.cardBorder, CircleShape)
                            .clickable { librarySection = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = "Back",
                            tint = uiColors.textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = currentSection,
                        color = uiColors.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (currentSection == "History") {
                    if (historySongs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No songs here yet",
                                color = uiColors.textSecondary,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        PullToRefreshContainer(
                            isRefreshing = false,
                            onRefresh = {},
                            accentColor = uiColors.progressAccent,
                            backgroundColor = uiColors.cardBackground,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .fillMaxHeight(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(bottom = 100.dp)
                                ) {
                                    items(historySongs) { song ->
                                        TrackStandingCard(
                                            song = song,
                                            onClick = { onSongSelect(song) },
                                            uiColors = uiColors
                                        )
                                    }
                                    item {
                                        Spacer(modifier = Modifier.height(80.dp))
                                    }
                                }
                            }
                        }
                    }
                } else if (currentSection == "Local") {
                    // Local state to delay the scan and show the loading animation
                    var isWaitingToScan by remember { mutableStateOf(true) }

                    LaunchedEffect(Unit) {
                        delay(2000)
                        isWaitingToScan = false
                        if (!isScanning && localTracks.isEmpty()) onScanLocalFiles()
                    }

                    if (isWaitingToScan || isScanning) {
                        // Sonar scanning animation
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            SonarScanAnimation(accentColor = uiColors.progressAccent)
                        }
                    } else if (localTracks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "No audio files found",
                                    color = uiColors.textSecondary,
                                    fontSize = 14.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(uiColors.progressAccent.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                        .clickable { onScanLocalFiles() }
                                        .padding(horizontal = 20.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "Scan Again",
                                        color = uiColors.progressAccent,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    } else {
                        PullToRefreshContainer(
                            isRefreshing = isScanning,
                            onRefresh = onScanLocalFiles,
                            accentColor = uiColors.progressAccent,
                            backgroundColor = uiColors.cardBackground,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .fillMaxHeight(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(bottom = 100.dp)
                                ) {
                                    items(localTracks) { song ->
                                        TrackStandingCard(
                                            song = song,
                                            onClick = { onSongSelect(song) },
                                            uiColors = uiColors
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (currentSection == "Downloads") {
                    if (downloadedSongs.isEmpty() && downloadingQueue.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No songs here yet",
                                color = uiColors.textSecondary,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        PullToRefreshContainer(
                            isRefreshing = false,
                            onRefresh = {},
                            accentColor = uiColors.progressAccent,
                            backgroundColor = uiColors.cardBackground,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .fillMaxHeight(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(bottom = 100.dp)
                                ) {
                                    items(downloadingQueue) { song ->
                                        val progress = downloadProgress[song.id] ?: 0
                                        DownloadingTrackCard(
                                            song = song,
                                            progress = progress,
                                            uiColors = uiColors
                                        )
                                    }
                                    items(downloadedSongs) { song ->
                                        TrackStandingCard(
                                            song = song,
                                            onClick = { onSongSelect(song) },
                                            uiColors = uiColors
                                        )
                                    }
                                    item { Spacer(modifier = Modifier.height(80.dp)) }
                                }
                            }
                        }
                    }
                } else if (currentSection == "Movies") {
                    var isWaitingToScan by remember { mutableStateOf(true) }

                    LaunchedEffect(Unit) {
                        delay(2000)
                        isWaitingToScan = false
                        if (!isVideoScanning && localVideos.isEmpty()) onScanLocalVideos()
                    }

                    if (isWaitingToScan || isVideoScanning) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            SonarScanAnimation(accentColor = uiColors.progressAccent)
                        }
                    } else if (localVideos.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "No video files found",
                                    color = uiColors.textSecondary,
                                    fontSize = 14.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(uiColors.progressAccent.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                        .clickable { onScanLocalVideos() }
                                        .padding(horizontal = 20.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "Scan Again",
                                        color = uiColors.progressAccent,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    } else {
                        PullToRefreshContainer(
                            isRefreshing = isVideoScanning,
                            onRefresh = onScanLocalVideos,
                            accentColor = uiColors.progressAccent,
                            backgroundColor = uiColors.cardBackground,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .fillMaxHeight(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(bottom = 100.dp)
                                ) {
                                    items(localVideos) { movie ->
                                        MovieTrackCard(
                                            movie = movie,
                                            onClick = {
                                                onPauseBackgroundMusic()
                                                onPlayMovie(movie)
                                            },
                                            uiColors = uiColors
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

@Composable
fun SonarScanAnimation(accentColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "sonar")
    // Three staggered rings
    val wave1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "w1"
    )
    val wave2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, 600, easing = LinearEasing), RepeatMode.Restart),
        label = "w2"
    )
    val wave3 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, 1200, easing = LinearEasing), RepeatMode.Restart),
        label = "w3"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(200.dp)
        ) {
            // Pulsing rings
            listOf(wave1, wave2, wave3).forEach { progress ->
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = accentColor.copy(alpha = (1f - progress) * 0.5f),
                        radius = size.minDimension / 2f * progress
                    )
                }
            }
            // Center dot with subtle pulse
            val pulse by infiniteTransition.animateFloat(
                initialValue = 0.85f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                label = "pulse"
            )
            Box(
                modifier = Modifier
                    .size(52.dp * pulse)
                    .background(accentColor.copy(alpha = 0.15f), CircleShape)
                    .border(1.5.dp, accentColor.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Text(
            text = "Scanning device…",
            color = accentColor.copy(alpha = 0.8f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
@Composable
fun DownloadingTrackCard(
    song: Song,
    progress: Int,
    uiColors: UIColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(uiColors.cardBackground, shape = RoundedCornerShape(20.dp))
            .border(1.dp, uiColors.cardBorder, RoundedCornerShape(20.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = "Thumbnail",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                color = uiColors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Downloading...",
                    color = uiColors.progressAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$progress%",
                    color = uiColors.textSecondary,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress.toFloat() / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = uiColors.progressAccent,
                trackColor = uiColors.cardBorder
            )
        }
    }
}

@Composable
fun HomeScreen(
    song: Song?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onVinylClick: () -> Unit,
    uiColors: UIColors,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var albumAccentColor by remember(song?.id) { mutableStateOf<Color?>(null) }
    val accentColor = albumAccentColor ?: uiColors.progressAccent

    // Dynamic rotation angle
    var rotationAngle by remember { mutableStateOf(0f) }
    LaunchedEffect(isPlaying, song?.id) {
        if (isPlaying && song != null) {
            val startTime = System.currentTimeMillis()
            val startAngle = rotationAngle
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                rotationAngle = (startAngle + (elapsed.toFloat() / 8000f) * 360f) % 360f
                kotlinx.coroutines.delay(16)
            }
        }
    }

    var isEntered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isEntered = true
    }

    var currentTime by remember { mutableStateOf(java.util.Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = java.util.Calendar.getInstance()
            delay(1000)
        }
    }
    
    val sdfHour = remember { java.text.SimpleDateFormat("HH", java.util.Locale.getDefault()) }
    val sdfMin = remember { java.text.SimpleDateFormat("mm", java.util.Locale.getDefault()) }
    val hourStr = sdfHour.format(currentTime.time)
    val minStr = sdfMin.format(currentTime.time)

    val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
    val cardWidth = screenWidth - 48.dp
    val coverSize = (cardWidth - 40.dp) / 1.52f
    val cardHeight = coverSize
    val diskSize = coverSize * 0.94f

    val slideOffset by animateDpAsState(
        targetValue = if (isEntered) {
            if (song != null && isPlaying) {
                -(coverSize * 0.52f)
            } else {
                -(coverSize * 0.22f)
            }
        } else {
            0.dp
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "vinylSlideOffset"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Top Bar: Logo & CONVENE, Hello/User & Clock
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            // Top line: Launcher Icon & CONVENE
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = com.example.convenemusic.R.mipmap.ic_launcher_foreground),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "CONVENE",
                    color = uiColors.textPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Second line: Hello/User on left, Clock on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hello",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = "User",
                        color = uiColors.progressAccent,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                
                // Clock in XX:XX format on the right side of the same line (spacier & modern monospace font)
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = hourStr,
                        color = Color.White,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = ":",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = minStr,
                        color = accentColor,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dark grey filled card container Box (with dynamic themed glow and responsive layout)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight)
                .dropShadow(
                    shape = RoundedCornerShape(24.dp),
                    shadow = Shadow(
                        radius = 24.dp,
                        color = accentColor
                    )
                )
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1E1E24))
                .border(1.dp, Color(0xFF2E2E38), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Vertical Progress Bar at the left portion of the vinyl grid card
            if (song != null) {
                val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 20.dp)
                        .width(4.dp)
                        .height(cardHeight * 0.66f)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = size.width
                        val progressHeight = size.height * progress
                        // Draw background track
                        drawLine(
                            color = Color.White.copy(alpha = 0.1f),
                            start = Offset(size.width / 2f, 0f),
                            end = Offset(size.width / 2f, size.height),
                            strokeWidth = strokeWidth,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        // Draw active progress track (bottom-to-top)
                        if (progressHeight > 0f) {
                            drawLine(
                                color = accentColor,
                                start = Offset(size.width / 2f, size.height),
                                end = Offset(size.width / 2f, size.height - progressHeight),
                                strokeWidth = strokeWidth,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                    }
                }
            }
            // Enlarged Vinyl Player centered inside the card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onVinylClick() },
                contentAlignment = Alignment.CenterEnd
            ) {
                // The Vinyl Record Disk (renders behind the cover, offset to the left)
                Box(
                    modifier = Modifier
                        .size(diskSize)
                        .offset(x = slideOffset)
                        .rotate(rotationAngle)
                        .clip(CircleShape)
                        .background(Color(0xFF151515))
                        .border(1.dp, Color.Black.copy(alpha = 0.8f), CircleShape)
                        .align(Alignment.CenterEnd),
                    contentAlignment = Alignment.Center
                ) {
                    // Groove rings & Light reflections
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val maxRadius = size.minDimension / 2f
                        
                        // Groove rings
                        val grooveRadii = listOf(0.9f, 0.82f, 0.74f, 0.66f, 0.58f, 0.5f)
                        grooveRadii.forEach { factor ->
                            drawCircle(
                                color = Color.White.copy(alpha = 0.08f),
                                radius = maxRadius * factor,
                                center = center,
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }

                        // Conical light reflection sheens (wedges) that rotate with the disk
                        drawArc(
                            color = Color.White.copy(alpha = 0.07f),
                            startAngle = -50f,
                            sweepAngle = 35f,
                            useCenter = true
                        )
                        drawArc(
                            color = Color.White.copy(alpha = 0.07f),
                            startAngle = 130f,
                            sweepAngle = 35f,
                            useCenter = true
                        )
                    }

                    // Themed Center Label
                    Box(
                        modifier = Modifier
                            .size(diskSize * 0.32f)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = 0.95f),
                                        accentColor.copy(alpha = 0.5f),
                                        Color(0xFF1F1F1F)
                                    )
                                )
                            )
                            .border(1.5.dp, Color.Black, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.size(20.dp)
                        )
                        
                        // Center spindle hole
                        Box(
                            modifier = Modifier
                                .size(diskSize * 0.06f)
                                .clip(CircleShape)
                                .background(uiColors.background)
                                .border(1.2.dp, Color.Black.copy(alpha = 0.5f), CircleShape)
                        )
                    }
                }

                // The Disk Cover (renders on top on the right side)
                Box(
                    modifier = Modifier
                        .size(coverSize)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E1E24))
                        .border(1.dp, Color(0xFF2E2E38).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .align(Alignment.CenterEnd)
                ) {
                    // Thumbnail background
                    if (song != null) {
                        AsyncImage(
                            model = song.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            onSuccess = { state ->
                                val drawable = state.result.image.asDrawable(context.resources)
                                val bitmap = (drawable as? BitmapDrawable)?.bitmap
                                if (bitmap != null) {
                                    albumAccentColor = extractAccentColor(bitmap, uiColors.progressAccent)
                                }
                            }
                        )
                    } else {
                        // Fallback default background when nothing is playing
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            accentColor,
                                            accentColor.copy(alpha = 0.8f)
                                        )
                                    )
                                )
                        )
                    }

                    // Gradient overlay to make text highly legible
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.25f),
                                        Color.Black.copy(alpha = 0.75f)
                                    )
                                )
                            )
                    )

                    // Content details & Playback controls
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Song Info
                        Column {
                            Text(
                                text = song?.title ?: "No Track Selected",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            Text(
                                text = song?.artist?.replace(Regex("\\s*•\\s*\\d+:\\d+\\s*$"), "") ?: "Select a song to play",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Playback controls row inside the cover card (Enlarged)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Previous Button
                            IconButton(
                                onClick = { onPreviousClick() },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Play/Pause Button
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = if (song != null) 1f else 0.4f), shape = CircleShape)
                                    .clickable(enabled = song != null) { onPlayPauseClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying && song != null) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = if (song != null) accentColor else Color.Gray,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Next Button
                            IconButton(
                                onClick = { onNextClick() },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MovieTrackCard(
    movie: Movie,
    onClick: () -> Unit,
    uiColors: UIColors
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
                .size(56.dp)
                .background(uiColors.cardBorder.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Movie,
                contentDescription = null,
                tint = uiColors.progressAccent,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = movie.title,
                color = uiColors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${movie.durationText} • ${movie.sizeText}",
                color = uiColors.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Play",
            tint = uiColors.textSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun VideoPlayerScreen(
    movie: Movie,
    onClose: () -> Unit,
    uiColors: UIColors
) {
    var isPlaying by remember { mutableStateOf(true) }
    var currentPos by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(1L) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            videoViewRef?.let {
                currentPos = it.currentPosition.toLong()
                val dur = it.duration.toLong()
                if (dur > 0) duration = dur
            }
            delay(500)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoPath(movie.path)
                    setOnPreparedListener { mp ->
                        duration = mp.duration.toLong()
                        mp.start()
                        isPlaying = true
                    }
                    setOnCompletionListener {
                        isPlaying = false
                        currentPos = duration
                    }
                    videoViewRef = this
                }
            },
            update = { view ->
                videoViewRef = view
            }
        )

        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = movie.title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Center Click Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {
                    videoViewRef?.let {
                        if (it.isPlaying) {
                            it.pause()
                            isPlaying = false
                        } else {
                            it.start()
                            isPlaying = true
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (!isPlaying) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }

        // Bottom Bar controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                    )
                )
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val elapsedMins = (currentPos / 1000) / 60
                val elapsedSecs = (currentPos / 1000) % 60
                Text(
                    text = "$elapsedMins:${elapsedSecs.toString().padStart(2, '0')}",
                    color = Color.White,
                    fontSize = 14.sp
                )

                Slider(
                    value = currentPos.toFloat(),
                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                    onValueChange = { newValue ->
                        currentPos = newValue.toLong()
                        videoViewRef?.seekTo(newValue.toInt())
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = uiColors.progressAccent,
                        activeTrackColor = uiColors.progressAccent,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )

                val totalMins = (duration / 1000) / 60
                val totalSecs = (duration / 1000) % 60
                Text(
                    text = "$totalMins:${totalSecs.toString().padStart(2, '0')}",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
}
