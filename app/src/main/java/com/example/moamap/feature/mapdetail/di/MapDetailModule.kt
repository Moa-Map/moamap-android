package com.example.moamap.feature.mapdetail.di

import com.example.moamap.core.network.di.KakaoLocalClient
import com.example.moamap.feature.mapdetail.data.remote.KakaoLocalService
import com.example.moamap.feature.mapdetail.data.repository.KakaoPlaceSearchRepository
import com.example.moamap.feature.mapdetail.data.repository.MapActivityRepositoryImpl
import com.example.moamap.feature.mapdetail.data.repository.MapDetailRepositoryImpl
import com.example.moamap.feature.mapdetail.data.repository.PlaceAddRepositoryImpl
import com.example.moamap.feature.mapdetail.data.repository.PlaceReviewRepositoryImpl
import com.example.moamap.feature.mapdetail.domain.repository.MapActivityRepository
import com.example.moamap.feature.mapdetail.domain.repository.MapDetailRepository
import com.example.moamap.feature.mapdetail.domain.repository.PlaceAddRepository
import com.example.moamap.feature.mapdetail.domain.repository.PlaceReviewRepository
import com.example.moamap.feature.mapdetail.domain.repository.PlaceSearchRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * 지도(`MapService`)·장소(`PlaceService`)·사용자(`UserService`) 는 각 feature 모듈이
 * 이미 제공하고 있어 그대로 가져다 쓴다. 새로 만드는 건 카카오 로컬 서비스뿐이다.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class MapDetailModule {

    @Binds
    @Singleton
    abstract fun bindMapDetailRepository(impl: MapDetailRepositoryImpl): MapDetailRepository

    /**
     * 지금은 카카오를 직접 부른다. 서버에 장소 검색 엔드포인트가 생기면 **이 바인딩 한 줄과
     * 구현체만** 바꾼다. 화면·ViewModel 은 그대로다.
     */
    @Binds
    @Singleton
    abstract fun bindPlaceSearchRepository(impl: KakaoPlaceSearchRepository): PlaceSearchRepository

    @Binds
    @Singleton
    abstract fun bindPlaceAddRepository(impl: PlaceAddRepositoryImpl): PlaceAddRepository

    @Binds
    @Singleton
    abstract fun bindPlaceReviewRepository(impl: PlaceReviewRepositoryImpl): PlaceReviewRepository

    @Binds
    @Singleton
    abstract fun bindMapActivityRepository(impl: MapActivityRepositoryImpl): MapActivityRepository

    companion object {
        @Provides
        @Singleton
        fun provideKakaoLocalService(@KakaoLocalClient retrofit: Retrofit): KakaoLocalService =
            retrofit.create(KakaoLocalService::class.java)
    }
}
