package com.example.moamap.feature.collection.data.remote

import com.example.moamap.core.network.model.PageResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
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
}
