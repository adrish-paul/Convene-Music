package com.example.convenemusic.playback

import android.util.Log
import com.example.convenemusic.network.InnerTubeClient
import com.example.convenemusic.network.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AutoplayEngine(
    private val client: InnerTubeClient,
    private val queue: MutableStateFlow<List<Song>>,
    private val currentQueueIndex: StateFlow<Int>,
    private val isQueueEndless: StateFlow<Boolean>,
    private val isQueueLoadingMore: MutableStateFlow<Boolean>,
    private val autoplayContinuationTokens: MutableMap<String, String>,
    private val getOriginalPlaylistTracks: () -> List<Song>,
    private val getSongsForQuery: suspend (String) -> List<Song>
) {
    private var activeJob: Job? = null

    fun generateAutoplaySongs(scope: CoroutineScope): Job? {
        if (!isQueueEndless.value) return null
        
        synchronized(this) {
            val running = activeJob
            if (running != null && running.isActive) {
                return running
            }
            
            if (isQueueLoadingMore.value) return null
            val currentList = queue.value
            val currentIndex = currentQueueIndex.value
            val currentSong = currentList.getOrNull(currentIndex) ?: return null

            isQueueLoadingMore.value = true
            val job = scope.launch(Dispatchers.IO) {
                try {
                    val recommendations = mutableListOf<Song>()
                    val existingIds = currentList.map { it.id }.toSet()

                    val originalPlaylistTracks = getOriginalPlaylistTracks()
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
                                    Log.e("AutoplayEngine", "Autoplay search error for artist $artist", e)
                                }
                            }
                        } else {
                            try {
                                val newSongs = getSongsForQuery(currentSong.artist)
                                recommendations.addAll(newSongs)
                            } catch (e: Exception) {
                                Log.e("AutoplayEngine", "Autoplay search error for artist ${currentSong.artist}", e)
                            }
                        }
                    } else {
                        try {
                            val newSongs = getSongsForQuery(currentSong.artist)
                            recommendations.addAll(newSongs)
                        } catch (e: Exception) {
                            Log.e("AutoplayEngine", "Autoplay search error for artist ${currentSong.artist}", e)
                        }
                    }

                    var filteredRecs = recommendations
                        .filter { it.id !in existingIds }
                        .distinctBy { it.id }

                    if (filteredRecs.isEmpty() && currentList.isNotEmpty()) {
                        Log.d("AutoplayEngine", "Autoplay: Fallback 1 - Trying other unique artists from the queue")
                        val uniqueArtists = currentList.map { it.artist }
                            .filter { it.isNotBlank() && !it.contains("unknown", ignoreCase = true) }
                            .distinct()
                        val shuffledArtists = uniqueArtists.shuffled().take(3)
                        for (artist in shuffledArtists) {
                            try {
                                val newRecs = getSongsForQuery(artist).filter { it.id !in existingIds }
                                recommendations.addAll(newRecs)
                            } catch (e: Exception) {
                                Log.e("AutoplayEngine", "Autoplay fallback search error for artist $artist", e)
                            }
                        }
                        filteredRecs = recommendations
                            .filter { it.id !in existingIds }
                            .distinctBy { it.id }
                    }

                    if (filteredRecs.isEmpty() && currentList.isNotEmpty()) {
                        Log.d("AutoplayEngine", "Autoplay: Fallback 2 - Searching by song titles from the queue")
                        val shuffledSongs = currentList.shuffled().take(2)
                        for (songItem in shuffledSongs) {
                            try {
                                val newRecs = getSongsForQuery(songItem.title).filter { it.id !in existingIds }
                                recommendations.addAll(newRecs)
                            } catch (e: Exception) {
                                Log.e("AutoplayEngine", "Autoplay fallback search error for title ${songItem.title}", e)
                            }
                        }
                        filteredRecs = recommendations
                            .filter { it.id !in existingIds }
                            .distinctBy { it.id }
                    }

                    if (filteredRecs.isEmpty()) {
                        Log.d("AutoplayEngine", "Autoplay: Fallback 3 - Generic query search")
                        val genericQueries = listOf("popular music", "hits", "trending songs", "lofi chill", "acoustic hits", "top tracks")
                        val query = genericQueries.random()
                        try {
                            val newRecs = getSongsForQuery(query)
                            recommendations.addAll(newRecs)
                        } catch (e: Exception) {
                            Log.e("AutoplayEngine", "Autoplay fallback search error for query $query", e)
                        }
                        filteredRecs = recommendations
                            .filter { it.id !in existingIds }
                            .distinctBy { it.id }
                    }

                    val finalRecs = filteredRecs
                        .shuffled()
                        .take(15)

                    if (finalRecs.isNotEmpty()) {
                        queue.value = currentList + finalRecs
                        Log.d("AutoplayEngine", "Autoplay: Appended ${finalRecs.size} tracks to the queue. Total size: ${queue.value.size}")
                    } else {
                        Log.e("AutoplayEngine", "Autoplay: Failed to find any new tracks even after all fallbacks!")
                    }
                } finally {
                    isQueueLoadingMore.value = false
                }
            }
            activeJob = job
            return job
        }
    }
}
