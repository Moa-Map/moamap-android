package com.example.moamap.feature.officialmap.domain.repository

import com.example.moamap.feature.officialmap.domain.model.OfficialMap

interface OfficialMapRepository {

    suspend fun getOfficialMaps(): List<OfficialMap>
}
