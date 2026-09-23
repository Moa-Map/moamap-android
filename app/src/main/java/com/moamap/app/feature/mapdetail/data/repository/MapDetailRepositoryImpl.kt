package com.moamap.app.feature.mapdetail.data.repository

import android.util.Log
import com.moamap.app.feature.collection.data.remote.MapService
import com.moamap.app.feature.explore.data.remote.PlaceService
import com.moamap.app.feature.mapdetail.domain.model.MapDetail
import com.moamap.app.feature.mapdetail.domain.model.MapPlace
import com.moamap.app.feature.mapdetail.domain.repository.MapDetailRepository
import com.moamap.app.feature.mypage.data.remote.UserService
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
