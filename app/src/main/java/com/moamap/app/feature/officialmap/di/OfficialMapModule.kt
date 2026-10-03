package com.moamap.app.feature.officialmap.di

import com.moamap.app.feature.officialmap.data.remote.FootTrafficService
import com.moamap.app.feature.officialmap.data.remote.OfficialMapService
import com.moamap.app.feature.officialmap.data.remote.RestroomService
import com.moamap.app.feature.officialmap.data.repository.FootTrafficRepositoryImpl
import com.moamap.app.feature.officialmap.data.repository.OfficialMapRepositoryImpl
import com.moamap.app.feature.officialmap.data.repository.RestroomRepositoryImpl
import com.moamap.app.feature.officialmap.domain.repository.FootTrafficRepository
import com.moamap.app.feature.officialmap.domain.repository.OfficialMapRepository
import com.moamap.app.feature.officialmap.domain.repository.RestroomRepository
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

    @Binds
    @Singleton
    abstract fun bindOfficialMapRepository(impl: OfficialMapRepositoryImpl): OfficialMapRepository

    @Binds
    @Singleton
    abstract fun bindRestroomRepository(impl: RestroomRepositoryImpl): RestroomRepository

    companion object {
        @Provides
        @Singleton
        fun provideFootTrafficService(retrofit: Retrofit): FootTrafficService =
            retrofit.create(FootTrafficService::class.java)

        @Provides
        @Singleton
        fun provideOfficialMapService(retrofit: Retrofit): OfficialMapService =
            retrofit.create(OfficialMapService::class.java)

        @Provides
        @Singleton
        fun provideRestroomService(retrofit: Retrofit): RestroomService =
            retrofit.create(RestroomService::class.java)
    }
}
