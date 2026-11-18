package com.auramusic.app

import android.app.Application
import com.auramusic.app.data.repository.MusicRepository
import com.auramusic.app.data.repository.UserManager
import com.auramusic.app.playback.PlayerManager
import com.auramusic.app.utils.InnerTubeXPlayer
import timber.log.Timber

class AuraApplication : Application() {

    companion object {
        lateinit var instance: AuraApplication
            private set

        lateinit var repository: MusicRepository
            private set

        lateinit var userManager: UserManager
            private set

        lateinit var playerManager: PlayerManager
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        Timber.plant(Timber.DebugTree())

        // Inicializar el extractor de streaming de YouTube Music
        InnerTubeXPlayer.initialize(this)

        userManager = com.auramusic.app.data.repository.UserManager(this)
        repository = MusicRepository(this)
        playerManager = PlayerManager(this, repository)
    }
}
