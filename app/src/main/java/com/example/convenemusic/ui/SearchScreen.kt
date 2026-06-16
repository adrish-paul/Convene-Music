package com.example.convenemusic.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import com.example.convenemusic.ui.components.PullToRefreshContainer
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import coil3.compose.AsyncImage
import com.example.convenemusic.network.Song
import com.example.convenemusic.network.Playlist
import com.example.convenemusic.ui.components.SquigglyProgressBar
import com.example.convenemusic.ui.theme.UIColors
import com.example.convenemusic.ui.theme.LocalUIColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.distinctUntilChanged

private fun splitArtistNames(combinedName: String): List<String> {
    val bulletParts = combinedName.split(Regex(" • | •|• |•|\\u2022"))
    val mainArtistPart = bulletParts.firstOrNull()?.trim() ?: combinedName
    val separators = Regex(",|/|\\||&|\\s+\\.\\s+|\\bx\\b|\\bX\\b|\\band\\b|\\bfeat\\.?\\b|\\bft\\.?\\b", RegexOption.IGNORE_CASE)
    return mainArtistPart.split(separators)
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.equals("unknown", ignoreCase = true) }
}

@Composable
fun SearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedSegment: String,
    onSegmentChange: (String) -> Unit,
    activePlaylist: Playlist?,
    onActivePlaylistChange: (Playlist?) -> Unit,
    onSongSelect: (Song) -> Unit,
    onPlayPlaylist: (Playlist) -> Unit,
    searchSongs: List<Song>,
    playlistResults: List<Playlist>,
    playlistTracks: List<Song>,
    artistTracks: List<Song>,
    isArtistLoading: Boolean,
    historySongs: List<Song>,
    downloadedSongs: List<Song>,
    onPerformSearch: (String) -> Unit,
    onPerformPlaylistSearch: (String) -> Unit,
    onLoadPlaylistTracks: (String) -> Unit,
    onClearPlaylistTracks: () -> Unit,
    onLoadArtistTracks: (String) -> Unit,
    onClearArtistTracks: () -> Unit,
    onLoadMoreArtistTracks: (String) -> Unit,
    onLoadMore: () -> Unit,
    isLoading: Boolean,
    isPlaylistLoading: Boolean,
    modifier: Modifier = Modifier,
    isLoadMoreLoading: Boolean = false,
    isPlayerOpen: Boolean = false,
    artistThumbnails: Map<String, String> = emptyMap(),
    genreTracks: List<Song> = emptyList(),
    isGenreLoading: Boolean = false,
    onLoadGenreTracks: (String) -> Unit = {},
    onClearGenreTracks: () -> Unit = {},
    onLoadMoreGenreTracks: (String) -> Unit = {}
) {
    val uiColors = LocalUIColors.current
    val focusManager = LocalFocusManager.current

    val songListState = rememberLazyListState()
    val playlistListState = rememberLazyListState()

    // Detect when scrolling near the end to trigger loading more
    val shouldLoadMoreSongs by remember {
        derivedStateOf {
            val lastVisibleItem = songListState.layoutInfo.visibleItemsInfo.lastOrNull()
                ?: return@derivedStateOf false
            lastVisibleItem.index >= songListState.layoutInfo.totalItemsCount - 4
        }
    }
    val shouldLoadMorePlaylists by remember {
        derivedStateOf {
            val lastVisibleItem = playlistListState.layoutInfo.visibleItemsInfo.lastOrNull()
                ?: return@derivedStateOf false
            lastVisibleItem.index >= playlistListState.layoutInfo.totalItemsCount - 4
        }
    }

    LaunchedEffect(shouldLoadMoreSongs) {
        if (shouldLoadMoreSongs && selectedSegment == "Songs") {
            onLoadMore()
        }
    }

    LaunchedEffect(shouldLoadMorePlaylists) {
        if (shouldLoadMorePlaylists && selectedSegment == "Playlists") {
            onLoadMore()
        }
    }

    var activeArtist by remember { mutableStateOf<String?>(null) }
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val transitionProgress = remember { Animatable(0f) }
    var activeArtistState by remember { mutableStateOf<String?>(null) }

    var activeGenre by remember { mutableStateOf<String?>(null) }
    var viewAllGenres by remember { mutableStateOf(false) }
    val viewAllTransitionProgress = remember { Animatable(0f) }
    val genreDetailTransitionProgress = remember { Animatable(0f) }
    var activeGenreState by remember { mutableStateOf<String?>(null) }
    var viewAllGenresState by remember { mutableStateOf(false) }

    var viewAllArtists by remember { mutableStateOf(false) }
    val viewAllArtistsTransitionProgress = remember { Animatable(0f) }
    var viewAllArtistsState by remember { mutableStateOf(false) }

    LaunchedEffect(activeArtist) {
        if (activeArtist != null) {
            activeArtistState = activeArtist
            transitionProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = EmphasizedEasing)
            )
        } else {
            transitionProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 400, easing = EmphasizedEasing)
            )
            activeArtistState = null
        }
    }

    LaunchedEffect(activeGenre) {
        if (activeGenre != null) {
            activeGenreState = activeGenre
            genreDetailTransitionProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = EmphasizedEasing)
            )
        } else {
            genreDetailTransitionProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 400, easing = EmphasizedEasing)
            )
            activeGenreState = null
        }
    }

    LaunchedEffect(viewAllGenres) {
        if (viewAllGenres) {
            viewAllGenresState = true
            viewAllTransitionProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = EmphasizedEasing)
            )
        } else {
            viewAllTransitionProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 400, easing = EmphasizedEasing)
            )
            viewAllGenresState = false
        }
    }

    LaunchedEffect(viewAllArtists) {
        if (viewAllArtists) {
            viewAllArtistsState = true
            viewAllArtistsTransitionProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = EmphasizedEasing)
            )
        } else {
            viewAllArtistsTransitionProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 400, easing = EmphasizedEasing)
            )
            viewAllArtistsState = false
        }
    }

    PredictiveBackHandler(enabled = !isPlayerOpen && activePlaylist == null && activeArtist != null) { progressFlow ->
        try {
            progressFlow.collect { backEvent ->
                transitionProgress.snapTo(1f - backEvent.progress)
            }
            transitionProgress.animateTo(
                targetValue = 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            )
            activeArtist = null
            activeArtistState = null
        } catch (e: CancellationException) {
            transitionProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            )
        }
    }

    PredictiveBackHandler(enabled = !isPlayerOpen && activePlaylist == null && activeArtist == null && activeGenre != null) { progressFlow ->
        try {
            progressFlow.collect { backEvent ->
                genreDetailTransitionProgress.snapTo(1f - backEvent.progress)
            }
            genreDetailTransitionProgress.animateTo(
                targetValue = 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            )
            activeGenre = null
            activeGenreState = null
            onClearGenreTracks()
        } catch (e: CancellationException) {
            genreDetailTransitionProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            )
        }
    }

    PredictiveBackHandler(enabled = !isPlayerOpen && activePlaylist == null && activeArtist == null && activeGenre == null && viewAllGenres) { progressFlow ->
        try {
            progressFlow.collect { backEvent ->
                viewAllTransitionProgress.snapTo(1f - backEvent.progress)
            }
            viewAllTransitionProgress.animateTo(
                targetValue = 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            )
            viewAllGenres = false
            viewAllGenresState = false
        } catch (e: CancellationException) {
            viewAllTransitionProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            )
        }
    }

    PredictiveBackHandler(enabled = !isPlayerOpen && activePlaylist == null && activeArtist == null && activeGenre == null && !viewAllGenres && viewAllArtists) { progressFlow ->
        try {
            progressFlow.collect { backEvent ->
                viewAllArtistsTransitionProgress.snapTo(1f - backEvent.progress)
            }
            viewAllArtistsTransitionProgress.animateTo(
                targetValue = 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            )
            viewAllArtists = false
            viewAllArtistsState = false
        } catch (e: CancellationException) {
            viewAllArtistsTransitionProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            )
        }
    }

    // Intercept back button when viewing a playlist detail
    BackHandler(enabled = !isPlayerOpen && activePlaylist != null) {
        onActivePlaylistChange(null)
    }

    // Intercept back button when query is entered, and we are on the main search screen (not detail)
    BackHandler(enabled = !isPlayerOpen && activePlaylist == null && activeArtist == null && activeGenre == null && !viewAllGenres && !viewAllArtists && query.isNotEmpty()) {
        onQueryChange("")
    }

    // Load tracks when active playlist selection changes
    LaunchedEffect(activePlaylist) {
        if (activePlaylist != null) {
            onLoadPlaylistTracks(activePlaylist.id)
        } else {
            onClearPlaylistTracks()
        }
    }

    // Load tracks when active artist changes
    LaunchedEffect(activeArtist) {
        if (activeArtist != null) {
            onLoadArtistTracks(activeArtist!!)
        } else {
            onClearArtistTracks()
        }
    }

    // Real-time debounced query processing
    LaunchedEffect(query, selectedSegment) {
        if (query.isNotBlank()) {
            if (selectedSegment == "Songs") {
                onPerformSearch(query)
            } else {
                delay(300) // Keep debounce for playlist searches
                onPerformPlaylistSearch(query)
            }
        } else {
            onPerformSearch("")
            onPerformPlaylistSearch("")
        }
    }

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val density = LocalDensity.current

    if (activePlaylist != null) {
        PlaylistDetailView(
            playlist = activePlaylist,
            tracks = playlistTracks,
            isLoading = isPlaylistLoading,
            onBackClick = { onActivePlaylistChange(null) },
            onSongSelect = { song ->
                focusManager.clearFocus()
                onSongSelect(song)
            },
            onRefresh = { onLoadPlaylistTracks(activePlaylist.id) },
            uiColors = uiColors,
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .background(uiColors.background)
                .padding(horizontal = 16.dp)
        )
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            val maxTransitionProgress = maxOf(
                transitionProgress.value,
                genreDetailTransitionProgress.value,
                viewAllTransitionProgress.value,
                viewAllArtistsTransitionProgress.value
            )

            val hideMainColumn = (activeArtistState != null && transitionProgress.value >= 1f) ||
                                 (activeGenreState != null && genreDetailTransitionProgress.value >= 1f) ||
                                 (viewAllGenresState && viewAllTransitionProgress.value >= 1f) ||
                                 (viewAllArtistsState && viewAllArtistsTransitionProgress.value >= 1f)

            if (!hideMainColumn) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset {
                            IntOffset(
                                x = (-(screenWidth.value * density.density * 0.3f) * maxTransitionProgress).toInt(),
                                y = 0
                            )
                        }
                        .background(uiColors.background)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Search Header Title
                    Text(
                        text = "Convene Music",
                        color = uiColors.textPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Flat Premium Search Input Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(uiColors.cardBackground, shape = RoundedCornerShape(24.dp))
                            .border(1.dp, uiColors.cardBorder, RoundedCornerShape(24.dp))
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = uiColors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (query.isEmpty()) {
                                Text(
                                    text = if (selectedSegment == "Songs") "Search songs, artists, albums..." else "Search playlists...",
                                    color = uiColors.textSecondary,
                                    fontSize = 14.sp
                                )
                            }
                            BasicTextField(
                                value = query,
                                onValueChange = onQueryChange,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    if (query.isNotBlank()) {
                                        if (selectedSegment == "Songs") {
                                            onPerformSearch(query)
                                        } else {
                                            onPerformPlaylistSearch(query)
                                        }
                                        focusManager.clearFocus()
                                    }
                                }),
                                textStyle = LocalTextStyle.current.copy(
                                    color = uiColors.textPrimary,
                                    fontSize = 14.sp
                                ),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(uiColors.progressAccent),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (query.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Search",
                                tint = uiColors.textSecondary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable {
                                        onQueryChange("")
                                    }
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = query.isNotEmpty(),
                        enter = slideInVertically(
                            initialOffsetY = { -it },
                            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(durationMillis = 200)),
                        exit = slideOutVertically(
                            targetOffsetY = { -it },
                            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(durationMillis = 200))
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))

                            // Tab Capsule [Songs] [Playlists]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .background(uiColors.cardBorder, shape = CircleShape)
                                    .padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("Songs", "Playlists").forEach { option ->
                                    val isSelected = selectedSegment == option
                                    val tabBgColor by animateColorAsState(
                                        targetValue = if (isSelected) uiColors.cardBackground else Color.Transparent,
                                        animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                    )
                                    val tabTextColor by animateColorAsState(
                                        targetValue = if (isSelected) uiColors.textPrimary else uiColors.textSecondary
                                    )

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .shadow(
                                                elevation = if (isSelected) 2.dp else 0.dp,
                                                shape = CircleShape
                                            )
                                            .background(tabBgColor, shape = CircleShape)
                                            .clickable { onSegmentChange(option) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = option,
                                            color = tabTextColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    if (query.isEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    val isEmpty = if (selectedSegment == "Songs") searchSongs.isEmpty() else playlistResults.isEmpty()
                    if (isLoading && isEmpty) {
                        // Placed just below the segment bar so keyboard doesn't hide it
                        Spacer(modifier = Modifier.height(24.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (selectedSegment == "Songs") "Searching songs..." else "Searching playlists...",
                                color = uiColors.textSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 14.dp)
                            )
                            SquigglyProgressBar(
                                progress = null,
                                color = uiColors.progressAccent,
                                trackColor = uiColors.cardBorder,
                                wavelength = 56.dp,
                                amplitude = 5.dp,
                                strokeWidth = 3.dp,
                                animated = true,
                                animationSpeed = 700,
                                modifier = Modifier
                                    .fillMaxWidth(0.55f)
                                    .height(16.dp)
                            )
                        }
                        // fill remaining space so layout stays consistent
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        PullToRefreshContainer(
                            isRefreshing = isLoading,
                            onRefresh = {
                                if (query.isNotBlank()) {
                                    if (selectedSegment == "Songs") {
                                        onPerformSearch(query)
                                    } else {
                                        onPerformPlaylistSearch(query)
                                    }
                                }
                            },
                            accentColor = uiColors.progressAccent,
                            backgroundColor = uiColors.cardBackground,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                if (isLoading) {
                                    SquigglyProgressBar(
                                        progress = null,
                                        color = uiColors.progressAccent,
                                        trackColor = uiColors.cardBorder,
                                        wavelength = 56.dp,
                                        amplitude = 5.dp,
                                        strokeWidth = 3.dp,
                                        animated = true,
                                        animationSpeed = 700,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(14.dp)
                                            .padding(vertical = 4.dp, horizontal = 16.dp)
                                    )
                                }
                                if (query.isEmpty()) {
                                    val famousGenres = listOf("Pop", "Rock", "Hip Hop", "EDM", "R&B")
                                    val topArtists = listOf("Taylor Swift", "Drake", "Arijit Singh", "The Weeknd", "Billie Eilish")
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        contentPadding = PaddingValues(bottom = 100.dp)
                                    ) {
                                        item {
                                            Text(
                                                text = "Genres",
                                                color = uiColors.textPrimary,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp, start = 4.dp)
                                            )
                                        }
                                        val genreItems = famousGenres + "View All"
                                        val genreRows = genreItems.chunked(2)
                                        items(genreRows) { rowItems ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                rowItems.forEach { item ->
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        if (item == "View All") {
                                                            ViewAllGenreCard(
                                                                onClick = {
                                                                    focusManager.clearFocus()
                                                                    viewAllGenres = true
                                                                },
                                                                uiColors = uiColors
                                                            )
                                                        } else {
                                                            GenreCard(
                                                                genreName = item,
                                                                onClick = {
                                                                    focusManager.clearFocus()
                                                                    activeGenre = item
                                                                    onLoadGenreTracks(item)
                                                                },
                                                                onPlayClick = null,
                                                                uiColors = uiColors
                                                            )
                                                        }
                                                    }
                                                }
                                                if (rowItems.size < 2) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }

                                        item {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                text = "Artists",
                                                color = uiColors.textPrimary,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp, start = 4.dp)
                                            )
                                        }
                                        val artistItems = topArtists + "View All"
                                        val artistRows = artistItems.chunked(2)
                                        items(artistRows) { rowItems ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                rowItems.forEach { artistName ->
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        if (artistName == "View All") {
                                                            ViewAllGenreCard(
                                                                onClick = {
                                                                    focusManager.clearFocus()
                                                                    viewAllArtists = true
                                                                },
                                                                uiColors = uiColors
                                                            )
                                                        } else {
                                                            EmptyStateArtistCard(
                                                                artistName = artistName,
                                                                onClick = {
                                                                    focusManager.clearFocus()
                                                                    activeArtist = artistName
                                                                },
                                                                thumbnailUrl = artistThumbnails[artistName],
                                                                uiColors = uiColors
                                                            )
                                                        }
                                                    }
                                                }
                                                if (rowItems.size < 2) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                        item {
                                            Spacer(modifier = Modifier.height(100.dp))
                                        }
                                    }
                                } else if (selectedSegment == "Songs") {
                                    val localMatches = remember(searchSongs, query, downloadedSongs, historySongs) {
                                        if (query.isBlank()) emptyList()
                                        else {
                                            val localIds = downloadedSongs
                                                .map { it.id }
                                                .toSet()
                                            searchSongs.filter { it.id in localIds }
                                        }
                                    }
                                    val otherSongs = remember(searchSongs, query, downloadedSongs, historySongs) {
                                        if (query.isBlank()) emptyList()
                                        else {
                                            val localIds = downloadedSongs
                                                .map { it.id }
                                                .toSet()
                                            searchSongs.filter { it.id !in localIds }
                                        }
                                    }
                                    val matchedArtists = remember(searchSongs, query, artistThumbnails) {
                                        if (query.isBlank()) emptyList()
                                        else {
                                            searchSongs
                                                .flatMap { splitArtistNames(it.artist) }
                                                .filter { it.contains(query, ignoreCase = true) }
                                                .distinctBy { it.lowercase().trim() }
                                                .map { artistName ->
                                                    val officialThumb = artistThumbnails[artistName]
                                                    if (officialThumb != null) {
                                                        artistName to officialThumb
                                                    } else {
                                                        val firstSong = searchSongs.firstOrNull { song ->
                                                            splitArtistNames(song.artist).any { it.equals(artistName, ignoreCase = true) }
                                                        }
                                                        artistName to firstSong?.thumbnailUrl
                                                    }
                                                }
                                        }
                                    }

                                    LazyColumn(
                                        state = songListState,
                                        modifier = Modifier.fillMaxSize().weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        contentPadding = PaddingValues(bottom = 100.dp)
                                    ) {
                                        if (matchedArtists.isNotEmpty()) {
                                            item {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 12.dp)
                                                ) {
                                                    Text(
                                                        text = "Artists",
                                                        color = uiColors.textPrimary,
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
                                                    )
                                                    LazyRow(
                                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                                        contentPadding = PaddingValues(horizontal = 4.dp)
                                                    ) {
                                                        items(matchedArtists) { (artistName, thumbnailUrl) ->
                                                            ArtistCard(
                                                                artistName = artistName,
                                                                thumbnailUrl = thumbnailUrl,
                                                                onClick = {
                                                                    focusManager.clearFocus()
                                                                    activeArtist = artistName
                                                                },
                                                                uiColors = uiColors
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        items(localMatches) { song ->
                                            TrackStandingCard(
                                                song = song,
                                                onClick = {
                                                    focusManager.clearFocus()
                                                    onSongSelect(song)
                                                },
                                                uiColors = uiColors
                                            )
                                        }

                                        items(otherSongs) { song ->
                                            TrackStandingCard(
                                                song = song,
                                                onClick = {
                                                    focusManager.clearFocus()
                                                    onSongSelect(song)
                                                },
                                                uiColors = uiColors
                                            )
                                        }

                                        // Load-more footer squiggle
                                        item {
                                            LoadMoreSquiggle(uiColors = uiColors)
                                        }
                                        item {
                                            Spacer(modifier = Modifier.height(80.dp))
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        state = playlistListState,
                                        modifier = Modifier.fillMaxSize().weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        contentPadding = PaddingValues(bottom = 100.dp)
                                    ) {
                                        items(playlistResults) { playlist ->
                                            PlaylistStandingCard(
                                                playlist = playlist,
                                                onClick = {
                                                    focusManager.clearFocus()
                                                    onActivePlaylistChange(playlist)
                                                },
                                                onPlayClick = {
                                                    onPlayPlaylist(playlist)
                                                },
                                                uiColors = uiColors
                                            )
                                        }
                                        // Load-more footer squiggle
                                        item {
                                            LoadMoreSquiggle(uiColors = uiColors)
                                        }
                                        item {
                                            Spacer(modifier = Modifier.height(80.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }


            val showViewAll = viewAllGenresState
            if (showViewAll) {
                val allGenresList = listOf(
                    "Pop", "Rock", "Hip Hop", "EDM", "R&B", "Jazz", "Classical", "K-Pop", "Lo-Fi",
                    "Metal", "Country", "Blues", "Soul", "Reggae", "Folk", "Acoustic", "Indie",
                    "Latin", "Punk", "Synthwave", "J-Pop", "House", "Techno", "Ambient",
                    "Trap", "Gospel", "Afrobeats", "Dancehall", "Alternative", "Grunge",
                    "Funk", "Disco", "Bossa Nova", "Flamenco", "Opera", "New Age",
                    "Chill", "Workout", "Study", "Sleep", "Party", "Romance"
                )
                ViewAllGenresView(
                    genres = allGenresList,
                    onBackClick = { viewAllGenres = false },
                    onGenreClick = { genre ->
                        activeGenre = genre
                        onLoadGenreTracks(genre)
                    },
                    uiColors = uiColors,
                    modifier = Modifier
                        .fillMaxSize()
                        .offset {
                            IntOffset(
                                x = ((screenWidth.value * density.density * (1f - viewAllTransitionProgress.value)) -
                                     (screenWidth.value * density.density * 0.3f * genreDetailTransitionProgress.value)).toInt(),
                                y = 0
                            )
                        }
                        .shadow(8.dp, clip = false)
                        .background(uiColors.background)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp)
                )
            }

            // ViewAllArtists: stays visible while artist detail slides in on top
            val showViewAllArtists = viewAllArtistsState
            if (showViewAllArtists) {
                val famousArtistsList = TopArtists.names
                ViewAllArtistsView(
                    artists = famousArtistsList,
                    artistThumbnails = artistThumbnails,
                    onBackClick = { viewAllArtists = false },
                    onArtistClick = { artistName ->
                        activeArtist = artistName
                    },
                    uiColors = uiColors,
                    modifier = Modifier
                        .fillMaxSize()
                        .offset {
                            IntOffset(
                                x = ((screenWidth.value * density.density * (1f - viewAllArtistsTransitionProgress.value)) -
                                     (screenWidth.value * density.density * 0.3f * transitionProgress.value)).toInt(),
                                y = 0
                            )
                        }
                        .shadow(8.dp, clip = false)
                        .background(uiColors.background)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp)
                )
            }

            if (activeGenreState != null) {
                val currentGenre = activeGenreState!!
                GenreDetailView(
                    genreName = currentGenre,
                    tracks = genreTracks,
                    isLoading = isGenreLoading,
                    onBackClick = {
                        activeGenre = null
                        onClearGenreTracks()
                    },
                    onSongSelect = { song ->
                        focusManager.clearFocus()
                        onSongSelect(song)
                    },
                    onRefresh = { onLoadGenreTracks(currentGenre) },
                    onLoadMore = { onLoadMoreGenreTracks(currentGenre) },
                    uiColors = uiColors,
                    modifier = Modifier
                        .fillMaxSize()
                        .offset {
                            IntOffset(
                                x = (screenWidth.value * density.density * (1f - genreDetailTransitionProgress.value)).toInt(),
                                y = 0
                            )
                        }
                        .shadow(8.dp, clip = false)
                        .background(uiColors.background)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp)
                )
            }

            // Artist detail renders LAST so it's always on top of all other panels
            if (activeArtistState != null) {
                val currentArtist = activeArtistState!!
                ArtistDetailView(
                    artistName = currentArtist,
                    tracks = artistTracks,
                    isLoading = isArtistLoading,
                    onBackClick = { activeArtist = null },
                    onSongSelect = { song ->
                        focusManager.clearFocus()
                        onSongSelect(song)
                    },
                    onRefresh = { onLoadArtistTracks(currentArtist) },
                    onLoadMore = { onLoadMoreArtistTracks(currentArtist) },
                    uiColors = uiColors,
                    artistThumbnailUrl = artistThumbnails[currentArtist],
                    modifier = Modifier
                        .fillMaxSize()
                        .offset {
                            IntOffset(
                                x = (screenWidth.value * density.density * (1f - transitionProgress.value)).toInt(),
                                y = 0
                            )
                        }
                        .shadow(16.dp, clip = false)
                        .background(uiColors.background)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
fun TrackStandingCard(
    song: Song,
    onClick: () -> Unit,
    uiColors: UIColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(uiColors.cardBackground, shape = RoundedCornerShape(20.dp))
            .border(1.dp, uiColors.cardBorder, RoundedCornerShape(20.dp))
            .clickable { onClick() }
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

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(32.dp)
                .background(uiColors.cardBorder, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = uiColors.textPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun PlaylistStandingCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    uiColors: UIColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(uiColors.cardBackground, shape = RoundedCornerShape(20.dp))
            .border(1.dp, uiColors.cardBorder, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = playlist.thumbnailUrl,
            contentDescription = "Playlist Thumbnail",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.LightGray)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = playlist.title,
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
                    text = "By ${playlist.author}",
                    color = uiColors.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false),
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))

                if (!playlist.songCountText.isNullOrBlank()) {
                    BadgeCapsule(text = playlist.songCountText, uiColors = uiColors)
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(uiColors.cardBorder, shape = CircleShape)
                .clickable { onPlayClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play Playlist",
                tint = uiColors.textPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun PlaylistDetailView(
    playlist: Playlist,
    tracks: List<Song>,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onSongSelect: (Song) -> Unit,
    onRefresh: () -> Unit,
    uiColors: UIColors,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
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
                    .clickable { onBackClick() },
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
                text = "Playlist Details",
                color = uiColors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(146.dp)
                .background(uiColors.cardBackground, shape = RoundedCornerShape(24.dp))
                .border(1.dp, uiColors.cardBorder, RoundedCornerShape(24.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = playlist.thumbnailUrl,
                    contentDescription = "Playlist Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.LightGray)
                )
                Spacer(modifier = Modifier.width(18.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = playlist.title,
                        color = uiColors.textPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Created by ${playlist.author}",
                        color = uiColors.textSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    playlist.songCountText?.let {
                        Spacer(modifier = Modifier.height(6.dp))
                        BadgeCapsule(text = it, uiColors = uiColors)
                    }
                }
            }

            if (tracks.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 12.dp, end = 12.dp)
                        .size(44.dp)
                        .background(uiColors.progressAccent, shape = CircleShape)
                        .clickable { onSongSelect(tracks.first()) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play All",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (isLoading && tracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Loading tracks...",
                        color = uiColors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    SquigglyProgressBar(
                        progress = null,
                        color = uiColors.progressAccent,
                        trackColor = uiColors.cardBorder,
                        animated = true,
                        animationSpeed = 700,
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(16.dp)
                    )
                }
            }
        } else {
            PullToRefreshContainer(
                isRefreshing = isLoading,
                onRefresh = onRefresh,
                accentColor = uiColors.progressAccent,
                backgroundColor = uiColors.cardBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (isLoading) {
                        SquigglyProgressBar(
                            progress = null,
                            color = uiColors.progressAccent,
                            trackColor = uiColors.cardBorder,
                            wavelength = 56.dp,
                            amplitude = 5.dp,
                            strokeWidth = 3.dp,
                            animated = true,
                            animationSpeed = 700,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp)
                                .padding(vertical = 4.dp, horizontal = 16.dp)
                        )
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .align(Alignment.CenterHorizontally)
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(tracks) { song ->
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
fun LoadMoreSquiggle(uiColors: UIColors) {
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
                color = uiColors.progressAccent,
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

@Composable
fun ArtistCard(
    artistName: String,
    thumbnailUrl: String?,
    onClick: () -> Unit,
    uiColors: UIColors
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(90.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(uiColors.cardBackground)
                .border(1.dp, uiColors.cardBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (!thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = artistName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = artistName,
                    tint = uiColors.textSecondary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = artistName,
            color = uiColors.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ArtistDetailView(
    artistName: String,
    tracks: List<Song>,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onSongSelect: (Song) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    uiColors: UIColors,
    artistThumbnailUrl: String? = null,
    modifier: Modifier = Modifier
) {
    val artistListState = rememberLazyListState()

    val shouldLoadMoreArtist by remember {
        derivedStateOf {
            val lastVisibleItem = artistListState.layoutInfo.visibleItemsInfo.lastOrNull()
                ?: return@derivedStateOf false
            lastVisibleItem.index >= artistListState.layoutInfo.totalItemsCount - 4
        }
    }

    LaunchedEffect(shouldLoadMoreArtist) {
        if (shouldLoadMoreArtist) {
            onLoadMore()
        }
    }

    Column(
        modifier = modifier
    ) {
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
                    .clickable { onBackClick() },
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
                text = "Artist Profile",
                color = uiColors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(146.dp)
                .background(uiColors.cardBackground, shape = RoundedCornerShape(24.dp))
                .border(1.dp, uiColors.cardBorder, RoundedCornerShape(24.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val representativeThumbnail = artistThumbnailUrl ?: tracks.firstOrNull()?.thumbnailUrl
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(uiColors.cardBorder)
                        .border(2.dp, uiColors.progressAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!representativeThumbnail.isNullOrBlank()) {
                        AsyncImage(
                            model = representativeThumbnail,
                            contentDescription = "Artist Art",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Artist Art",
                            tint = uiColors.textSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(18.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = artistName,
                        color = uiColors.textPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Artist",
                        color = uiColors.textSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (tracks.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 12.dp, end = 12.dp)
                        .size(44.dp)
                        .background(uiColors.progressAccent, shape = CircleShape)
                        .clickable { onSongSelect(tracks.first()) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play All",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (isLoading && tracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Loading tracks...",
                        color = uiColors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    SquigglyProgressBar(
                        progress = null,
                        color = uiColors.progressAccent,
                        trackColor = uiColors.cardBorder,
                        animated = true,
                        animationSpeed = 700,
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(16.dp)
                    )
                }
            }
        } else {
            PullToRefreshContainer(
                isRefreshing = isLoading,
                onRefresh = onRefresh,
                accentColor = uiColors.progressAccent,
                backgroundColor = uiColors.cardBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (isLoading) {
                        SquigglyProgressBar(
                            progress = null,
                            color = uiColors.progressAccent,
                            trackColor = uiColors.cardBorder,
                            wavelength = 56.dp,
                            amplitude = 5.dp,
                            strokeWidth = 3.dp,
                            animated = true,
                            animationSpeed = 700,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp)
                                .padding(vertical = 4.dp, horizontal = 16.dp)
                        )
                    }
                    LazyColumn(
                        state = artistListState,
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .align(Alignment.CenterHorizontally)
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(tracks) { song ->
                            TrackStandingCard(
                                song = song,
                                onClick = { onSongSelect(song) },
                                uiColors = uiColors
                            )
                        }
                        // Load-more footer squiggle
                        item {
                            LoadMoreSquiggle(uiColors = uiColors)
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }
}

fun getGenreGradient(genre: String): androidx.compose.ui.graphics.Brush {
    val colors = when (genre) {
        "Pop" -> listOf(Color(0xFFE91E63), Color(0xFFFF4081))
        "Rock" -> listOf(Color(0xFFF44336), Color(0xFFFF5252))
        "Hip Hop" -> listOf(Color(0xFF9C27B0), Color(0xFFE040FB))
        "EDM" -> listOf(Color(0xFF00BCD4), Color(0xFF18FFFF))
        "R&B" -> listOf(Color(0xFF673AB7), Color(0xFF7C4DFF))
        "Jazz" -> listOf(Color(0xFFFF9800), Color(0xFFFFAB40))
        "Classical" -> listOf(Color(0xFF3F51B5), Color(0xFF536DFE))
        "K-Pop" -> listOf(Color(0xFF4CAF50), Color(0xFF69F0AE))
        "Lo-Fi" -> listOf(Color(0xFF607D8B), Color(0xFF90A4AE))
        "Metal" -> listOf(Color(0xFF212121), Color(0xFF484848))
        "Country" -> listOf(Color(0xFF8D6E63), Color(0xFFA1887F))
        "Blues" -> listOf(Color(0xFF0D47A1), Color(0xFF1976D2))
        "Soul" -> listOf(Color(0xFFE65100), Color(0xFFF57C00))
        "Reggae" -> listOf(Color(0xFF2E7D32), Color(0xFF4CAF50))
        "Folk" -> listOf(Color(0xFF4E342E), Color(0xFF5D4037))
        "Acoustic" -> listOf(Color(0xFF827717), Color(0xFF9E9D24))
        "Indie" -> listOf(Color(0xFF00695C), Color(0xFF00897B))
        "Latin" -> listOf(Color(0xFFD84315), Color(0xFFE64A19))
        "Punk" -> listOf(Color(0xFFC62828), Color(0xFFD32F2F))
        "Synthwave" -> listOf(Color(0xFFAD1457), Color(0xFFC2185B))
        "J-Pop" -> listOf(Color(0xFF283593), Color(0xFF303F9F))
        "House" -> listOf(Color(0xFF1565C0), Color(0xFF1E88E5))
        "Techno" -> listOf(Color(0xFF37474F), Color(0xFF455A64))
        "Ambient" -> listOf(Color(0xFF00838F), Color(0xFF0097A7))
        // New genres — all unique vibrant colors
        "Trap" -> listOf(Color(0xFF6A1B9A), Color(0xFFAB47BC))
        "Gospel" -> listOf(Color(0xFFFF8F00), Color(0xFFFFCA28))
        "Afrobeats" -> listOf(Color(0xFF558B2F), Color(0xFF8BC34A))
        "Dancehall" -> listOf(Color(0xFFBF360C), Color(0xFFFF7043))
        "Alternative" -> listOf(Color(0xFF37474F), Color(0xFF78909C))
        "Grunge" -> listOf(Color(0xFF4E342E), Color(0xFF795548))
        "Funk" -> listOf(Color(0xFFE91E63), Color(0xFFFF6090))
        "Disco" -> listOf(Color(0xFFF9A825), Color(0xFFFDD835))
        "Bossa Nova" -> listOf(Color(0xFF00796B), Color(0xFF4DB6AC))
        "Flamenco" -> listOf(Color(0xFFC62828), Color(0xFFE57373))
        "Opera" -> listOf(Color(0xFF311B92), Color(0xFF7C4DFF))
        "New Age" -> listOf(Color(0xFF006064), Color(0xFF4DD0E1))
        "Chill" -> listOf(Color(0xFF1A237E), Color(0xFF5C6BC0))
        "Workout" -> listOf(Color(0xFFB71C1C), Color(0xFFEF5350))
        "Study" -> listOf(Color(0xFF0D47A1), Color(0xFF64B5F6))
        "Sleep" -> listOf(Color(0xFF1A237E), Color(0xFF9FA8DA))
        "Party" -> listOf(Color(0xFFE040FB), Color(0xFFFF4081))
        "Romance" -> listOf(Color(0xFFC2185B), Color(0xFFF48FB1))
        else -> listOf(Color(0xFF546E7A), Color(0xFF90A4AE))
    }
    return androidx.compose.ui.graphics.Brush.linearGradient(colors)
}

@Composable
fun GenreCard(
    genreName: String,
    onClick: () -> Unit,
    onPlayClick: (() -> Unit)?,
    uiColors: UIColors,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(uiColors.cardBackground, shape = RoundedCornerShape(16.dp))
            .border(1.dp, uiColors.cardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(getGenreGradient(genreName)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = genreName,
            color = uiColors.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (onPlayClick != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(uiColors.cardBorder, shape = CircleShape)
                    .clickable { onPlayClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Genre",
                    tint = uiColors.textPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun ViewAllGenreCard(
    onClick: () -> Unit,
    uiColors: UIColors,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(uiColors.cardBackground, shape = RoundedCornerShape(16.dp))
            .border(1.dp, uiColors.cardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            uiColors.progressAccent,
                            uiColors.cardBorder
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "View All",
            color = uiColors.textPrimary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ArrowBackIosNew,
            contentDescription = "View All",
            tint = uiColors.textSecondary,
            modifier = Modifier
                .size(14.dp)
                .rotate(180f)
        )
    }
}

@Composable
fun ViewAllGenresView(
    genres: List<String>,
    onBackClick: () -> Unit,
    onGenreClick: (String) -> Unit,
    uiColors: UIColors,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = modifier
    ) {
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
                    .clickable { onBackClick() },
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
                text = "All Genres",
                color = uiColors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            val rows = genres.chunked(2)
            items(rows) { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { genre ->
                        Box(modifier = Modifier.weight(1f)) {
                            GenreCard(
                                genreName = genre,
                                onClick = {
                                    focusManager.clearFocus()
                                    onGenreClick(genre)
                                },
                                onPlayClick = null,
                                uiColors = uiColors
                            )
                        }
                    }
                    if (rowItems.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun GenreDetailView(
    genreName: String,
    tracks: List<Song>,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onSongSelect: (Song) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    uiColors: UIColors,
    modifier: Modifier = Modifier
) {
    val genreListState = rememberLazyListState()

    val shouldLoadMoreGenre by remember {
        derivedStateOf {
            val lastVisibleItem = genreListState.layoutInfo.visibleItemsInfo.lastOrNull()
                ?: return@derivedStateOf false
            lastVisibleItem.index >= genreListState.layoutInfo.totalItemsCount - 4
        }
    }

    LaunchedEffect(shouldLoadMoreGenre) {
        if (shouldLoadMoreGenre) {
            onLoadMore()
        }
    }

    Column(
        modifier = modifier
    ) {
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
                    .clickable { onBackClick() },
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
                text = "Genre Playlist",
                color = uiColors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(146.dp)
                .background(uiColors.cardBackground, shape = RoundedCornerShape(24.dp))
                .border(1.dp, uiColors.cardBorder, RoundedCornerShape(24.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val firstThumbnail = tracks.firstOrNull()?.thumbnailUrl
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(getGenreGradient(genreName)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!firstThumbnail.isNullOrBlank()) {
                        AsyncImage(
                            model = firstThumbnail,
                            contentDescription = "Genre Art",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Genre Art",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(18.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = genreName,
                        color = uiColors.textPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Curated Genre Playlist",
                        color = uiColors.textSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    BadgeCapsule(text = "Sorted by Views", uiColors = uiColors)
                }
            }

            if (tracks.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 12.dp, end = 12.dp)
                        .size(44.dp)
                        .background(uiColors.progressAccent, shape = CircleShape)
                        .clickable { onSongSelect(tracks.first()) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play All",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (isLoading && tracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Loading tracks...",
                        color = uiColors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    SquigglyProgressBar(
                        progress = null,
                        color = uiColors.progressAccent,
                        trackColor = uiColors.cardBorder,
                        animated = true,
                        animationSpeed = 700,
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(16.dp)
                    )
                }
            }
        } else {
            PullToRefreshContainer(
                isRefreshing = isLoading,
                onRefresh = onRefresh,
                accentColor = uiColors.progressAccent,
                backgroundColor = uiColors.cardBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (isLoading) {
                        SquigglyProgressBar(
                            progress = null,
                            color = uiColors.progressAccent,
                            trackColor = uiColors.cardBorder,
                            wavelength = 56.dp,
                            amplitude = 5.dp,
                            strokeWidth = 3.dp,
                            animated = true,
                            animationSpeed = 700,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp)
                                .padding(vertical = 4.dp, horizontal = 16.dp)
                        )
                    }
                    LazyColumn(
                        state = genreListState,
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .align(Alignment.CenterHorizontally)
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(tracks) { song ->
                            TrackStandingCard(
                                song = song,
                                onClick = { onSongSelect(song) },
                                uiColors = uiColors
                            )
                        }
                        item {
                            LoadMoreSquiggle(uiColors = uiColors)
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateArtistCard(
    artistName: String,
    onClick: () -> Unit,
    thumbnailUrl: String?,
    uiColors: UIColors,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(uiColors.cardBackground, shape = RoundedCornerShape(16.dp))
            .border(1.dp, uiColors.cardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(uiColors.cardBorder),
            contentAlignment = Alignment.Center
        ) {
            if (!thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = artistName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = uiColors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = artistName,
            color = uiColors.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ViewAllArtistsView(
    artists: List<String>,
    artistThumbnails: Map<String, String>,
    onBackClick: () -> Unit,
    onArtistClick: (String) -> Unit,
    uiColors: UIColors,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val initialCount = 20
    val pageSize = 10
    var visibleCount by remember { mutableStateOf(initialCount) }
    var isLoadingMore by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Without distinctUntilChanged: after each batch loads, snapshotFlow re-evaluates
    // and triggers the next batch if user is still near the bottom.
    // isLoadingMore guard prevents concurrent triggers.
    LaunchedEffect(listState) {
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            val total = listState.layoutInfo.totalItemsCount
            lastVisible != null && total > 0 && lastVisible.index >= total - 3
        }.collect { nearEnd ->
            if (nearEnd && !isLoadingMore && visibleCount < artists.size) {
                isLoadingMore = true
                delay(500) // let squiggly animate before revealing next batch
                visibleCount = (visibleCount + pageSize).coerceAtMost(artists.size)
                isLoadingMore = false
            }
        }
    }

    val visibleArtists = artists.take(visibleCount)

    Column(
        modifier = modifier
    ) {
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
                    .clickable { onBackClick() },
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
                text = "All Artists",
                color = uiColors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            val rows = visibleArtists.chunked(2)
            items(rows) { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { artist ->
                        Box(modifier = Modifier.weight(1f)) {
                            EmptyStateArtistCard(
                                artistName = artist,
                                onClick = {
                                    focusManager.clearFocus()
                                    onArtistClick(artist)
                                },
                                thumbnailUrl = artistThumbnails[artist],
                                uiColors = uiColors
                            )
                        }
                    }
                    if (rowItems.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            // Only show the squiggly while actively loading — not as a permanent footer
            if (isLoadingMore) {
                item {
                    LoadMoreSquiggle(uiColors = uiColors)
                }
            }
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
