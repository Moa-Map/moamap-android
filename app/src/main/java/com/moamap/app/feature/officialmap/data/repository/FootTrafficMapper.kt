package com.moamap.app.feature.officialmap.data.repository

import com.moamap.app.feature.officialmap.data.remote.CongestionDto
import com.moamap.app.feature.officialmap.data.remote.FootTrafficAreaDto
import com.moamap.app.feature.officialmap.domain.model.AgeGroup
import com.moamap.app.feature.officialmap.domain.model.AreaCongestion
import com.moamap.app.feature.officialmap.domain.model.CongestionLevel
import com.moamap.app.feature.officialmap.domain.model.DensityArea

internal fun FootTrafficAreaDto.toDomain(congestion: CongestionDto?): DensityArea =
    DensityArea(
        code = footTrafficAreaCd,
        name = areaNm,
        lat = lat,
        lng = lng,
        boundaryGeoJson = boundary?.toString(),
        congestion = congestion?.toDomain(),
    )

private fun CongestionDto.toDomain(): AreaCongestion =
    AreaCongestion(
        level = CongestionLevel.fromLabel(congestLvl),
        message = congestMsg,
        populationMin = ppltnMin,
        populationMax = ppltnMax,
        ageRates = buildMap {
            ppltnRate0?.let { put(AgeGroup.UNDER_10, it) }
            ppltnRate10?.let { put(AgeGroup.TEENS, it) }
            ppltnRate20?.let { put(AgeGroup.TWENTIES, it) }
            ppltnRate30?.let { put(AgeGroup.THIRTIES, it) }
            ppltnRate40?.let { put(AgeGroup.FORTIES, it) }
            ppltnRate50?.let { put(AgeGroup.FIFTIES, it) }
            ppltnRate60?.let { put(AgeGroup.SIXTIES, it) }
            ppltnRate70?.let { put(AgeGroup.SEVENTIES_UP, it) }
        },
        maleRate = maleRate,
        femaleRate = femaleRate,
    )
