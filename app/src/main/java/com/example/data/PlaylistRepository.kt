package com.example.data

import com.example.data.room.Playlist
import com.example.data.room.PlaylistDao
import com.example.data.room.PlaylistTrack
import kotlinx.coroutines.flow.Flow

class PlaylistRepository(val playlistDao: PlaylistDao) {
    val allPlaylists: Flow<List<Playlist>> = playlistDao.getAllPlaylists()

    suspend fun createPlaylist(name: String): Long {
        return playlistDao.insertPlaylist(Playlist(name = name))
    }

    suspend fun deletePlaylist(id: Int) {
        playlistDao.deletePlaylistById(id)
    }

    suspend fun renamePlaylist(playlist: Playlist) {
        playlistDao.updatePlaylist(playlist)
    }

    suspend fun addTrackToPlaylist(playlistId: Int, track: com.example.domain.AudioItem) {
        playlistDao.insertTrackToPlaylist(
            PlaylistTrack(
                playlistId = playlistId,
                trackId = track.id,
                trackTitle = track.title,
                trackArtist = track.artist,
                trackAlbum = track.album,
                trackDuration = track.duration,
                trackUri = track.uri.toString(),
                albumId = track.albumId
            )
        )
    }

    suspend fun addTracksToPlaylist(playlistId: Int, tracks: List<com.example.domain.AudioItem>) {
        val playlistTracks = tracks.map { track ->
            PlaylistTrack(
                playlistId = playlistId,
                trackId = track.id,
                trackTitle = track.title,
                trackArtist = track.artist,
                trackAlbum = track.album,
                trackDuration = track.duration,
                trackUri = track.uri.toString(),
                albumId = track.albumId
            )
        }
        playlistDao.insertTracksToPlaylist(playlistTracks)
    }

    fun getTracksForPlaylist(playlistId: Int): Flow<List<PlaylistTrack>> {
        return playlistDao.getTracksForPlaylist(playlistId)
    }
}
