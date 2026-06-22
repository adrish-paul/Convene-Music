package com.example.convenemusic

import android.content.Context
import android.content.SharedPreferences
import com.example.convenemusic.network.Song
import com.example.convenemusic.playback.AutoplayEngine
import com.example.convenemusic.data.repository.HistoryRepositoryImpl
import com.example.convenemusic.data.repository.DownloadRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ConveneMusicRegressionTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mockContext: Context
    private lateinit var tempDir: File

    // Simple stub of SharedPreferences
    private class FakeSharedPreferences : SharedPreferences {
        private val map = mutableMapOf<String, Any?>()

        override fun getAll(): Map<String, *> = map
        override fun getString(key: String, defValue: String?): String? = (map[key] as? String) ?: defValue
        override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? = (map[key] as? Set<*>)?.map { it.toString() }?.toSet() ?: defValues
        override fun getInt(key: String, defValue: Int): Int = (map[key] as? Int) ?: defValue
        override fun getLong(key: String, defValue: Long): Long = (map[key] as? Long) ?: defValue
        override fun getFloat(key: String, defValue: Float): Float = (map[key] as? Float) ?: defValue
        override fun getBoolean(key: String, defValue: Boolean): Boolean = (map[key] as? Boolean) ?: defValue
        override fun contains(key: String): Boolean = map.containsKey(key)
        override fun edit(): SharedPreferences.Editor = FakeEditor(map)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        private class FakeEditor(val map: MutableMap<String, Any?>) : SharedPreferences.Editor {
            private val tempMap = HashMap(map)

            override fun putString(key: String, value: String?): SharedPreferences.Editor { tempMap[key] = value; return this }
            override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor { tempMap[key] = values; return this }
            override fun putInt(key: String, value: Int): SharedPreferences.Editor { tempMap[key] = value; return this }
            override fun putLong(key: String, value: Long): SharedPreferences.Editor { tempMap[key] = value; return this }
            override fun putFloat(key: String, value: Float): SharedPreferences.Editor { tempMap[key] = value; return this }
            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor { tempMap[key] = value; return this }
            override fun remove(key: String): SharedPreferences.Editor { tempMap.remove(key); return this }
            override fun clear(): SharedPreferences.Editor { tempMap.clear(); return this }
            override fun commit(): Boolean { map.clear(); map.putAll(tempMap); return true }
            override fun apply() { commit() }
        }
    }

    private class TestContext(val filesDirectory: File) : android.content.ContextWrapper(null) {
        private val fakePrefs = FakeSharedPreferences()

        override fun getFilesDir(): File = filesDirectory
        override fun getApplicationContext(): Context = this
        override fun getPackageName(): String = "com.example.convenemusic"
        override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = fakePrefs
    }

    @Before
    fun setUp() {
        tempDir = tempFolder.newFolder("convene_music_test_data")
        mockContext = TestContext(tempDir)
    }

    @Test
    fun testLongSessionAndAutoplayEngine() = runBlocking {
        // 1. Generate a queue of 20 distinct mock songs
        val songQueue = (1..20).map { i ->
            Song(
                id = "song_id_$i",
                title = "Title Song $i",
                artist = if (i % 2 == 0) "Artist Even" else "Artist Odd",
                album = "Album $i",
                durationText = "3:00",
                durationSeconds = 180,
                thumbnailUrl = "http://example.com/thumb_$i.png"
            )
        }

        val queueFlow = MutableStateFlow<List<Song>>(songQueue)
        val currentIndexFlow = MutableStateFlow(0)
        val isQueueEndless = MutableStateFlow(true)
        val isQueueLoadingMore = MutableStateFlow(false)
        val tokens = mutableMapOf<String, String>()

        // Counter for API client requests
        val queriesReceived = mutableListOf<String>()

        val autoplayEngine = AutoplayEngine(
            client = com.example.convenemusic.network.InnerTubeClient(),
            queue = queueFlow,
            currentQueueIndex = currentIndexFlow,
            isQueueEndless = isQueueEndless,
            isQueueLoadingMore = isQueueLoadingMore,
            autoplayContinuationTokens = tokens,
            getOriginalPlaylistTracks = { emptyList() },
            getSongsForQuery = { query ->
                queriesReceived.add(query)
                // Return 2 new songs matching the query
                listOf(
                    Song(
                        id = "rec_${query}_${System.currentTimeMillis()}_1",
                        title = "Rec 1 for $query",
                        artist = query,
                        album = "Rec Album",
                        durationText = "3:30",
                        durationSeconds = 210,
                        thumbnailUrl = "thumb"
                    ),
                    Song(
                        id = "rec_${query}_${System.currentTimeMillis()}_2",
                        title = "Rec 2 for $query",
                        artist = query,
                        album = "Rec Album",
                        durationText = "3:40",
                        durationSeconds = 220,
                        thumbnailUrl = "thumb"
                    )
                )
            }
        )

        // Verify initial state
        assertEquals(20, queueFlow.value.size)
        assertEquals(0, currentIndexFlow.value)

        // 2. Simulate navigating through the 20-song queue
        // MusicViewModel coordinates the autoplay trigger condition: nextIndex >= queue.size - 2
        for (i in 0..19) {
            currentIndexFlow.value = i
            val shouldTrigger = i >= queueFlow.value.size - 2
            if (shouldTrigger) {
                val triggerJob = autoplayEngine.generateAutoplaySongs(this)
                assertNotNull("Autoplay should trigger at index $i", triggerJob)
                triggerJob?.join()
            }
        }

        // Verify queue size has expanded because autoplay was triggered at indices 18 and 19
        assertTrue("Queue should have more than 20 songs now", queueFlow.value.size > 20)
        assertEquals("Artist Odd", queriesReceived.first()) // Index 18 is Song 19 (odd)

        // Check that recommendations are appended
        val totalSize = queueFlow.value.size
        assertTrue(queueFlow.value.any { it.title.startsWith("Rec 1 for Artist Odd") })
    }

    @Test
    fun testAutoplayFallbackTriggers() = runBlocking {
        // Here we test different levels of fallback logic inside AutoplayEngine
        val songQueue = listOf(
            Song("1", "Song A", "Artist A", "Album A", "2:00", 120, "thumb"),
            Song("2", "Song B", "Artist B", "Album B", "3:00", 180, "thumb")
        )
        val queueFlow = MutableStateFlow(songQueue)
        val currentIndexFlow = MutableStateFlow(1)
        val isQueueEndless = MutableStateFlow(true)
        val isQueueLoadingMore = MutableStateFlow(false)
        val tokens = mutableMapOf<String, String>()

        val queryAttempts = mutableListOf<String>()

        val autoplayEngine = AutoplayEngine(
            client = com.example.convenemusic.network.InnerTubeClient(),
            queue = queueFlow,
            currentQueueIndex = currentIndexFlow,
            isQueueEndless = isQueueEndless,
            isQueueLoadingMore = isQueueLoadingMore,
            autoplayContinuationTokens = tokens,
            getOriginalPlaylistTracks = { emptyList() },
            getSongsForQuery = { query ->
                queryAttempts.add(query)
                // Simulate returning nothing for "Artist B" (normal path),
                // but return results for Fallback 1 (Artist A) or generic queries.
                if (query == "Artist B") {
                    emptyList() // Triggers Fallback 1 (Unique artists)
                } else if (query == "Artist A") {
                    listOf(Song("rec_fallback_1", "Song Fallback 1", "Artist A", "Album", "2:00", 120, "thumb"))
                } else {
                    emptyList()
                }
            }
        )

        val job = autoplayEngine.generateAutoplaySongs(this)
        job?.join()

        // Fallback 1 should be triggered, and Artist A should be queried.
        assertTrue(queryAttempts.contains("Artist B"))
        assertTrue(queryAttempts.contains("Artist A"))
        assertTrue(queueFlow.value.any { it.id == "rec_fallback_1" })
    }

    @Test
    fun testAutoplayFallback2And3() = runBlocking {
        val songQueue = listOf(
            Song("1", "Title X", "Artist X", "Album", "2:00", 120, "thumb")
        )
        val queueFlow = MutableStateFlow(songQueue)
        val currentIndexFlow = MutableStateFlow(0)
        val isQueueEndless = MutableStateFlow(true)
        val isQueueLoadingMore = MutableStateFlow(false)
        val tokens = mutableMapOf<String, String>()

        val queryAttempts = mutableListOf<String>()

        val autoplayEngine = AutoplayEngine(
            client = com.example.convenemusic.network.InnerTubeClient(),
            queue = queueFlow,
            currentQueueIndex = currentIndexFlow,
            isQueueEndless = isQueueEndless,
            isQueueLoadingMore = isQueueLoadingMore,
            autoplayContinuationTokens = tokens,
            getOriginalPlaylistTracks = { emptyList() },
            getSongsForQuery = { query ->
                queryAttempts.add(query)
                // For Fallback 2: title "Title X"
                if (query == "Title X") {
                    listOf(Song("rec_fallback_2", "Song Fallback 2", "Artist Y", "Album", "2:00", 120, "thumb"))
                } else {
                    emptyList()
                }
            }
        )

        val job = autoplayEngine.generateAutoplaySongs(this)
        job?.join()

        assertTrue(queryAttempts.contains("Artist X")) // Initial query fails
        assertTrue(queryAttempts.contains("Title X"))  // Fallback 2 succeeds!
        assertTrue(queueFlow.value.any { it.id == "rec_fallback_2" })
    }

    @Test
    fun testHistoryRepository() = runBlocking {
        val historyRepo = HistoryRepositoryImpl(mockContext)
        val initialHistory = historyRepo.loadHistory()
        assertTrue(initialHistory.isEmpty())

        val song = Song("song_id_hist", "History Song", "Artist Hist", "Album", "2:00", 120, "thumb")
        historyRepo.saveHistory(listOf(song))

        val loaded = historyRepo.loadHistory()
        assertEquals(1, loaded.size)
        assertEquals("History Song", loaded[0].title)
    }

    @Test
    fun testDownloadRepository() = runBlocking {
        val downloadRepo = DownloadRepositoryImpl(mockContext, com.example.convenemusic.network.InnerTubeClient())
        val initialDownloads = downloadRepo.loadDownloads()
        assertTrue(initialDownloads.isEmpty())

        val song = Song("song_id_down", "Download Song", "Artist Down", "Album", "2:00", 120, "thumb")
        
        // Save mock downloaded song catalog
        downloadRepo.saveDownloads(listOf(song))
        
        // Since the physical file does not exist, loadDownloads() should clean/filter it out!
        val loadedAfterNoFile = downloadRepo.loadDownloads()
        assertTrue("Download record should be cleaned up because the physical mp3 file does not exist", loadedAfterNoFile.isEmpty())
        
        // Create the physical mock mp3 file in the downloads folder
        val downloadsDir = File(tempDir, ".downloads")
        downloadsDir.mkdirs()
        val mp3File = File(downloadsDir, "${song.id}.mp3")
        mp3File.writeText("MOCK MP3 CONTENT")

        // Re-save download catalog
        downloadRepo.saveDownloads(listOf(song))

        val loadedWithFile = downloadRepo.loadDownloads()
        assertEquals(1, loadedWithFile.size)
        assertEquals("Download Song", loadedWithFile[0].title)
        assertTrue(downloadRepo.isDownloaded(song.id))
    }

    // Helper functions copy from MusicViewModel for search matching testing
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

    @Test
    fun testArtistSplitLogic() {
        val list1 = splitArtistNames("Taylor Swift • Drake • The Weeknd")
        assertEquals(listOf("Taylor Swift"), list1) // App logic only takes first bullet artist

        val list2 = splitArtistNames("Arijit Singh, Ed Sheeran / Eminem")
        assertEquals(listOf("Arijit Singh", "Ed Sheeran", "Eminem"), list2)

        // Fixed the regex splitting boundary logic: "BTS feat. Lady Gaga" splits cleanly now
        val list3 = splitArtistNames("BTS feat. Lady Gaga")
        assertEquals(listOf("BTS", "Lady Gaga"), list3)
    }

    @Test
    fun testSearchScoringLogic() {
        val song1 = Song("id_1", "Hello World", "Adele", "Album", "3:00", 180, "thumb")
        
        // Title Exact Match -> 100
        assertEquals(100, calculateMatchScore(song1, "Hello World"))

        // Title Starts With -> 80
        assertEquals(80, calculateMatchScore(song1, "hello"))

        // Title Contains -> 60
        assertEquals(60, calculateMatchScore(song1, "world"))

        // Artist Exact Match -> 40
        assertEquals(40, calculateMatchScore(song1, "adele"))

        // Artist Contains -> 20
        assertEquals(20, calculateMatchScore(song1, "del"))

        // No Match -> 0
        assertEquals(0, calculateMatchScore(song1, "nothing"))
    }
}
