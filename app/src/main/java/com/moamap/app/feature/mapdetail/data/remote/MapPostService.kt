package com.moamap.app.feature.mapdetail.data.remote

import com.moamap.app.core.network.model.PageResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
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

    /** 게시물 작성. 지도 멤버만 쓸 수 있고, 읽기가 열린 커뮤니티 지도도 멤버여야 한다. */
    @POST("api/v1/maps/{mapId}/posts")
    suspend fun createPost(
        @Path("mapId") mapId: Long,
        @Body request: MapPostCreateRequestDto,
    ): MapPostDto

    /**
     * 게시물 사진 업로드용 presigned PUT URL 을 **한 장** 발급받는다.
     *
     * 장소 사진과 달리 여러 장을 한 번에 발급하지 않는다. 허용 형식은 jpeg/png/webp, 장당 최대 5MB.
     */
    @POST("api/v1/maps/{mapId}/posts/photo-upload-url")
    suspend fun createPhotoUploadUrl(
        @Path("mapId") mapId: Long,
        @Body request: MapPostPhotoUploadUrlRequestDto,
    ): MapPostPhotoUploadUrlDto
}
