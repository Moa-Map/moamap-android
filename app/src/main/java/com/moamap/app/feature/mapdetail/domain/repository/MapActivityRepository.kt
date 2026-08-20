package com.moamap.app.feature.mapdetail.domain.repository

import com.moamap.app.feature.mapdetail.domain.model.MapActivity

interface MapActivityRepository {

    /**
     * 지도의 활동 내역. 최신 것이 앞에 온다.
     *
     * 페이지를 이어 받지 않는다. 로그 탭은 최근 활동을 훑는 곳이라 첫 장이면 충분하고,
     * 장소 목록과 달리 시간이 지날수록 계속 쌓이기만 해서 전량을 받을 끝이 없다.
     *
     * 공식 지도는 서버가 제공하지 않고, 프라이빗 지도는 멤버가 아니면 거절당한다.
     */
    suspend fun getActivities(mapId: Long): List<MapActivity>
}
