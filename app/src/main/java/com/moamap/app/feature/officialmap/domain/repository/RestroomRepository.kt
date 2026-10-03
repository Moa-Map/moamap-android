package com.moamap.app.feature.officialmap.domain.repository

import com.moamap.app.feature.officialmap.domain.model.RestroomDetail
import com.moamap.app.feature.officialmap.domain.model.RestroomMarkers

interface RestroomRepository {
    /** 경계 안의 화장실. 실패 시 예외를 던진다. */
    suspend fun getRestrooms(south: Double, west: Double, north: Double, east: Double): RestroomMarkers

    /** 화장실 한 곳의 자세한 정보. 실패 시 예외를 던진다. */
    suspend fun getRestroom(id: Long): RestroomDetail
}
