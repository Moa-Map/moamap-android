package com.moamap.app.feature.mapdetail.data.repository

import com.moamap.app.feature.collection.data.remote.MapService
import com.moamap.app.feature.explore.data.remote.PlaceService
import com.moamap.app.feature.mapdetail.domain.model.MapDetail
import com.moamap.app.feature.mapdetail.domain.model.MapPlace
import com.moamap.app.feature.mapdetail.domain.model.PlaceLike
import com.moamap.app.feature.mapdetail.domain.repository.MapDetailRepository
import com.moamap.app.feature.mypage.data.remote.UserService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapDetailRepositoryImpl @Inject constructor(
    private val mapService: MapService,
    private val placeService: PlaceService,
    private val userService: UserService,
) : MapDetailRepository {

    override suspend fun getMapDetail(mapId: Long): MapDetail {
        val dto = mapService.getMap(mapId)
        // 곁들이는 정보라 실패해도 지도는 연다. 이름을 못 받으면 null 이고 화면이 그 줄을 숨긴다.
        val ownerName = userService.fetchAuthorProfiles(listOf(dto.ownerId))[dto.ownerId]?.nickname
        return dto.toMapDetail(ownerName = ownerName)
    }

    /**
     * 최신순(새로 등록한 장소가 맨 위)이다. 화면이 이 순서 그대로 보여 준다.
     *
     * 정렬을 안 보내면 서버가 순서를 정하지 않아, 같은 지도도 열 때마다 순서가 바뀔 수 있고 여러 페이지로 나눠 받을 때
     * 장소가 겹치거나 빠질 수 있다. 장소 번호는 등록할 때 차례로 매겨지고 겹치지 않아 등록 시각보다 순서가 단단하다.
     */
    override suspend fun getPlaces(mapId: Long): List<MapPlace> = collectAllPages { page ->
        placeService.getPlaces(mapId = mapId, page = page, size = PLACE_PAGE_SIZE, sort = "id,desc")
    }.map { dto -> dto.toMapPlace() }

    override suspend fun joinMap(mapId: Long) {
        mapService.joinMap(mapId)
    }

    override suspend fun leaveMap(mapId: Long) {
        mapService.leaveMap(mapId)
    }

    override suspend fun deleteMap(mapId: Long) {
        mapService.deleteMap(mapId)
    }

    override suspend fun setPlaceLiked(placeId: Long, liked: Boolean): PlaceLike {
        val dto = if (liked) placeService.likePlace(placeId) else placeService.unlikePlace(placeId)
        return PlaceLike(liked = dto.liked, likeCount = dto.likeCount)
    }
}
