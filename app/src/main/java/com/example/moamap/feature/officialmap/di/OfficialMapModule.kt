package com.example.moamap.feature.officialmap.di

import com.example.moamap.feature.officialmap.data.remote.FootTrafficService
import com.example.moamap.feature.officialmap.data.repository.FootTrafficRepositoryImpl
import com.example.moamap.feature.officialmap.domain.repository.FootTrafficRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class OfficialMapModule {

    @Binds
    @Singleton
    abstract fun bindFootTrafficRepository(impl: FootTrafficRepositoryImpl): FootTrafficRepository

    companion object {
        @Provides
        @Singleton
        fun provideFootTrafficService(retrofit: Retrofit): FootTrafficService =
            retrofit.create(FootTrafficService::class.java)
    }
}
