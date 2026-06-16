package com.example.convenemusic.playback

import android.content.Intent
import android.media.audiofx.BassBoost
import android.media.audiofx.LoudnessEnhancer
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null
    private var bassBoost: BassBoost? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    override fun onCreate() {
        super.onCreate()
        Log.e("PlaybackService", "onCreate: Creating playback service")

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true // Handles audio focus automatically
            )
            .setHandleAudioBecomingNoisy(true) // Pauses automatically on unplug / Bluetooth disconnect
            .build()

        exoPlayer = player

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                Log.e("PlaybackService", "onAudioSessionIdChanged: id=$audioSessionId")
                setupAudioEffects(audioSessionId)
            }
        })

        // Wrap ExoPlayer in ForwardingPlayer to expose next/previous capability and route clicks
        val forwardingPlayer = object : ForwardingPlayer(player) {
            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .build()
            }

            override fun hasNextMediaItem(): Boolean {
                return true
            }

            override fun hasPreviousMediaItem(): Boolean {
                return true
            }

            override fun seekToNext() {
                PlaybackServiceConnector.onNext?.invoke()
            }

            override fun seekToNextMediaItem() {
                PlaybackServiceConnector.onNext?.invoke()
            }

            override fun seekToPrevious() {
                PlaybackServiceConnector.onPrevious?.invoke()
            }

            override fun seekToPreviousMediaItem() {
                PlaybackServiceConnector.onPrevious?.invoke()
            }
        }

        mediaSession = MediaSession.Builder(this, forwardingPlayer).build()
    }

    private fun setupAudioEffects(audioSessionId: Int) {
        if (audioSessionId == 0) return
        try {
            bassBoost?.release()
            loudnessEnhancer?.release()

            bassBoost = BassBoost(0, audioSessionId).apply {
                if (strengthSupported) {
                    setStrength(1000.toShort())
                    enabled = true
                    Log.d("PlaybackService", "BassBoost enabled with strength 1000 on session $audioSessionId")
                }
            }

            loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                setTargetGain(300)
                enabled = true
                Log.d("PlaybackService", "LoudnessEnhancer enabled with target gain 300 on session $audioSessionId")
            }
        } catch (e: Exception) {
            Log.e("PlaybackService", "Error setting up audio effects", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // Ensure the service restarts if terminated by the system under memory pressure
        return START_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        Log.e("PlaybackService", "onDestroy: Releasing service resources")
        bassBoost?.release()
        bassBoost = null
        loudnessEnhancer?.release()
        loudnessEnhancer = null
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        exoPlayer = null
        super.onDestroy()
    }
}
