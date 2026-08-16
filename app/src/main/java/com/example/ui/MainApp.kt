package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.domain.AudioItem
import com.example.viewmodel.MainViewModel

@Composable
fun MainApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()
    var isPlayerExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Scaffold(
            bottomBar = {
                if (!isPlayerExpanded) {
                    Column {
                        if (uiState.currentTrack != null) {
                            MiniPlayer(
                                track = uiState.currentTrack!!,
                                isPlaying = uiState.isPlaying,
                                onPlayPauseClick = { viewModel.playPause() },
                                onPlayerClick = { isPlayerExpanded = true },
                                currentPosition = uiState.currentPosition,
                                totalDuration = uiState.totalDuration
                            )
                        }
                        BottomNavigationBar(navController = navController)
                    }
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(paddingValues)
            ) {
                composable("home") {
                    HomeScreen(uiState = uiState, onTrackSelected = { viewModel.setAndPlay(it) }, viewModel = viewModel)
                }
                composable("search") {
                    SearchScreen(uiState = uiState, onTrackSelected = { viewModel.setAndPlay(it) }, viewModel = viewModel)
                }
                composable("library") {
                    LibraryScreen(
                        uiState = uiState, 
                        onTrackSelected = { viewModel.setAndPlay(it) }, 
                        onPlaylistSelected = { playlist -> navController.navigate("playlist/${playlist.id}") },
                        viewModel = viewModel
                    )
                }
                composable("playlist/{playlistId}") { backStackEntry ->
                    val playlistId = backStackEntry.arguments?.getString("playlistId")?.toIntOrNull()
                    if (playlistId != null) {
                        PlaylistScreen(
                            playlistId = playlistId,
                            uiState = uiState,
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }

        if (isPlayerExpanded && uiState.currentTrack != null) {
            BackHandler(onBack = { isPlayerExpanded = false })
            ExpandedPlayer(
                track = uiState.currentTrack!!,
                isPlaying = uiState.isPlaying,
                currentPosition = uiState.currentPosition,
                totalDuration = uiState.totalDuration,
                shuffleEnabled = uiState.shuffleModeEnabled,
                repeatMode = uiState.repeatMode,
                uiState = uiState,
                viewModel = viewModel,
                onClose = { isPlayerExpanded = false }
            )
        }
    }
}

@Composable
fun MiniPlayer(
    track: AudioItem,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onPlayerClick: () -> Unit,
    currentPosition: Long,
    totalDuration: Long
) {
    val progress = if (totalDuration > 0) currentPosition.toFloat() / totalDuration else 0f
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .height(58.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(com.example.ui.theme.SpotifyLightGray)
            .clickable(onClick = onPlayerClick)
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.example.ui.components.TrackThumbnail(
                    uri = track.albumArtUri,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    audioContentUri = track.uri
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        color = com.example.ui.theme.SpotifyTextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                IconButton(onClick = onPlayPauseClick, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.3f),
                drawStopIndicator = {}
            )
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val screens = listOf("home", "search", "library")
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onSecondary,
        modifier = Modifier.height(64.dp).background(MaterialTheme.colorScheme.background),
        tonalElevation = 0.dp
    ) {
        screens.forEach { screen ->
            val isSelected = currentDestination == screen
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    navController.navigate(screen) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    when (screen) {
                        "home" -> Icon(Icons.Default.Home, contentDescription = "Home", modifier = Modifier.size(24.dp))
                        "search" -> Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(24.dp))
                        "library" -> Icon(Icons.Default.LibraryMusic, contentDescription = "Library", modifier = Modifier.size(24.dp))
                    }
                },
                label = {
                    Text(
                        text = screen.replaceFirstChar { it.uppercase() },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onBackground,
                    unselectedIconColor = com.example.ui.theme.SpotifyTextSecondary,
                    selectedTextColor = MaterialTheme.colorScheme.onBackground,
                    unselectedTextColor = com.example.ui.theme.SpotifyTextSecondary,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
