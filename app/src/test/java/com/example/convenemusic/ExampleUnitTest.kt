package com.example.convenemusic

import com.example.convenemusic.network.InnerTubeClient
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Test
import org.junit.Assert.*

class ExampleUnitTest {
    
    @Test
    fun testGetStreamUrl() = runBlocking {
        val client = InnerTubeClient()
        val searchResult = client.search("f1 theme")
        val songs = searchResult.songs
        println("SEARCH RESULTS SIZE: ${songs.size}")
        println("CONTINUATION TOKEN: ${searchResult.continuationToken}")
        songs.forEachIndexed { i, song ->
            println("Song $i: ID=${song.id}, Title=${song.title}, Artist=${song.artist}")
        }
        
        if (songs.isNotEmpty()) {
            val firstSong = songs[0]
            val url = client.getStreamUrl(firstSong.id)
            println("RESOLVED STREAM URL FOR '${firstSong.title}': $url")
            assertNotNull(url)
        } else {
            fail("Search returned empty results")
        }
    }

    @Test
    fun testSongAndPlaylistPagination() = runBlocking {
        val client = InnerTubeClient()
        
        println("=== TESTING SONG PAGINATION ===")
        val songSearch = client.search("linkin park")
        println("Initial songs size: ${songSearch.songs.size}")
        println("Initial song continuation token: ${songSearch.continuationToken}")
        
        if (songSearch.continuationToken != null) {
            val moreSongs = client.searchMoreSongs(songSearch.continuationToken)
            println("More songs size: ${moreSongs.songs.size}")
            println("Next song continuation token: ${moreSongs.continuationToken}")
            moreSongs.songs.forEachIndexed { idx, s ->
                println("  Paged Song $idx: ${s.title} by ${s.artist}")
            }
            assertTrue(moreSongs.songs.isNotEmpty())
        } else {
            println("No song continuation token returned!")
        }

        println("=== TESTING PLAYLIST PAGINATION ===")
        val playlistSearch = client.searchPlaylists("rock hits")
        println("Initial playlists size: ${playlistSearch.playlists.size}")
        println("Initial playlist continuation token: ${playlistSearch.continuationToken}")
        
        if (playlistSearch.continuationToken != null) {
            val morePlaylists = client.searchMorePlaylists(playlistSearch.continuationToken)
            println("More playlists size: ${morePlaylists.playlists.size}")
            println("Next playlist continuation token: ${morePlaylists.continuationToken}")
            morePlaylists.playlists.forEachIndexed { idx, p ->
                println("  Paged Playlist $idx: ${p.title} by ${p.author}")
            }
            assertTrue(morePlaylists.playlists.isNotEmpty())
        } else {
            println("No playlist continuation token returned!")
        }
    }

