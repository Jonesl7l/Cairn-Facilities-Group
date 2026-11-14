package com.jonesl7l.carinfg.core.di

import com.jonesl7l.carinfg.BuildConfig
import com.jonesl7l.carinfg.core.network.api.AuthApi
import com.jonesl7l.carinfg.core.network.datasource.AuthRemoteDataSource
import com.jonesl7l.carinfg.core.network.datasource.AuthRemoteDataSourceImpl
import com.jonesl7l.carinfg.core.network.http.MediaTypes
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.json.Json
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

    companion object {
        private val connectTimeout = 15.seconds
        private val readTimeout = 30.seconds

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
    }
}
