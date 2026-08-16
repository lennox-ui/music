package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.AudioItem
import com.example.ui.components.TrackListItem
import com.example.viewmodel.UIState
import com.example.viewmodel.MainViewModel

enum class SortType { NONE, NAME, DATE }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SearchScreen(uiState: UIState, onTrackSelected: (AudioItem) -> Unit, viewModel: MainViewModel) {
    var searchQuery by remember { mutableStateOf("") }
    var sortType by remember { mutableStateOf(SortType.NONE) }
    
    var filteredAndSortedList by remember { mutableStateOf<List<AudioItem>>(emptyList()) }

    LaunchedEffect(uiState.audioList, searchQuery, sortType) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val list = uiState.audioList.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true) ||
                it.album.contains(searchQuery, ignoreCase = true)
            }
            val sortedList = when (sortType) {
                SortType.NAME -> list.sortedBy { it.title }
                SortType.DATE -> list.sortedBy { it.id } // Using ID as a proxy for date added
                SortType.NONE -> list
            }
            filteredAndSortedList = sortedList
        }
    }
    
    var showAddToPlaylistDialog by remember { mutableStateOf<List<AudioItem>?>(null) }
    var selectedTracks by remember { mutableStateOf(setOf<AudioItem>()) }
    val isSelectionMode = selectedTracks.isNotEmpty()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)) {
            if (isSelectionMode) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp, top = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { selectedTracks = emptySet() }) {
                            Icon(Icons.Default.Close, "Cancel Selection", tint = MaterialTheme.colorScheme.onBackground)
                        }
                        Text("${selectedTracks.size} Selected", color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Row {
                        TextButton(onClick = { 
                            if (selectedTracks.size == filteredAndSortedList.size) {
                                selectedTracks = emptySet()
                            } else {
                                selectedTracks = filteredAndSortedList.toSet() 
                            }
                        }) {
                            Text("Select All", color = MaterialTheme.colorScheme.primary)
                        }
                        TextButton(onClick = { showAddToPlaylistDialog = selectedTracks.toList() }) {
                            Text("Add", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            } else {
                Text(
                    text = "Search",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp, top = 32.dp)
                )
            }
            
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                placeholder = { Text("What do you want to listen to?", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp)
            )

            // Sorting buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = sortType == SortType.NAME,
                    onClick = { sortType = if(sortType == SortType.NAME) SortType.NONE else SortType.NAME },
                    label = { Text("Sort by Name") }
                )
                FilterChip(
                    selected = sortType == SortType.DATE,
                    onClick = { sortType = if(sortType == SortType.DATE) SortType.NONE else SortType.DATE },
                    label = { Text("Sort by Date") }
                )
            }

            LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
                items(filteredAndSortedList, key = { it.id }, contentType = { "track" }) { track ->
                    val isSelected = selectedTracks.contains(track)
                    TrackListItem(
                        track = track, 
                        isPlaying = uiState.currentTrack?.id == track.id,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onClick = { 
                            if (isSelectionMode) {
                                if (isSelected) selectedTracks -= track else selectedTracks += track
                            } else {
                                onTrackSelected(track)
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                selectedTracks += track
                            }
                        },
                        onMoreClick = { showAddToPlaylistDialog = listOf(track) }
                    )
                }
            }
        }
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
                        items(uiState.playlists, key = { it.id }) { playlist ->
                            Text(
                                text = playlist.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addTracksToPlaylist(playlist.id, showAddToPlaylistDialog!!)
                                        showAddToPlaylistDialog = null
                                        scope.launch { snackbarHostState.showSnackbar("Tracks added to ${playlist.name}!") }
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
