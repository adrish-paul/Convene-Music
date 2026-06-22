package com.example.convenemusic.ui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.convenemusic.network.InnerTubeClient
import com.example.convenemusic.network.Song
import com.example.convenemusic.network.LyricLine
import com.example.convenemusic.network.LyricsData
import com.example.convenemusic.playback.AndroidMediaPlayerEngine
import com.example.convenemusic.playback.AutoplayEngine
import com.example.convenemusic.playback.PlaybackServiceConnector
import com.example.convenemusic.data.repository.LocalMediaRepository
import com.example.convenemusic.data.repository.LocalMediaRepositoryImpl
import com.example.convenemusic.data.repository.HistoryRepository
import com.example.convenemusic.data.repository.HistoryRepositoryImpl
import com.example.convenemusic.data.repository.DownloadRepository
import com.example.convenemusic.data.repository.DownloadRepositoryImpl
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
val Song.viewCount: Long
    get() {
        val text = viewsText ?: return 0L
        val clean = text.lowercase().replace("views", "").replace("view", "").trim()
        return try {
            if (clean.endsWith("b")) {
                (clean.removeSuffix("b").toDouble() * 1_000_000_000).toLong()
            } else if (clean.endsWith("m")) {
                (clean.removeSuffix("m").toDouble() * 1_000_000).toLong()
            } else if (clean.endsWith("k")) {
                (clean.removeSuffix("k").toDouble() * 1_000).toLong()
            } else {
                clean.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L
            }
        } catch (e: Exception) {
            0L
        }
    }

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val client = InnerTubeClient()
    private val loadingThumbnails = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    val mediaPlayerEngine = AndroidMediaPlayerEngine(application.applicationContext)
    private lateinit var autoplayEngine: AutoplayEngine
    private val backgroundScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /** Returns true if the device has an active network connection with internet capability */
    fun isNetworkAvailable(): Boolean {
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun showNoInternetToast() {
        Toast.makeText(
            getApplication<Application>().applicationContext,
            "No internet connection",
            Toast.LENGTH_SHORT
        ).show()
    }

    private val localMediaRepository: LocalMediaRepository = LocalMediaRepositoryImpl(application)
    private val historyRepository: HistoryRepository = HistoryRepositoryImpl(application)
    private val downloadRepository: DownloadRepository = DownloadRepositoryImpl(application, client)
    private val downloadJobs = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.Job>()

    private val _customPlaylists = MutableStateFlow<List<CustomPlaylist>>(emptyList())
    val customPlaylists: StateFlow<List<CustomPlaylist>> = _customPlaylists.asStateFlow()
    private val playlistsFile = File(application.filesDir, "custom_playlists.json")

    private val _searchResults = MutableStateFlow<List<Song>>(emptyList())
    val searchResults: StateFlow<List<Song>> = _searchResults.asStateFlow()

    private val _lyrics = MutableStateFlow<LyricsData?>(null)
    val lyrics: StateFlow<LyricsData?> = _lyrics.asStateFlow()

    private val _isLyricsLoading = MutableStateFlow(false)
    val isLyricsLoading: StateFlow<Boolean> = _isLyricsLoading.asStateFlow()

    private val _lyricsError = MutableStateFlow<String?>(null)
    val lyricsError: StateFlow<String?> = _lyricsError.asStateFlow()

    private val _playlistResults = MutableStateFlow<List<com.example.convenemusic.network.Playlist>>(emptyList())
    val playlistResults: StateFlow<List<com.example.convenemusic.network.Playlist>> = _playlistResults.asStateFlow()

    private val _playlistTracks = MutableStateFlow<List<Song>>(emptyList())
    val playlistTracks: StateFlow<List<Song>> = _playlistTracks.asStateFlow()

    private val _artistTracks = MutableStateFlow<List<Song>>(emptyList())
    val artistTracks: StateFlow<List<Song>> = _artistTracks.asStateFlow()

    private val _isArtistLoading = MutableStateFlow(false)
    val isArtistLoading: StateFlow<Boolean> = _isArtistLoading.asStateFlow()

    private val _artistThumbnails = MutableStateFlow<Map<String, String>>(emptyMap())
    val artistThumbnails: StateFlow<Map<String, String>> = _artistThumbnails.asStateFlow()

    private val _genreTracks = MutableStateFlow<List<Song>>(emptyList())
    val genreTracks: StateFlow<List<Song>> = _genreTracks.asStateFlow()

    private val _isGenreLoading = MutableStateFlow(false)
    val isGenreLoading: StateFlow<Boolean> = _isGenreLoading.asStateFlow()

    private var genreContinuationToken: String? = null

    private var lastSearchQuery: String = ""

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _currentQueueIndex = MutableStateFlow(0)
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()

    private val _isQueueEndless = MutableStateFlow(true)
    val isQueueEndless: StateFlow<Boolean> = _isQueueEndless.asStateFlow()

    private val _isQueueLoadingMore = MutableStateFlow(false)
    val isQueueLoadingMore: StateFlow<Boolean> = _isQueueLoadingMore.asStateFlow()

    private val _loopMode = MutableStateFlow(0)
    val loopMode: StateFlow<Int> = _loopMode.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val autoplayContinuationTokens = mutableMapOf<String, String>()

    private var _originalPlaylistTracks: List<Song> = emptyList()

    private val _historyList = MutableStateFlow<List<Song>>(emptyList())
    val historyList: StateFlow<List<Song>> = _historyList.asStateFlow()

    private val _downloadedList = MutableStateFlow<List<Song>>(emptyList())
    val downloadedList: StateFlow<List<Song>> = _downloadedList.asStateFlow()

    private val _downloadingSongs = MutableStateFlow<Set<String>>(emptySet())
    val downloadingSongs: StateFlow<Set<String>> = _downloadingSongs.asStateFlow()

    private val _downloadingQueue = MutableStateFlow<List<Song>>(emptyList())
    val downloadingQueue: StateFlow<List<Song>> = _downloadingQueue.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Int>> = _downloadProgress.asStateFlow()

    // Local device audio scanner
    private val _localTracks = MutableStateFlow<List<Song>>(emptyList())
    val localTracks: StateFlow<List<Song>> = _localTracks.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // Local device video scanner
    private val _localVideos = MutableStateFlow<List<Movie>>(emptyList())
    val localVideos: StateFlow<List<Movie>> = _localVideos.asStateFlow()

    private val _isVideoScanning = MutableStateFlow(false)
    val isVideoScanning: StateFlow<Boolean> = _isVideoScanning.asStateFlow()

    private val CHANNEL_ID = "downloads_channel"

    init {
        autoplayEngine = AutoplayEngine(
            client = client,
            queue = _queue,
            currentQueueIndex = _currentQueueIndex,
            isQueueEndless = _isQueueEndless,
            isQueueLoadingMore = _isQueueLoadingMore,
            autoplayContinuationTokens = autoplayContinuationTokens,
            getOriginalPlaylistTracks = { _originalPlaylistTracks },
            getSongsForQuery = { query -> getSongsForQuery(query) }
        )

        viewModelScope.launch {
            delay(4000)
            _showSplash.value = false
        }
        // Pre-buffer the last played song in the background during the splash
        // so playback starts instantly when the user taps Play
        preloadLastSong()
        PlaybackServiceConnector.onNext = { isAuto ->
            playNextSong(isManual = !isAuto)
        }
        PlaybackServiceConnector.onPrevious = {
            playPreviousSong()
        }
        loadHistory()
        loadDownloads()
        loadCustomPlaylists()
        createNotificationChannel()
        prefetchFamousArtists()

        // Observe current song changes to update index, pre-buffer the next song, and fetch lyrics
        viewModelScope.launch {
            var fetchLyricsJob: kotlinx.coroutines.Job? = null
            mediaPlayerEngine.currentSong.collect { song ->
                fetchLyricsJob?.cancel()
                _lyrics.value = null
                _lyricsError.value = null
                if (song != null) {
                    val index = _queue.value.indexOfFirst { it.id == song.id }
                    if (index >= 0 && index != _currentQueueIndex.value) {
                        _currentQueueIndex.value = index
                        prebufferNextSong()
                    }
                    fetchLyricsJob = viewModelScope.launch {
                        fetchLyricsForSong(song)
                    }
                }
            }
        }

        // Automatically save queue and index on any modification
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(_queue, _currentQueueIndex) { q, idx -> q to idx }
                .collect {
                    saveQueueState()
                }
        }
    }

    /**
     * Fetches the stream URL for the last played song (already in PlaybackManager state)
     * and calls playbackManager.preload() — preparing ExoPlayer without starting playback.
     * When the user later taps Play, ExoPlayer is already buffered → instant start.
     */
    private fun preloadLastSong() {
        loadQueueState()
        val song = mediaPlayerEngine.currentSong.value ?: return
        
        // If queue wasn't restored successfully, initialize it with the current song
        if (_queue.value.isEmpty()) {
            _queue.value = listOf(song)
            _currentQueueIndex.value = 0
        }
        _isQueueEndless.value = true

        // Only preload network songs (not local files)
        if (song.id.startsWith("/")) return

        viewModelScope.launch {
            try {
                if (isNetworkAvailable()) {
                    generateAutoplaySongs()
                }
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Failed to generate autoplay recommendations for restored song", e)
            }
        }

        viewModelScope.launch {
            try {
                val url = if (downloadRepository.isDownloaded(song.id)) {
                    val downloadsDir = File(getApplication<Application>().filesDir, ".downloads")
                    File(downloadsDir, "${song.id}.mp3").absolutePath
                } else {
                    if (!isNetworkAvailable()) return@launch
                    var streamUrl: String? = null
                    for (attempt in 1..3) {
                        streamUrl = try { client.getStreamUrl(song.id) } catch (e: Exception) { null }
                        if (streamUrl != null) break
                        if (attempt < 3) delay(500L * (1 shl attempt))
                    }
                    streamUrl
                } ?: return@launch

                val artworkBytes = mediaPlayerEngine.getArtworkBytes(song.thumbnailUrl)
                mediaPlayerEngine.preload(song, url, artworkBytes)
                Log.e("MusicViewModel", "preloadLastSong: Pre-buffered '${song.title}'")
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    Log.e("MusicViewModel", "preloadLastSong: Failed for ${song.title}: ${e.message}")
                }
            }
        }
    }

    private fun prefetchFamousArtists() {
        viewModelScope.launch {
            val famousArtists = listOf(
                // Same 20 as ViewAllArtistsView — curated across genres
                "Taylor Swift", "Drake", "The Weeknd", "Billie Eilish", "Arijit Singh",
                "Ed Sheeran", "Eminem", "Bruno Mars", "Coldplay", "BTS",
                "Dua Lipa", "Ariana Grande", "Post Malone", "Adele", "Justin Bieber",
                "Imagine Dragons", "Kendrick Lamar", "Rihanna", "Lady Gaga", "Bad Bunny"
            )
            famousArtists.forEach { artistName ->
                if (!_artistThumbnails.value.containsKey(artistName)) {
                    launch {
                        try {
                            val thumb = client.getArtistThumbnail(artistName)
                            if (thumb != null) {
                                _artistThumbnails.value = _artistThumbnails.value + (artistName to thumb)
                            }
                        } catch (e: Exception) {
                            Log.e("MusicViewModel", "Failed to prefetch thumbnail for $artistName", e)
                        }
                    }
                }
            }
        }
    }

    fun loadArtistThumbnail(artistName: String) {
        if (artistName.isBlank() || _artistThumbnails.value.containsKey(artistName)) return
        if (!loadingThumbnails.add(artistName)) return
        viewModelScope.launch {
            try {
                if (!isNetworkAvailable()) return@launch
                val thumb = client.getArtistThumbnail(artistName)
                if (thumb != null) {
                    _artistThumbnails.value = _artistThumbnails.value + (artistName to thumb)
                }
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Failed to load thumbnail for $artistName", e)
            } finally {
                loadingThumbnails.remove(artistName)
            }
        }
    }

    private val _showSplash = MutableStateFlow(true)
    val showSplash = _showSplash.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isPlaylistLoading = MutableStateFlow(false)
    val isPlaylistLoading: StateFlow<Boolean> = _isPlaylistLoading.asStateFlow()

    val currentSong: StateFlow<Song?> = mediaPlayerEngine.currentSong
    val isPlaying: StateFlow<Boolean> = mediaPlayerEngine.isPlaying
    val currentPosition: StateFlow<Long> = mediaPlayerEngine.currentPosition
    val duration: StateFlow<Long> = mediaPlayerEngine.duration
    /** Reflects ExoPlayer's STATE_BUFFERING — true while the player is filling its network buffer */
    val isBuffering: StateFlow<Boolean> = mediaPlayerEngine.isBuffering

    private var songContinuationToken: String? = null
    private var playlistContinuationToken: String? = null

    private val _isLoadMoreLoading = MutableStateFlow(false)
    val isLoadMoreLoading: StateFlow<Boolean> = _isLoadMoreLoading.asStateFlow()

    private val _isStreamLoading = MutableStateFlow(false)
    /** True while the stream URL is being fetched OR while ExoPlayer is buffering */
    val isStreamLoading: StateFlow<Boolean> = _isStreamLoading.asStateFlow()

    private fun splitArtistNames(combinedName: String): List<String> {
        val bulletParts = combinedName.split(Regex(" • | •|• |•|\\u2022"))
        val mainArtistPart = bulletParts.firstOrNull()?.trim() ?: combinedName
        val separators = Regex(",|/|\\||&|\\s+\\.\\s+|\\bx\\b|\\bX\\b|\\band\\b|\\bfeat(?:\\.|\\b)|\\bft(?:\\.|\\b)", RegexOption.IGNORE_CASE)
        return mainArtistPart.split(separators)
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.equals("unknown", ignoreCase = true) }
    }

    private fun editDistance(s1: String, s2: String): Int {
        val dp = IntArray(s2.length + 1) { it }
        for (i in 1..s1.length) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..s2.length) {
                val temp = dp[j]
                if (s1[i - 1] == s2[j - 1]) {
                    dp[j] = prev
                } else {
                    dp[j] = minOf(dp[j - 1], dp[j], prev) + 1
                }
                prev = temp
            }
        }
        return dp[s2.length]
    }

    private fun isArtistMatch(songArtist: String, targetArtist: String): Boolean {
        val target = targetArtist.trim().lowercase()
        val artists = splitArtistNames(songArtist).map { it.lowercase() }
        return artists.any { artist ->
            if (artist == target) return@any true
            if (artist.contains(target) || target.contains(artist)) return@any true
            
            val distance = editDistance(artist, target)
            val maxLength = maxOf(artist.length, target.length)
            if (maxLength == 0) return@any false
            val similarity = 1.0 - (distance.toDouble() / maxLength.toDouble())
            similarity >= 0.84
        }
    }

    private fun calculateMatchScore(song: Song, query: String): Int {
        val title = song.title.lowercase()
        val q = query.lowercase().trim()
        if (q.isEmpty()) return 0

        val artists = splitArtistNames(song.artist).map { it.lowercase() }
        val isExactArtistMatch = artists.any { it == q }
        val isArtistStartMatch = artists.any { it.startsWith(q) }
        val isArtistContainMatch = artists.any { it.contains(q) }

        return when {
            title == q -> 100 // Exact title match
            title.startsWith(q) -> 80 // Title starts with query
            title.contains(q) -> 60 // Title contains query
            isExactArtistMatch -> 40 // Exact artist match
            isArtistStartMatch -> 30 // Artist starts with query
            isArtistContainMatch -> 20 // Artist contains query
            else -> 0
        }
    }

    private fun getSortedLocalMatches(query: String): List<Song> {
        val downloads = _downloadedList.value
        val localScanned = _localTracks.value

        val allLocal = (downloads + localScanned)
            .distinctBy { it.id }

        return allLocal
            .map { song -> song to calculateMatchScore(song, query) }
            .filter { (_, score) -> score > 0 }
            .sortedWith(
                compareByDescending<Pair<Song, Int>> { it.second }
                    .thenByDescending { downloadRepository.isDownloaded(it.first.id) }
            )
            .map { it.first }
    }

    private fun prefetchArtistThumbnails(songs: List<Song>, query: String) {
        viewModelScope.launch {
            val uniqueArtists = songs
                .flatMap { splitArtistNames(it.artist) }
                .filter { it.contains(query, ignoreCase = true) }
                .distinctBy { it.lowercase().trim() }
            
            uniqueArtists.forEach { artistName ->
                if (!_artistThumbnails.value.containsKey(artistName)) {
                    launch {
                        val thumb = client.getArtistThumbnail(artistName)
                        if (thumb != null) {
                            _artistThumbnails.value = _artistThumbnails.value + (artistName to thumb)
                        }
                    }
                }
            }
        }
    }

    private var searchJob: kotlinx.coroutines.Job? = null

    fun search(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            songContinuationToken = null
            lastSearchQuery = ""
            searchJob?.cancel()
            _isLoading.value = false
            return
        }
        lastSearchQuery = query
        
        // Instantly populate local matches (in-memory, extremely fast)
        val localMatches = getSortedLocalMatches(query)
        _searchResults.value = localMatches
        prefetchArtistThumbnails(localMatches, query)
        
        // Cancel the previous job and launch a debounced online search
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            // Check internet before hitting the network
            if (!isNetworkAvailable()) {
                showNoInternetToast()
                _isLoading.value = false
                return@launch
            }
            _isLoading.value = true
            // Debounce online search for 300ms
            kotlinx.coroutines.delay(300)
            try {
                val result = client.search(query)
                val freshLocalMatches = getSortedLocalMatches(query)
                val combined = (freshLocalMatches + result.songs).distinctBy { it.id }
                
                _searchResults.value = combined
                songContinuationToken = result.continuationToken
                prefetchArtistThumbnails(combined, query)
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    Log.e("MusicViewModel", "Search online failed: ${e.message}", e)
                    if (!isNetworkAvailable()) showNoInternetToast()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMoreSongs() {
        val token = songContinuationToken ?: return
        if (_isLoadMoreLoading.value) return

        viewModelScope.launch {
            _isLoadMoreLoading.value = true
            val result = client.searchMoreSongs(token)
            val combined = (_searchResults.value + result.songs).distinctBy { it.id }
            _searchResults.value = combined
            songContinuationToken = result.continuationToken
            _isLoadMoreLoading.value = false
            prefetchArtistThumbnails(result.songs, lastSearchQuery)
        }
    }

    private var artistContinuationToken: String? = null

    fun loadArtistTracks(artistName: String) {
        if (artistName.isBlank()) {
            _artistTracks.value = emptyList()
            artistContinuationToken = null
            return
        }
        viewModelScope.launch {
            _isArtistLoading.value = true
            
            if (!_artistThumbnails.value.containsKey(artistName)) {
                launch {
                    val thumb = client.getArtistThumbnail(artistName)
                    if (thumb != null) {
                        _artistThumbnails.value = _artistThumbnails.value + (artistName to thumb)
                    }
                }
            }

            val localMatches = _downloadedList.value
                .filter { isArtistMatch(it.artist, artistName) }
                .distinctBy { it.id }
            
            val searchResult = client.search(artistName)
            artistContinuationToken = searchResult.continuationToken
            val apiSongs = searchResult.songs.filter { isArtistMatch(it.artist, artistName) }
            
            val combined = (localMatches + apiSongs).distinctBy { it.id }
            val sorted = combined.sortedByDescending { it.viewCount }
            _artistTracks.value = sorted
            _isArtistLoading.value = false
        }
    }

    fun loadMoreArtistTracks(artistName: String) {
        val token = artistContinuationToken ?: return
        if (_isLoadMoreLoading.value) return

        viewModelScope.launch {
            _isLoadMoreLoading.value = true
            val result = client.searchMoreSongs(token)
            val newApiSongs = result.songs.filter { isArtistMatch(it.artist, artistName) }
            val combined = (_artistTracks.value + newApiSongs).distinctBy { it.id }
            val sorted = combined.sortedByDescending { it.viewCount }
            _artistTracks.value = sorted
            artistContinuationToken = result.continuationToken
            _isLoadMoreLoading.value = false
        }
    }

    fun clearArtistTracks() {
        _artistTracks.value = emptyList()
        artistContinuationToken = null
    }

    fun loadGenreTracks(genre: String) {
        if (genre.isBlank()) {
            _genreTracks.value = emptyList()
            genreContinuationToken = null
            return
        }
        viewModelScope.launch {
            _isGenreLoading.value = true
            
            val searchResult = client.search("$genre songs")
            genreContinuationToken = searchResult.continuationToken
            
            val sorted = searchResult.songs.sortedByDescending { it.viewCount }
            _genreTracks.value = sorted
            _isGenreLoading.value = false
        }
    }

    fun loadMoreGenreTracks(genre: String) {
        val token = genreContinuationToken ?: return
        if (_isLoadMoreLoading.value) return

        viewModelScope.launch {
            _isLoadMoreLoading.value = true
            val result = client.searchMoreSongs(token)
            
            val combined = (_genreTracks.value + result.songs).distinctBy { it.id }
            val sorted = combined.sortedByDescending { it.viewCount }
            _genreTracks.value = sorted
            genreContinuationToken = result.continuationToken
            _isLoadMoreLoading.value = false
        }
    }

    fun clearGenreTracks() {
        _genreTracks.value = emptyList()
        genreContinuationToken = null
    }

    fun searchPlaylists(query: String) {
        if (query.isBlank()) {
            _playlistResults.value = emptyList()
            playlistContinuationToken = null
            _isLoading.value = false
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val result = client.searchPlaylists(query)
            _playlistResults.value = result.playlists
            playlistContinuationToken = result.continuationToken
            _isLoading.value = false
        }
    }

    fun loadMorePlaylists() {
        val token = playlistContinuationToken ?: return
        if (_isLoadMoreLoading.value) return

        viewModelScope.launch {
            _isLoadMoreLoading.value = true
            val result = client.searchMorePlaylists(token)
            _playlistResults.value = _playlistResults.value + result.playlists
            playlistContinuationToken = result.continuationToken
            _isLoadMoreLoading.value = false
        }
    }

    fun loadPlaylistTracks(playlistId: String) {
        viewModelScope.launch {
            _isPlaylistLoading.value = true
            val tracks = client.getPlaylistTracks(playlistId)
            _playlistTracks.value = tracks
            _isPlaylistLoading.value = false
        }
    }

    fun clearPlaylistTracks() {
        _playlistTracks.value = emptyList()
    }

    private fun playSongInternal(song: Song) {
        Log.e("MusicViewModel", "playSongInternal: Playing ${song.title}")
        acquireTransitionWakeLock()
        mediaPlayerEngine.setCurrentSong(song)
        addToHistory(song)
        backgroundScope.launch(Dispatchers.IO) {
            _isStreamLoading.value = true
            try {
                val urlDeferred = async(Dispatchers.IO) {
                    if (song.id.startsWith("/")) {
                        song.id
                    } else if (downloadRepository.isDownloaded(song.id)) {
                        val downloadsDir = File(getApplication<Application>().filesDir, ".downloads")
                        val localFile = File(downloadsDir, "${song.id}.mp3")
                        Log.e("MusicViewModel", "playSongInternal: Local file found. Playing offline: ${localFile.absolutePath}")
                        localFile.absolutePath
                    } else if (song.id == prefetchedSongId && prefetchedUrl != null) {
                        Log.e("MusicViewModel", "playSongInternal: Using prefetched URL for ${song.title}")
                        prefetchedUrl
                    } else {
                        // Retry up to 3 times for network stream URLs with exponential backoff
                        var streamUrl: String? = null
                        for (attempt in 1..3) {
                            streamUrl = try {
                                client.getStreamUrl(song.id)
                            } catch (e: Exception) {
                                Log.e("MusicViewModel", "playSongInternal: Attempt $attempt failed for ${song.title}: ${e.message}")
                                null
                            }
                            if (streamUrl != null) break
                            if (attempt < 3) delay(500L * (1 shl attempt))
                        }
                        streamUrl
                    }
                }
                val artworkDeferred = async(Dispatchers.IO) {
                    if (song.id == prefetchedSongId && prefetchedArtwork != null) {
                        prefetchedArtwork
                    } else {
                        mediaPlayerEngine.getArtworkBytes(song.thumbnailUrl)
                    }
                }

                val url = urlDeferred.await()
                val artworkBytes = artworkDeferred.await()

                if (url != null) {
                    withContext(Dispatchers.Main) {
                        mediaPlayerEngine.play(song, url, artworkBytes)
                        // Start prebuffering the next track immediately after starting playback
                        prebufferNextSong()
                    }
                } else {
                    Log.e("MusicViewModel", "playSongInternal: Failed to resolve stream URL for ${song.title} after retries")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            getApplication<Application>().applicationContext,
                            "Could not load: ${song.title.take(40)}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    Log.e("MusicViewModel", "playSongInternal: Unexpected error for ${song.title}: ${e.message}", e)
                }
            } finally {
                _isStreamLoading.value = false
                releaseTransitionWakeLock()
            }
        }
    }

    fun playSong(song: Song, initialQueue: List<Song> = emptyList(), isPremadePlaylist: Boolean = false) {
        Log.e("MusicViewModel", "playSong: Requested playback for ${song.title} (ID: ${song.id})")
        autoplayContinuationTokens.clear()
        _isQueueEndless.value = !isPremadePlaylist
        _originalPlaylistTracks = initialQueue
        if (initialQueue.isNotEmpty()) {
            if (_shuffleEnabled.value) {
                val shuffled = initialQueue.shuffled().toMutableList()
                shuffled.remove(song)
                shuffled.add(0, song)
                _queue.value = shuffled
                _currentQueueIndex.value = 0
            } else {
                _queue.value = initialQueue
                val index = initialQueue.indexOfFirst { it.id == song.id }
                _currentQueueIndex.value = if (index >= 0) index else 0
            }
        } else {
            _queue.value = listOf(song)
            _currentQueueIndex.value = 0
        }

        playSongInternal(song)

        if ((initialQueue.isEmpty() || _currentQueueIndex.value >= _queue.value.size - 2) && _isQueueEndless.value) {
            generateAutoplaySongs()
        }
    }

    fun playNextSong(isManual: Boolean = false) {
        acquireTransitionWakeLock()
        val currentList = _queue.value
        val currentIndex = _currentQueueIndex.value

        if (_loopMode.value == 1 && !isManual) {
            val current = currentSong.value
            if (current != null) {
                playSongInternal(current)
            } else {
                releaseTransitionWakeLock()
            }
            return
        }

        val nextIndex = currentIndex + 1
        if (nextIndex < currentList.size) {
            _currentQueueIndex.value = nextIndex
            val nextSong = currentList[nextIndex]
            playSongInternal(nextSong)

            if (nextIndex >= currentList.size - 2 && _isQueueEndless.value) {
                generateAutoplaySongs()
            }
        } else {
            if (_isQueueEndless.value) {
                backgroundScope.launch(Dispatchers.IO) {
                    val job = generateAutoplaySongs()
                    job?.join()
                    val updatedList = _queue.value
                    if (nextIndex < updatedList.size) {
                        withContext(Dispatchers.Main) {
                            _currentQueueIndex.value = nextIndex
                            val nextSong = updatedList[nextIndex]
                            playSongInternal(nextSong)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            handleQueueEnd(currentList)
                        }
                    }
                }
            } else {
                handleQueueEnd(currentList)
            }
        }
    }

    private fun handleQueueEnd(currentList: List<Song>) {
        if (_loopMode.value == 2) {
            if (currentList.isNotEmpty()) {
                _currentQueueIndex.value = 0
                playSongInternal(currentList[0])
            } else {
                releaseTransitionWakeLock()
            }
        } else {
            releaseTransitionWakeLock()
            mediaPlayerEngine.pause()
        }
    }

    fun playPreviousSong() {
        acquireTransitionWakeLock()
        val currentPos = mediaPlayerEngine.currentPosition.value
        if (currentPos >= 5000L) {
            mediaPlayerEngine.seekTo(0L)
            releaseTransitionWakeLock()
        } else {
            val prevIndex = _currentQueueIndex.value - 1
            if (prevIndex >= 0) {
                _currentQueueIndex.value = prevIndex
                val prevSong = _queue.value[prevIndex]
                playSongInternal(prevSong)
            } else {
                mediaPlayerEngine.seekTo(0L)
                releaseTransitionWakeLock()
            }
        }
    }

    fun playQueueSong(index: Int) {
        if (index in _queue.value.indices) {
            _currentQueueIndex.value = index
            playSongInternal(_queue.value[index])
        }
    }

    private suspend fun getSongsForQuery(query: String): List<Song> = withContext(Dispatchers.IO) {
        val token = autoplayContinuationTokens[query]
        try {
            val result = if (token != null) {
                client.searchMoreSongs(token)
            } else {
                client.search(query)
            }
            if (result.continuationToken != null) {
                autoplayContinuationTokens[query] = result.continuationToken
            } else {
                autoplayContinuationTokens.remove(query)
            }
            result.songs
        } catch (e: Exception) {
            Log.e("MusicViewModel", "Error getSongsForQuery for $query: ${e.message}", e)
            emptyList()
        }
    }

    fun generateAutoplaySongs(): kotlinx.coroutines.Job? {
        return autoplayEngine.generateAutoplaySongs(backgroundScope)
    }

    fun togglePlayPause() {
        val current = currentSong.value
        if (current != null && mediaPlayerEngine.isIdle()) {
            if (_queue.value.isEmpty()) {
                _queue.value = listOf(current)
                _currentQueueIndex.value = 0
            }
            playSongInternal(current)
        } else {
            if (isPlaying.value) {
                mediaPlayerEngine.pause()
            } else {
                mediaPlayerEngine.resume()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayerEngine.seekTo(positionMs)
    }

    fun loadHistory() {
        viewModelScope.launch {
            _historyList.value = historyRepository.loadHistory()
        }
    }

    fun loadDownloads() {
        viewModelScope.launch {
            _downloadedList.value = downloadRepository.loadDownloads()
        }
    }

    fun addToHistory(song: Song) {
        viewModelScope.launch {
            val currentHistory = historyRepository.loadHistory()
            val updated = (listOf(song) + currentHistory.filter { it.id != song.id }).take(50)
            historyRepository.saveHistory(updated)
            _historyList.value = updated
        }
    }

    fun scanLocalFiles() {
        if (_isScanning.value) return
        _isScanning.value = true
        _localTracks.value = emptyList()
        viewModelScope.launch {
            try {
                _localTracks.value = localMediaRepository.scanAudio()
            } catch (e: Exception) {
                Log.e("MusicViewModel", "scanLocalFiles: Error scanning device audio", e)
            } finally {
                _isScanning.value = false
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Downloads"
            val descriptionText = "Shows the progress of offline music downloads"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun downloadSong(song: Song) {
        if (_downloadingSongs.value.contains(song.id)) {
            cancelDownload(song.id)
            return
        }
        if (downloadRepository.isDownloaded(song.id)) {
            deleteDownloadedSong(song.id)
            return
        }

        _downloadingSongs.value = _downloadingSongs.value + song.id
        _downloadingQueue.value = _downloadingQueue.value + song
        _downloadProgress.value = _downloadProgress.value + (song.id to 0)

        val job = viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationId = song.id.hashCode()

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Downloading ${song.title}")
                .setContentText("0%")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setProgress(100, 0, false)

            try {
                notificationManager.notify(notificationId, builder.build())
            } catch (e: SecurityException) {
                Log.e("MusicViewModel", "Notification permission not granted: ${e.message}")
            }

            val success = downloadRepository.download(song) { progress ->
                _downloadProgress.value = _downloadProgress.value + (song.id to progress)
                builder.setProgress(100, progress, false)
                    .setContentText("$progress%")
                try {
                    notificationManager.notify(notificationId, builder.build())
                } catch (e: SecurityException) {
                    // Ignore
                }
            }

            if (success) {
                loadDownloads()
                val completeBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setContentTitle("Download Complete")
                    .setContentText(song.title)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(false)
                try {
                    notificationManager.notify(notificationId, completeBuilder.build())
                } catch (e: SecurityException) {
                    // Ignore
                }
            } else {
                // If it wasn't cancelled, show fail notification
                if (_downloadingSongs.value.contains(song.id)) {
                    val failBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                        .setContentTitle("Download Failed")
                        .setContentText(song.title)
                        .setSmallIcon(android.R.drawable.stat_notify_error)
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setOngoing(false)
                    try {
                        notificationManager.notify(notificationId, failBuilder.build())
                    } catch (se: SecurityException) {
                        // Ignore
                    }
                }
            }

            downloadJobs.remove(song.id)
            _downloadingSongs.value = _downloadingSongs.value - song.id
            _downloadingQueue.value = _downloadingQueue.value.filter { it.id != song.id }
            _downloadProgress.value = _downloadProgress.value - song.id
        }
        downloadJobs[song.id] = job
    }

    fun cancelDownload(songId: String) {
        val job = downloadJobs.remove(songId)
        job?.cancel()

        _downloadingSongs.value = _downloadingSongs.value - songId
        _downloadingQueue.value = _downloadingQueue.value.filter { it.id != songId }
        _downloadProgress.value = _downloadProgress.value - songId

        val notificationId = songId.hashCode()
        val context = getApplication<Application>().applicationContext
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)

        viewModelScope.launch(Dispatchers.IO) {
            val downloadsDir = File(getApplication<Application>().filesDir, ".downloads")
            val tempFile = File(downloadsDir, "${songId}.tmp")
            val mp3File = File(downloadsDir, "${songId}.mp3")
            if (tempFile.exists()) tempFile.delete()
            if (mp3File.exists()) mp3File.delete()
            loadDownloads()
        }
    }

    fun deleteDownloadedSong(songId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val downloadsDir = File(getApplication<Application>().filesDir, ".downloads")
            val mp3File = File(downloadsDir, "${songId}.mp3")
            if (mp3File.exists()) {
                mp3File.delete()
            }
            val current = downloadRepository.loadDownloads()
            val updated = current.filter { it.id != songId }
            downloadRepository.saveDownloads(updated)
            loadDownloads()
        }
    }

    fun loadCustomPlaylists() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = if (playlistsFile.exists()) {
                try {
                    Json.decodeFromString<List<CustomPlaylist>>(playlistsFile.readText())
                } catch (e: Exception) {
                    Log.e("MusicViewModel", "Failed to load custom playlists", e)
                    emptyList()
                }
            } else {
                emptyList()
            }
            _customPlaylists.value = list
        }
    }

    fun createCustomPlaylist(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _customPlaylists.value
            if (current.any { it.name.equals(name, ignoreCase = true) }) return@launch
            val updated = current + CustomPlaylist(name = name, songs = emptyList())
            saveCustomPlaylists(updated)
        }
    }

    fun deleteCustomPlaylist(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = _customPlaylists.value.filter { !it.name.equals(name, ignoreCase = true) }
            saveCustomPlaylists(updated)
        }
    }

    fun addSongToCustomPlaylist(playlistName: String, song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = _customPlaylists.value.map { playlist ->
                if (playlist.name.equals(playlistName, ignoreCase = true)) {
                    if (playlist.songs.any { it.id == song.id }) {
                        playlist
                    } else {
                        playlist.copy(songs = playlist.songs + song)
                    }
                } else {
                    playlist
                }
            }
            saveCustomPlaylists(updated)
        }
    }

    fun removeSongFromCustomPlaylist(playlistName: String, songId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = _customPlaylists.value.map { playlist ->
                if (playlist.name.equals(playlistName, ignoreCase = true)) {
                    playlist.copy(songs = playlist.songs.filter { it.id != songId })
                } else {
                    playlist
                }
            }
            saveCustomPlaylists(updated)
        }
    }

    private fun saveCustomPlaylists(list: List<CustomPlaylist>) {
        try {
            playlistsFile.writeText(Json.encodeToString(list))
            _customPlaylists.value = list
        } catch (e: Exception) {
            Log.e("MusicViewModel", "Failed to save custom playlists", e)
        }
    }

    private var transitionWakeLock: android.os.PowerManager.WakeLock? = null

    private fun acquireTransitionWakeLock(timeoutMs: Long = 20000) {
        try {
            if (transitionWakeLock == null) {
                val powerManager = getApplication<Application>().getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                transitionWakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "ConveneMusic:AutoplayWakeLock").apply {
                    setReferenceCounted(false)
                }
            }
            transitionWakeLock?.acquire(timeoutMs)
            Log.d("MusicViewModel", "Transition WakeLock acquired for $timeoutMs ms")
        } catch (e: Exception) {
            Log.e("MusicViewModel", "Failed to acquire WakeLock", e)
        }
    }

    private fun releaseTransitionWakeLock() {
        try {
            transitionWakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d("MusicViewModel", "Transition WakeLock released")
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun isSongDownloaded(songId: String): Boolean {
        return downloadRepository.isDownloaded(songId)
    }

    fun isSongDownloading(songId: String): Boolean {
        return _downloadingSongs.value.contains(songId)
    }

    fun playPlaylist(playlist: com.example.convenemusic.network.Playlist) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val tracks = client.getPlaylistTracks(playlist.id)
                if (tracks.isNotEmpty()) {
                    playSong(tracks.first(), tracks, isPremadePlaylist = true)
                }
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Error playing playlist ${playlist.title}", e)
            }
            _isLoading.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        backgroundScope.cancel()
        mediaPlayerEngine.release()
    }

    fun clearLocalTracks() {
        _localTracks.value = emptyList()
    }

    fun clearLocalVideos() {
        _localVideos.value = emptyList()
    }

    fun scanLocalVideos() {
        if (_isVideoScanning.value) return
        _isVideoScanning.value = true
        _localVideos.value = emptyList()
        viewModelScope.launch {
            try {
                _localVideos.value = localMediaRepository.scanVideo()
            } catch (e: Exception) {
                Log.e("MusicViewModel", "scanLocalVideos: Error scanning device videos", e)
            } finally {
                _isVideoScanning.value = false
            }
        }
    }

    private var isQueueLoaded = false

    private fun saveQueueState() {
        if (!isQueueLoaded) return
        try {
            val prefs = getApplication<Application>().getSharedPreferences("convenemusic_prefs", Context.MODE_PRIVATE)
            val queueJson = Json.encodeToString(_queue.value)
            prefs.edit()
                .putString("saved_queue", queueJson)
                .putInt("saved_queue_index", _currentQueueIndex.value)
                .apply()
            Log.d("MusicViewModel", "Queue state saved: ${_queue.value.size} songs, index: ${_currentQueueIndex.value}")
        } catch (e: Exception) {
            Log.e("MusicViewModel", "Failed to save queue state", e)
        }
    }

    private fun loadQueueState() {
        try {
            val prefs = getApplication<Application>().getSharedPreferences("convenemusic_prefs", Context.MODE_PRIVATE)
            val queueJson = prefs.getString("saved_queue", null)
            val index = prefs.getInt("saved_queue_index", 0)
            if (queueJson != null) {
                val savedQueue = Json.decodeFromString<List<Song>>(queueJson)
                if (savedQueue.isNotEmpty()) {
                    _queue.value = savedQueue
                    _currentQueueIndex.value = if (index in savedQueue.indices) index else 0
                    Log.d("MusicViewModel", "Queue state restored: ${savedQueue.size} songs, index: ${_currentQueueIndex.value}")
                }
            }
        } catch (e: Exception) {
            Log.e("MusicViewModel", "Failed to load queue state", e)
        } finally {
            isQueueLoaded = true
        }
    }

    private var prefetchedSongId: String? = null
    private var prefetchedUrl: String? = null
    private var prefetchedArtwork: ByteArray? = null
    private var prebufferJob: kotlinx.coroutines.Job? = null

    private fun prebufferNextSong() {
        prebufferJob?.cancel()
        val currentList = _queue.value
        val nextIndex = _currentQueueIndex.value + 1
        if (nextIndex < currentList.size) {
            val nextSong = currentList[nextIndex]
            if (nextSong.id.startsWith("/")) {
                mediaPlayerEngine.setNextSong(nextSong, nextSong.id, null)
                return
            }
            prebufferJob = backgroundScope.launch(Dispatchers.IO) {
                try {
                    val url = if (downloadRepository.isDownloaded(nextSong.id)) {
                        val downloadsDir = File(getApplication<Application>().filesDir, ".downloads")
                        File(downloadsDir, "${nextSong.id}.mp3").absolutePath
                    } else {
                        if (!isNetworkAvailable()) null
                        else {
                            var streamUrl: String? = null
                            for (attempt in 1..3) {
                                streamUrl = try {
                                    client.getStreamUrl(nextSong.id)
                                } catch (e: Exception) {
                                    null
                                }
                                if (streamUrl != null) break
                                if (attempt < 3) delay(500L * (1 shl attempt))
                            }
                            streamUrl
                        }
                    }
                    if (url != null) {
                        prefetchedSongId = nextSong.id
                        prefetchedUrl = url
                        val artworkBytes = mediaPlayerEngine.getArtworkBytes(nextSong.thumbnailUrl)
                        prefetchedArtwork = artworkBytes
                        withContext(Dispatchers.Main) {
                            mediaPlayerEngine.setNextSong(nextSong, url, artworkBytes)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MusicViewModel", "Failed to prebuffer next song: ${e.message}")
                }
            }
        } else if (_isQueueEndless.value) {
            backgroundScope.launch(Dispatchers.IO) {
                val job = generateAutoplaySongs()
                job?.join()
                val updatedIndex = _currentQueueIndex.value + 1
                if (updatedIndex < _queue.value.size) {
                    withContext(Dispatchers.Main) {
                        prebufferNextSong()
                    }
                }
            }
        }
    }

    fun toggleLoopMode() {
        _loopMode.value = (_loopMode.value + 1) % 3
    }

    fun toggleShuffle() {
        val nextShuffle = !_shuffleEnabled.value
        _shuffleEnabled.value = nextShuffle

        val current = currentSong.value
        if (nextShuffle) {
            val currentQueue = _queue.value
            if (currentQueue.isNotEmpty()) {
                val shuffled = currentQueue.shuffled().toMutableList()
                if (current != null) {
                    shuffled.remove(current)
                    shuffled.add(0, current)
                }
                _queue.value = shuffled
                _currentQueueIndex.value = 0
            }
        } else {
            val original = _originalPlaylistTracks
            if (original.isNotEmpty()) {
                _queue.value = original
                val index = original.indexOfFirst { it.id == current?.id }
                _currentQueueIndex.value = if (index >= 0) index else 0
            }
        }
    }

    private fun cleanTrackName(title: String): String {
        return title
            .replace(Regex("""\s*[\(\[][Oo]fficial\s+[Vv]ideo[\)\]]"""), "")
            .replace(Regex("""\s*[\(\[][Oo]fficial\s+[Mm]usic\s+[Vv]ideo[\)\]]"""), "")
            .replace(Regex("""\s*[\(\[][Oo]fficial\s+[Aa]udio[\)\]]"""), "")
            .replace(Regex("""\s*[\(\[][Mm]usic\s+[Vv]ideo[\)\]]"""), "")
            .replace(Regex("""\s*[\(\[][Ll]yric\s+[Vv]ideo[\)\]]"""), "")
            .replace(Regex("""\s*[\(\[][Ll]yrics[\)\]]"""), "")
            .replace(Regex("""\s*-\s*Topic$"""), "")
            .trim()
    }

    private fun cleanArtistName(artist: String): String {
        val separators = Regex(""",|/|\||&|\bfeat\b|\bft\b|\bX\b|\bx\b|\band\b""", RegexOption.IGNORE_CASE)
        val firstArtist = artist.split(separators).firstOrNull()?.trim() ?: artist
        return firstArtist.replace(Regex("\\s*•\\s*\\d+:\\d+\\s*$"), "").trim()
    }

    private suspend fun fetchLyricsForSong(song: Song) {
        val videoId = song.id
        if (videoId.isBlank()) return
        if (videoId.startsWith("/")) {
            _lyrics.value = null
            return
        }
        _isLyricsLoading.value = true
        _lyricsError.value = null
        try {
            if (!isNetworkAvailable()) {
                _lyricsError.value = "No internet connection"
                _lyrics.value = null
                return
            }
            
            val cleanTitle = cleanTrackName(song.title)
            val cleanArtist = cleanArtistName(song.artist)
            
            Log.i("MusicViewModel", "Fetching LRCLib lyrics for: $cleanTitle by $cleanArtist (duration: ${song.durationSeconds})")
            var lrcText = client.getLrcLyrics(cleanTitle, cleanArtist, song.durationSeconds)
            
            if (lrcText.isNullOrBlank()) {
                Log.w("MusicViewModel", "LRCLib lyrics not found. Falling back to YTM lyrics for videoId $videoId")
                val browseId = kotlinx.coroutines.withTimeoutOrNull(5000) {
                    client.getLyricsBrowseId(videoId)
                }
                if (browseId != null) {
                    lrcText = kotlinx.coroutines.withTimeoutOrNull(5000) {
                        client.getLyrics(browseId)
                    }
                }
            }
            
            if (!lrcText.isNullOrBlank()) {
                val parsed = client.parseLyricsText(lrcText)
                _lyrics.value = parsed
            } else {
                _lyrics.value = null
            }
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("MusicViewModel", "Error fetching lyrics for ${song.title}: ${e.message}", e)
            _lyricsError.value = e.message ?: "Failed to fetch lyrics"
            _lyrics.value = null
        } finally {
            _isLyricsLoading.value = false
        }
    }

    fun retryLyrics() {
        val song = currentSong.value ?: return
        viewModelScope.launch {
            fetchLyricsForSong(song)
        }
    }
}

@kotlinx.serialization.Serializable
data class CustomPlaylist(
    val name: String,
    val songs: List<Song> = emptyList()
)
