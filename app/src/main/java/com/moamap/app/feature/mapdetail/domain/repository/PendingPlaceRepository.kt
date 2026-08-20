package com.moamap.app.feature.mapdetail.domain.repository

import com.moamap.app.feature.mapdetail.domain.model.PendingPlace

interface PendingPlaceRepository {

    /** 승인 대기 중인 장소 전부. 서버가 준 순서를 그대로 돌려준다. */
    suspend fun getPendingPlaces(mapId: Long): List<PendingPlace>

    /** 요청을 수락한다. 그 장소가 지도에 올라간다. */
    suspend fun approve(placeId: Long)

    /** 요청을 거절한다. */
    suspend fun reject(placeId: Long)
}
