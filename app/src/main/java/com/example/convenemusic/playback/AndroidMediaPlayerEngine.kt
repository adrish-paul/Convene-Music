package com.example.convenemusic.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.convenemusic.network.Song
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import coil3.asDrawable
import coil3.request.allowHardware

@OptIn(UnstableApi::class)
class AndroidMediaPlayerEngine(private val context: Context) {
    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var pendingPlay: (() -> Unit)? = null
    
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private var isPreloaded = false

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    init {
        val serviceIntent = android.content.Intent(context, PlaybackService::class.java)
        try {
            context.startService(serviceIntent)
        } catch (e: Exception) {
            Log.e("AndroidMediaPlayerEngine", "Failed to start PlaybackService in init", e)
        }

        try {
            val prefs = context.getSharedPreferences("convenemusic_prefs", Context.MODE_PRIVATE)
            val savedSongJson = prefs.getString("last_played_song", null)
            if (savedSongJson != null) {
                val song = Json.decodeFromString<Song>(savedSongJson)
                _currentSong.value = song
            }
        } catch (e: Exception) {
            Log.e("AndroidMediaPlayerEngine", "Failed to load saved song", e)
        }

        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.let { future ->
            future.addListener({
                try {
                    val controller = future.get()
                    mediaController = controller
                    setupController(controller)
                    pendingPlay?.invoke()
                    pendingPlay = null
                    Log.e("AndroidMediaPlayerEngine", "MediaController successfully connected")
                } catch (e: Exception) {
                    Log.e("AndroidMediaPlayerEngine", "Failed to connect MediaController", e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }

    private fun getActivePlayer(): Player? {
        val directPlayer = PlaybackServiceConnector.player
        if (directPlayer != null) {
            Log.d("AndroidMediaPlayerEngine", "getActivePlayer: Using direct service player reference")
            return directPlayer
        }
        Log.d("AndroidMediaPlayerEngine", "getActivePlayer: Direct player null. Using mediaController")
        return mediaController
    }

    private fun saveSongToPrefs(song: Song) {
        try {
            val prefs = context.getSharedPreferences("convenemusic_prefs", Context.MODE_PRIVATE)
            val songJson = Json.encodeToString(song)
            prefs.edit().putString("last_played_song", songJson).apply()
        } catch (e: Exception) {
            Log.e("AndroidMediaPlayerEngine", "Failed to save song to prefs", e)
        }
    }

    private fun updateCurrentSong(song: Song?) {
        _currentSong.value = song
        if (song != null) {
            saveSongToPrefs(song)
        }
    }

    private fun setupController(controller: MediaController) {
        updateStateFromController(controller)

        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                Log.d("AndroidMediaPlayerEngine", "onIsPlayingChanged: playing=$playing")
                _isPlaying.value = playing
                if (playing) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                val stateString = when (state) {
                    Player.STATE_IDLE -> "IDLE"
                    Player.STATE_BUFFERING -> "BUFFERING"
                    Player.STATE_READY -> "READY"
                    Player.STATE_ENDED -> "ENDED"
                    else -> "UNKNOWN"
                }
                Log.d("AndroidMediaPlayerEngine", "onPlaybackStateChanged: state=$stateString")

                _isBuffering.value = state == Player.STATE_BUFFERING

                if (state == Player.STATE_READY) {
                    _duration.value = controller.duration
                    isPreloaded = false
                } else if (state == Player.STATE_ENDED) {
                    _isPlaying.value = false
                    _isBuffering.value = false
                    stopProgressTracker()
                } else if (state == Player.STATE_IDLE) {
                    _isBuffering.value = false
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (mediaItem != null) {
                    try {
                        val song = Json.decodeFromString<Song>(mediaItem.mediaId)
                        updateCurrentSong(song)
                        
                        val activePlayer = getActivePlayer()
                        if (activePlayer != null && activePlayer.currentMediaItemIndex == 1) {
                            activePlayer.removeMediaItem(0)
                            Log.e("AndroidMediaPlayerEngine", "onMediaItemTransition: Removed old media item at index 0")
                        }
                    } catch (e: Exception) {
                        Log.e("AndroidMediaPlayerEngine", "Failed to decode song from mediaId: ${mediaItem.mediaId}", e)
                    }
                }
            }
        })

        if (controller.isPlaying) {
            startProgressTracker()
        }
    }

    private fun updateStateFromController(controller: MediaController) {
        _isPlaying.value = controller.isPlaying
        _duration.value = controller.duration
        _currentPosition.value = controller.currentPosition
        controller.currentMediaItem?.let { item ->
            try {
                val song = Json.decodeFromString<Song>(item.mediaId)
                updateCurrentSong(song)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun setCurrentSong(song: Song) {
        updateCurrentSong(song)
        _currentPosition.value = 0L
        _duration.value = 0L
    }

    private fun scaleAndCompressBitmap(bitmap: android.graphics.Bitmap): ByteArray? {
        return try {
            val maxDim = 256
            val width = bitmap.width
            val height = bitmap.height
            val scaledBmp = if (width > maxDim || height > maxDim) {
                val ratio = width.toFloat() / height.toFloat()
                val newWidth = if (ratio > 1) maxDim else (maxDim * ratio).toInt()
                val newHeight = if (ratio > 1) (maxDim / ratio).toInt() else maxDim
                android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            } else {
                bitmap
            }
            val stream = java.io.ByteArrayOutputStream()
            scaledBmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 75, stream)
            val bytes = stream.toByteArray()
            if (scaledBmp != bitmap) {
                scaledBmp.recycle()
            }
            bytes
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getArtworkBytes(url: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val loader = coil3.SingletonImageLoader.get(context)
            val request = coil3.request.ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            if (result is coil3.request.SuccessResult) {
                val drawable = result.image.asDrawable(context.resources)
                val bitmap = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    scaleAndCompressBitmap(bitmap)
                } else {
                    drawDrawableToBytes(drawable)
                }
            } else {
                getDefaultArtworkBytes()
            }
        } catch (e: Exception) {
            Log.e("AndroidMediaPlayerEngine", "Failed to load artwork bytes for $url: ${e.message}", e)
            getDefaultArtworkBytes()
        }
    }

    private fun drawDrawableToBytes(drawable: android.graphics.drawable.Drawable?): ByteArray? {
        if (drawable == null) return null
        return try {
            val width = drawable.intrinsicWidth.takeIf { it > 0 && it <= 256 } ?: 256
            val height = drawable.intrinsicHeight.takeIf { it > 0 && it <= 256 } ?: 256
            val bmp = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)
            val stream = java.io.ByteArrayOutputStream()
            bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 75, stream)
            val bytes = stream.toByteArray()
            bmp.recycle()
            bytes
        } catch (e: Exception) {
            null
        }
    }

    private fun getDefaultArtworkBytes(): ByteArray? {
        return try {
            val drawable = ContextCompat.getDrawable(context, com.example.convenemusic.R.mipmap.ic_launcher)
            drawDrawableToBytes(drawable)
        } catch (e: Exception) {
            null
        }
    }

    fun preload(song: Song, streamUrl: String, artworkBytes: ByteArray? = null) {
        Log.e("AndroidMediaPlayerEngine", "preload: Pre-buffering '${song.title}'")
        updateCurrentSong(song)
        _currentPosition.value = 0L
        _duration.value = 0L

        val action: () -> Unit = {
            getActivePlayer()?.let { player ->
                val songJson = Json.encodeToString(song)
                val finalArtworkBytes = artworkBytes ?: getDefaultArtworkBytes()
                val metadataBuilder = MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setDisplayTitle(song.title)
                    .setAlbumTitle("Convene")
                    .setArtworkUri(Uri.parse(song.thumbnailUrl))
                    .setFolderType(MediaMetadata.FOLDER_TYPE_NONE)
                    .setIsPlayable(true)

                if (finalArtworkBytes != null) {
                    metadataBuilder.setArtworkData(finalArtworkBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                }

                val mediaMetadata = metadataBuilder.build()

                val uri = if (streamUrl.startsWith("http://") || streamUrl.startsWith("https://")) {
                    Uri.parse(streamUrl)
                } else {
                    Uri.fromFile(java.io.File(streamUrl))
                }

                val mediaItem = MediaItem.Builder()
                    .setMediaId(songJson)
                    .setUri(uri)
                    .setMediaMetadata(mediaMetadata)
                    .build()

                player.setMediaItem(mediaItem)
                player.prepare()
                isPreloaded = true
                Log.e("AndroidMediaPlayerEngine", "preload: Player prepared for ${song.title} (paused)")
            }
        }

        if (getActivePlayer() != null) {
            action()
        } else {
            pendingPlay = action
        }
    }

    fun setNextSong(song: Song, streamUrl: String, artworkBytes: ByteArray? = null) {
        val action: () -> Unit = {
            getActivePlayer()?.let { player ->
                val songJson = Json.encodeToString(song)
                val finalArtworkBytes = artworkBytes ?: getDefaultArtworkBytes()
                val metadataBuilder = MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setDisplayTitle(song.title)
                    .setAlbumTitle("Convene")
                    .setArtworkUri(Uri.parse(song.thumbnailUrl))
                    .setFolderType(MediaMetadata.FOLDER_TYPE_NONE)
                    .setIsPlayable(true)

                if (finalArtworkBytes != null) {
                    metadataBuilder.setArtworkData(finalArtworkBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                }

                val mediaMetadata = metadataBuilder.build()

                val uri = if (streamUrl.startsWith("http://") || streamUrl.startsWith("https://")) {
                    Uri.parse(streamUrl)
                } else {
                    Uri.fromFile(java.io.File(streamUrl))
                }

                val mediaItem = MediaItem.Builder()
                    .setMediaId(songJson)
                    .setUri(uri)
                    .setMediaMetadata(mediaMetadata)
                    .build()

                if (player.mediaItemCount > 1) {
                    for (i in player.mediaItemCount - 1 downTo 1) {
                        player.removeMediaItem(i)
                    }
                }
                player.addMediaItem(mediaItem)
                Log.e("AndroidMediaPlayerEngine", "setNextSong: Added next media item: ${song.title}")
            }
        }

        if (getActivePlayer() != null) {
            action()
        } else {
            pendingPlay = action
        }
    }

    fun play(song: Song, streamUrl: String, artworkBytes: ByteArray? = null) {
        val serviceIntent = android.content.Intent(context, PlaybackService::class.java)
        try {
            context.startService(serviceIntent)
        } catch (e: Exception) {
            Log.e("AndroidMediaPlayerEngine", "Failed to start PlaybackService in play", e)
        }

        Log.e("AndroidMediaPlayerEngine", "play: Loading song '${song.title}' with URL: $streamUrl")
        updateCurrentSong(song)
        _currentPosition.value = 0L
        _duration.value = 0L

        val action: () -> Unit = {
            getActivePlayer()?.let { player ->
                val alreadyPreloaded = isPreloaded &&
                        (player.playbackState == Player.STATE_READY ||
                         player.playbackState == Player.STATE_BUFFERING) &&
                        player.currentMediaItem?.mediaId?.let {
                            try { Json.decodeFromString<Song>(it).id == song.id } catch (e: Exception) { false }
                        } == true

                if (alreadyPreloaded) {
                    Log.e("AndroidMediaPlayerEngine", "play: Using pre-buffered track for ${song.title} — instant play!")
                    isPreloaded = false
                    player.play()
                } else {
                    val songJson = Json.encodeToString(song)
                    val finalArtworkBytes = artworkBytes ?: getDefaultArtworkBytes()
                    val metadataBuilder = MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setDisplayTitle(song.title)
                        .setAlbumTitle("Convene")
                        .setArtworkUri(Uri.parse(song.thumbnailUrl))
                        .setFolderType(MediaMetadata.FOLDER_TYPE_NONE)
                        .setIsPlayable(true)
                    
                    if (finalArtworkBytes != null) {
                        metadataBuilder.setArtworkData(finalArtworkBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                    }
                    
                    val mediaMetadata = metadataBuilder.build()

                    val uri = if (streamUrl.startsWith("http://") || streamUrl.startsWith("https://")) {
                        Uri.parse(streamUrl)
                    } else {
                        Uri.fromFile(java.io.File(streamUrl))
                    }

                    val mediaItem = MediaItem.Builder()
                        .setMediaId(songJson)
                        .setUri(uri)
                        .setMediaMetadata(mediaMetadata)
                        .build()

                    if (player.isPlaying || player.playbackState == Player.STATE_READY ||
                        player.playbackState == Player.STATE_BUFFERING) {
                        player.stop()
                    }
                    if (player.mediaItemCount > 0) {
                        player.clearMediaItems()
                    }
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.play()

                    Log.e("AndroidMediaPlayerEngine", "play: Player instructed to play ${song.title}")
                }
            }
        }

        if (getActivePlayer() != null) {
            action()
        } else {
            pendingPlay = action
        }
    }

    fun pause() {
        getActivePlayer()?.pause()
    }

    fun resume() {
        val serviceIntent = android.content.Intent(context, PlaybackService::class.java)
        try {
            context.startService(serviceIntent)
        } catch (e: Exception) {
            Log.e("AndroidMediaPlayerEngine", "Failed to start PlaybackService in resume", e)
        }

        getActivePlayer()?.let { player ->
            Log.d("AndroidMediaPlayerEngine", "resume: state=${player.playbackState}, error=${player.playerError?.message}")
            if (player.playbackState == Player.STATE_IDLE) {
                player.prepare()
            } else if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(0L)
            }
            player.play()
        }
    }

    fun isIdle(): Boolean {
        return getActivePlayer()?.currentMediaItem == null
    }

    fun seekTo(positionMs: Long) {
        getActivePlayer()?.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun release() {
        coroutineScope.cancel()
        controllerFuture?.let { future ->
            MediaController.releaseFuture(future)
        }
        controllerFuture = null
        mediaController = null
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = coroutineScope.launch {
            while (isActive) {
                getActivePlayer()?.let { player ->
                    _currentPosition.value = player.currentPosition
                }
                delay(250)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }
}
