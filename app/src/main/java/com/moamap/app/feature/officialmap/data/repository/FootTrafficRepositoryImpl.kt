package com.moamap.app.feature.officialmap.data.repository

import com.moamap.app.feature.officialmap.data.remote.FootTrafficService
import com.moamap.app.feature.officialmap.domain.model.DensityArea
import com.moamap.app.feature.officialmap.domain.repository.FootTrafficRepository
import javax.inject.Inject

class FootTrafficRepositoryImpl @Inject constructor(
    private val service: FootTrafficService,
) : FootTrafficRepository {

    override suspend fun getDensityAreas(): List<DensityArea> {
        val congestionByCode = service.getCongestions().associateBy { it.footTrafficAreaCd }
        return service.getAreas().map { area ->
            area.toDomain(congestionByCode[area.footTrafficAreaCd])
        }
    }
}
