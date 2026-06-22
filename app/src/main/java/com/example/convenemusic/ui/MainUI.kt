package com.example.convenemusic.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.VideoView
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import com.example.convenemusic.ui.components.PullToRefreshContainer
import com.example.convenemusic.ui.Movie
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.basicMarquee
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.convenemusic.ui.components.RetroCassetteTape
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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.StrokeCap
import android.graphics.PathMeasure
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.animation.core.animate
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import com.example.convenemusic.ui.ArtistPhotoSlideshowBackground
import com.example.convenemusic.ui.getPlaylistSlideshowUrls

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
    var searchActiveArtist by remember { mutableStateOf<String?>(null) }
    var searchActiveGenre by remember { mutableStateOf<String?>(null) }
    var searchViewAllGenres by remember { mutableStateOf(false) }
    var searchViewAllArtists by remember { mutableStateOf(false) }
    var libraryActiveSection by remember { mutableStateOf<String?>(null) }
    var searchFocusTrigger by remember { mutableStateOf(false) }

    val currentTab = NavigationController.currentTab
    val activePlaylist = NavigationController.activePlaylist
    val isPlayerOpen = NavigationController.isPlayerOpen
    val isVideoPlayerOpen = NavigationController.isVideoPlayerOpen

    val showSplash by viewModel.showSplash.collectAsState()

    if (showSplash) {
        SplashHeartbeatScreen()
        return
    }

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
    val isBuffering by viewModel.isBuffering.collectAsState()

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
    val customPlaylists by viewModel.customPlaylists.collectAsState()
    val loopMode by viewModel.loopMode.collectAsState()
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsState()

    val lyrics by viewModel.lyrics.collectAsState()
    val isLyricsLoading by viewModel.isLyricsLoading.collectAsState()
    val lyricsError by viewModel.lyricsError.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .hazeSource(hazeState)
            .background(uiColors.background)
    ) {
            val nonPlayerDest = NavigationController.history.lastOrNull { it !is Destination.Player && it !is Destination.VideoPlayer } ?: Destination.Home
            AnimatedContent(
                targetState = nonPlayerDest,
                transitionSpec = {
                    val initialIndex = when (initialState) {
                        is Destination.Home -> 0
                        is Destination.SearchList, is Destination.PlaylistDetail -> 1
                        is Destination.Library -> 2
                        else -> 0
                    }
                    val targetIndex = when (targetState) {
                        is Destination.Home -> 0
                        is Destination.SearchList, is Destination.PlaylistDetail -> 1
                        is Destination.Library -> 2
                        else -> 0
                    }
                    if (targetIndex > initialIndex) {
                        slideInHorizontally(animationSpec = tween(400)) { it } togetherWith
                                slideOutHorizontally(animationSpec = tween(400)) { -it }
                    } else {
                        slideInHorizontally(animationSpec = tween(400)) { -it } togetherWith
                                slideOutHorizontally(animationSpec = tween(400)) { it }
                    }
                },
                label = "screen_slide",
                modifier = Modifier.fillMaxSize()
            ) { targetDest ->
                when (targetDest) {
                    is Destination.Home -> {
                        HomeScreen(
                            song = currentSong,
                            isPlaying = isPlaying,
                            positionMs = positionMs,
                            durationMs = durationMs,
                            onPlayPauseClick = { viewModel.togglePlayPause() },
                            onNextClick = { viewModel.playNextSong() },
                            onPreviousClick = { viewModel.playPreviousSong() },
                            onVinylClick = {
                                if (currentSong != null) {
                                    NavigationController.navigateTo(Destination.Player)
                                }
                            },
                            onNoInternet = { viewModel.showNoInternetToast() },
                            onGenreClick = { genre ->
                                searchActiveGenre = genre
                                viewModel.loadGenreTracks(genre)
                                NavigationController.navigateTo(Destination.SearchList)
                            },
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
                            activeArtist = searchActiveArtist,
                            onActiveArtistChange = { searchActiveArtist = it },
                            activeGenre = searchActiveGenre,
                            onActiveGenreChange = { searchActiveGenre = it },
                            viewAllGenres = searchViewAllGenres,
                            onViewAllGenresChange = { searchViewAllGenres = it },
                            viewAllArtists = searchViewAllArtists,
                            onViewAllArtistsChange = { searchViewAllArtists = it },
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
                            onPlaySongWithoutNavigation = { song ->
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
                            searchFocusTrigger = searchFocusTrigger,
                            artistThumbnails = artistThumbnails,
                            genreTracks = genreTracks,
                            isGenreLoading = isGenreLoading,
                            onLoadGenreTracks = { viewModel.loadGenreTracks(it) },
                            onClearGenreTracks = { viewModel.clearGenreTracks() },
                            onLoadMoreGenreTracks = { viewModel.loadMoreGenreTracks(it) },
                            onLoadArtistThumbnail = { viewModel.loadArtistThumbnail(it) },
                            isPlaying = isPlaying,
                            currentSong = currentSong,
                            onPlayPauseClick = { viewModel.togglePlayPause() },
                            loopMode = loopMode,
                            shuffleEnabled = shuffleEnabled,
                            onLoopModeToggle = { viewModel.toggleLoopMode() },
                            onShuffleToggle = { viewModel.toggleShuffle() },
                            onNextClick = { viewModel.playNextSong() },
                            onPreviousClick = { viewModel.playPreviousSong() }
                        )
                    }

                    is Destination.Library -> {
                        LibraryScreen(
                            librarySection = libraryActiveSection,
                            onLibrarySectionChange = { libraryActiveSection = it },
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
                            onPlaySongWithoutNavigation = { song, queue ->
                                viewModel.playSong(song, queue)
                            },
                            onPauseBackgroundMusic = { viewModel.mediaPlayerEngine.pause() },
                            onPlayMovie = { movie ->
                                NavigationController.navigateTo(Destination.VideoPlayer(movie))
                            },
                            uiColors = uiColors,
                            isPlayerOpen = isPlayerOpen,
                            customPlaylists = customPlaylists,
                            onCreatePlaylist = { viewModel.createCustomPlaylist(it) },
                            onDeletePlaylist = { viewModel.deleteCustomPlaylist(it) },
                            onRemoveSongFromPlaylist = { playlistName, songId -> viewModel.removeSongFromCustomPlaylist(playlistName, songId) },
                            isPlaying = isPlaying,
                            currentSong = currentSong,
                            onPlayPauseClick = { viewModel.togglePlayPause() },
                            loopMode = loopMode,
                            shuffleEnabled = shuffleEnabled,
                            onShuffleToggle = { viewModel.toggleShuffle() },
                            onLoopModeToggle = { viewModel.toggleLoopMode() },
                            onNextClick = { viewModel.playNextSong() },
                            onPreviousClick = { viewModel.playPreviousSong() },
                            artistThumbnails = artistThumbnails,
                            onLoadArtistThumbnail = { viewModel.loadArtistThumbnail(it) }
                        )
                    }

                    else -> {}
                }
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
                    isStreamLoading = isStreamLoading || isBuffering,
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
                    isQueueEndless = isQueueEndless,
                    customPlaylists = customPlaylists,
                    onAddToPlaylist = { name, song -> viewModel.addSongToCustomPlaylist(name, song) },
                    onCreatePlaylist = { name -> viewModel.createCustomPlaylist(name) },
                    lyrics = lyrics,
                    isLyricsLoading = isLyricsLoading,
                    lyricsError = lyricsError,
                    onRetryLyrics = { viewModel.retryLyrics() }
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

            val shouldShowBottomBar = !isPlayerOpen && !isVideoPlayerOpen
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .then(
                        if (shouldShowBottomBar) {
                            Modifier
                                .padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 12.dp,
                                    bottom = 28.dp
                                )
                                .fillMaxWidth()
                        } else {
                            Modifier.size(0.dp)
                        }
                    )
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
                                    style = HazeMaterials.thin()
                                ) {
                                    blurRadius = 32.dp
                                    noiseFactor = 0.01f
                                    alpha = 0.92f
                                }
                                .background(
                                    Color(0xFF0A0A0A).copy(alpha = 0.88f),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.25f),
                                            Color.White.copy(alpha = 0.05f),
                                        )
                                    ),
                                    shape = RoundedCornerShape(24.dp)
                                )
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
                                    .graphicsLayer { rotationZ = rotationAngle }
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = cleanTitle,
                                color = uiColors.textPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier
                                    .weight(1f)
                                    .basicMarquee()
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
                                style = HazeMaterials.thin()
                            ) {
                                blurRadius = 32.dp
                                noiseFactor = 0.02f
                                inputScale = HazeInputScale.Auto
                                alpha = 0.95f
                            }
                            .clip(CircleShape)
                            .background(
                                Color(0xFF0A0A0A).copy(alpha = 0.88f)
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.25f),
                                        Color.White.copy(alpha = 0.05f),
                                    )
                                ),
                                shape = CircleShape
                            )
                    ) {
                        BottomBarTabs(
                            selectedTab = currentTab,
                            onTabSelected = { tab ->
                                if (tab == currentTab) {
                                    when (tab) {
                                        MusicTab.Home -> {
                                            // No nested states
                                        }
                                        MusicTab.Search -> {
                                            searchQuery = ""
                                            searchSegment = "Songs"
                                            searchActiveArtist = null
                                            searchActiveGenre = null
                                            searchViewAllGenres = false
                                            searchViewAllArtists = false
                                            NavigationController.activePlaylist = null
                                            NavigationController.navigateTo(Destination.SearchList)
                                            searchFocusTrigger = !searchFocusTrigger
                                        }
                                        MusicTab.Library -> {
                                            libraryActiveSection = null
                                            NavigationController.navigateTo(Destination.Library)
                                        }
                                    }
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
fun LibrarySectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    uiColors: UIColors,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(uiColors.cardBackground, shape = RoundedCornerShape(16.dp))
            .border(1.dp, uiColors.cardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(uiColors.cardBorder.copy(alpha = 0.4f), shape = RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = uiColors.progressAccent,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = uiColors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = uiColors.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LibraryScreen(
    librarySection: String?,
    onLibrarySectionChange: (String?) -> Unit,
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
    onPlaySongWithoutNavigation: (Song, List<Song>) -> Unit = { _, _ -> },
    onPauseBackgroundMusic: () -> Unit,
    onPlayMovie: (Movie) -> Unit,
    uiColors: UIColors,
    isPlayerOpen: Boolean,
    customPlaylists: List<CustomPlaylist> = emptyList(),
    onCreatePlaylist: (String) -> Unit = {},
    onDeletePlaylist: (String) -> Unit = {},
    onRemoveSongFromPlaylist: (String, String) -> Unit = { _, _ -> },
    isPlaying: Boolean = false,
    currentSong: Song? = null,
    onPlayPauseClick: () -> Unit = {},
    loopMode: Int = 0,
    shuffleEnabled: Boolean = false,
    onShuffleToggle: () -> Unit = {},
    onLoopModeToggle: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onPreviousClick: () -> Unit = {},
    artistThumbnails: Map<String, String> = emptyMap(),
    onLoadArtistThumbnail: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    
    val videoPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onLibrarySectionChange("Local")
        } else {
            Toast.makeText(context, "Permission required to scan local music files", Toast.LENGTH_SHORT).show()
        }
    }
    
    val videoPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onLibrarySectionChange("Movies")
        } else {
            Toast.makeText(context, "Permission required to scan device video files", Toast.LENGTH_SHORT).show()
        }
    }

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
            onLibrarySectionChange(null)
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
                    .verticalScroll(rememberScrollState())
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Row 1: History & Downloads
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LibrarySectionCard(
                            title = "History",
                            subtitle = "${historySongs.size} songs",
                            icon = Icons.Default.History,
                            onClick = { onLibrarySectionChange("History") },
                            uiColors = uiColors,
                            modifier = Modifier.weight(1f)
                        )
                        val downloadsSubText = if (downloadingQueue.isNotEmpty()) {
                            "${downloadedSongs.size} offline • ${downloadingQueue.size} downloading"
                        } else {
                            "${downloadedSongs.size} offline"
                        }
                        LibrarySectionCard(
                            title = "Downloads",
                            subtitle = downloadsSubText,
                            icon = Icons.Default.Download,
                            onClick = { onLibrarySectionChange("Downloads") },
                            uiColors = uiColors,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2: Local & Movies
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LibrarySectionCard(
                            title = "Local",
                            subtitle = if (localTracks.isNotEmpty()) "${localTracks.size} files" else "Scan device",
                            icon = Icons.Default.FolderOpen,
                            onClick = {
                                val hasPermission = ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
                                if (hasPermission) {
                                    onLibrarySectionChange("Local")
                                } else {
                                    audioPermissionLauncher.launch(audioPermission)
                                }
                            },
                            uiColors = uiColors,
                            modifier = Modifier.weight(1f)
                        )
                        LibrarySectionCard(
                            title = "Movies",
                            subtitle = if (localVideos.isNotEmpty()) "${localVideos.size} videos" else "Scan videos",
                            icon = Icons.Default.Movie,
                            onClick = {
                                val hasPermission = ContextCompat.checkSelfPermission(context, videoPermission) == PackageManager.PERMISSION_GRANTED
                                if (hasPermission) {
                                    onLibrarySectionChange("Movies")
                                } else {
                                    videoPermissionLauncher.launch(videoPermission)
                                }
                            },
                            uiColors = uiColors,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Playlists",
                    color = uiColors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (customPlaylists.isEmpty()) {
                    Text(
                        text = "No custom playlists. Tap the button below to create one.",
                        color = uiColors.textSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        customPlaylists.forEach { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(uiColors.cardBackground, shape = RoundedCornerShape(16.dp))
                                    .border(1.dp, uiColors.cardBorder, RoundedCornerShape(16.dp))
                                    .clickable {
                                        onLibrarySectionChange("Playlist: ${playlist.name}")
                                    }
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(uiColors.progressAccent.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.List,
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
                                        text = playlist.name,
                                        color = uiColors.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee()
                                    )
                                    Text(
                                        text = "${playlist.songs.size} songs",
                                        color = uiColors.textSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = uiColors.textSecondary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        if (activeSection != null) {
            val currentSection = activeSection!!
            val isCustomPlaylist = currentSection.startsWith("Playlist: ")
            if (isCustomPlaylist) {
                val playlistName = currentSection.removePrefix("Playlist: ")
                val playlist = customPlaylists.find { it.name.equals(playlistName, ignoreCase = true) }
                val playlistSongs = playlist?.songs ?: emptyList()
                val isPlaylistPlaying = isPlaying && playlistSongs.isNotEmpty() && playlistSongs.any { it.id == currentSong?.id }

                val lazyListState = rememberLazyListState()
                val density = LocalDensity.current

                val titleAlpha = remember {
                    derivedStateOf {
                        if (lazyListState.firstVisibleItemIndex > 0) {
                            1f
                        } else {
                            val offset = lazyListState.firstVisibleItemScrollOffset.toFloat()
                            val fadeRangePx = with(density) { 120.dp.toPx() }
                            (offset / fadeRangePx).coerceIn(0f, 1f)
                        }
                    }
                }

                val hazeStateCustom = rememberHazeState()
                val statusBarHeightCustom = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

                LaunchedEffect(playlistSongs) {
                    val seen = mutableSetOf<String>()
                    val uniqueArtists = mutableListOf<String>()
                    for (song in playlistSongs) {
                        val artist = song.artist.trim()
                        if (artist.isNotEmpty() && seen.add(artist)) {
                            uniqueArtists.add(artist)
                        }
                        if (uniqueArtists.size >= 20) break
                    }
                    uniqueArtists.forEachIndexed { index, artistName ->
                        if (index > 0) {
                            kotlinx.coroutines.delay(3000)
                        }
                        onLoadArtistThumbnail(artistName)
                    }
                }

                val slideshowUrlsCustom = remember(playlistSongs, artistThumbnails) {
                    getPlaylistSlideshowUrls(playlistSongs, artistThumbnails)
                }
                val hasBgCustom = slideshowUrlsCustom.isNotEmpty()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset {
                            IntOffset(
                                x = (screenWidth.value * density.density * (1f - transitionProgress.value)).toInt(),
                                y = 0
                            )
                        }
                        .shadow(8.dp, clip = false)
                        .clipToBounds()
                ) {
                    // Photo slideshow or fallback
                    ArtistPhotoSlideshowBackground(
                        imageUrls = slideshowUrlsCustom,
                        hazeState = hazeStateCustom,
                        uiColors = uiColors
                    )

                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .align(Alignment.TopCenter),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = statusBarHeightCustom + 64.dp, bottom = 100.dp)
                    ) {
                        item {
                            RetroCassetteTape(
                                title = playlistName,
                                author = "Custom Playlist",
                                trackCount = playlistSongs.size,
                                isPlaying = isPlaylistPlaying,
                                uiColors = uiColors,
                                showControls = true,
                                shuffleEnabled = shuffleEnabled,
                                loopMode = loopMode,
                                onShuffleClick = onShuffleToggle,
                                onLoopClick = onLoopModeToggle,
                                onNextClick = onNextClick,
                                onPreviousClick = onPreviousClick,
                                onPlayClick = {
                                    if (playlistSongs.isNotEmpty()) {
                                        val isCurrentLoaded = currentSong != null && playlistSongs.any { it.id == currentSong.id }
                                        if (isCurrentLoaded) {
                                            onPlayPauseClick()
                                        } else {
                                            onPlaySongWithoutNavigation(playlistSongs.first(), playlistSongs)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (playlistSongs.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No songs in this playlist yet.",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        } else {
                            items(playlistSongs) { song ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        TrackStandingCard(
                                            song = song,
                                            onClick = { onSongSelect(song) },
                                            uiColors = uiColors,
                                            hazeState = if (hasBgCustom) hazeStateCustom else null
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { onRemoveSongFromPlaylist(playlistName, song.id) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove from Playlist",
                                            tint = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }

                    // Pinned action bar — fully transparent
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(56.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.35f), shape = CircleShape)
                                    .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                                    .clickable { onLibrarySectionChange(null) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBackIosNew,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = playlistName,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier
                                    .basicMarquee()
                                    .graphicsLayer { alpha = titleAlpha.value }
                            )
                        }

                        IconButton(
                            onClick = {
                                onDeletePlaylist(playlistName)
                                onLibrarySectionChange(null)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Playlist",
                                tint = Color(0xFFEF5350)
                            )
                        }
                    }
                }
            } else {
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(uiColors.cardBackground, shape = CircleShape)
                                .border(1.dp, uiColors.cardBorder, CircleShape)
                                .clickable { onLibrarySectionChange(null) },
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
                            text = if (currentSection.startsWith("Playlist: ")) {
                                currentSection.removePrefix("Playlist: ")
                            } else {
                                currentSection
                            },
                            color = uiColors.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }

                    if (currentSection.startsWith("Playlist: ")) {
                        val playlistName = currentSection.removePrefix("Playlist: ")
                        IconButton(
                            onClick = {
                                onDeletePlaylist(playlistName)
                                onLibrarySectionChange(null)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Playlist",
                                tint = Color(0xFFEF5350)
                            )
                        }
                    }
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
                } else if (currentSection.startsWith("Playlist: ")) {
                    val playlistName = currentSection.removePrefix("Playlist: ")
                    val playlist = customPlaylists.find { it.name.equals(playlistName, ignoreCase = true) }
                    val playlistSongs = playlist?.songs ?: emptyList()

                    if (playlistSongs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No songs in this playlist yet.",
                                color = uiColors.textSecondary,
                                fontSize = 14.sp
                            )
                        }
                    } else {
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
                                items(playlistSongs) { song ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            TrackStandingCard(
                                                song = song,
                                                onClick = { onSongSelect(song) },
                                                uiColors = uiColors
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = {
                                                onRemoveSongFromPlaylist(playlistName, song.id)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove from Playlist",
                                                tint = uiColors.textSecondary
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

        if (activeSection == null) {
            var showCreateDialog by remember { mutableStateOf(false) }

            if (showCreateDialog) {
                var newPlaylistName by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showCreateDialog = false },
                    title = {
                        Text(
                            text = "New Playlist",
                            color = uiColors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    text = {
                        OutlinedTextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            label = { Text("Playlist Name", color = uiColors.textSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = uiColors.progressAccent,
                                unfocusedBorderColor = uiColors.cardBorder,
                                focusedTextColor = uiColors.textPrimary,
                                unfocusedTextColor = uiColors.textPrimary,
                                cursorColor = uiColors.progressAccent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (newPlaylistName.isNotBlank()) {
                                    onCreatePlaylist(newPlaylistName.trim())
                                    showCreateDialog = false
                                }
                            }
                        ) {
                            Text("Create", color = uiColors.progressAccent, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreateDialog = false }) {
                            Text("Cancel", color = uiColors.textSecondary)
                        }
                    },
                    containerColor = uiColors.background,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.border(1.dp, uiColors.cardBorder, RoundedCornerShape(24.dp))
                )
            }

            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = uiColors.progressAccent,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 100.dp)
                    .size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Playlist"
                )
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
            // Pulsing rings drawn in a single Canvas to avoid layout and allocation overhead
            Canvas(modifier = Modifier.fillMaxSize()) {
                val maxRadius = size.minDimension / 2f
                drawCircle(
                    color = accentColor.copy(alpha = (1f - wave1) * 0.5f),
                    radius = maxRadius * wave1
                )
                drawCircle(
                    color = accentColor.copy(alpha = (1f - wave2) * 0.5f),
                    radius = maxRadius * wave2
                )
                drawCircle(
                    color = accentColor.copy(alpha = (1f - wave3) * 0.5f),
                    radius = maxRadius * wave3
                )
            }
            // Center dot with subtle pulse scaled layout-free on GPU
            val pulse by infiniteTransition.animateFloat(
                initialValue = 0.85f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                label = "pulse"
            )
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .graphicsLayer {
                        scaleX = pulse
                        scaleY = pulse
                    }
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
                modifier = Modifier.basicMarquee()
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
    onNoInternet: () -> Unit = {},
    onGenreClick: (String) -> Unit = {},
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

    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Top Bar: Logo & CONVENE, Hello/User & Clock
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(vertical = 12.dp)
        ) {
            // Top line: CONVENE in center
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
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
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = ":",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = minStr,
                        color = accentColor,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dark grey filled card container Box (with dynamic themed glow and responsive layout)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
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
                        .offset { IntOffset(slideOffset.roundToPx(), 0) }
                        .graphicsLayer { rotationZ = rotationAngle }
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
                                maxLines = 1,
                                modifier = Modifier
                                    .padding(bottom = 2.dp)
                                    .basicMarquee()
                            )
                            Text(
                                text = song?.artist?.replace(Regex("\\s*•\\s*\\d+:\\d+\\s*$"), "") ?: "Select a song to play",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
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

        Spacer(modifier = Modifier.height(40.dp))

        // Dynamic beat-reactive Audio Visualizer (centered horizontally)
        key(song?.id) {
            AudioVisualizer(
                isPlaying = isPlaying && song != null,
                color = accentColor,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .height(40.dp)
                    .width(180.dp)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Genres marquee section (spacious, height = 260.dp)
        val row1Genres = remember { listOf("Pop", "Rock", "Hip Hop", "EDM", "R&B", "Jazz", "Classical", "K-Pop", "Lo-Fi", "Metal") }
        val row2Genres = remember { listOf("Country", "Blues", "Soul", "Reggae", "Folk", "Acoustic", "Indie", "Latin", "Punk", "Synthwave") }
        val row3Genres = remember { listOf("House", "Techno", "Ambient", "Trap", "Gospel", "Afrobeats", "Dancehall", "Alternative", "Grunge", "Funk") }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 2.dp, end = 16.dp)
                .height(260.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vertical landscape-oriented heading "GENRES" (system-themed & single line)
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "GENRES",
                    color = uiColors.progressAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.layout { measurable, constraints ->
                        val placeable = measurable.measure(
                            constraints.copy(
                                minWidth = 0,
                                maxWidth = constraints.maxHeight,
                                minHeight = 0,
                                maxHeight = constraints.maxWidth
                            )
                        )
                        layout(placeable.height, placeable.width) {
                            placeable.placeWithLayer(
                                x = -(placeable.width - placeable.height) / 2,
                                y = (placeable.width - placeable.height) / 2
                            ) {
                                rotationZ = -90f
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // 3 Rows of automatically moving CDs
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AutoScrollingMarqueeRow(genres = row1Genres, leftToRight = true, onGenreClick = onGenreClick, uiColors = uiColors, speed = 0.8f)
                AutoScrollingMarqueeRow(genres = row2Genres, leftToRight = false, onGenreClick = onGenreClick, uiColors = uiColors, speed = 1.3f)
                AutoScrollingMarqueeRow(genres = row3Genres, leftToRight = true, onGenreClick = onGenreClick, uiColors = uiColors, speed = 1.8f)
            }
        }

        Spacer(modifier = Modifier.height(96.dp))
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
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = movie.title,
                    color = uiColors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .basicMarquee()
                )
                Spacer(modifier = Modifier.width(8.dp))
                BadgeCapsule(text = "Preview", uiColors = uiColors)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${movie.durationText} • ${movie.sizeText}",
                color = uiColors.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun BadgeCapsule(text: String, uiColors: UIColors) {
    Box(
        modifier = Modifier
            .background(uiColors.cardBorder, shape = RoundedCornerShape(8.dp))
            .border(0.5.dp, uiColors.cardBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = uiColors.textSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
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

@Composable
fun SplashHeartbeatScreen() {

    val uiColors = LocalUIColors.current
    val accentColor = uiColors.progressAccent

    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {

        // First ECG pass
        animate(
            initialValue = 0f,
            targetValue = 0.999f,
            animationSpec = tween(
                durationMillis = 1800,
                easing = LinearEasing
            )
        ) { value, _ ->
            progress = value
        }

        // Second ECG pass
        animate(
            initialValue = 0f,
            targetValue = 0.999f,
            animationSpec = tween(
                durationMillis = 1800,
                easing = LinearEasing
            )
        ) { value, _ ->
            progress = value
        }

        // Keep the whole line visible
        progress = 0.999f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .drawWithCache {
                    val w = size.width
                    val h = size.height
                    val centerY = h / 2

                    val path = Path().apply {
                        moveTo(0f, centerY)

                        // Left flat line
                        lineTo(w * 0.24f, centerY)

                        // Small left pulse
                        lineTo(w * 0.28f, centerY - 50f)
                        lineTo(w * 0.31f, centerY + 45f)
                        lineTo(w * 0.34f, centerY)

                        // Middle flat section
                        lineTo(w * 0.44f, centerY)

                        // Main pulse
                        lineTo(w * 0.50f, centerY - 120f)
                        lineTo(w * 0.56f, centerY + 140f)
                        lineTo(w * 0.63f, centerY)

                        // Right flat section
                        lineTo(w * 0.74f, centerY)

                        // Small right pulse
                        lineTo(w * 0.78f, centerY - 50f)
                        lineTo(w * 0.81f, centerY + 45f)
                        lineTo(w * 0.84f, centerY)

                        // End
                        lineTo(w, centerY)
                    }

                    val androidPath = path.asAndroidPath()
                    val measure = PathMeasure(androidPath, false)
                    val segmentPath = android.graphics.Path()
                    val pos = FloatArray(2)

                    onDrawBehind {
                        segmentPath.reset()
                        measure.getSegment(
                            0f,
                            measure.length * progress,
                            segmentPath,
                            true
                        )

                        val composeSegment = segmentPath.asComposePath()

                        drawPath(
                            path = composeSegment,
                            color = accentColor.copy(alpha = 0.3f),
                            style = Stroke(
                                width = 10f,
                                cap = StrokeCap.Round
                            )
                        )

                        drawPath(
                            path = composeSegment,
                            color = accentColor,
                            style = Stroke(
                                width = 4f,
                                cap = StrokeCap.Round
                            )
                        )

                        measure.getPosTan(
                            measure.length * progress,
                            pos,
                            null
                        )

                        drawCircle(
                            color = accentColor.copy(alpha = 0.3f),
                            radius = 18f,
                            center = Offset(pos[0], pos[1])
                        )

                        drawCircle(
                            color = accentColor,
                            radius = 8f,
                            center = Offset(pos[0], pos[1])
                        )
                    }
                }
        )
    }
}

@Composable
fun MiniGenreCDCard(
    genreName: String,
    onClick: () -> Unit,
    uiColors: UIColors
) {
    val coverBrush = remember(genreName) {
        val hash = genreName.hashCode()
        val absHash = if (hash == Int.MIN_VALUE) Int.MAX_VALUE else if (hash < 0) -hash else hash
        val gradients = listOf(
            Brush.linearGradient(colors = listOf(Color(0xFFFF0844), Color(0xFFFFB199))), // Sunset Red
            Brush.linearGradient(colors = listOf(Color(0xFFF12711), Color(0xFFF5AF19))), // Sunrise Yellow
            Brush.linearGradient(colors = listOf(Color(0xFF00C6FF), Color(0xFF0072FF))), // Neon Blue
            Brush.linearGradient(colors = listOf(Color(0xFF7000FF), Color(0xFFFF007F))), // Purple Pink
            Brush.linearGradient(colors = listOf(Color(0xFF11998E), Color(0xFF38EF7D))), // Emerald Green
            Brush.linearGradient(colors = listOf(Color(0xFF8A2387), Color(0xFFE94057))), // Cosmic Rose
            Brush.linearGradient(colors = listOf(Color(0xFFFF007F), Color(0xFF7F00FF))), // Vaporwave
            Brush.linearGradient(colors = listOf(Color(0xFF1AD1A5), Color(0xFF007AD9))), // Teal Ocean
            Brush.linearGradient(colors = listOf(Color(0xFFFF9966), Color(0xFFFF5E62))), // Orange Coral
            Brush.linearGradient(colors = listOf(Color(0xFFE805FF), Color(0xFF00E2FF))), // Cyberpunk
            Brush.linearGradient(colors = listOf(Color(0xFF3A1C71), Color(0xFFD76D77))), // Grapefruit
            Brush.linearGradient(colors = listOf(Color(0xFFFF7B00), Color(0xFFFF0055)))  // Fire
        )
        gradients[absHash % gradients.size]
    }

    val labelColor = remember(genreName) {
        val hash = genreName.hashCode()
        val absHash = if (hash == Int.MIN_VALUE) Int.MAX_VALUE else if (hash < 0) -hash else hash
        val hue = (absHash % 360).toFloat()
        Color.Companion.hsl(hue = hue, saturation = 0.85f, lightness = 0.45f)
    }

    // Mini Vinyl Record Disk continuous rotation animation
    var rotationAngle by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        while (true) {
            val elapsed = System.currentTimeMillis() - startTime
            rotationAngle = ((elapsed.toFloat() / 6000f) * 360f) % 360f
            delay(16)
        }
    }

    // A container Box of size 136.dp width and 80.dp height
    Box(
        modifier = Modifier
            .width(136.dp)
            .height(80.dp)
            .clickable { onClick() }
    ) {
        // 1. The mini rotating Vinyl record peeking out from the left (drawn first -> underneath)
        Box(
            modifier = Modifier
                .size(76.dp)
                .align(Alignment.CenterStart)
                .graphicsLayer { rotationZ = rotationAngle }
                .clip(CircleShape)
                .background(Color(0xFF151515))
                .border(1.dp, Color.Black.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.minDimension / 2f
                val radii = listOf(0.85f, 0.7f, 0.55f, 0.4f)
                radii.forEach { factor ->
                    drawCircle(
                        color = Color.White.copy(alpha = 0.1f),
                        radius = maxRadius * factor,
                        center = center,
                        style = Stroke(width = 0.5.dp.toPx())
                    )
                }
            }

            // Center Label
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(labelColor)
                    .border(0.5.dp, Color.Black, CircleShape)
            )
        }

        // 2. The custom gradient Album sleeve jacket on the right (drawn second -> on top)
        Box(
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.CenterEnd)
                .clip(RoundedCornerShape(12.dp))
                .background(coverBrush)
                .border(1.dp, uiColors.cardBorder, RoundedCornerShape(12.dp))
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            // Dark overlay for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.18f))
            )
            
            Text(
                text = genreName,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AutoScrollingMarqueeRow(
    genres: List<String>,
    leftToRight: Boolean,
    onGenreClick: (String) -> Unit,
    uiColors: UIColors,
    speed: Float = 1.2f
) {
    if (genres.isEmpty()) return
    val repeatedGenres = remember(genres) { List(80) { genres }.flatten() }
    val listState = rememberLazyListState()
    var isPressed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val middleIndex = repeatedGenres.size / 2
        listState.scrollToItem(middleIndex)
    }

    LaunchedEffect(listState.firstVisibleItemIndex) {
        val total = repeatedGenres.size
        if (genres.isNotEmpty() && (listState.firstVisibleItemIndex < 15 || listState.firstVisibleItemIndex > total - 15)) {
            val offset = listState.firstVisibleItemScrollOffset
            val offsetItem = listState.firstVisibleItemIndex % genres.size
            val targetIndex = total / 2 + offsetItem
            listState.scrollToItem(targetIndex, offset)
        }
    }

    // Scroll delta.
    // Negative delta moves content left-to-right (backwards scroll), positive delta moves it right-to-left (forward scroll).
    val scrollSpeed = if (leftToRight) -speed else speed
    LaunchedEffect(listState.isScrollInProgress, isPressed) {
        if (!listState.isScrollInProgress && !isPressed) {
            while (true) {
                listState.scrollBy(scrollSpeed)
                delay(16)
            }
        }
    }

    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        isPressed = event.changes.any { it.pressed }
                    }
                }
            },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
        userScrollEnabled = true
    ) {
        items(repeatedGenres) { genre ->
            MiniGenreCDCard(
                genreName = genre,
                onClick = { onGenreClick(genre) },
                uiColors = uiColors
            )
        }
    }
}

@Composable
fun AudioVisualizer(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val barCount = 18
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer")
    
    // Smooth multiplier that rises and falls slowly
    val multiplier by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "visualizer_multiplier"
    )

    val heights = List(barCount) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.15f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 300 + (index * 35) % 200,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar_$index"
        )
    }

    Canvas(modifier = modifier) {
        val spacing = 4.dp.toPx()
        val totalSpacing = spacing * (barCount - 1)
        val barWidth = (size.width - totalSpacing) / barCount
        val cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())

        for (i in 0 until barCount) {
            val hFraction = 0.15f + (heights[i].value - 0.15f) * multiplier
            val barHeight = size.height * hFraction
            val x = i * (barWidth + spacing)
            val y = size.height - barHeight

            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = cornerRadius
            )
            // Round only the top corners
            if (barHeight > cornerRadius.y) {
                drawRect(
                    color = color,
                    topLeft = Offset(x, y + barHeight - cornerRadius.y),
                    size = Size(barWidth, cornerRadius.y)
                )
            }
        }
    }
}