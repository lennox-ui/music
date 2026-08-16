package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.domain.AudioItem
import com.example.viewmodel.UIState
import kotlin.random.Random
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import com.example.viewmodel.MainViewModel
import android.net.Uri

@Composable
fun HomeScreen(uiState: UIState, onTrackSelected: (AudioItem) -> Unit, viewModel: MainViewModel) {
    val favorites by viewModel.favoriteTracks.collectAsState()
    
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            androidx.compose.material3.CircularProgressIndicator(color = com.example.ui.theme.SpotifyGreen)
        }
        return
    }

    if (uiState.audioList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "LennoxWAV",
                    color = com.example.ui.theme.SpotifyGreen,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                Text("No music found on device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    val shuffled = remember(uiState.audioList) {
        uiState.audioList.shuffled(Random(42))
    }
    val recentlyPlayed = remember(shuffled) {
        shuffled.take(6)
    }
    val recentlyAdded = remember(uiState.audioList) {
        uiState.audioList.take(8)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            com.example.ui.theme.GradientTop,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 48.dp, bottom = 120.dp)
        ) {
            item {
                Text(
                    "LennoxWAV",
                    color = com.example.ui.theme.SpotifyGreen,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
                )
            }
        
            // Recently Played Mock Grid
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    for (i in recentlyPlayed.indices step 2) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RecentGridCard(track = recentlyPlayed[i], isPlaying = recentlyPlayed[i].id == uiState.currentTrack?.id, onTrackSelected = onTrackSelected, modifier = Modifier.weight(1f))
                            if (i + 1 < recentlyPlayed.size) {
                                RecentGridCard(track = recentlyPlayed[i+1], isPlaying = recentlyPlayed[i+1].id == uiState.currentTrack?.id, onTrackSelected = onTrackSelected, modifier = Modifier.weight(1f))
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Your Favorite Tracks Array
            item {
                if (favorites.isNotEmpty()) {
                    SectionRow("Your Favorite Tracks", favorites, uiState.currentTrack?.id, onTrackSelected)
                }
            }

            // Recently Added Mock Row
            item {
                SectionRow("Recently Added", recentlyAdded, uiState.currentTrack?.id, onTrackSelected)
            }
        }
    }
}

@Composable
fun RecentGridCard(track: AudioItem, isPlaying: Boolean, onTrackSelected: (AudioItem) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(com.example.ui.theme.SurfaceVariantDark)
            .clickable { onTrackSelected(track) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        com.example.ui.components.TrackThumbnail(
            uri = track.albumArtUri,
            modifier = Modifier
                .size(56.dp)
                .background(Color.DarkGray),
            audioContentUri = track.uri
        )
        Text(
            text = track.title,
            color = if (isPlaying) com.example.ui.theme.SpotifyGreen else MaterialTheme.colorScheme.onBackground,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
fun SectionRow(title: String, tracks: List<AudioItem>, currentTrackId: Long?, onTrackSelected: (AudioItem) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(tracks, key = { it.id }, contentType = { "track" }) { track ->
                TrackCard(track = track, isPlaying = track.id == currentTrackId, onClick = { onTrackSelected(track) })
            }
        }
    }
}

@Composable
fun TrackCard(track: AudioItem, isPlaying: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
    ) {
        com.example.ui.components.TrackThumbnail(
            uri = track.albumArtUri,
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(com.example.ui.theme.SpotifyLightGray),
            audioContentUri = track.uri
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = track.title,
            color = if (isPlaying) com.example.ui.theme.SpotifyGreen else MaterialTheme.colorScheme.onBackground,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist,
            color = com.example.ui.theme.SpotifyTextSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
