package com.moamap.app.feature.explore.domain.repository

import com.moamap.app.feature.explore.domain.model.CommunityMap
import com.moamap.app.feature.explore.domain.model.CommunityMapSort

interface CommunityMapRepository {
    /**
     * 커뮤니티 지도 목록의 첫 페이지를 가져온다. 실패 시 예외를 던진다.
     *
     * @param tag null 이면 전체. 그 외에는 태그가 정확히 일치하는 지도만 걸러진다.
     */
    suspend fun getCommunityMaps(tag: String?, sort: CommunityMapSort): List<CommunityMap>

    /**
     * 추천 커뮤니티 지도를 가져온다. 실패 시 예외를 던진다.
     *
     * 참여 이력이 없어도 서버가 인기·신선도로 채워 주므로, 빈 목록은 추천할 커뮤니티
     * 지도가 하나도 없을 때뿐이다.
     *
     * 돌아온 지도는 모두 아직 참여하지 않은 것이다([CommunityMap.joined] 가 false).
     */
    suspend fun getRecommendedMaps(): List<CommunityMap>
}
