package com.example.convenemusic.data.repository

import android.content.Context
import android.util.Log
import com.example.convenemusic.data.scanner.LocalAudioScanner
import com.example.convenemusic.data.scanner.LocalVideoScanner
import com.example.convenemusic.network.InnerTubeClient
import com.example.convenemusic.network.Song
import com.example.convenemusic.ui.Movie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class LocalMediaRepositoryImpl(private val context: Context) : LocalMediaRepository {
    override suspend fun scanAudio(): List<Song> {
        return LocalAudioScanner(context).scan()
    }

    override suspend fun scanVideo(): List<Movie> {
        return LocalVideoScanner(context).scan()
    }
}

class HistoryRepositoryImpl(context: Context) : HistoryRepository {
    private val historyFile = File(context.filesDir, "history.json")

    override suspend fun loadHistory(): List<Song> = withContext(Dispatchers.IO) {
        if (historyFile.exists()) {
            try {
                Json.decodeFromString<List<Song>>(historyFile.readText())
            } catch (e: Exception) {
                Log.e("HistoryRepository", "Failed to load history", e)
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    override suspend fun saveHistory(songs: List<Song>): Unit = withContext(Dispatchers.IO) {
        try {
            historyFile.writeText(Json.encodeToString(songs))
        } catch (e: Exception) {
            Log.e("HistoryRepository", "Failed to save history", e)
        }
    }
}

class DownloadRepositoryImpl(
    private val context: Context,
    private val client: InnerTubeClient
) : DownloadRepository {
    private val downloadsFile = File(context.filesDir, "downloads.json")
    private val downloadsDir = File(context.filesDir, ".downloads")

    override suspend fun loadDownloads(): List<Song> = withContext(Dispatchers.IO) {
        if (downloadsFile.exists()) {
            try {
                val list = Json.decodeFromString<List<Song>>(downloadsFile.readText())
                val validList = list.filter { song ->
                    File(downloadsDir, "${song.id}.mp3").exists()
                }
                if (validList.size != list.size) {
                    downloadsFile.writeText(Json.encodeToString(validList))
                }
                validList
            } catch (e: Exception) {
                Log.e("DownloadRepository", "Failed to load downloads", e)
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    override suspend fun saveDownloads(songs: List<Song>): Unit = withContext(Dispatchers.IO) {
        try {
            downloadsFile.writeText(Json.encodeToString(songs))
        } catch (e: Exception) {
            Log.e("DownloadRepository", "Failed to save downloads", e)
        }
    }

    override fun isDownloaded(songId: String): Boolean {
        return File(downloadsDir, "$songId.mp3").exists()
    }

    override suspend fun download(song: Song, onProgress: (Int) -> Unit): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = client.getStreamUrl(song.id) ?: return@withContext false
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val tempFile = File(downloadsDir, "${song.id}.tmp")
            val destinationFile = File(downloadsDir, "${song.id}.mp3")

            val connection = URL(url).openConnection()
            connection.connect()
            val contentLength = connection.contentLength

            connection.getInputStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    var totalBytesRead = 0L
                    var lastProgressUpdate = -1

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        if (contentLength > 0) {
                            val progress = ((totalBytesRead * 100) / contentLength).toInt()
                            if (progress != lastProgressUpdate) {
                                lastProgressUpdate = progress
                                onProgress(progress)
                            }
                        }
                    }
                }
            }

            if (tempFile.renameTo(destinationFile)) {
                // Add to list and save
                val current = loadDownloads()
                val updated = current.filter { it.id != song.id } + song
                saveDownloads(updated)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("DownloadRepository", "Error downloading song ${song.title}", e)
            false
        }
    }
}
