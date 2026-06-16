package com.example.convenemusic.playback

object PlaybackServiceConnector {
    var onNext: (() -> Unit)? = null
    var onPrevious: (() -> Unit)? = null
}
