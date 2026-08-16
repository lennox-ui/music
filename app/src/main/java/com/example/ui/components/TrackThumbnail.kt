package com.example.ui.components

import android.media.MediaMetadataRetriever
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun TrackThumbnail(
    uri: Any?, // Standard albumArt Uri
    modifier: Modifier = Modifier,
    audioContentUri: android.net.Uri? = null // Passed if we want to directly extract ID3 APIC
) {
    val context = LocalContext.current
    var cachedArtUri by remember(audioContentUri) { mutableStateOf<Any?>(com.example.domain.AlbumArtCache.getCachedArt(audioContentUri)) }
    var hasCheckedCache by remember(audioContentUri) { mutableStateOf(com.example.domain.AlbumArtCache.hasChecked(audioContentUri)) }

    LaunchedEffect(audioContentUri) {
        if (audioContentUri != null && !hasCheckedCache) {
            val fileUri = com.example.domain.AlbumArtCache.getAlbumArt(context, audioContentUri)
            cachedArtUri = fileUri
            hasCheckedCache = true
        }
    }

    val finalData = cachedArtUri ?: uri
    val finalDataString = finalData?.toString() ?: ""
    val isKnownToFail = remember(finalDataString) { com.example.domain.AlbumArtCache.isKnownToFail(finalDataString) }

    if (finalData == null || isKnownToFail) {
        DefaultMusicIcon(modifier = modifier)
    } else {
        Box(modifier = modifier) {
            var isError by remember(finalData) { mutableStateOf(false) }
            
            if (!isError) {
                DefaultMusicIcon(modifier = Modifier.fillMaxSize())
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(finalData)
                        .build(),
                    contentDescription = "Album Art",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onState = { state ->
                        if (state is AsyncImagePainter.State.Error) {
                            if (cachedArtUri != null || finalData != uri) {
                                isError = true
                                com.example.domain.AlbumArtCache.markAsFailed(finalDataString)
                            }
                            if (finalData == uri) {
                                isError = true
                                com.example.domain.AlbumArtCache.markAsFailed(finalDataString)
                            }
                        } else if (state is AsyncImagePainter.State.Success && finalData != null) {
                             isError = false
                        }
                    }
                )
            } else {
                DefaultMusicIcon(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun DefaultMusicIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxSize(0.5f)
        )
    }
}

