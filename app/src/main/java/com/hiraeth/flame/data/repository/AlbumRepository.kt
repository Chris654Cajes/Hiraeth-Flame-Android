package com.hiraeth.flame.data.repository

import com.hiraeth.flame.data.db.AlbumDao
import com.hiraeth.flame.data.db.AlbumEntity
import com.hiraeth.flame.data.db.AlbumMediaCrossRef
import com.hiraeth.flame.data.db.AlbumWithMedia
import kotlinx.coroutines.flow.Flow

class AlbumRepository(
    private val albumDao: AlbumDao,
) {
    fun observeAlbums(): Flow<List<AlbumWithMedia>> = albumDao.observeAlbumsWithMedia()

    suspend fun createAlbum(name: String, category: String = "General"): Long {
        val album = AlbumEntity(name = name.trim(), category = category.trim().ifBlank { "General" })
        return albumDao.insertAlbum(album)
    }

    suspend fun addToAlbum(albumId: Long, mediaId: Long) {
        albumDao.linkMedia(AlbumMediaCrossRef(albumId = albumId, mediaId = mediaId))
    }

    suspend fun removeFromAlbum(albumId: Long, mediaId: Long) {
        albumDao.unlinkMedia(albumId, mediaId)
    }

    suspend fun deleteAlbum(albumId: Long) {
        albumDao.deleteAlbum(albumId)
    }
}
