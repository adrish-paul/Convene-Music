package com.example.convenemusic.playback

object PlaybackServiceConnector {
    var onNext: ((isAuto: Boolean) -> Unit)? = null
    var onPrevious: (() -> Unit)? = null
    var player: androidx.media3.common.Player? = null
    var onEqualizerChanged: (() -> Unit)? = null
}