    @Test
    fun testSearchPlaylistsParsing() = runBlocking {
        val client = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json()
            }
        }
        val apiKey = "AIzaSyAo2qkyT98BEPg0IY4v1T-3Z6kH67f1nK8"
        val url = "https://music.youtube.com/youtubei/v1/search?key=$apiKey"
        val payload = buildJsonObject {
            put("context", buildJsonObject {
                put("client", buildJsonObject {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", "1.20230718.01.00")
                    put("hl", "en")
                    put("gl", "US")
                })
            })
            put("query", "Formula 1")
            put("params", "EgWKAQIoAWoKEAkQBRAKEAMQHg==") // Playlists filter
        }

        val response = client.post(url) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            setBody(payload)
        }
        
        val bodyText = response.bodyAsText()
        val json = Json { ignoreUnknownKeys = true }
        val root = json.parseToJsonElement(bodyText)
        
        val items = findObjectsWithKey(root, "musicResponsiveListItemRenderer")
        println("PARSED PLAYLIST ITEMS COUNT: ${items.size}")
        
        val playlists = items.mapNotNull { item ->
            // Extract browseId/playlistId
            val browseId = item["navigationEndpoint"]?.jsonObject
                ?.get("browseEndpoint")?.jsonObject
                ?.get("browseId")?.jsonPrimitive?.content
                ?: item["onTap"]?.jsonObject
                ?.get("watchPlaylistEndpoint")?.jsonObject
                ?.get("playlistId")?.jsonPrimitive?.content
                ?: return@mapNotNull null
            
            val playlistId = if (browseId.startsWith("VL")) browseId.removePrefix("VL") else browseId
            
            val title = getFlexColumnText(item, 0) ?: "Unknown Playlist"
            
            val runs = getFlexColumnRuns(item, 1).filter { it != " • " }
            var author = "Unknown"
            var songCount = ""
            if (runs.size == 1) {
                author = runs[0]
            } else if (runs.size >= 2) {
                if (runs[0].contains("playlist", ignoreCase = true) || runs[0].contains("album", ignoreCase = true)) {
                    author = runs.getOrNull(1) ?: "Unknown"
                    songCount = runs.getOrNull(2) ?: ""
                } else {
                    author = runs[0]
                    songCount = runs.getOrNull(1) ?: ""
                }
            }
            
            val thumbnails = item["thumbnail"]?.jsonObject
                ?.get("musicThumbnailRenderer")?.jsonObject
                ?.get("thumbnail")?.jsonObject
                ?.get("thumbnails")?.jsonArray
            
            val rawThumbUrl = thumbnails?.firstOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content ?: ""
            val cleanThumbUrl = if (rawThumbUrl.contains("=w") && rawThumbUrl.contains("-h")) {
                rawThumbUrl.substringBefore("=w") + "=w400-h400-l90-rj"
            } else {
                rawThumbUrl
            }
            
            println("PLAYLIST: ID=$playlistId, Title='$title', Author='$author', Songs='$songCount', Thumb='$cleanThumbUrl'")
            playlistId
        }
        
        assertTrue(playlists.isNotEmpty())
    }

    @Test
    fun testFindContinuationInJson() = runBlocking {
        val client = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json()
            }
        }
        val apiKey = "AIzaSyAo2qkyT98BEPg0IY4v1T-3Z6kH67f1nK8"
        
        // 1. Search Songs
        val url = "https://music.youtube.com/youtubei/v1/search?key=$apiKey"
        val payload = buildJsonObject {
            put("context", buildJsonObject {
                put("client", buildJsonObject {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", "1.20230718.01.00")
                    put("hl", "en")
                    put("gl", "US")
                })
            })
            put("query", "linkin park")
            put("params", "EgWKAQIIAWoKEAkQBRAKEAMQHg==") // Songs filter
        }

        val response = client.post(url) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            setBody(payload)
        }
        
        val bodyText = response.bodyAsText()
        println("SONG SEARCH JSON LENGTH: ${bodyText.length}")
        
        // Search for occurrences of "continuation" or "token" in the string to see what is present
        val lines = bodyText.split("\n")
        lines.forEachIndexed { index, line ->
            if (line.contains("continuation", ignoreCase = true) || line.contains("token", ignoreCase = true)) {
                // Print the line and a few lines around it
                println("Line $index matching: ${line.trim()}")
                val start = maxOf(0, index - 2)
                val end = minOf(lines.size - 1, index + 5)
                for (i in start..end) {
                    println("  $i: ${lines[i]}")
                }
                println("-------------------------------------")
            }
        }
    }

    @Test
    fun testGetPlaylistTracks() = runBlocking {
        val client = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json()
            }
        }
        val apiKey = "AIzaSyAo2qkyT98BEPg0IY4v1T-3Z6kH67f1nK8"
        val playlistId = "PLcoDB8LYXFNTmk98O2ayYLksrlvGQsOG2" // Real F1 playlist from previous output
        val browseId = "VL$playlistId"
        val url = "https://music.youtube.com/youtubei/v1/browse?key=$apiKey"
        val payload = buildJsonObject {
            put("context", buildJsonObject {
                put("client", buildJsonObject {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", "1.20230718.01.00")
                    put("hl", "en")
                    put("gl", "US")
                })
            })
            put("browseId", browseId)
        }

        val response = client.post(url) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            setBody(payload)
        }
        
        val bodyText = response.bodyAsText()
        val json = Json { ignoreUnknownKeys = true }
        val root = json.parseToJsonElement(bodyText)
        
        val items = findObjectsWithKey(root, "musicResponsiveListItemRenderer")
        println("PARSED PLAYLIST TRACKS COUNT: ${items.size}")
        
        val tracks = items.mapNotNull { item ->
            val videoId = item["playlistItemData"]?.jsonObject?.get("videoId")?.jsonPrimitive?.content
                ?: item["onTap"]?.jsonObject?.get("watchEndpoint")?.jsonObject?.get("videoId")?.jsonPrimitive?.content
                ?: return@mapNotNull null

            val title = getFlexColumnText(item, 0) ?: "Unknown Track"
            val artist = getFlexColumnText(item, 1) ?: "Unknown Artist"
            val album = getFlexColumnText(item, 2)
            
            println("TRACK: ID=$videoId, Title='$title', Artist='$artist', Album='$album'")
            videoId
        }
        
        assertTrue(tracks.isNotEmpty())
    }

    private fun findObjectsWithKey(root: JsonElement, key: String, result: MutableList<JsonObject> = mutableListOf()): List<JsonObject> {
        when (root) {
            is JsonObject -> {
                if (root.containsKey(key)) {
                    root[key]?.jsonObject?.let { result.add(it) }
                }
                root.values.forEach { findObjectsWithKey(it, key, result) }
            }
            is JsonArray -> {
                root.forEach { findObjectsWithKey(it, key, result) }
            }
            else -> {}
        }
        return result
    }

    private fun getFlexColumnText(obj: JsonObject, index: Int): String? {
        val flexColumns = obj["flexColumns"]?.jsonArray ?: return null
        if (index >= flexColumns.size) return null
        val column = flexColumns[index].jsonObject
        val textRenderer = column["musicResponsiveListItemFlexColumnRenderer"]?.jsonObject?.get("text")?.jsonObject ?: return null
        val runs = textRenderer["runs"]?.jsonArray ?: return null
        return runs.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.content }.joinToString("")
    }

    private fun getFlexColumnRuns(obj: JsonObject, index: Int): List<String> {
        val flexColumns = obj["flexColumns"]?.jsonArray ?: return emptyList()
        if (index >= flexColumns.size) return emptyList()
        val column = flexColumns[index].jsonObject
        val textRenderer = column["musicResponsiveListItemFlexColumnRenderer"]?.jsonObject?.get("text")?.jsonObject ?: return emptyList()
        val runs = textRenderer["runs"]?.jsonArray ?: return emptyList()
        return runs.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.content }
    }
}