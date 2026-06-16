package com.example.convenemusic.data.scanner

import android.content.Context
import android.net.Uri
import android.database.Cursor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

abstract class MediaStoreScanner<T>(protected val context: Context) {
    protected abstract val contentUri: Uri
    protected abstract val projection: Array<String>
    protected open val selection: String? = null
    protected open val selectionArgs: Array<String>? = null
    protected open val sortOrder: String? = null

    suspend fun scan(): List<T> = withContext(Dispatchers.IO) {
        val list = mutableListOf<T>()
        try {
            context.contentResolver.query(
                contentUri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    mapCursorRow(cursor)?.let { list.add(it) }
                }
            }
        } catch (e: Exception) {
            Log.e("MediaStoreScanner", "Error scanning media", e)
        }
        list
    }

    protected abstract fun mapCursorRow(cursor: Cursor): T?
}
