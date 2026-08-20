package com.moamap.app.feature.mapdetail.domain.repository

import com.moamap.app.feature.mapdetail.domain.model.MapMember

interface MapMemberRepository {

    /** 지도에 참여한 사람 전부. 서버가 준 순서를 그대로 돌려준다. */
    suspend fun getMembers(mapId: Long): List<MapMember>

    /**
     * 일반 멤버를 관리자로 올린다.
     */
    suspend fun grantAdmin(mapId: Long, userId: Long)
}
