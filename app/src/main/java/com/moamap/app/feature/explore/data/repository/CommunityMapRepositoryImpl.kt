package com.moamap.app.feature.explore.data.repository

import com.moamap.app.feature.explore.data.remote.CommunityMapService
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
}
