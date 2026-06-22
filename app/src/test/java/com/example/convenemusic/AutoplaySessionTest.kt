package com.example.convenemusic

import com.example.convenemusic.network.Song
import com.example.convenemusic.playback.AutoplayEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AutoplaySessionTest {

    @Test
    fun testAutoplayQueueProgression() = runBlocking {
        // 1. Setup mock states
        val queue = MutableStateFlow<List<Song>>(
            listOf(
                Song(id = "1", title = "Song A", artist = "Artist X", album = "Album A", durationText = "2:00", durationSeconds = 120, thumbnailUrl = "UrlA"),
                Song(id = "2", title = "Song B", artist = "Artist X", album = "Album B", durationText = "3:00", durationSeconds = 180, thumbnailUrl = "UrlB")
            )
        )
        val currentQueueIndex = MutableStateFlow(0)
        val isQueueEndless = MutableStateFlow(true)
        val isQueueLoadingMore = MutableStateFlow(false)
        val tokens = mutableMapOf<String, String>()

        // Mock recommender that returns a static batch of songs
        val autoplayEngine = AutoplayEngine(
            client = com.example.convenemusic.network.InnerTubeClient(), // Not accessed due to mock queries
            queue = queue,
            currentQueueIndex = currentQueueIndex,
            isQueueEndless = isQueueEndless,
            isQueueLoadingMore = isQueueLoadingMore,
            autoplayContinuationTokens = tokens,
            getOriginalPlaylistTracks = { emptyList() },
            getSongsForQuery = { query ->
                val queryId = query.hashCode().toString()
                listOf(
                    Song(id = "3_$queryId", title = "Song C by $query", artist = query, album = "Album C", durationText = "2:30", durationSeconds = 150, thumbnailUrl = "UrlC"),
                    Song(id = "4_$queryId", title = "Song D by $query", artist = query, album = "Album D", durationText = "2:40", durationSeconds = 160, thumbnailUrl = "UrlD")
                )
            }
        )

        // 2. Simulate reaching track index 0 (currentQueueIndex = 0, queue size = 2)
        // Since nextIndex will be 1 (which is >= queue.size - 2), we should trigger recommendations
        assertEquals(2, queue.value.size)
        assertEquals(0, currentQueueIndex.value)

        val job = autoplayEngine.generateAutoplaySongs(this)
        job?.join()

        // Recommendations should be appended!
        assertEquals(4, queue.value.size)
        val titles = queue.value.map { it.title }
        assertTrue(titles.contains("Song C by Artist X"))
        assertTrue(titles.contains("Song D by Artist X"))

        // 3. Advance play queue
        currentQueueIndex.value = 1
        
        // Advance play queue again (index = 2, which is >= queue.size - 2, triggering next recommendations)
        currentQueueIndex.value = 2
        val job2 = autoplayEngine.generateAutoplaySongs(this)
        job2?.join()

        // Additional recommendations appended!
        assertTrue(queue.value.size > 4)
    }
}
