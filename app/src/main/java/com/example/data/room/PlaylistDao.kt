package com.example.data.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY timestamp DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylistById(id: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackToPlaylist(track: PlaylistTrack)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracksToPlaylist(tracks: List<PlaylistTrack>)

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId")
    fun getTracksForPlaylist(playlistId: Int): Flow<List<PlaylistTrack>>
    
    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Int, trackId: Long)

    @Update
    suspend fun updatePlaylist(playlist: Playlist)
}
