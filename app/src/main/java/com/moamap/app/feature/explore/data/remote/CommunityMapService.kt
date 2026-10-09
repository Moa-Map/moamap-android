package com.moamap.app.feature.explore.data.remote

import com.moamap.app.core.network.interceptor.ANONYMOUS_REQUEST_HEADER
import com.moamap.app.core.network.model.PageResponse
import retrofit2.http.GET
import retrofit2.http.Headers
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
     * [getCommunityMaps] 와 같은 목록을 로그인 정보 없이 받는다.
     *
     * 로그인하면 서버가 참여한 지도를 빼고 주는데(백엔드 #115), 전체보기는 참여 여부와 상관없이 다 보여준다
     * (10-10 사용자 결정). 비로그인 응답이라 [CommunityMapDto.joined] 는 늘 false 다 - 참여 여부는 따로 채운다.
     */
    @GET("api/v1/maps")
    @Headers("$ANONYMOUS_REQUEST_HEADER: true")
    suspend fun getAllCommunityMaps(
        @Query("tag") tag: String? = null,
        @Query("sort") sort: String? = null,
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null,
    ): PageResponse<CommunityMapDto>

    /**
     * 지도 검색. 커뮤니티 지도와 공식지도의 이름·설명·태그 일부로 찾는다(프라이빗 제외, 대소문자 무시).
     *
     * 목록과 달리 참여한 지도도 나온다([CommunityMapDto.joined]). 빈 검색어나 50자 넘는 검색어는 400 이다.
     *
     * @param sort null 이면 서버 기본값(POPULAR).
     */
    @GET("api/v1/maps/search")
    suspend fun searchMaps(
        @Query("keyword") keyword: String,
        @Query("sort") sort: String? = null,
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null,
    ): PageResponse<CommunityMapDto>
}
