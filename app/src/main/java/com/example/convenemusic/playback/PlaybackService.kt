package com.example.convenemusic.playback

import android.content.Intent
import android.media.audiofx.BassBoost
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
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
import androidx.media3.session.DefaultMediaNotificationProvider


@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null
    private var bassBoost: BassBoost? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var equalizer: android.media.audiofx.Equalizer? = null
    private var virtualizer: Virtualizer? = null
    private var currentAudioSessionId: Int = 0
    private var lastAppliedAudioSessionId: Int = 0

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

        player.setWakeMode(C.WAKE_MODE_NETWORK)

        exoPlayer = player

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                Log.e("PlaybackService", "onAudioSessionIdChanged: id=$audioSessionId")
                setupAudioEffects(audioSessionId)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.e("PlaybackService", "onPlaybackStateChanged: state=$playbackState")
                if (playbackState == Player.STATE_ENDED) {
                    Log.e("PlaybackService", "Playback ended naturally. Triggering next song.")
                    PlaybackServiceConnector.onNext?.invoke(true)
                }
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
                PlaybackServiceConnector.onNext?.invoke(false)
            }

            override fun seekToNextMediaItem() {
                PlaybackServiceConnector.onNext?.invoke(false)
            }

            override fun seekToPrevious() {
                PlaybackServiceConnector.onPrevious?.invoke()
            }

            override fun seekToPreviousMediaItem() {
                PlaybackServiceConnector.onPrevious?.invoke()
            }
        }

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setNotificationId(1001)
                .setChannelId("playback_channel")
                .setChannelName(com.example.convenemusic.R.string.app_name)
                .build()
        )

        val intent = packageManager.getLaunchIntentForPackage(packageName)?.let { launchIntent ->
            android.app.PendingIntent.getActivity(
                this,
                0,
                launchIntent,
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        val builder = MediaSession.Builder(this, forwardingPlayer)
        intent?.let { builder.setSessionActivity(it) }
        mediaSession = builder.build()
        PlaybackServiceConnector.player = forwardingPlayer

        PlaybackServiceConnector.onEqualizerChanged = {
            applyEqualizerSettings()
        }
    }

    private fun setupAudioEffects(audioSessionId: Int) {
        if (audioSessionId == 0) return
        currentAudioSessionId = audioSessionId
        applyEqualizerSettings()
    }

    private fun applyEqualizerSettings() {
        val audioSessionId = exoPlayer?.audioSessionId ?: currentAudioSessionId
        if (audioSessionId == 0 || audioSessionId == androidx.media3.common.C.AUDIO_SESSION_ID_UNSET) return
        try {
            if (audioSessionId != lastAppliedAudioSessionId) {
                Log.d("PlaybackService", "applyEqualizerSettings: Session changed from $lastAppliedAudioSessionId to $audioSessionId. Recreating effects.")
                bassBoost?.release()
                bassBoost = null
                loudnessEnhancer?.release()
                loudnessEnhancer = null
                equalizer?.release()
                equalizer = null
                virtualizer?.release()
                virtualizer = null
                lastAppliedAudioSessionId = audioSessionId
            }

            val prefs = getSharedPreferences("convenemusic_prefs", android.content.Context.MODE_PRIVATE)
            val mode = prefs.getString("eq_mode", "Default") ?: "Default"
            Log.d("PlaybackService", "applyEqualizerSettings: Mode = $mode, Session = $audioSessionId")

            if (mode == "Default") {
                // Disable/release equalizer
                equalizer?.enabled = false
                equalizer?.release()
                equalizer = null

                // Read on/off toggles from preferences
                val bassEnabled = prefs.getBoolean("eq_default_bass_enabled", true)
                val audioBoostEnabled = prefs.getBoolean("eq_default_audio_boost_enabled", true)
                val surroundEnabled = prefs.getBoolean("eq_default_surround_enabled", false)

                // BassBoost
                if (bassEnabled) {
                    if (bassBoost == null) {
                        bassBoost = BassBoost(0, audioSessionId)
                    }
                    bassBoost?.apply {
                        if (strengthSupported) {
                            setStrength(1000.toShort())
                            enabled = true
                            Log.d("PlaybackService", "Default BassBoost enabled with strength 1000")
                        }
                    }
                } else {
                    bassBoost?.enabled = false
                    bassBoost?.release()
                    bassBoost = null
                    Log.d("PlaybackService", "Default BassBoost disabled")
                }

                // LoudnessEnhancer (Audio Boost)
                if (audioBoostEnabled) {
                    if (loudnessEnhancer == null) {
                        loudnessEnhancer = LoudnessEnhancer(audioSessionId)
                    }
                    loudnessEnhancer?.apply {
                        setTargetGain(300)
                        enabled = true
                        Log.d("PlaybackService", "Default LoudnessEnhancer enabled with target gain 300")
                    }
                } else {
                    loudnessEnhancer?.enabled = false
                    loudnessEnhancer?.release()
                    loudnessEnhancer = null
                    Log.d("PlaybackService", "Default LoudnessEnhancer disabled")
                }

                // Virtualizer (Surround Sound)
                if (surroundEnabled) {
                    if (virtualizer == null) {
                        virtualizer = Virtualizer(0, audioSessionId)
                    }
                    virtualizer?.apply {
                        if (strengthSupported) {
                            setStrength(1000.toShort())
                            enabled = true
                            Log.d("PlaybackService", "Default Virtualizer enabled with strength 1000")
                        } else {
                            enabled = true
                        }
                    }
                } else {
                    virtualizer?.enabled = false
                    virtualizer?.release()
                    virtualizer = null
                    Log.d("PlaybackService", "Default Virtualizer disabled")
                }
            } else {
                // Enable and configure Equalizer
                if (equalizer == null) {
                    equalizer = android.media.audiofx.Equalizer(0, audioSessionId)
                }
                equalizer?.apply {
                    val numBands = numberOfBands.toInt()
                    val range = bandLevelRange
                    val minLevel = range[0]
                    val maxLevel = range[1]

                    for (i in 0 until 5) {
                        if (i < numBands) {
                            val dbVal = prefs.getFloat("eq_band_$i", 0f)
                            var mbVal = (dbVal * 100).toInt()
                            mbVal = mbVal.coerceIn(minLevel.toInt(), maxLevel.toInt())
                            setBandLevel(i.toShort(), mbVal.toShort())
                        }
                    }
                    enabled = true
                    Log.d("PlaybackService", "Custom Equalizer configured and enabled")
                }

                // Configure Custom BassBoost
                val bassStrength = prefs.getFloat("eq_bass_strength", 0f).toInt().toShort()
                if (bassStrength > 0) {
                    if (bassBoost == null) {
                        bassBoost = BassBoost(0, audioSessionId)
                    }
                    bassBoost?.apply {
                        if (strengthSupported) {
                            setStrength(bassStrength)
                            enabled = true
                            Log.d("PlaybackService", "Custom BassBoost set to strength $bassStrength")
                        } else {
                            enabled = true
                        }
                    }
                } else {
                    bassBoost?.enabled = false
                    bassBoost?.release()
                    bassBoost = null
                }

                // Configure Custom Audio Boost (LoudnessEnhancer)
                val audioBoostStrength = prefs.getFloat("eq_audio_boost_strength", 300f).toInt()
                if (audioBoostStrength > 0) {
                    if (loudnessEnhancer == null) {
                        loudnessEnhancer = LoudnessEnhancer(audioSessionId)
                    }
                    loudnessEnhancer?.apply {
                        setTargetGain(audioBoostStrength)
                        enabled = true
                        Log.d("PlaybackService", "Custom LoudnessEnhancer set to target gain $audioBoostStrength")
                    }
                } else {
                    loudnessEnhancer?.enabled = false
                    loudnessEnhancer?.release()
                    loudnessEnhancer = null
                }

                // Configure Custom Virtualizer (Surround Sound)
                val surroundStrength = prefs.getFloat("eq_surround_strength", 0f).toInt().toShort()
                if (surroundStrength > 0) {
                    if (virtualizer == null) {
                        virtualizer = Virtualizer(0, audioSessionId)
                    }
                    virtualizer?.apply {
                        if (strengthSupported) {
                            setStrength(surroundStrength)
                            enabled = true
                            Log.d("PlaybackService", "Custom Virtualizer set to strength $surroundStrength")
                        } else {
                            enabled = true
                        }
                    }
                } else {
                    virtualizer?.enabled = false
                    virtualizer?.release()
                    virtualizer = null
                }
            }
        } catch (e: Exception) {
            Log.e("PlaybackService", "Error applying equalizer settings", e)
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
        PlaybackServiceConnector.onEqualizerChanged = null
        bassBoost?.release()
        bassBoost = null
        loudnessEnhancer?.release()
        loudnessEnhancer = null
        equalizer?.release()
        equalizer = null
        virtualizer?.release()
        virtualizer = null
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        PlaybackServiceConnector.player = null
        exoPlayer = null
        super.onDestroy()
    }
}
