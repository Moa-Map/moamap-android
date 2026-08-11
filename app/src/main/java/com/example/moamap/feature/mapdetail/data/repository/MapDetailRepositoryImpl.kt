package com.example.moamap.feature.mapdetail.data.repository

import android.util.Log
import com.example.moamap.feature.collection.data.remote.MapService
import com.example.moamap.feature.explore.data.remote.PlaceService
import com.example.moamap.feature.mapdetail.domain.model.MapDetail
import com.example.moamap.feature.mapdetail.domain.model.MapPlace
import com.example.moamap.feature.mapdetail.domain.model.MapPlacePreview
import com.example.moamap.feature.mapdetail.domain.repository.MapDetailRepository
import com.example.moamap.feature.mypage.data.remote.UserService
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MapDetailRepository"

@Singleton
class MapDetailRepositoryImpl @Inject constructor(
    private val mapService: MapService,
    private val placeService: PlaceService,
    private val userService: UserService,
) : MapDetailRepository {

    override suspend fun getMapDetail(mapId: Long): MapDetail {
        val dto = mapService.getMap(mapId)
        return dto.toMapDetail(ownerName = fetchOwnerName(dto.ownerId))
    }

    /**
     * 보여줄 개수보다 한 건 더 받아, 그 한 건의 유무로 `더보기` 를 띄울지 정한다.
     *
     * 총 개수(`placeCount`)로 판단하지 않는 이유는 승인 대기 중인 장소가 목록에서 빠져
     * 두 값이 어긋날 수 있기 때문이다.
     */
    override suspend fun getPlacePreview(mapId: Long, visibleCount: Int): MapPlacePreview {
        val places = placeService
            .getPlaces(mapId = mapId, size = visibleCount + 1)
            .content
            .map { dto -> dto.toMapPlace() }

        return MapPlacePreview(
            places = places.take(visibleCount),
            hasMore = places.size > visibleCount,
        )
    }

    override suspend fun getPlaces(mapId: Long): List<MapPlace> = collectAllPages { page ->
        placeService.getPlaces(mapId = mapId, page = page, size = PLACE_PAGE_SIZE)
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

    /**
     * 제작자 닉네임.
     *
     * 곁들이는 정보라 실패를 삼킨다. 이름 한 줄 때문에 지도를 못 여는 게 더 나쁘다.
     */
    private suspend fun fetchOwnerName(ownerId: Long): String? {
        if (ownerId <= 0) return null

        return try {
            userService.getProfiles(listOf(ownerId))
                .firstOrNull { profile -> profile.id == ownerId }
                ?.nickname
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 사용자 식별자는 남기지 않는다. 로그가 수집·보관되는 경로를 타기 때문이다.
            Log.w(TAG, "제작자 프로필 조회 실패", e)
            null
        }
    }
}
