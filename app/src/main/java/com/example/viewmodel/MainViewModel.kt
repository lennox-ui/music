package com.example.viewmodel

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.C
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.room.Room
import com.example.data.LocalMediaRepository
import com.example.data.PlaylistRepository
import com.example.data.FavoriteRepository
import com.example.data.room.AppDatabase
import com.example.domain.AudioItem
import com.example.service.PlaybackService
import com.example.data.room.Playlist
import com.example.data.room.FavoriteTrack
import com.example.data.room.PlaylistTrack
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LocalMediaRepository(application)
    
    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "playlist_db"
    )
    .fallbackToDestructiveMigration()
    .build()
    
    private val playlistRepository = PlaylistRepository(db.playlistDao())
    private val favoriteRepository = FavoriteRepository(db.favoriteDao())

    private val _uiState = MutableStateFlow(UIState())
    val uiState: StateFlow<UIState> = _uiState.asStateFlow()
    
    // Expose favorites as AudioItems to avoid mapping on the UI thread
    val favoriteTracks: StateFlow<List<AudioItem>> = favoriteRepository.allFavorites
        .map { favorites ->
            favorites.map { it.toAudioItem() }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private var mediaControllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    init {
        initializeController()
        loadLocalSongs()
        startUpdatingProgress()
        
        viewModelScope.launch {
            delay(500L) // Wait minimum splash duration
            _uiState.update { it.copy(showSplash = false) }
        }
        
        viewModelScope.launch {
            playlistRepository.allPlaylists.collect { playlists ->
                _uiState.update { it.copy(playlists = playlists) }
            }
        }
    }

    private fun initializeController() {
        val sessionToken = SessionToken(
            getApplication(),
            ComponentName(getApplication(), PlaybackService::class.java)
        )
        mediaControllerFuture = MediaController.Builder(getApplication(), sessionToken).buildAsync()
        mediaControllerFuture?.addListener({
            try {
                mediaController = mediaControllerFuture?.get()
                mediaController?.addListener(playerListener)
                
                val currentMediaItem = mediaController?.currentMediaItem
                val trackId = currentMediaItem?.mediaId?.toLongOrNull()
                
                _uiState.update { currentState ->
                    val currentTrack = if (trackId != null) currentState.audioList.find { it.id == trackId } else null
                    currentState.copy(
                        isPlaying = mediaController?.isPlaying ?: false,
                        currentPosition = mediaController?.currentPosition ?: 0,
                        totalDuration = mediaController?.duration?.coerceAtLeast(0) ?: 0,
                        currentTrack = currentTrack,
                        isReady = true
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val trackId = mediaItem?.mediaId?.toLongOrNull()
            if (trackId != null) {
                val currentTrack = _uiState.value.audioList.find { it.id == trackId }
                _uiState.update { it.copy(currentTrack = currentTrack) }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val duration = mediaController?.duration ?: 0
            if (duration > 0) {
                _uiState.update { it.copy(totalDuration = duration) }
            }
        }
        
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _uiState.update { it.copy(shuffleModeEnabled = shuffleModeEnabled) }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _uiState.update { it.copy(repeatMode = repeatMode) }
        }
    }

    fun loadLocalSongs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val tracks = repository.getLocalAudioFiles()
            _uiState.update { currentState ->
                val currentMediaItem = mediaController?.currentMediaItem
                val trackId = currentMediaItem?.mediaId?.toLongOrNull()
                val currentTrack = if (trackId != null) tracks.find { it.id == trackId } else currentState.currentTrack
                
                currentState.copy(audioList = tracks, currentTrack = currentTrack, isLoading = false)
            }
        }
    }

    private fun startUpdatingProgress() {
        viewModelScope.launch {
            while (true) {
                if (_uiState.value.isPlaying) {
                    val currentPosition = mediaController?.currentPosition ?: 0
                    _uiState.update { it.copy(currentPosition = currentPosition) }
                }
                delay(1000L)
            }
        }
    }

    fun setAndPlay(track: AudioItem) {
        val controller = mediaController ?: return
        val list = _uiState.value.audioList
        
        viewModelScope.launch {
            val (mediaItems, startIndex) = withContext(Dispatchers.Default) {
                val items = list.map { audio ->
                    audio.toMediaItem()
                }
                
                val index = list.indexOfFirst { it.id == track.id }.takeIf { it >= 0 } ?: 0
                Pair(items, index)
            }
            
            controller.setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
            controller.prepare()
            controller.play()
        }
    }

    fun playPause() {
        mediaController?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
    }

    fun skipNext() {
        mediaController?.seekToNextMediaItem()
    }

    fun skipPrevious() {
        mediaController?.seekToPreviousMediaItem()
    }

    fun toggleShuffle() {
        mediaController?.let {
            it.shuffleModeEnabled = !it.shuffleModeEnabled
        }
    }

    fun toggleRepeat() {
        mediaController?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }
    
    fun getAlbumArtUri(albumId: Long): Uri {
        return Uri.parse("content://media/external/audio/albumart/$albumId")
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            playlistRepository.createPlaylist(name)
        }
    }

    fun deletePlaylist(playlistId: Int) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlistId)
        }
    }

    fun addTracksToPlaylist(playlistId: Int, tracks: List<AudioItem>) {
        viewModelScope.launch {
            val existingTracks = playlistRepository.getTracksForPlaylist(playlistId).first()
            val newTracks = tracks.filter { track -> 
                existingTracks.none { it.trackId == track.id }
            }
            if (newTracks.isNotEmpty()) {
                playlistRepository.addTracksToPlaylist(playlistId, newTracks)
            }
        }
    }

    fun getPlaylistTracks(playlistId: Int): Flow<List<AudioItem>> {
        return playlistRepository.getTracksForPlaylist(playlistId)
            .map { tracks ->
                tracks.map { it.toAudioItem() }
            }
            .flowOn(Dispatchers.Default)
    }

    fun removeTrackFromPlaylist(playlistId: Int, trackId: Long) {
        viewModelScope.launch {
            playlistRepository.playlistDao.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    fun renamePlaylist(playlist: Playlist, newName: String) {
        viewModelScope.launch {
            playlistRepository.renamePlaylist(playlist.copy(name = newName))
        }
    }

    fun playPlaylist(playlistId: Int, tracks: List<AudioItem>, shuffle: Boolean = false, startIndex: Int = 0) {
        val controller = mediaController ?: return
        
        viewModelScope.launch {
            val mediaItems = withContext(Dispatchers.Default) {
                val list = if (shuffle) tracks.shuffled() else tracks
                list.map { it.toMediaItem() }
            }
            
            controller.setMediaItems(mediaItems, if (shuffle) 0 else startIndex, C.TIME_UNSET)
            controller.prepare()
            controller.play()
        }
    }
    
    fun toggleFavorite(track: AudioItem) {
        viewModelScope.launch {
            val favorites = favoriteRepository.allFavorites.first()
            val existing = favorites.find { it.id == track.id }
            if (existing != null) {
                favoriteRepository.removeFavorite(track.id)
            } else {
                favoriteRepository.addFavorite(track.toFavoriteTrack())
            }
        }
    }

    private fun AudioItem.toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(getAlbumArtUri(albumId))
            .build()

        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(uri)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun FavoriteTrack.toAudioItem(): AudioItem {
        return AudioItem(
            id = id,
            title = title,
            artist = artist,
            album = album,
            duration = duration,
            uri = Uri.parse(uri),
            albumId = albumId,
            albumArtUri = getAlbumArtUri(albumId)
        )
    }
    
    private fun PlaylistTrack.toAudioItem(): AudioItem {
        return AudioItem(
            id = trackId,
            title = trackTitle,
            artist = trackArtist,
            album = trackAlbum,
            duration = trackDuration,
            uri = Uri.parse(trackUri),
            albumId = albumId,
            albumArtUri = getAlbumArtUri(albumId)
        )
    }
    
    private fun AudioItem.toFavoriteTrack(): FavoriteTrack {
        return FavoriteTrack(
            id = id,
            title = title,
            artist = artist,
            album = album,
            duration = duration,
            uri = uri.toString(),
            albumId = albumId
        )
    }

    override fun onCleared() {
        super.onCleared()
        mediaController?.removeListener(playerListener)
        mediaControllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}
