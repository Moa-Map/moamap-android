package com.moamap.app.feature.explore.data.repository

import com.moamap.app.feature.explore.data.remote.CommunityMapService
import com.moamap.app.feature.explore.domain.model.CommunityMap
import com.moamap.app.feature.explore.domain.model.CommunityMapPage
import com.moamap.app.feature.explore.domain.model.CommunityMapSort
import com.moamap.app.feature.explore.domain.repository.CommunityMapRepository
import javax.inject.Inject

class CommunityMapRepositoryImpl @Inject constructor(
    private val service: CommunityMapService,
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

    override suspend fun getRecommendedMaps(): List<CommunityMap> = service
        .getRecommendedMaps(size = RECOMMENDATION_SIZE)
        .map { it.toDomain() }

    private companion object {
        /** 가로 스크롤 한 줄에 담는 수. 서버 기본값과 같다(상한 20). */
        const val RECOMMENDATION_SIZE = 5
    }
}
