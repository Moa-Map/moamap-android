package com.example.moamap.feature.explore.data.repository

import com.example.moamap.feature.explore.data.remote.CommunityMapService
import com.example.moamap.feature.explore.domain.model.CommunityMap
import com.example.moamap.feature.explore.domain.model.CommunityMapSort
import com.example.moamap.feature.explore.domain.repository.CommunityMapRepository
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

    private companion object {
        /** 서버 기본값과 같다. 무한 스크롤을 붙이기 전까지는 첫 페이지만 쓴다. */
        const val PAGE_SIZE = 20
    }
}
