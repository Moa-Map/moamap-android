package com.moamap.app.feature.explore.data.repository

import com.moamap.app.feature.explore.data.remote.CommunityMapService
import com.moamap.app.feature.explore.domain.model.CommunityMap
import com.moamap.app.feature.explore.domain.model.CommunityMapSort
import com.moamap.app.feature.explore.domain.repository.CommunityMapRepository
import javax.inject.Inject

class CommunityMapRepositoryImpl @Inject constructor(
    private val service: CommunityMapService,
) : CommunityMapRepository {

    override suspend fun getCommunityMaps(
        tag: String?,
        sort: CommunityMapSort,
    ): List<CommunityMap> = service
        .getCommunityMaps(tag = tag, sort = sort.name, size = PAGE_SIZE)
        .content
        .map { it.toDomain() }

    override suspend fun getRecommendedMaps(): List<CommunityMap> = service
        .getRecommendedMaps(size = RECOMMENDATION_SIZE)
        .map { it.toDomain() }

    private companion object {
        /** 서버 기본값과 같다. 무한 스크롤을 붙이기 전까지는 첫 페이지만 쓴다. */
        const val PAGE_SIZE = 20

        /** 가로 스크롤 한 줄에 담는 수. 서버 기본값과 같다(상한 20). */
        const val RECOMMENDATION_SIZE = 5
    }
}
