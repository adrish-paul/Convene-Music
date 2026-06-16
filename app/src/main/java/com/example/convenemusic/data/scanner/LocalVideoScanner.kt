package com.example.convenemusic.data.scanner

import android.content.Context
import android.net.Uri
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import com.example.convenemusic.ui.Movie

class LocalVideoScanner(context: Context) : MediaStoreScanner<Movie>(context) {
    override val contentUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    }

    override val projection: Array<String> = arrayOf(
        MediaStore.Video.Media.DATA,
        MediaStore.Video.Media.TITLE,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.SIZE
    )

    override val sortOrder: String = "${MediaStore.Video.Media.TITLE} ASC"

    override fun mapCursorRow(cursor: Cursor): Movie? {
        val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
        val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
        val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)

        val path = cursor.getString(dataCol) ?: return null
        val title = cursor.getString(titleCol) ?: return null
        val durationMs = cursor.getLong(durationCol)
        val sizeBytes = cursor.getLong(sizeCol)

        val durationSec = (durationMs / 1000).toInt()
        val mins = durationSec / 60
        val secs = durationSec % 60
        val durationText = "$mins:${secs.toString().padStart(2, '0')}"

        val sizeMb = sizeBytes.toFloat() / (1024f * 1024f)
        val sizeText = String.format("%.1f MB", sizeMb)

        return Movie(
            path = path,
            title = title,
            durationText = durationText,
            sizeText = sizeText
        )
    }
}
