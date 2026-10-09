package com.moamap.app.feature.explore.data.repository

import com.moamap.app.feature.collection.domain.model.MapType
import com.moamap.app.feature.collection.domain.repository.MapRepository
import com.moamap.app.feature.explore.data.remote.CommunityMapService
import com.moamap.app.feature.explore.domain.model.CommunityMapPage
import com.moamap.app.feature.explore.domain.model.CommunityMapSort
import com.moamap.app.feature.explore.domain.repository.CommunityMapRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class CommunityMapRepositoryImpl @Inject constructor(
    private val service: CommunityMapService,
    private val myMaps: MapRepository,
) : CommunityMapRepository {

    override suspend fun getCommunityMaps(
        tag: String?,
        sort: CommunityMapSort,
        page: Int,
        size: Int,
    ): CommunityMapPage = service
        .getCommunityMaps(tag = tag, sort = sort.name, page = page, size = size)
        .let { response ->
            CommunityMapPage(maps = response.content.map { it.toDomain() }, isLast = response.last)
        }

    /**
     * 로그인 없이 받은 전체 목록에, 내가 참여한 커뮤니티 지도를 참여로 표시한다. 둘을 함께 받는다.
     *
     * 참여 여부가 틀리면 참여한 지도를 눌렀을 때 소개 화면으로 간다 - 내 지도를 못 받으면 목록도 실패로 본다.
     */
    // ponytail: 페이지마다 내 지도 목록을 다시 받는다(요청 하나). 느려지면 첫 페이지 때 받은 것을 이어 쓴다.
    override suspend fun getAllCommunityMaps(
        tag: String?,
        sort: CommunityMapSort,
        page: Int,
        size: Int,
    ): CommunityMapPage = coroutineScope {
        val joinedIds = async { myMaps.getMyMaps(MapType.Community).mapTo(HashSet()) { map -> map.id } }
        val response = service.getAllCommunityMaps(tag = tag, sort = sort.name, page = page, size = size)
        val joined = joinedIds.await()
        CommunityMapPage(
            maps = response.content.map { dto -> dto.toDomain().copy(joined = dto.id in joined) },
            isLast = response.last,
        )
    }

    override suspend fun searchMaps(keyword: String, page: Int, size: Int): CommunityMapPage = service
        .searchMaps(keyword = keyword, page = page, size = size)
        .let { response ->
            CommunityMapPage(maps = response.content.map { it.toDomain() }, isLast = response.last)
        }
}
