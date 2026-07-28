package com.example.moamap.feature.collection.data.repository

import com.example.moamap.feature.collection.data.remote.MapService
import com.example.moamap.feature.collection.domain.model.MapType
import com.example.moamap.feature.collection.domain.model.MyMap
import com.example.moamap.feature.collection.domain.model.NewMap
import com.example.moamap.feature.collection.domain.repository.MapRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapRepositoryImpl @Inject constructor(
    private val mapService: MapService,
) : MapRepository {

    override suspend fun getMyMaps(type: MapType): List<MyMap> = mapService
        .getMyMaps(type = type.requestValue, size = PAGE_SIZE)
        .content
        .map { dto -> dto.toMyMap() }

    override suspend fun createMap(newMap: NewMap): Long =
        mapService.createMap(newMap.toCreateRequest()).id

    private companion object {
        /** 탐색 탭과 같은 값. 무한 스크롤을 붙이기 전까지는 첫 페이지만 쓴다. */
        const val PAGE_SIZE = 20
    }
}
