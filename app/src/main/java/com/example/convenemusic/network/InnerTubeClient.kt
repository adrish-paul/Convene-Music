package com.example.convenemusic.network

import android.util.Log
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val durationText: String = "0:00",
    val durationSeconds: Int = 0,
    val thumbnailUrl: String,
    val viewsText: String? = null
)

@Serializable
data class LyricLine(
    val timestampMs: Long,
    val text: String
)

@Serializable
data class LyricsData(
    val isSynced: Boolean,
    val lines: List<LyricLine>
)

@Serializable
data class Playlist(
    val id: String,
    val title: String,
    val author: String,
    val songCountText: String?,
    val thumbnailUrl: String
)

@Serializable
data class SongSearchResult(
    val songs: List<Song>,
    val continuationToken: String?
)

@Serializable
data class PlaylistSearchResult(
    val playlists: List<Playlist>,
    val continuationToken: String?
)

class InnerTubeClient {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        prettyPrint = true
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    private val apiKey = "AIzaSyAo2qkyT98BEPg0IY4v1T-3Z6kH67f1nK8"
    private var visitorData: String? = "CgtJVDlTVnlIbWw2byiMzr3RBjIKCgJJThIEGgAgaA=="

    suspend fun search(query: String): SongSearchResult {
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
            put("query", query)
            put("params", "EgWKAQIIAWoKEAkQBRAKEAMQHg==") // Filter songs to keep search clean
        }

