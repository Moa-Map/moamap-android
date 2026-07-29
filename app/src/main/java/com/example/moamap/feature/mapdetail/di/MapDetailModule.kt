package com.example.moamap.feature.mapdetail.di

import com.example.moamap.feature.mapdetail.data.repository.MapDetailRepositoryImpl
import com.example.moamap.feature.mapdetail.domain.repository.MapDetailRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 지도 상세는 새로 만드는 Retrofit 서비스가 없다. 지도(`MapService`)·장소(`PlaceService`)·
 * 사용자(`UserService`) 를 각 feature 모듈이 이미 제공하고 있어 그대로 가져다 쓴다.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class MapDetailModule {

    @Binds
    @Singleton
    abstract fun bindMapDetailRepository(impl: MapDetailRepositoryImpl): MapDetailRepository
}
