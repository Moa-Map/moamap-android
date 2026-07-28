package com.example.moamap.feature.collection.data.repository

import com.example.moamap.feature.collection.data.remote.MapService
import com.example.moamap.feature.collection.domain.model.NewMap
import com.example.moamap.feature.collection.domain.repository.MapRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapRepositoryImpl @Inject constructor(
    private val mapService: MapService,
) : MapRepository {

    override suspend fun createMap(newMap: NewMap): Long =
        mapService.createMap(newMap.toCreateRequest()).id
}
