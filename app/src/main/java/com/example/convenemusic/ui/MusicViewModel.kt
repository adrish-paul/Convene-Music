package com.example.convenemusic.ui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.convenemusic.network.InnerTubeClient
import com.example.convenemusic.network.Song
import com.example.convenemusic.playback.PlaybackManager
import com.example.convenemusic.playback.PlaybackServiceConnector
import com.example.convenemusic.data.repository.LocalMediaRepository
import com.example.convenemusic.data.repository.LocalMediaRepositoryImpl
import com.example.convenemusic.data.repository.HistoryRepository
import com.example.convenemusic.data.repository.HistoryRepositoryImpl
import com.example.convenemusic.data.repository.DownloadRepository
import com.example.convenemusic.data.repository.DownloadRepositoryImpl
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.Deferred
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File
import java.io.FileOutputStream
import java.net.URL
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
    val playbackManager = PlaybackManager(application.applicationContext)

    private val localMediaRepository: LocalMediaRepository = LocalMediaRepositoryImpl(application)
    private val historyRepository: HistoryRepository = HistoryRepositoryImpl(application)
    private val downloadRepository: DownloadRepository = DownloadRepositoryImpl(application, client)

    private val _searchResults = MutableStateFlow<List<Song>>(emptyList())
    val searchResults: StateFlow<List<Song>> = _searchResults.asStateFlow()

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
        playbackManager.onSongEnded = {
            playNextSong()
        }
        PlaybackServiceConnector.onNext = {
            playNextSong()
        }
        PlaybackServiceConnector.onPrevious = {
            playPreviousSong()
        }
        loadHistory()
        loadDownloads()
        createNotificationChannel()
        prefetchFamousArtists()
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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isPlaylistLoading = MutableStateFlow(false)
    val isPlaylistLoading: StateFlow<Boolean> = _isPlaylistLoading.asStateFlow()

    val currentSong: StateFlow<Song?> = playbackManager.currentSong
    val isPlaying: StateFlow<Boolean> = playbackManager.isPlaying
    val currentPosition: StateFlow<Long> = playbackManager.currentPosition
    val duration: StateFlow<Long> = playbackManager.duration

    private var songContinuationToken: String? = null
    private var playlistContinuationToken: String? = null

    private val _isLoadMoreLoading = MutableStateFlow(false)
    val isLoadMoreLoading: StateFlow<Boolean> = _isLoadMoreLoading.asStateFlow()

    private val _isStreamLoading = MutableStateFlow(false)
    val isStreamLoading: StateFlow<Boolean> = _isStreamLoading.asStateFlow()

    private fun splitArtistNames(combinedName: String): List<String> {
        val bulletParts = combinedName.split(Regex(" • | •|• |•|\\u2022"))
        val mainArtistPart = bulletParts.firstOrNull()?.trim() ?: combinedName
        val separators = Regex(",|/|\\||&|\\s+\\.\\s+|\\bx\\b|\\bX\\b|\\band\\b|\\bfeat\\.?\\b|\\bft\\.?\\b", RegexOption.IGNORE_CASE)
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
        playbackManager.setCurrentSong(song)
        addToHistory(song)
        viewModelScope.launch {
            _isStreamLoading.value = true
            val urlDeferred = async {
                if (song.id.startsWith("/")) {
                    song.id
                } else if (downloadRepository.isDownloaded(song.id)) {
                    val downloadsDir = File(getApplication<Application>().filesDir, ".downloads")
                    val localFile = File(downloadsDir, "${song.id}.mp3")
                    Log.e("MusicViewModel", "playSongInternal: Local file found. Playing offline: ${localFile.absolutePath}")
                    localFile.absolutePath
                } else {
                    client.getStreamUrl(song.id)
                }
            }
            val artworkDeferred = async {
                playbackManager.getArtworkBytes(song.thumbnailUrl)
            }
            
            val url = urlDeferred.await()
            val artworkBytes = artworkDeferred.await()
            
            if (url != null) {
                playbackManager.play(song, url, artworkBytes)
            } else {
                Log.e("MusicViewModel", "playSongInternal: Failed to resolve stream URL for ${song.title}")
            }
            _isStreamLoading.value = false
        }
    }

    fun playSong(song: Song, initialQueue: List<Song> = emptyList(), isPremadePlaylist: Boolean = false) {
        Log.e("MusicViewModel", "playSong: Requested playback for ${song.title} (ID: ${song.id})")
        autoplayContinuationTokens.clear()
        _isQueueEndless.value = !isPremadePlaylist
        _originalPlaylistTracks = initialQueue
        if (initialQueue.isNotEmpty()) {
            _queue.value = initialQueue
            val index = initialQueue.indexOfFirst { it.id == song.id }
            _currentQueueIndex.value = if (index >= 0) index else 0
        } else {
            _queue.value = listOf(song)
            _currentQueueIndex.value = 0
        }

        playSongInternal(song)

        if ((initialQueue.isEmpty() || _currentQueueIndex.value >= _queue.value.size - 2) && _isQueueEndless.value) {
            generateAutoplaySongs()
        }
    }

    fun playNextSong() {
        val currentList = _queue.value
        val nextIndex = _currentQueueIndex.value + 1
        if (nextIndex < currentList.size) {
            _currentQueueIndex.value = nextIndex
            val nextSong = currentList[nextIndex]
            playSongInternal(nextSong)

            if (nextIndex >= currentList.size - 2 && _isQueueEndless.value) {
                generateAutoplaySongs()
            }
        } else {
            if (_isQueueEndless.value) {
                viewModelScope.launch {
                    val job = generateAutoplaySongs()
                    job?.join()
                    val updatedList = _queue.value
                    if (nextIndex < updatedList.size) {
                        _currentQueueIndex.value = nextIndex
                        val nextSong = updatedList[nextIndex]
                        playSongInternal(nextSong)
                    } else {
                        restartQueueOrStop(currentList)
                    }
                }
            } else {
                restartQueueOrStop(currentList)
            }
        }
    }

    private fun restartQueueOrStop(currentList: List<Song>) {
        if (currentList.size > 1) {
            _currentQueueIndex.value = 0
            val firstSong = currentList[0]
            playSongInternal(firstSong)
        }
    }

    fun playPreviousSong() {
        val currentPos = playbackManager.currentPosition.value
        if (currentPos >= 5000L) {
            playbackManager.seekTo(0L)
        } else {
            val prevIndex = _currentQueueIndex.value - 1
            if (prevIndex >= 0) {
                _currentQueueIndex.value = prevIndex
                val prevSong = _queue.value[prevIndex]
                playSongInternal(prevSong)
            } else {
                playbackManager.seekTo(0L)
            }
        }
    }

    fun playQueueSong(index: Int) {
        if (index in _queue.value.indices) {
            _currentQueueIndex.value = index
            playSongInternal(_queue.value[index])
        }
    }

    private suspend fun getSongsForQuery(query: String): List<Song> {
        val token = autoplayContinuationTokens[query]
        return try {
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
        if (!_isQueueEndless.value) return null
        if (_isQueueLoadingMore.value) return null
        val currentList = _queue.value
        val currentIndex = _currentQueueIndex.value
        val currentSong = currentList.getOrNull(currentIndex) ?: return null

        _isQueueLoadingMore.value = true
        return viewModelScope.launch {
            try {
                val recommendations = mutableListOf<Song>()
                val existingIds = currentList.map { it.id }.toSet()

                val originalPlaylistTracks = _originalPlaylistTracks
                if (originalPlaylistTracks.isNotEmpty()) {
                    val artistFrequencies = originalPlaylistTracks
                        .map { it.artist }
                        .filter { it.isNotBlank() && !it.contains("unknown", ignoreCase = true) }
                        .groupingBy { it }
                        .eachCount()

                    val topArtists = artistFrequencies.entries
                        .sortedByDescending { it.value }
                        .map { it.key }
                        .take(3)

                    if (topArtists.isNotEmpty()) {
                        for (artist in topArtists) {
                            try {
                                val newSongs = getSongsForQuery(artist)
                                recommendations.addAll(newSongs)
                            } catch (e: Exception) {
                                Log.e("MusicViewModel", "Autoplay search error for artist $artist", e)
                            }
                        }
                    } else {
                        try {
                            val newSongs = getSongsForQuery(currentSong.artist)
                            recommendations.addAll(newSongs)
                        } catch (e: Exception) {
                            Log.e("MusicViewModel", "Autoplay search error for artist ${currentSong.artist}", e)
                        }
                    }
                } else {
                    try {
                        val newSongs = getSongsForQuery(currentSong.artist)
                        recommendations.addAll(newSongs)
                    } catch (e: Exception) {
                        Log.e("MusicViewModel", "Autoplay search error for artist ${currentSong.artist}", e)
                    }
                }

                var filteredRecs = recommendations
                    .filter { it.id !in existingIds }
                    .distinctBy { it.id }

                if (filteredRecs.isEmpty() && currentList.isNotEmpty()) {
                    Log.d("MusicViewModel", "Autoplay: Fallback 1 - Trying other unique artists from the queue")
                    val uniqueArtists = currentList.map { it.artist }
                        .filter { it.isNotBlank() && !it.contains("unknown", ignoreCase = true) }
                        .distinct()
                    val shuffledArtists = uniqueArtists.shuffled().take(3)
                    for (artist in shuffledArtists) {
                        try {
                            val newRecs = getSongsForQuery(artist).filter { it.id !in existingIds }
                            recommendations.addAll(newRecs)
                        } catch (e: Exception) {
                            Log.e("MusicViewModel", "Autoplay fallback search error for artist $artist", e)
                        }
                    }
                    filteredRecs = recommendations
                        .filter { it.id !in existingIds }
                        .distinctBy { it.id }
                }

                if (filteredRecs.isEmpty() && currentList.isNotEmpty()) {
                    Log.d("MusicViewModel", "Autoplay: Fallback 2 - Searching by song titles from the queue")
                    val shuffledSongs = currentList.shuffled().take(2)
                    for (songItem in shuffledSongs) {
                        try {
                            val newRecs = getSongsForQuery(songItem.title).filter { it.id !in existingIds }
                            recommendations.addAll(newRecs)
                        } catch (e: Exception) {
                            Log.e("MusicViewModel", "Autoplay fallback search error for title ${songItem.title}", e)
                        }
                    }
                    filteredRecs = recommendations
                        .filter { it.id !in existingIds }
                        .distinctBy { it.id }
                }

                if (filteredRecs.isEmpty()) {
                    Log.d("MusicViewModel", "Autoplay: Fallback 3 - Generic query search")
                    val genericQueries = listOf("popular music", "hits", "trending songs", "lofi chill", "acoustic hits", "top tracks")
                    val query = genericQueries.random()
                    try {
                        val newRecs = getSongsForQuery(query)
                        recommendations.addAll(newRecs)
                    } catch (e: Exception) {
                        Log.e("MusicViewModel", "Autoplay fallback search error for query $query", e)
                    }
                    filteredRecs = recommendations
                        .filter { it.id !in existingIds }
                        .distinctBy { it.id }
                }

                val finalRecs = filteredRecs
                    .shuffled()
                    .take(15)

                if (finalRecs.isNotEmpty()) {
                    _queue.value = currentList + finalRecs
                    Log.d("MusicViewModel", "Autoplay: Appended ${finalRecs.size} tracks to the queue. Total size: ${_queue.value.size}")
                } else {
                    Log.e("MusicViewModel", "Autoplay: Failed to find any new tracks even after all fallbacks!")
                }
            } finally {
                _isQueueLoadingMore.value = false
            }
        }
    }

    fun togglePlayPause() {
        val current = currentSong.value
        if (current != null && playbackManager.isIdle()) {
            if (_queue.value.isEmpty()) {
                _queue.value = listOf(current)
                _currentQueueIndex.value = 0
            }
            playSongInternal(current)
        } else {
            if (isPlaying.value) {
                playbackManager.pause()
            } else {
                playbackManager.resume()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        playbackManager.seekTo(positionMs)
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
        if (_downloadingSongs.value.contains(song.id)) return
        if (downloadRepository.isDownloaded(song.id)) return

        _downloadingSongs.value = _downloadingSongs.value + song.id
        _downloadingQueue.value = _downloadingQueue.value + song
        _downloadProgress.value = _downloadProgress.value + (song.id to 0)

        viewModelScope.launch {
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

            _downloadingSongs.value = _downloadingSongs.value - song.id
            _downloadingQueue.value = _downloadingQueue.value.filter { it.id != song.id }
            _downloadProgress.value = _downloadProgress.value - song.id
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
        playbackManager.release()
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
}
