package com.example.moamap.feature.officialmap.data.repository

import com.example.moamap.feature.officialmap.data.remote.OfficialMapService
import com.example.moamap.feature.officialmap.domain.model.OfficialMap
import com.example.moamap.feature.officialmap.domain.repository.OfficialMapRepository
import javax.inject.Inject

class OfficialMapRepositoryImpl @Inject constructor(
    private val service: OfficialMapService,
) : OfficialMapRepository {

    override suspend fun getOfficialMaps(): List<OfficialMap> = service
        .getOfficialMaps(size = PAGE_SIZE)
        .content
        .map { it.toOfficialMap() }

    private companion object {
        /** 서버 기본값과 같다. 무한 스크롤을 붙이기 전까지는 첫 페이지만 쓴다. */
        const val PAGE_SIZE = 20
    }
}
