package com.moamap.app.feature.explore.data.remote

import com.moamap.app.core.network.model.PageResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PlaceService {

    @GET("api/v1/places")
    suspend fun getPlaces(
        @Query("mapId") mapId: Long,
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null,
        @Query("sort") sort: String? = null,
    ): PageResponse<PlaceDto>

    @POST("api/v1/places")
    suspend fun createPlace(@Body request: PlaceCreateRequestDto): PlaceDto

    /** 지도 하나에 장소를 한꺼번에 등록한다. 건별로 부분 성공한다. */
    @POST("api/v1/places/bulk")
    suspend fun createPlacesBulk(
        @Body request: PlaceBulkCreateRequestDto,
    ): PlaceBulkCreateResponseDto

    /**
     * 지도 활동 내역. 장소 추가·삭제와 후기 작성 이력이다.
     *
     * `sort` 를 받지 않는다. 서버가 발생 시각 내림차순으로 고정하고 보낸 값은 무시한다.
     * `size` 는 서버가 100 으로 자른다.
     *
     * 공식 지도는 제공하지 않고, 프라이빗 지도는 멤버만 볼 수 있다.
     */
    @GET("api/v1/places/activities")
    suspend fun getActivities(
        @Query("mapId") mapId: Long,
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null,
    ): PageResponse<PlaceActivityDto>

    /**
     * 승인 대기 중인 장소. 방장·관리자는 지도 전체를, 멤버는 자기가 신청한 건만 받는다.
     *
     * @param mapId 서버 필수값이라 빠뜨리면 400 이 난다.
     */
    @GET("api/v1/places/pending")
    suspend fun getPendingPlaces(
        @Query("mapId") mapId: Long,
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null,
        @Query("sort") sort: String? = null,
    ): PageResponse<PendingPlaceDto>

    @GET("api/v1/places/{id}")
    suspend fun getPlace(@Path("id") id: Long): PlaceDto

    /**
     * 장소 정보를 고친다. 보낸 칸만 바뀌고 null 인 칸은 그대로다. 태그는 보낸 목록으로 통째로 바뀐다.
     *
     * 프라이빗 지도는 멤버 누구나, 그 밖에는 방장·관리자와 등록한 본인만 받는다(스웨거 설명보다 넓다,
     * 백엔드 `PlaceService.checkModifyPermission`). 사진은 보낸 목록으로 통째로 바뀐다(이슈 #398).
     */
    @PATCH("api/v1/places/{id}")
    suspend fun updatePlace(
        @Path("id") id: Long,
        @Body request: PlaceUpdateRequestDto,
    ): PlaceDto

    /** 장소를 지운다(소프트 삭제). 권한은 [updatePlace] 와 같다. */
    @DELETE("api/v1/places/{id}")
    suspend fun deletePlace(@Path("id") id: Long)

    /**
     * 장소를 신고한다. 볼 수 있는 사람이면 누구나(프라이빗은 멤버만 403), 승인된 장소만(아니면 404).
     * 내 장소는 400 `PLACE_021`, 이미 신고했으면 409 `PLACE_022`.
     */
    @POST("api/v1/places/{id}/reports")
    suspend fun reportPlace(
        @Path("id") id: Long,
        @Body request: ReportRequestDto,
    )

    @PATCH("api/v1/places/{id}/approve")
    suspend fun approvePlace(@Path("id") id: Long): PlaceDto

    @PATCH("api/v1/places/{id}/reject")
    suspend fun rejectPlace(@Path("id") id: Long): PlaceDto

    @POST("api/v1/places/instagram-extractions")
    suspend fun extractFromInstagram(
        @Body request: InstagramExtractRequestDto,
    ): List<PlaceCandidateDto>

    /** 네이버·카카오·구글 지도 공유 리스트에서 장소를 뽑는다. */
    @POST("api/v1/places/map-share-extractions")
    suspend fun extractFromMapShare(
        @Body request: MapShareExtractRequestDto,
    ): MapShareExtractResponseDto

    /**
     * 장소 사진 업로드용 presigned PUT URL 을 최대 5장 일괄 발급받는다.
     *
     * 사진은 우리 서버로 올리지 않는다. 여기서 받은 주소로 앱이 직접 올리고, 응답의
     * `fileUrl` 을 장소 등록 요청의 `photoUrls` 에 담는다.
     */
    @POST("api/v1/places/photo-upload-url")
    suspend fun createPhotoUploadUrls(
        @Body request: PhotoUploadUrlRequestDto,
    ): List<PhotoUploadUrlDto>

    /**
     * 하트 누르기·취소. 지도 멤버만 할 수 있다.
     *
     * 이미 그 상태여도 에러 없이 현재 상태를 돌려준다. 응답을 그대로 화면에 반영하면 된다.
     */
    @POST("api/v1/places/{placeId}/likes")
    suspend fun likePlace(@Path("placeId") placeId: Long): PlaceLikeDto

    @DELETE("api/v1/places/{placeId}/likes")
    suspend fun unlikePlace(@Path("placeId") placeId: Long): PlaceLikeDto
}
