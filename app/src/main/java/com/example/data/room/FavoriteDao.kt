package com.example.data.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite_tracks ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteTrack>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(track: FavoriteTrack)

    @Query("DELETE FROM favorite_tracks WHERE id = :id")
    suspend fun deleteFavoriteById(id: Long)
    
    @Query("SELECT EXISTS(SELECT 1 FROM favorite_tracks WHERE id = :id)")
    fun isFavorite(id: Long): Flow<Boolean>
}
