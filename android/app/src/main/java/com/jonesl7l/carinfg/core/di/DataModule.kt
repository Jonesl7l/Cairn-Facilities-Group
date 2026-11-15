package com.jonesl7l.carinfg.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.jonesl7l.carinfg.core.data.incident.IncidentRepository
import com.jonesl7l.carinfg.core.data.incident.IncidentRepositoryImpl
import com.jonesl7l.carinfg.core.data.preferences.DataStoreUserPreferencesStore
import com.jonesl7l.carinfg.core.data.preferences.UserPreferencesStore
import com.jonesl7l.carinfg.core.data.session.DataStoreSessionStore
import com.jonesl7l.carinfg.core.data.session.SessionStore
import com.jonesl7l.carinfg.core.sync.SyncScheduler
import com.jonesl7l.carinfg.core.sync.WorkManagerSyncScheduler
import com.jonesl7l.carinfg.core.sync.store.RoomSyncLocalStore
import com.jonesl7l.carinfg.core.sync.store.SyncLocalStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    abstract fun bindSessionStore(impl: DataStoreSessionStore): SessionStore

    @Binds
    abstract fun bindUserPreferencesStore(impl: DataStoreUserPreferencesStore): UserPreferencesStore

    @Binds
    abstract fun bindIncidentRepository(impl: IncidentRepositoryImpl): IncidentRepository

    @Binds
    abstract fun bindSyncLocalStore(impl: RoomSyncLocalStore): SyncLocalStore

    @Binds
    abstract fun bindSyncScheduler(impl: WorkManagerSyncScheduler): SyncScheduler

    companion object {
        private const val PREFERENCES_FILE_NAME = "carinfg"

        @Provides
        @Singleton
        fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create(produceFile = { context.preferencesDataStoreFile(PREFERENCES_FILE_NAME) })
    }
}
