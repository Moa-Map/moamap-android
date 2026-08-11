package com.example.moamap.feature.officialmap.domain.repository

import com.example.moamap.feature.officialmap.domain.model.OfficialMap

interface OfficialMapRepository {

    suspend fun getOfficialMaps(): List<OfficialMap>

    /** 공식지도에 참여한다. 서버는 커뮤니티 지도와 같은 `POST /maps/{mapId}/join` 을 받는다. */
    suspend fun joinMap(mapId: Long)
}
