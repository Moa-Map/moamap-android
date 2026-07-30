package com.example.moamap.feature.explore.di

import com.example.moamap.feature.explore.data.remote.CommunityMapService
import com.example.moamap.feature.explore.data.remote.PlaceService
import com.example.moamap.feature.explore.data.remote.ReviewService
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

        /** 장소 가져오기(`feature/collection`)가 인스타그램 추출 API 를 호출할 때 쓴다. */
        @Provides
        @Singleton
        fun providePlaceService(retrofit: Retrofit): PlaceService =
            retrofit.create(PlaceService::class.java)

        /** 지도 상세(`feature/mapdetail`)의 장소 상세 시트가 후기를 읽고 쓸 때 쓴다. */
        @Provides
        @Singleton
        fun provideReviewService(retrofit: Retrofit): ReviewService =
            retrofit.create(ReviewService::class.java)
    }
}
