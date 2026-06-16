package com.example.convenemusic.data.repository

import com.example.convenemusic.network.Song
import com.example.convenemusic.ui.Movie

interface LocalMediaRepository {
    suspend fun scanAudio(): List<Song>
    suspend fun scanVideo(): List<Movie>
}

interface HistoryRepository {
    suspend fun loadHistory(): List<Song>
    suspend fun saveHistory(songs: List<Song>)
}

interface DownloadRepository {
    suspend fun loadDownloads(): List<Song>
    suspend fun saveDownloads(songs: List<Song>)
    fun isDownloaded(songId: String): Boolean
    suspend fun download(song: Song, onProgress: (Int) -> Unit): Boolean
}
