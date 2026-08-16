package com.example.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@Entity(
    tableName = "playlist_tracks",
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index(value = ["playlistId"])
    ]
)
data class PlaylistTrack(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playlistId: Int,
    val trackId: Long,
    val trackTitle: String,
    val trackArtist: String,
    val trackAlbum: String,
    val trackDuration: Long,
    val trackUri: String,
    val albumId: Long
)
