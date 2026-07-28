package com.example.moamap.feature.collection.di

import com.example.moamap.feature.collection.data.remote.MapService
import com.example.moamap.feature.collection.data.repository.MapRepositoryImpl
import com.example.moamap.feature.collection.data.repository.PlaceImportRepositoryImpl
import com.example.moamap.feature.collection.domain.repository.MapRepository
import com.example.moamap.feature.collection.domain.repository.PlaceImportRepository
import com.example.moamap.feature.collection.instagram.CaptionExtractor
import com.example.moamap.feature.collection.instagram.InstagramCaptionExtractor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CollectionModule {

    @Binds
    @Singleton
    abstract fun bindPlaceImportRepository(
        impl: PlaceImportRepositoryImpl,
    ): PlaceImportRepository

    @Binds
    @Singleton
    abstract fun bindMapRepository(impl: MapRepositoryImpl): MapRepository

    companion object {
        @Provides
        @Singleton
        fun provideCaptionExtractor(): CaptionExtractor = InstagramCaptionExtractor()

        @Provides
        @Singleton
        fun provideMapService(retrofit: Retrofit): MapService =
            retrofit.create(MapService::class.java)
    }
}
