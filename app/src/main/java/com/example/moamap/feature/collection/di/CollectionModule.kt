package com.example.moamap.feature.collection.di

import com.example.moamap.feature.collection.data.repository.PlaceImportRepositoryImpl
import com.example.moamap.feature.collection.domain.repository.PlaceImportRepository
import com.example.moamap.feature.collection.instagram.CaptionExtractor
import com.example.moamap.feature.collection.instagram.InstagramCaptionExtractor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CollectionModule {

    @Binds
    @Singleton
    abstract fun bindPlaceImportRepository(
        impl: PlaceImportRepositoryImpl,
    ): PlaceImportRepository

    companion object {
        @Provides
        @Singleton
        fun provideCaptionExtractor(): CaptionExtractor = InstagramCaptionExtractor()
    }
}
