package com.example.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.AudioItem
import com.example.ui.components.TrackListItem
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.UIState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    playlistId: Int,
    uiState: UIState,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val playlist = uiState.playlists.find { it.id == playlistId }
    val audioItems by viewModel.getPlaylistTracks(playlistId).collectAsState(initial = emptyList())
    var selectedTracks by remember { mutableStateOf(setOf<Long>()) }
    val isSelectionMode = selectedTracks.isNotEmpty()

    var isEditingName by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(playlist?.name ?: "") }
    var isShuffled by remember { mutableStateOf(false) }
    var showMenuForTrack by remember { mutableStateOf<AudioItem?>(null) }
    var trackToRemove by remember { mutableStateOf<AudioItem?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp)
                .statusBarsPadding()
        ) {
            if (isEditingName) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = newName,
                        onValueChange = { newName = it },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        playlist?.let { viewModel.renamePlaylist(it, newName) }
                        isEditingName = false
                    }) {
                        Icon(androidx.compose.material.icons.Icons.Default.Check, contentDescription = "Save", tint = Color.White)
                    }
                }
            } else if (isSelectionMode) {
                Row(
                   modifier = Modifier.fillMaxWidth(),
                   horizontalArrangement = Arrangement.SpaceBetween,
                   verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { selectedTracks = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${selectedTracks.size} Selected", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val allTrackIds = audioItems.map { it.id }.toSet()
                        val isAllSelected = selectedTracks.size == allTrackIds.size && allTrackIds.isNotEmpty()
                        
                        TextButton(
                            onClick = {
                                selectedTracks = if (isAllSelected) emptySet() else allTrackIds
                            }
                        ) {
                            Text(
                                text = if (isAllSelected) "Deselect All" else "Select All",
                                color = com.example.ui.theme.SpotifyGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = playlist?.name ?: "Playlist",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { isEditingName = true }) {
                        Icon(androidx.compose.material.icons.Icons.Default.Edit, contentDescription = "Edit Playlist", tint = Color.White)
                    }
                }
            }

            // Actions Row
            Row(modifier = Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                FloatingActionButton(
                    onClick = { 
                        viewModel.playPlaylist(playlistId, audioItems, shuffle = isShuffled) 
                    },
                    containerColor = com.example.ui.theme.SpotifyGreen,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(androidx.compose.material.icons.Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.Black)
                }
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(onClick = { isShuffled = !isShuffled }) {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.Shuffle, 
                        contentDescription = "Shuffle", 
                        tint = if (isShuffled) com.example.ui.theme.SpotifyGreen else Color.White
                    )
                }
            }
        }

        LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
            items(audioItems, key = { it.id }, contentType = { "track" }) { audioItem ->
                val isSelected = selectedTracks.contains(audioItem.id)
                
                Box {
                    TrackListItem(
                        track = audioItem,
                        isPlaying = uiState.currentTrack?.id == audioItem.id,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onClick = { 
                            if (isSelectionMode) {
                                if (isSelected) selectedTracks -= audioItem.id else selectedTracks += audioItem.id
                            } else {
                                val startIndex = audioItems.indexOfFirst { it.id == audioItem.id }
                                viewModel.playPlaylist(playlistId, audioItems, startIndex = startIndex)
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                selectedTracks += audioItem.id
                            }
                        },
                        onMoreClick = {
                            showMenuForTrack = audioItem
                        }
                    )
                    
                    DropdownMenu(
                        expanded = showMenuForTrack?.id == audioItem.id,
                        onDismissRequest = { showMenuForTrack = null }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Remove from Playlist") },
                            onClick = {
                                trackToRemove = audioItem
                                showMenuForTrack = null
                            }
                        )
                    }
                }
            }
        }
        
        if (trackToRemove != null) {
            AlertDialog(
                onDismissRequest = { trackToRemove = null },
                title = { Text("Remove from playlist?") },
                text = { Text("Are you sure you want to remove ${trackToRemove?.title} from this playlist?") },
                confirmButton = {
                    TextButton(onClick = {
                        trackToRemove?.let {
                            viewModel.removeTrackFromPlaylist(playlistId, it.id)
                        }
                        trackToRemove = null
                    }) {
                        Text("Remove")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { trackToRemove = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Delete selected songs?") },
                text = { Text("Are you sure you want to remove the ${selectedTracks.size} selected songs from this playlist?") },
                confirmButton = {
                    TextButton(onClick = {
                        selectedTracks.forEach { trackId -> viewModel.removeTrackFromPlaylist(playlistId, trackId) }
                        selectedTracks = emptySet()
                        showDeleteConfirmation = false
                    }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Cancel")
                    }
                }
            )
        }


    }
}
