package com.example.moamap.feature.officialmap.domain.repository

import com.example.moamap.feature.officialmap.domain.model.DensityArea

interface FootTrafficRepository {
    /** 지역 목록과 실시간 혼잡도를 합쳐 반환한다. 실패 시 예외를 던진다. */
    suspend fun getDensityAreas(): List<DensityArea>
}
