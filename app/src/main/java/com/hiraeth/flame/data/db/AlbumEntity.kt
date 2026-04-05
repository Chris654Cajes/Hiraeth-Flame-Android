package com.hiraeth.flame.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** User-defined grouping (e.g. Travel, Work). */
    val category: String = "General",
    val createdAtEpochMs: Long = System.currentTimeMillis(),
)
