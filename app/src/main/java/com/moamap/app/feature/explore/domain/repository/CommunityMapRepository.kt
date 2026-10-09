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

    /**
     * 전체보기 목록의 한 페이지. [getCommunityMaps] 와 달리 **참여한 지도도 함께** 준다(10-10 사용자 결정).
     * 참여 여부(`joined`)는 내가 참여한 커뮤니티 지도 목록으로 채운다.
     */
    suspend fun getAllCommunityMaps(
        tag: String?,
        sort: CommunityMapSort,
        page: Int,
        size: Int,
    ): CommunityMapPage

    /**
     * 지도 검색의 한 페이지. 커뮤니티 지도와 공식지도가 섞여 오고, 참여한 지도도 나온다. 순서는 서버 기본(인기순)이다.
     *
     * @param keyword 앞뒤 공백을 지운, 비지 않은 검색어. 50자까지.
     */
    suspend fun searchMaps(keyword: String, page: Int, size: Int): CommunityMapPage
}
