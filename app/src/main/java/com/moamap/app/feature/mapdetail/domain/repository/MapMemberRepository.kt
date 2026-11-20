package com.moamap.app.feature.mapdetail.domain.repository

import com.moamap.app.feature.mapdetail.domain.model.MapMember

interface MapMemberRepository {

    /** 지도에 참여한 사람 전부. 서버가 준 순서를 그대로 돌려준다. */
    suspend fun getMembers(mapId: Long): List<MapMember>

    /**
     * 일반 멤버를 관리자로 올린다.
     */
    suspend fun grantAdmin(mapId: Long, userId: Long)

    /**
     * 방장을 넘겨받을 수 있는 사람. 관리자가 있으면 관리자만, 없으면 멤버 전체를 먼저 참여한 순으로 준다.
     * 비어 있으면 혼자 남은 지도다. 방장만 부를 수 있다.
     */
    suspend fun getOwnerCandidates(mapId: Long): List<MapMember>

    /** [newOwnerId] 에게 방장을 넘기고 지도에서 나간다. 서버가 둘을 한 번에 처리한다. */
    suspend fun handOverAndLeave(mapId: Long, newOwnerId: Long)
}
