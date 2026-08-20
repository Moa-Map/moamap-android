package com.moamap.app.feature.mapdetail.di

import com.moamap.app.core.network.di.KakaoLocalClient
import com.moamap.app.core.network.kakao.KakaoLocalService
import com.moamap.app.feature.mapdetail.data.repository.KakaoPlaceSearchRepository
import com.moamap.app.feature.mapdetail.data.repository.MapActivityRepositoryImpl
import com.moamap.app.feature.mapdetail.data.repository.MapDetailRepositoryImpl
import com.moamap.app.feature.mapdetail.data.repository.MapMemberRepositoryImpl
import com.moamap.app.feature.mapdetail.data.repository.PendingPlaceRepositoryImpl
import com.moamap.app.feature.mapdetail.data.repository.PlaceAddRepositoryImpl
import com.moamap.app.feature.mapdetail.data.repository.PlaceReviewRepositoryImpl
import com.moamap.app.feature.mapdetail.domain.repository.MapActivityRepository
import com.moamap.app.feature.mapdetail.domain.repository.MapDetailRepository
import com.moamap.app.feature.mapdetail.domain.repository.MapMemberRepository
import com.moamap.app.feature.mapdetail.domain.repository.PendingPlaceRepository
import com.moamap.app.feature.mapdetail.domain.repository.PlaceAddRepository
import com.moamap.app.feature.mapdetail.domain.repository.PlaceReviewRepository
import com.moamap.app.feature.mapdetail.domain.repository.PlaceSearchRepository
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

    @Binds
    @Singleton
    abstract fun bindMapMemberRepository(impl: MapMemberRepositoryImpl): MapMemberRepository

    @Binds
    @Singleton
    abstract fun bindPendingPlaceRepository(
        impl: PendingPlaceRepositoryImpl,
    ): PendingPlaceRepository

    companion object {
        @Provides
        @Singleton
        fun provideKakaoLocalService(@KakaoLocalClient retrofit: Retrofit): KakaoLocalService =
            retrofit.create(KakaoLocalService::class.java)
    }
}
