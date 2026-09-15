package com.moamap.app.feature.mapdetail.data.remote

import com.moamap.app.core.network.model.PageResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 지도 로그 탭 게시물.
 *
 * 지도 API(`MapService`)와 같은 map-service 에 있지만 따로 둔다. 게시물·댓글 엔드포인트가
 * 계속 붙을 자리라, 지도 서비스에 섞으면 그 인터페이스를 흉내 내는 테스트 가짜가 전부 함께 커진다.
 */
interface MapPostService {

    /**
     * 게시물 목록. 커뮤니티 지도는 누구나, 프라이빗 지도는 멤버만 볼 수 있다.
     *
     * @param sort `createdAt,desc`(최신순) 또는 `createdAt,asc`(등록순). 빼면 서버가 최신순으로 준다.
     */
    @GET("api/v1/maps/{mapId}/posts")
    suspend fun getPosts(
        @Path("mapId") mapId: Long,
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null,
        @Query("sort") sort: String? = null,
    ): PageResponse<MapPostDto>
}
