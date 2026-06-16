package com.example.convenemusic.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.convenemusic.network.Playlist
import com.example.convenemusic.ui.components.MusicTab

sealed class Destination {
    object Home : Destination()
    object SearchList : Destination()
    data class PlaylistDetail(val playlist: Playlist) : Destination()
    object Player : Destination()
    object Library : Destination()
    data class VideoPlayer(val movie: Movie) : Destination()
}

object NavigationController {
    val history = mutableStateListOf<Destination>(Destination.Home)
    var currentTab by mutableStateOf(MusicTab.Home)
    var activePlaylist by mutableStateOf<Playlist?>(null)

    val currentDestination: Destination
        get() = history.lastOrNull() ?: Destination.Home

    val isPlayerOpen: Boolean
        get() = history.lastOrNull() is Destination.Player

    val isVideoPlayerOpen: Boolean
        get() = history.lastOrNull() is Destination.VideoPlayer

    fun navigateTo(dest: Destination) {
        // Pop up to the destination if it already exists in the history (excluding player/video screen which can repeat)
        if (dest is Destination.Home || dest is Destination.SearchList || dest is Destination.Library) {
            val index = history.indexOfFirst { it::class == dest::class }
            if (index >= 0) {
                // Remove everything after this index
                while (history.size > index + 1) {
                    history.removeAt(history.lastIndex)
                }
                syncTab(dest)
                return
            }
        }

        if (history.isEmpty() || history.last() != dest) {
            history.add(dest)
        }
        syncTab(dest)
    }

    fun goBack(): Boolean {
        if (history.size > 1) {
            history.removeAt(history.lastIndex)
            val currentDest = history.lastOrNull() ?: Destination.Home
            syncTab(currentDest)
            return true
        }
        return false
    }

    private fun syncTab(dest: Destination) {
        when (dest) {
            is Destination.Home -> {
                currentTab = MusicTab.Home
                activePlaylist = null
            }
            is Destination.SearchList -> {
                currentTab = MusicTab.Search
                activePlaylist = null
            }
            is Destination.PlaylistDetail -> {
                currentTab = MusicTab.Search
                activePlaylist = dest.playlist
            }
            is Destination.Library -> {
                currentTab = MusicTab.Library
            }
            else -> {
                // Do not change selected bottom bar tab when opening player overlays
            }
        }
    }
}
