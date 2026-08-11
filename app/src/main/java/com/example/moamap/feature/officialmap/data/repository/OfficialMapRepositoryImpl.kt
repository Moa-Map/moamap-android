package com.example.moamap.feature.officialmap.data.repository

import com.example.moamap.feature.collection.data.remote.MapService
import com.example.moamap.feature.officialmap.data.remote.OfficialMapService
import com.example.moamap.feature.officialmap.domain.model.OfficialMap
import com.example.moamap.feature.officialmap.domain.repository.OfficialMapRepository
import javax.inject.Inject

/**
 * @param mapService 참여만 맡는다. 공식지도 목록에는 참여 API 가 따로 없고 커뮤니티 지도와
 *  같은 것을 쓴다.
 */
class OfficialMapRepositoryImpl @Inject constructor(
    private val service: OfficialMapService,
    private val mapService: MapService,
) : OfficialMapRepository {

    override suspend fun getOfficialMaps(): List<OfficialMap> = service
        .getOfficialMaps(size = PAGE_SIZE)
        .content
        .map { it.toOfficialMap() }

    override suspend fun joinMap(mapId: Long) {
        mapService.joinMap(mapId)
    }

    private companion object {
        /** 서버 기본값과 같다. 무한 스크롤을 붙이기 전까지는 첫 페이지만 쓴다. */
        const val PAGE_SIZE = 20
    }
}
