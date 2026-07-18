package com.example.moamap.feature.officialmap.data.repository

import com.example.moamap.feature.officialmap.data.remote.CongestionDto
import com.example.moamap.feature.officialmap.data.remote.FootTrafficAreaDto
import com.example.moamap.feature.officialmap.domain.model.AreaCongestion
import com.example.moamap.feature.officialmap.domain.model.CongestionLevel
import com.example.moamap.feature.officialmap.domain.model.DensityArea

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
        femaleRate = femaleRate,
        twentiesRate = ppltnRate20,
    )
