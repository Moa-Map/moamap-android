package com.example.moamap.feature.explore.di

import com.example.moamap.feature.explore.data.remote.PlaceService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ExploreModule {

    @Provides
    @Singleton
    fun providePlaceService(retrofit: Retrofit): PlaceService =
        retrofit.create(PlaceService::class.java)
}
