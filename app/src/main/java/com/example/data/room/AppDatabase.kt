package com.example.data.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Playlist::class, PlaylistTrack::class, FavoriteTrack::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao
}
