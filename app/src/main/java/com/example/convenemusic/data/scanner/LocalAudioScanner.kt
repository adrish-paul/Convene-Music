package com.example.convenemusic.data.scanner

import android.content.Context
import android.net.Uri
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import com.example.convenemusic.network.Song

class LocalAudioScanner(context: Context) : MediaStoreScanner<Song>(context) {
    override val contentUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }

    override val projection: Array<String> = arrayOf(
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.ALBUM_ID
    )

    override val selection: String = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 30000"

    override val sortOrder: String = "${MediaStore.Audio.Media.TITLE} ASC"

    override fun mapCursorRow(cursor: Cursor): Song? {
        val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

        val path = cursor.getString(dataCol) ?: return null
        val title = cursor.getString(titleCol) ?: return null
        val artist = cursor.getString(artistCol) ?: "Unknown Artist"
        val album = cursor.getString(albumCol) ?: ""
        val durationMs = cursor.getLong(durationCol)
        val albumId = cursor.getLong(albumIdCol)

        val albumArtUri = Uri.withAppendedPath(
            Uri.parse("content://media/external/audio/albumart"),
            albumId.toString()
        ).toString()

        val durationSec = (durationMs / 1000).toInt()
        val mins = durationSec / 60
        val secs = durationSec % 60
        val durationText = "$mins:${secs.toString().padStart(2, '0')}"

        return Song(
            id = path,
            title = title,
            artist = artist,
            album = album,
            thumbnailUrl = albumArtUri,
            durationText = durationText,
            durationSeconds = durationSec
        )
    }
}
