package com.example.viewmodel

import androidx.compose.runtime.Immutable
import com.example.domain.AudioItem
import com.example.data.room.Playlist

@Immutable
data class UIState(
    val audioList: List<AudioItem> = emptyList(),
    val currentTrack: AudioItem? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0,
    val totalDuration: Long = 0,
    val isReady: Boolean = false,
    val shuffleModeEnabled: Boolean = false,
    val repeatMode: Int = 0,
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = true,
    val showSplash: Boolean = true
)
