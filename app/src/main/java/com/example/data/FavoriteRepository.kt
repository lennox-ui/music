package com.example.data

import com.example.data.room.FavoriteDao
import com.example.data.room.FavoriteTrack
import kotlinx.coroutines.flow.Flow

class FavoriteRepository(private val favoriteDao: FavoriteDao) {
    val allFavorites: Flow<List<FavoriteTrack>> = favoriteDao.getAllFavorites()

    suspend fun addFavorite(track: FavoriteTrack) {
        favoriteDao.insertFavorite(track)
    }

    suspend fun removeFavorite(trackId: Long) {
        favoriteDao.deleteFavoriteById(trackId)
    }
    
    fun isFavorite(trackId: Long): Flow<Boolean> {
        return favoriteDao.isFavorite(trackId)
    }
}
