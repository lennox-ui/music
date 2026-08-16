package com.example.domain

import android.net.Uri

data class AudioItem(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uri: Uri,
    val albumId: Long,
    val albumArtUri: Uri
)
