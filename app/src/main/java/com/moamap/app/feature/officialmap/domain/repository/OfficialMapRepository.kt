package com.moamap.app.feature.officialmap.domain.repository

import com.moamap.app.feature.officialmap.domain.model.OfficialMap

interface OfficialMapRepository {

    suspend fun getOfficialMaps(): List<OfficialMap>
}
