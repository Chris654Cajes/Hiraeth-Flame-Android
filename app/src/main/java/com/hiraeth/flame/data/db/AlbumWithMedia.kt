package com.hiraeth.flame.data.db

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class AlbumWithMedia(
    @Embedded val album: AlbumEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = AlbumMediaCrossRef::class,
            parentColumn = "albumId",
            entityColumn = "mediaId",
        ),
    )
    val media: List<MediaEntity>,
)
