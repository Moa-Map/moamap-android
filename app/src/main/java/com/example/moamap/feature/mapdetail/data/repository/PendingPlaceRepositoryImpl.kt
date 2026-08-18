package com.example.moamap.feature.mapdetail.data.repository

import com.example.moamap.feature.explore.data.remote.PlaceService
import com.example.moamap.feature.mapdetail.domain.model.PendingPlace
import com.example.moamap.feature.mapdetail.domain.repository.PendingPlaceRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 한 번에 받아 오는 승인 대기 수.
 *
 * 장소 목록([PLACE_PAGE_SIZE])보다 작게 잡는다. 처리되면 목록에서 빠지는 값이라 수백 건씩
 * 쌓이는 자리가 아니고, 대개 첫 페이지에서 끝난다.
 */
private const val PENDING_PAGE_SIZE = 100

@Singleton
class PendingPlaceRepositoryImpl @Inject constructor(
    private val placeService: PlaceService,
) : PendingPlaceRepository {

    override suspend fun getPendingPlaces(mapId: Long): List<PendingPlace> =
        collectAllPages { page ->
            placeService.getPendingPlaces(mapId = mapId, page = page, size = PENDING_PAGE_SIZE)
        }.map { dto -> dto.toPendingPlace() }

    override suspend fun approve(placeId: Long) {
        placeService.approvePlace(placeId)
    }

    override suspend fun reject(placeId: Long) {
        placeService.rejectPlace(placeId)
    }
}
