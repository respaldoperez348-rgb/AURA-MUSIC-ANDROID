package com.auramusic.app

import android.app.Application
import android.content.ComponentCallbacks2
import android.os.Build
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.allowHardware
import coil3.request.crossfade
import com.auramusic.app.data.repository.MusicRepository
import com.auramusic.app.data.repository.UserManager
import com.auramusic.app.playback.PlayerManager
import com.auramusic.app.utils.InnerTubeXPlayer
import com.metrolist.innertube.YouTube
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class AuraApplication : Application(), SingletonImageLoader.Factory {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        lateinit var instance: AuraApplication
            private set

        val repository: MusicRepository by lazy {
            MusicRepository(instance)
        }

        val userManager: UserManager by lazy {
            UserManager(instance)
        }

        val playerManager: PlayerManager by lazy {
            PlayerManager(instance, repository)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        InnerTubeXPlayer.initialize(this)

        // Diferir el pre-calentamiento de red para no competir con el primer fotograma ni con las portadas de Inicio
        appScope.launch {
            delay(1500L)
            runCatching {
                if (YouTube.visitorData.isNullOrBlank()) {
                    YouTube.visitorData().getOrNull()?.let { YouTube.visitorData = it }
                }
            }
            delay(2000L)
            runCatching { InnerTubeXPlayer.prewarm() }
        }
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        val cache = SingletonImageLoader.get(this).memoryCache ?: return
        when {
            level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> cache.clear()
            level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> cache.trimToSize(cache.size / 4)
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> cache.trimToSize(cache.size / 2)
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(this)
            .crossfade(220)
            .allowHardware(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_covers"))
                    .maxSizeBytes(256L * 1024L * 1024L)
                    .build()
            }
            .build()
    }
}
