package com.moamap.app.feature.explore.data.remote

import com.moamap.app.core.network.model.PageResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface CommunityMapService {

    /**
     * 커뮤니티 지도 목록.
     *
     * @param tag 지도에 달린 태그와 정확히 일치하는 문자열. null 이면 전체.
     * @param sort POPULAR(참여 인원순) 또는 LATEST(최신순). null 이면 서버 기본값(POPULAR).
     */
    @GET("api/v1/maps")
    suspend fun getCommunityMaps(
        @Query("tag") tag: String? = null,
        @Query("sort") sort: String? = null,
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null,
    ): PageResponse<CommunityMapDto>

    /**
     * 추천 커뮤니티 지도.
     *
     * 목록과 달리 페이지가 아니라 배열로 온다. 누구에게 추천할지는 서버가 게이트웨이가
     * 넣어 주는 `X-User-Id` 로 판단하므로 클라이언트가 실어 보낼 것이 없다.
     *
     * @param size 1~20. null 이면 서버 기본값(5).
     */
    @GET("api/v1/maps/recommendations")
    suspend fun getRecommendedMaps(
        @Query("size") size: Int? = null,
    ): List<MapRecommendationDto>
}
