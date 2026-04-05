package com.hiraeth.flame.di

import android.content.Context
import com.hiraeth.flame.data.db.AppDatabase
import com.hiraeth.flame.data.local.MediaStorage
import com.hiraeth.flame.data.repository.AlbumRepository
import com.hiraeth.flame.data.repository.MediaRepository

/**
 * Simple service locator for repositories and storage paths.
 * Keeps ViewModels testable by accepting interfaces in constructors where needed.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.get(appContext)
    val mediaStorage = MediaStorage(appContext)
    val mediaRepository = MediaRepository(
        dao = database.mediaDao(),
        storage = mediaStorage,
    )
    val albumRepository = AlbumRepository(
        albumDao = database.albumDao(),
    )
}
