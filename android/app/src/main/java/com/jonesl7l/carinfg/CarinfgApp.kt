package com.jonesl7l.carinfg

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.jonesl7l.carinfg.core.data.photo.PhotoStorage
import com.jonesl7l.carinfg.core.data.reference.ReferenceDataInitializer
import com.jonesl7l.carinfg.core.di.ApplicationScope
import com.jonesl7l.carinfg.core.sync.SseSyncTrigger
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltAndroidApp
class CarinfgApp :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var referenceDataInitializer: ReferenceDataInitializer

    @Inject
    lateinit var photoStorage: PhotoStorage

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var sseSyncTrigger: SseSyncTrigger

    @Inject
    @field:ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        applicationScope.launch { referenceDataInitializer.initialize() }
        applicationScope.launch { photoStorage.sweep() }
        ProcessLifecycleOwner.get().lifecycle.addObserver(sseSyncTrigger)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
}
