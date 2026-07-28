package com.example.moamap.feature.explore.domain.repository

import com.example.moamap.feature.explore.domain.model.CommunityMap
import com.example.moamap.feature.explore.domain.model.CommunityMapSort

interface CommunityMapRepository {
    /**
     * 커뮤니티 지도 목록의 첫 페이지를 가져온다. 실패 시 예외를 던진다.
     *
     * @param tag null 이면 전체. 그 외에는 태그가 정확히 일치하는 지도만 걸러진다.
     */
    suspend fun getCommunityMaps(tag: String?, sort: CommunityMapSort): List<CommunityMap>
}
