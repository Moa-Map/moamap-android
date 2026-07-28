package com.example.moamap.feature.explore.data.remote

import com.example.moamap.core.network.model.PageResponse
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
}
