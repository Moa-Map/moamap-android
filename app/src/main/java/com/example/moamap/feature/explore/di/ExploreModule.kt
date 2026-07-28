package com.example.moamap.feature.explore.di

import com.example.moamap.feature.explore.data.remote.CommunityMapService
import com.example.moamap.feature.explore.data.repository.CommunityMapRepositoryImpl
import com.example.moamap.feature.explore.domain.repository.CommunityMapRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ExploreModule {

    @Binds
    @Singleton
    abstract fun bindCommunityMapRepository(impl: CommunityMapRepositoryImpl): CommunityMapRepository

    companion object {
        @Provides
        @Singleton
        fun provideCommunityMapService(retrofit: Retrofit): CommunityMapService =
            retrofit.create(CommunityMapService::class.java)
    }
}
