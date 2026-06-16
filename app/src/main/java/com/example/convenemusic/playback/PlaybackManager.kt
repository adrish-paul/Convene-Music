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
class PlaybackManager(private val context: Context) {
    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var pendingPlay: (() -> Unit)? = null
    
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private fun saveSongToPrefs(song: Song) {
        try {
            val prefs = context.getSharedPreferences("convenemusic_prefs", Context.MODE_PRIVATE)
            val songJson = Json.encodeToString(song)
            prefs.edit().putString("last_played_song", songJson).apply()
        } catch (e: Exception) {
            Log.e("PlaybackManager", "Failed to save song to prefs", e)
        }
    }

    private fun updateCurrentSong(song: Song?) {
        _currentSong.value = song
        if (song != null) {
            saveSongToPrefs(song)
        }
    }

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    var onSongEnded: (() -> Unit)? = null

    init {
        try {
            val prefs = context.getSharedPreferences("convenemusic_prefs", Context.MODE_PRIVATE)
            val savedSongJson = prefs.getString("last_played_song", null)
            if (savedSongJson != null) {
                val song = Json.decodeFromString<Song>(savedSongJson)
                _currentSong.value = song
            }
        } catch (e: Exception) {
            Log.e("PlaybackManager", "Failed to load saved song", e)
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
                    Log.e("PlaybackManager", "MediaController successfully connected")
                } catch (e: Exception) {
                    Log.e("PlaybackManager", "Failed to connect MediaController", e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }

    private fun setupController(controller: MediaController) {
        updateStateFromController(controller)

        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                Log.e("PlaybackManager", "onIsPlayingChanged: playing=$playing")
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
                Log.e("PlaybackManager", "onPlaybackStateChanged: state=$stateString")

                if (state == Player.STATE_READY) {
                    _duration.value = controller.duration
                } else if (state == Player.STATE_ENDED) {
                    _isPlaying.value = false
                    stopProgressTracker()
                    onSongEnded?.invoke()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (mediaItem != null) {
                    try {
                        val song = Json.decodeFromString<Song>(mediaItem.mediaId)
                        updateCurrentSong(song)
                    } catch (e: Exception) {
                        Log.e("PlaybackManager", "Failed to decode song from mediaId: ${mediaItem.mediaId}", e)
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
        
        mediaController?.let { controller ->
            val songJson = Json.encodeToString(song)
            val mediaMetadata = MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .setArtworkUri(Uri.parse(song.thumbnailUrl))
                .build()

            val mediaItem = MediaItem.Builder()
                .setMediaId(songJson)
                .setUri(Uri.EMPTY)
                .setMediaMetadata(mediaMetadata)
                .build()

            controller.stop()
            controller.setMediaItem(mediaItem)
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
                    val stream = java.io.ByteArrayOutputStream()
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                    stream.toByteArray()
                } else null
            } else null
        } catch (e: Exception) {
            Log.e("PlaybackManager", "Failed to load artwork bytes for $url: ${e.message}", e)
            null
        }
    }

    fun play(song: Song, streamUrl: String, artworkBytes: ByteArray? = null) {
        Log.e("PlaybackManager", "play: Loading song '${song.title}' with URL: $streamUrl")
        updateCurrentSong(song)
        _currentPosition.value = 0L
        _duration.value = 0L

        val action: () -> Unit = {
            mediaController?.let { controller ->
                val songJson = Json.encodeToString(song)
                val metadataBuilder = MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setArtworkUri(Uri.parse(song.thumbnailUrl))
                
                if (artworkBytes != null) {
                    metadataBuilder.setArtworkData(artworkBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
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

                controller.setMediaItem(mediaItem)
                controller.prepare()
                controller.play()
            }
        }

        if (mediaController != null) {
            action()
        } else {
            pendingPlay = action
        }
    }

    fun pause() {
        mediaController?.pause()
    }

    fun resume() {
        mediaController?.let { controller ->
            if (controller.playbackState == Player.STATE_ENDED) {
                controller.seekTo(0L)
            }
            controller.play()
        }
    }

    fun isIdle(): Boolean {
        return mediaController?.currentMediaItem == null
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
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
                mediaController?.let { controller ->
                    _currentPosition.value = controller.currentPosition
                }
                delay(250) // Refresh 4 times per second
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }
}
