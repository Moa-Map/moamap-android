package com.moamap.app.feature.mapdetail.data.repository

import com.moamap.app.feature.explore.data.remote.PlaceService
import com.moamap.app.feature.mapdetail.domain.model.MapActivity
import com.moamap.app.feature.mapdetail.domain.repository.MapActivityRepository
import javax.inject.Inject
import javax.inject.Singleton

/** 한 번에 받아 오는 활동 내역 수. 서버 상한과 같은 값이다. */
private const val ACTIVITY_PAGE_SIZE = 100

@Singleton
class MapActivityRepositoryImpl @Inject constructor(
    private val placeService: PlaceService,
) : MapActivityRepository {

    override suspend fun getActivities(mapId: Long): List<MapActivity> =
        placeService.getActivities(
            mapId = mapId,
            page = 0,
            size = ACTIVITY_PAGE_SIZE,
        ).content.mapNotNull { dto -> dto.toMapActivity() }
}