        return try {
            val response = client.post(url) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                setBody(payload)
            }
            if (response.status == HttpStatusCode.OK) {
                val responseBody = response.bodyAsText()
                val jsonElement = json.parseToJsonElement(responseBody)
                
                // Dynamically update session visitorData
                val vData = jsonElement.jsonObject["responseContext"]?.jsonObject?.get("visitorData")?.jsonPrimitive?.content
                if (vData != null) {
                    visitorData = vData
                    Log.e("InnerTubeClient", "Saved visitorData from search: $vData")
                }
                
                SongSearchResult(
                    songs = parseSearchResults(jsonElement),
                    continuationToken = parseContinuationToken(jsonElement)
                )
            } else {
                Log.e("InnerTubeClient", "Search response error: ${response.status}")
                SongSearchResult(emptyList(), null)
            }
        } catch (e: Exception) {
            Log.e("InnerTubeClient", "Search failed: ${e.message}", e)
            SongSearchResult(emptyList(), null)
        }
    }

    suspend fun searchMoreSongs(continuationToken: String): SongSearchResult {
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
            put("continuation", continuationToken)
        }

        return try {
            val response = client.post(url) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                setBody(payload)
            }
            if (response.status == HttpStatusCode.OK) {
                val responseBody = response.bodyAsText()
                val jsonElement = json.parseToJsonElement(responseBody)
                SongSearchResult(
                    songs = parseSearchResults(jsonElement),
                    continuationToken = parseContinuationToken(jsonElement)
                )
            } else {
                Log.e("InnerTubeClient", "Search continuation response error: ${response.status}")
                SongSearchResult(emptyList(), null)
            }
        } catch (e: Exception) {
            Log.e("InnerTubeClient", "Search continuation failed: ${e.message}", e)
            SongSearchResult(emptyList(), null)
        }
    }

    suspend fun searchPlaylists(query: String): PlaylistSearchResult {
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
            put("query", query)
            put("params", "EgWKAQIoAWoKEAkQBRAKEAMQHg==") // Playlists filter
        }

        return try {
            val response = client.post(url) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                setBody(payload)
            }
            if (response.status == HttpStatusCode.OK) {
                val responseBody = response.bodyAsText()
                val jsonElement = json.parseToJsonElement(responseBody)
                PlaylistSearchResult(
                    playlists = parsePlaylistSearchResults(jsonElement),
                    continuationToken = parseContinuationToken(jsonElement)
                )
            } else {
                Log.e("InnerTubeClient", "Playlist search response error: ${response.status}")
                PlaylistSearchResult(emptyList(), null)
            }
        } catch (e: Exception) {
            Log.e("InnerTubeClient", "Playlist search failed: ${e.message}", e)
            PlaylistSearchResult(emptyList(), null)
        }
    }

    suspend fun searchMorePlaylists(continuationToken: String): PlaylistSearchResult {
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
            put("continuation", continuationToken)
        }

        return try {
            val response = client.post(url) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                setBody(payload)
            }
            if (response.status == HttpStatusCode.OK) {
                val responseBody = response.bodyAsText()
                val jsonElement = json.parseToJsonElement(responseBody)
                PlaylistSearchResult(
                    playlists = parsePlaylistSearchResults(jsonElement),
                    continuationToken = parseContinuationToken(jsonElement)
                )
            } else {
                Log.e("InnerTubeClient", "Playlist search continuation response error: ${response.status}")
                PlaylistSearchResult(emptyList(), null)
            }
        } catch (e: Exception) {
            Log.e("InnerTubeClient", "Playlist search continuation failed: ${e.message}", e)
            PlaylistSearchResult(emptyList(), null)
        }
    }

    suspend fun getPlaylistTracks(playlistId: String): List<Song> {
        val browseId = if (playlistId.startsWith("VL")) playlistId else "VL$playlistId"
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

        return try {
            val response = client.post(url) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                setBody(payload)
            }
            if (response.status == HttpStatusCode.OK) {
                val responseBody = response.bodyAsText()
                val jsonElement = json.parseToJsonElement(responseBody)
                parseSearchResults(jsonElement)
            } else {
                Log.e("InnerTubeClient", "Playlist browse response error: ${response.status}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("InnerTubeClient", "Failed to get playlist tracks: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getStreamUrl(videoId: String): String? {
        val url = "https://www.youtube.com/youtubei/v1/player?key=$apiKey"
        val payload = buildJsonObject {
            put("context", buildJsonObject {
                put("client", buildJsonObject {
                    put("clientName", "ANDROID_VR")
                    put("clientVersion", "1.60.19")
                    if (visitorData != null) {
                        put("visitorData", visitorData)
                    }
                    put("hl", "en")
                    put("gl", "US")
                })
            })
            put("videoId", videoId)
        }

        return try {
            val response = client.post(url) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.UserAgent, "com.google.android.apps.youtube.vr.oculus/1.60.19 (Linux; U; Android 12L; Quest 3 Build/SQ3A.220605.009.A1) gzip")
                setBody(payload)
            }
            if (response.status == HttpStatusCode.OK) {
                val responseBody = response.bodyAsText()
                val jsonElement = json.parseToJsonElement(responseBody)
                
                val streamingData = jsonElement.jsonObject["streamingData"]?.jsonObject ?: run {
                    println("STREAMING DATA MISSING! Response Body was:\n$responseBody")
                    Log.e("InnerTubeClient", "streamingData is missing in response: $responseBody")
                    return null
                }
                
                // Fallback 1: adaptiveFormats (highest quality direct stream)
                val adaptiveFormats = streamingData["adaptiveFormats"]?.jsonArray
                if (adaptiveFormats != null) {
                    val bestAudio = adaptiveFormats.mapNotNull { it.jsonObject }
                        .filter {
                            val mimeType = it["mimeType"]?.jsonPrimitive?.content ?: ""
                            mimeType.startsWith("audio/") && it.containsKey("url")
                        }
                        .maxByOrNull {
                            it["bitrate"]?.jsonPrimitive?.longOrNull ?: 0L
                        }
                    
                    val directUrl = bestAudio?.get("url")?.jsonPrimitive?.content
                    if (directUrl != null) {
                        Log.e("InnerTubeClient", "Found direct adaptive audio URL: $directUrl")
                        return directUrl
                    }
                }

                // Fallback 2: HLS stream (m3u8 index)
                val hlsUrl = streamingData["hlsManifestUrl"]?.jsonPrimitive?.content
                if (hlsUrl != null) {
                    Log.e("InnerTubeClient", "Found fallback HLS manifest URL: $hlsUrl")
                    return hlsUrl
                }

                Log.e("InnerTubeClient", "No direct audio format or HLS URL found: $responseBody")
                null
            } else {
                Log.e("InnerTubeClient", "Player response error: ${response.status}")
                null
            }
        } catch (e: Exception) {
            println("GETSTREAMURL EXCEPTION: ${e.message}")
            e.printStackTrace()
            Log.e("InnerTubeClient", "GetStreamUrl failed for $videoId: ${e.message}", e)
            null
        }
    }

    private fun parseDurationTextToSeconds(durationText: String): Int {
        val parts = durationText.split(":")
        return try {
            when (parts.size) {
                1 -> parts[0].toIntOrNull() ?: 0
                2 -> {
                    val minutes = parts[0].toIntOrNull() ?: 0
                    val seconds = parts[1].toIntOrNull() ?: 0
                    minutes * 60 + seconds
                }
                3 -> {
                    val hours = parts[0].toIntOrNull() ?: 0
                    val minutes = parts[1].toIntOrNull() ?: 0
                    val seconds = parts[2].toIntOrNull() ?: 0
                    hours * 3600 + minutes * 60 + seconds
                }
                else -> 0
            }
        } catch (e: Exception) {
            0
        }
    }

    private fun parseSearchResults(root: JsonElement): List<Song> {
        val items = root.findObjectsWithKey("musicResponsiveListItemRenderer")
        return items.mapNotNull { item ->
            val videoId = item["playlistItemData"]?.jsonObject?.get("videoId")?.jsonPrimitive?.content
                ?: item["onTap"]?.jsonObject?.get("watchEndpoint")?.jsonObject?.get("videoId")?.jsonPrimitive?.content
                ?: item["navigationEndpoint"]?.jsonObject?.get("watchEndpoint")?.jsonObject?.get("videoId")?.jsonPrimitive?.content
                ?: return@mapNotNull null

            val title = item.getFlexColumnText(0) ?: "Unknown Track"
            val rawArtist = item.getFlexColumnText(1) ?: "Unknown Artist"
            val bulletParts = rawArtist.split(Regex(" • | •|• |•|\\u2022"))
            val artist = bulletParts.getOrNull(0)?.trim() ?: rawArtist
            val album = bulletParts.getOrNull(1)?.trim() ?: item.getFlexColumnText(2)

            var viewsText: String? = null
            for (i in 0 until 5) {
                val text = item.getFlexColumnText(i) ?: continue
                val parts = text.split(Regex(" • | •|• |•|\\u2022"))
                val match = parts.firstOrNull { it.contains("view", ignoreCase = true) }
                if (match != null) {
                    viewsText = match.trim()
                    break
                }
            }

            val durationRegex = Regex("""^\d+:\d{2}(:\d{2})?$""")
            var durationText = "0:00"
            var durationSeconds = 0
            var foundDuration = false
            for (i in 0 until 5) {
                if (foundDuration) break
                val text = item.getFlexColumnText(i) ?: continue
                val parts = text.split(Regex(" • | •|• |•|\\u2022"))
                for (part in parts) {
                    val trimmed = part.trim()
                    if (durationRegex.matches(trimmed)) {
                        durationText = trimmed
                        durationSeconds = parseDurationTextToSeconds(trimmed)
                        foundDuration = true
                        break
                    }
                }
            }

            // Extract high-res thumbnail
            val thumbnails = item["thumbnail"]?.jsonObject
                ?.get("musicThumbnailRenderer")?.jsonObject
                ?.get("thumbnail")?.jsonObject
                ?.get("thumbnails")?.jsonArray
            
            val rawThumbUrl = thumbnails?.firstOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content ?: ""
            // Standard youtube thumbnail upscaler
            val cleanThumbUrl = if (rawThumbUrl.contains("=w") && rawThumbUrl.contains("-h")) {
                rawThumbUrl.substringBefore("=w") + "=w400-h400-l90-rj"
            } else {
                rawThumbUrl
            }

            Song(
                id = videoId,
                title = title,
                artist = artist,
                album = album,
                durationText = durationText,
                durationSeconds = durationSeconds,
                thumbnailUrl = cleanThumbUrl,
                viewsText = viewsText
            )
        }
    }

    private fun parsePlaylistSearchResults(root: JsonElement): List<Playlist> {
        val items = root.findObjectsWithKey("musicResponsiveListItemRenderer")
        return items.mapNotNull { item ->
            val browseId = item["navigationEndpoint"]?.jsonObject
                ?.get("browseEndpoint")?.jsonObject
                ?.get("browseId")?.jsonPrimitive?.content
                ?: item["onTap"]?.jsonObject
                ?.get("watchPlaylistEndpoint")?.jsonObject
                ?.get("playlistId")?.jsonPrimitive?.content
                ?: return@mapNotNull null
            
            val playlistId = if (browseId.startsWith("VL")) browseId.removePrefix("VL") else browseId
            val title = item.getFlexColumnText(0) ?: "Unknown Playlist"
            
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
            
            Playlist(
                id = playlistId,
                title = title,
                author = author,
                songCountText = songCount.ifBlank { null },
                thumbnailUrl = cleanThumbUrl
            )
        }
    }

    private fun JsonElement.findObjectsWithKey(key: String, result: MutableList<JsonObject> = mutableListOf()): List<JsonObject> {
        when (this) {
            is JsonObject -> {
                if (this.containsKey(key)) {
                    this[key]?.jsonObject?.let { result.add(it) }
                }
                this.values.forEach { it.findObjectsWithKey(key, result) }
            }
            is JsonArray -> {
                this.forEach { it.findObjectsWithKey(key, result) }
            }
            else -> {}
        }
        return result
    }

    private fun JsonObject.getFlexColumnText(index: Int): String? {
        val flexColumns = this["flexColumns"]?.jsonArray ?: return null
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

    private fun parseContinuationToken(root: JsonElement): String? {
        // Pattern 1: nextContinuationData (used in search results)
        val nextContDataList = root.findObjectsWithKey("nextContinuationData")
        val nextContToken = nextContDataList.firstOrNull()
            ?.get("continuation")?.jsonPrimitive?.content
        if (nextContToken != null) return nextContToken

        // Pattern 2: continuationItemRenderer (used in browse/library results)
        val continuations = root.findObjectsWithKey("continuationItemRenderer")
        return continuations.firstOrNull()
            ?.get("continuationEndpoint")?.jsonObject
            ?.get("continuationCommand")?.jsonObject
            ?.get("token")?.jsonPrimitive?.content
    }

    suspend fun getArtistThumbnail(artistName: String): String? {
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
            put("query", artistName)
            put("params", "EgWKAQIgAWoKEAkQBRAKEAMQHg==") // Artists filter
        }

        return try {
            val response = client.post(url) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                setBody(payload)
            }
            if (response.status == HttpStatusCode.OK) {
                val responseBody = response.bodyAsText()
                val jsonElement = json.parseToJsonElement(responseBody)
                val items = jsonElement.findObjectsWithKey("musicResponsiveListItemRenderer")
                val firstArtistItem = items.firstOrNull { item ->
                    val title = item.getFlexColumnText(0) ?: ""
                    title.equals(artistName, ignoreCase = true)
                } ?: items.firstOrNull() // Fallback to first result if no exact match

                if (firstArtistItem != null) {
                    val thumbnails = firstArtistItem["thumbnail"]?.jsonObject
                        ?.get("musicThumbnailRenderer")?.jsonObject
                        ?.get("thumbnail")?.jsonObject
                        ?.get("thumbnails")?.jsonArray
                    val rawThumbUrl = thumbnails?.firstOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content
                    if (rawThumbUrl != null) {
                        if (rawThumbUrl.contains("=w") && rawThumbUrl.contains("-h")) {
                            rawThumbUrl.substringBefore("=w") + "=w400-h400-l90-rj"
                        } else {
                            rawThumbUrl
                        }
                    } else null
                } else null
            } else null
        } catch (e: Exception) {
            Log.e("InnerTubeClient", "Failed to get artist thumbnail for $artistName: ${e.message}", e)
            null
        }
    }

    suspend fun getLyricsBrowseId(videoId: String): String? {
        val url = "https://music.youtube.com/youtubei/v1/next?key=$apiKey"
        val payload = buildJsonObject {
            put("context", buildJsonObject {
                put("client", buildJsonObject {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", "1.20230718.01.00")
                    put("hl", "en")
                    put("gl", "US")
                    if (visitorData != null) {
                        put("visitorData", visitorData)
                    }
                })
            })
            put("videoId", videoId)
        }

        return try {
            val response = client.post(url) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                setBody(payload)
            }
            if (response.status == HttpStatusCode.OK) {
                val responseBody = response.bodyAsText()
                val jsonElement = json.parseToJsonElement(responseBody)
                jsonElement.findLyricsBrowseId()
            } else {
                Log.e("InnerTubeClient", "Next endpoint response error: ${response.status}")
                null
            }
        } catch (e: Exception) {
            Log.e("InnerTubeClient", "Failed to get lyrics browseId: ${e.message}", e)
            null
        }
    }

    private fun JsonElement.findLyricsBrowseId(): String? {
        when (this) {
            is JsonObject -> {
                if (containsKey("browseId")) {
                    val id = this["browseId"]?.jsonPrimitive?.content
                    if (id != null && id.startsWith("MPLYt")) {
                        return id
                    }
                }
                for (value in values) {
                    val found = value.findLyricsBrowseId()
                    if (found != null) return found
                }
            }
            is JsonArray -> {
                for (item in this) {
                    val found = item.findLyricsBrowseId()
                    if (found != null) return found
                }
            }
            else -> {}
        }
        return null
    }

    suspend fun getLyrics(browseId: String): String? {
        val url = "https://music.youtube.com/youtubei/v1/browse?key=$apiKey"
        val payload = buildJsonObject {
            put("context", buildJsonObject {
                put("client", buildJsonObject {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", "1.20230718.01.00")
                    put("hl", "en")
                    put("gl", "US")
                    if (visitorData != null) {
                        put("visitorData", visitorData)
                    }
                })
            })
            put("browseId", browseId)
        }

        return try {
            val response = client.post(url) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                setBody(payload)
            }
            if (response.status == HttpStatusCode.OK) {
                val responseBody = response.bodyAsText()
                val jsonElement = json.parseToJsonElement(responseBody)
                val shelfList = jsonElement.findObjectsWithKey("musicDescriptionShelfRenderer")
                if (shelfList.isNotEmpty()) {
                    val shelf = shelfList[0]
                    val runs = shelf["description"]?.jsonObject?.get("runs")?.jsonArray
                    val lyricsText = runs?.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.content }?.joinToString("")
                    
                    val footerRuns = shelf["footer"]?.jsonObject?.get("runs")?.jsonArray
                    val footerText = footerRuns?.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.content }?.joinToString("")
                    
                    if (!footerText.isNullOrBlank()) {
                        lyricsText + "\n\n" + footerText
                    } else {
                        lyricsText
                    }
                } else {
                    null
                }
            } else {
                Log.e("InnerTubeClient", "Browse endpoint response error: ${response.status}")
                null
            }
        } catch (e: Exception) {
            Log.e("InnerTubeClient", "Failed to get lyrics: ${e.message}", e)
            null
        }
    }

    private suspend fun getLrcLyricsInternal(url: String, trackName: String, artistName: String, durationSeconds: Int): String? {
        Log.i("InnerTubeClient", "getLrcLyricsInternal: url=$url, trackName='$trackName', artistName='$artistName', durationSeconds=$durationSeconds")
        return try {
            val response = kotlinx.coroutines.withTimeoutOrNull(15000) {
                client.get(url) {
                    parameter("track_name", trackName)
                    parameter("artist_name", artistName)
                    if (durationSeconds > 0) {
                        parameter("duration", durationSeconds)
                    }
                    header(HttpHeaders.UserAgent, "ConveneMusic/1.0 (https://github.com/adrish-paul/Convene-Music)")
                }
            }
            if (response != null) {
                Log.i("InnerTubeClient", "getLrcLyricsInternal: Received response status=${response.status}")
                if (response.status == HttpStatusCode.OK) {
                    val responseBody = response.bodyAsText()
                    val jsonElement = json.parseToJsonElement(responseBody)
                    val synced = jsonElement.jsonObject["syncedLyrics"]?.jsonPrimitive?.content
                    if (!synced.isNullOrBlank()) {
                        Log.i("InnerTubeClient", "getLrcLyricsInternal: Found synced lyrics (length=${synced.length})")
                        synced
                    } else {
                        val plain = jsonElement.jsonObject["plainLyrics"]?.jsonPrimitive?.content
                        Log.i("InnerTubeClient", "getLrcLyricsInternal: Synced lyrics null/blank, found plain lyrics (length=${plain?.length ?: 0})")
                        plain
                    }
                } else {
                    Log.e("InnerTubeClient", "getLrcLyricsInternal: LRCLib returned status ${response.status} for duration=$durationSeconds")
                    null
                }
            } else {
                Log.e("InnerTubeClient", "getLrcLyricsInternal: Query timed out (15000ms) for duration=$durationSeconds")
                null
            }
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException) {
                Log.w("InnerTubeClient", "getLrcLyricsInternal: Coroutine cancelled for duration=$durationSeconds")
                throw e
            }
            Log.e("InnerTubeClient", "getLrcLyricsInternal: Failed to fetch from LRCLib for duration=$durationSeconds", e)
            null
        }
    }

    suspend fun getLrcLyrics(trackName: String, artistName: String, durationSeconds: Int): String? {
        val url = "https://lrclib.net/api/get"
        if (durationSeconds > 0) {
            Log.i("InnerTubeClient", "getLrcLyrics: Querying LRCLib with duration=$durationSeconds for '$trackName' by '$artistName'")
            val result = getLrcLyricsInternal(url, trackName, artistName, durationSeconds)
            if (result != null) return result
            Log.i("InnerTubeClient", "getLrcLyrics: LRCLib query with duration failed/timed out, retrying WITHOUT duration")
        }
        Log.i("InnerTubeClient", "getLrcLyrics: Querying LRCLib without duration for '$trackName' by '$artistName'")
        return getLrcLyricsInternal(url, trackName, artistName, 0)
    }


    fun parseLyricsText(lyricsText: String): LyricsData {
        val lines = lyricsText.split("\n")
        val result = mutableListOf<LyricLine>()
        val regex = Regex("""^\[(\d+):(\d{2})(?:[.:](\d{1,3}))?\](.*)""")
        var isSynced = false
        
        for (line in lines) {
            val trimmed = line.trim()
            val match = regex.find(trimmed)
            if (match != null) {
                isSynced = true
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val msStr = match.groupValues[3]
                var ms = msStr.toLongOrNull() ?: 0L
                if (msStr.length == 2) {
                    ms *= 10
                } else if (msStr.length == 1) {
                    ms *= 100
                }
                val text = match.groupValues[4].trim()
                val timeMs = min * 60000 + sec * 1000 + ms
                result.add(LyricLine(timeMs, text))
            }
        }
        
        return if (isSynced) {
            LyricsData(isSynced = true, lines = result.sortedBy { it.timestampMs })
        } else {
            val plainLines = lines.map { LyricLine(-1L, it.trim()) }
            LyricsData(isSynced = false, lines = plainLines)
        }
    }
}
