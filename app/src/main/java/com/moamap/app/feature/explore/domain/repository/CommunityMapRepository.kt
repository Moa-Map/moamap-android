package com.moamap.app.feature.explore.domain.repository

import com.moamap.app.feature.explore.domain.model.CommunityMapPage
import com.moamap.app.feature.explore.domain.model.CommunityMapSort

interface CommunityMapRepository {
    /**
     * 커뮤니티 지도 목록의 한 페이지를 가져온다. 실패 시 예외를 던진다.
     *
     * @param tag null 이면 전체. 그 외에는 태그가 정확히 일치하는 지도만 걸러진다.
     * @param sort 인기순·최신순.
     * @param page 0 부터 센다.
     */
    suspend fun getCommunityMaps(
        tag: String?,
        sort: CommunityMapSort,
        page: Int,
        size: Int,
    ): CommunityMapPage
}
