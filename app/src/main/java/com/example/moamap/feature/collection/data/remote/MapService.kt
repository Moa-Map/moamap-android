package com.example.moamap.feature.collection.data.remote

import com.example.moamap.core.network.model.PageResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface MapService {

    @GET("api/v1/maps")
    suspend fun getMaps(
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null,
        @Query("sort") sort: String? = null,
    ): PageResponse<MapSummaryDto>

    @POST("api/v1/maps")
    suspend fun createMap(@Body request: MapCreateRequestDto): MapDetailDto

    /**
     * 지도 커버 이미지 업로드용 presigned PUT URL 을 발급받는다.
     *
     * 커버는 우리 서버로 올리지 않는다. 여기서 받은 주소로 앱이 직접 올리고, 응답의 `fileUrl`
     * 을 지도 생성 요청의 `imageUrl` 에 담는다.
     *
     * 지도를 만들기 전에 부르므로 `mapId` 를 넘기지 않는다. 허용 형식은 jpeg/png/webp, 최대 10MB.
     */
    @POST("api/v1/maps/cover-upload-url")
    suspend fun createCoverUploadUrl(
        @Body request: CoverUploadUrlRequestDto,
    ): CoverUploadUrlDto

    /**
     * 내가 참여한 지도 목록. 모음 화면이 쓴다.
     *
     * @param type OFFICIAL, COMMUNITY, PRIVATE 중 하나. 서버 필수값이라 빠뜨리면 400 이 난다.
     */
    @GET("api/v1/maps/me")
    suspend fun getMyMaps(
        @Query("type") type: String,
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null,
        @Query("sort") sort: String? = null,
    ): PageResponse<MapSummaryDto>

    @POST("api/v1/maps/join")
    suspend fun joinByInviteCode(@Body request: JoinByInviteCodeRequestDto): MapDetailDto

    @GET("api/v1/maps/{mapId}")
    suspend fun getMap(@Path("mapId") mapId: Long): MapDetailDto

    @PATCH("api/v1/maps/{mapId}")
    suspend fun updateMap(
        @Path("mapId") mapId: Long,
        @Body request: MapUpdateRequestDto,
    ): MapDetailDto

    @DELETE("api/v1/maps/{mapId}")
    suspend fun deleteMap(@Path("mapId") mapId: Long)

    @POST("api/v1/maps/{mapId}/join")
    suspend fun joinMap(@Path("mapId") mapId: Long): MapDetailDto

    @DELETE("api/v1/maps/{mapId}/members/me")
    suspend fun leaveMap(@Path("mapId") mapId: Long)

    @GET("api/v1/maps/{mapId}/members/{userId}")
    suspend fun getMemberRole(
        @Path("mapId") mapId: Long,
        @Path("userId") userId: Long,
    ): MapMemberRoleDto

    /** 지도에 참여한 사람 전부. 페이지를 나누지 않고 한 번에 내려온다. */
    @GET("api/v1/maps/{mapId}/members")
    suspend fun getMembers(@Path("mapId") mapId: Long): MapMemberListDto

    /** 멤버 역할 변경. OWNER 만 부를 수 있고, 서버가 권한을 다시 확인한다. */
    @PUT("api/v1/maps/{mapId}/members/{userId}/role")
    suspend fun updateMemberRole(
        @Path("mapId") mapId: Long,
        @Path("userId") userId: Long,
        @Body request: MapMemberRoleUpdateRequestDto,
    ): MapMemberRoleUpdateDto
}
