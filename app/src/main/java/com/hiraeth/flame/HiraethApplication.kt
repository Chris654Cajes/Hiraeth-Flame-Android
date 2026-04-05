package com.hiraeth.flame

import android.app.Application
import com.hiraeth.flame.di.AppContainer

/**
 * Application entry: holds a single [AppContainer] for manual dependency wiring (MVVM without Hilt).
 */
class HiraethApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
