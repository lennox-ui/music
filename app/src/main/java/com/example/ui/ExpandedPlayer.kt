package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.getValue
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.example.domain.AudioItem
import com.example.viewmodel.MainViewModel

@Composable
fun ExpandedPlayer(
    track: AudioItem,
    isPlaying: Boolean,
    currentPosition: Long,
    totalDuration: Long,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    uiState: com.example.viewmodel.UIState,
    viewModel: MainViewModel,
    onClose: () -> Unit
) {
    var showAddToPlaylistDialog by remember { mutableStateOf<List<AudioItem>?>(null) }
    val favoriteTracksFlow by viewModel.favoriteTracks.collectAsState()
    val isFavorite = favoriteTracksFlow.any { it.id == track.id }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .padding(16.dp)
            .systemBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Close", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                text = track.album,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            // Empty space to balance the back button
            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Artwork
        com.example.ui.components.TrackThumbnail(
            uri = track.albumArtUri,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.DarkGray),
            audioContentUri = track.uri // Triggers MedaMetadataRetriever ID3 APIC extraction
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Track Info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = { viewModel.toggleFavorite(track) }) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) com.example.ui.theme.SpotifyGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { showAddToPlaylistDialog = listOf(track) }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onBackground)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Slider
        var isDragging by remember { mutableStateOf(false) }
        var localSliderValue by remember { mutableStateOf(0f) }
        val justSeeked = remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        
        LaunchedEffect(currentPosition, totalDuration) {
            if (!isDragging && !justSeeked.value && totalDuration > 0) {
                // Ensure value is strictly within 0.0..1.0 range
                localSliderValue = (currentPosition.toFloat() / totalDuration).coerceIn(0f, 1f)
            }
        }
        
        Slider(
            value = localSliderValue,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp).height(24.dp), // Height 24dp for minimal look
            onValueChange = { 
                isDragging = true
                localSliderValue = it 
            },
            onValueChangeFinished = {
                isDragging = false
                justSeeked.value = true
                scope.launch {
                    viewModel.seekTo((localSliderValue * totalDuration).toLong())
                    delay(1000)
                    justSeeked.value = false
                }
            },
            colors = SliderDefaults.colors(
                thumbColor = com.example.ui.theme.SpotifyGreen,
                activeTrackColor = com.example.ui.theme.SpotifyGreen,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        
        // Time Info
        val displayPosition = if (isDragging) (localSliderValue * totalDuration).toLong() else currentPosition
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatTime(displayPosition), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Text(formatTime(totalDuration), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.toggleShuffle() }) {
                Icon(
                    Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (shuffleEnabled) com.example.ui.theme.SpotifyGreen else MaterialTheme.colorScheme.onBackground
                )
            }
            IconButton(onClick = { viewModel.skipPrevious() }) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(40.dp))
            }
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(com.example.ui.theme.SpotifyGreen)
                    .clickable { viewModel.playPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(40.dp)
                )
            }
            IconButton(onClick = { viewModel.skipNext() }) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(40.dp))
            }
            IconButton(onClick = { viewModel.toggleRepeat() }) {
                Icon(
                    if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    contentDescription = "Repeat",
                    tint = if (repeatMode != Player.REPEAT_MODE_OFF) com.example.ui.theme.SpotifyGreen else MaterialTheme.colorScheme.onBackground
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showAddToPlaylistDialog != null) {
        AlertDialog(
            onDismissRequest = { showAddToPlaylistDialog = null },
            title = { Text("Add to Playlist", color = MaterialTheme.colorScheme.onBackground) },
            text = {
                if (uiState.playlists.isEmpty()) {
                    Text("No playlists available. Go to Library to create one.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn {
                        items(uiState.playlists) { playlist ->
                            Text(
                                text = playlist.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addTracksToPlaylist(playlist.id, showAddToPlaylistDialog!!)
                                        showAddToPlaylistDialog = null
                                    }
                                    .padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddToPlaylistDialog = null }) {
                    Text("Close", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
