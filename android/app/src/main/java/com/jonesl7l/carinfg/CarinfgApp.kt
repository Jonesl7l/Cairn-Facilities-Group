package com.jonesl7l.carinfg

import android.app.Application
import com.jonesl7l.carinfg.core.data.reference.ReferenceDataInitializer
import com.jonesl7l.carinfg.core.di.ApplicationScope
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltAndroidApp
class CarinfgApp : Application() {
    @Inject
    lateinit var referenceDataInitializer: ReferenceDataInitializer

    @Inject
    @field:ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        applicationScope.launch { referenceDataInitializer.initialize() }
    }
}
