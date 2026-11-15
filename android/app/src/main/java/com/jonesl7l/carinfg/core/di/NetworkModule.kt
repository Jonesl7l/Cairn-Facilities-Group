package com.jonesl7l.carinfg.core.di

import com.jonesl7l.carinfg.BuildConfig
import com.jonesl7l.carinfg.core.network.api.AuthApi
import com.jonesl7l.carinfg.core.network.api.IncidentApi
import com.jonesl7l.carinfg.core.network.api.ReferenceApi
import com.jonesl7l.carinfg.core.network.datasource.AuthRemoteDataSource
import com.jonesl7l.carinfg.core.network.datasource.AuthRemoteDataSourceImpl
import com.jonesl7l.carinfg.core.network.datasource.IncidentRemoteDataSource
import com.jonesl7l.carinfg.core.network.datasource.IncidentRemoteDataSourceImpl
import com.jonesl7l.carinfg.core.network.datasource.ReferenceRemoteDataSource
import com.jonesl7l.carinfg.core.network.datasource.ReferenceRemoteDataSourceImpl
import com.jonesl7l.carinfg.core.network.events.OkHttpSyncEventSource
import com.jonesl7l.carinfg.core.network.events.SyncEventSource
import com.jonesl7l.carinfg.core.network.http.MediaTypes
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {
    @Binds
    abstract fun bindAuthRemoteDataSource(impl: AuthRemoteDataSourceImpl): AuthRemoteDataSource

    @Binds
    abstract fun bindIncidentRemoteDataSource(impl: IncidentRemoteDataSourceImpl): IncidentRemoteDataSource

    @Binds
    abstract fun bindReferenceRemoteDataSource(impl: ReferenceRemoteDataSourceImpl): ReferenceRemoteDataSource

    companion object {
        private val connectTimeout = 15.seconds
        private val readTimeout = 30.seconds

        // longer than the server's 20 second heartbeat
        private val eventStreamReadTimeout = 60.seconds
        private const val EVENTS_PATH = "events"

        @Provides
        @Singleton
        fun provideJson(): Json = Json { ignoreUnknownKeys = true }

        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(duration = connectTimeout)
            .readTimeout(duration = readTimeout)
            .build()

        @Provides
        @Singleton
        fun provideRetrofit(
            okHttpClient: OkHttpClient,
            json: Json,
        ): Retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.DEFAULT_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(MediaTypes.JSON.toMediaType()))
            .build()

        @Provides
        @Singleton
        fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create()

        @Provides
        @Singleton
        fun provideIncidentApi(retrofit: Retrofit): IncidentApi = retrofit.create()

        @Provides
        @Singleton
        fun provideReferenceApi(retrofit: Retrofit): ReferenceApi = retrofit.create()

        @Provides
        @Singleton
        fun provideSyncEventSource(okHttpClient: OkHttpClient): SyncEventSource = OkHttpSyncEventSource(
            client = okHttpClient.newBuilder().readTimeout(duration = eventStreamReadTimeout).build(),
            eventsUrl =
                BuildConfig.DEFAULT_BASE_URL.toHttpUrl().resolve(link = EVENTS_PATH) ?: error("invalid events url"),
        )
    }
}
