package com.moamap.app.feature.explore.data.remote

import com.moamap.app.core.network.model.PageResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 장소 댓글. 서버는 09-22 배포부터 경로를 `/reviews` 에서 `/comments` 로 바꿨다.
 *
 * 앱 안 이름은 후기(Review)로 둔다. 활동 내역 API 가 아직 `REVIEW_CREATED`·`reviewId` 를 쓴다.
 */
interface ReviewService {

    @GET("api/v1/places/{placeId}/comments")
    suspend fun getReviews(
        @Path("placeId") placeId: Long,
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null,
        @Query("sort") sort: String? = null,
    ): PageResponse<PlaceReviewDto>

    /**
     * 후기 사진 한 장을 올릴 주소. 발급 권한은 후기 작성과 같다(지도 멤버).
     *
     * [PlaceReviewPhotoUploadUrlDto.uploadUrl] 로 직접 PUT 한 뒤 `fileUrl` 을 작성 요청에 담는다.
     */
    @POST("api/v1/places/{placeId}/comments/photo-upload-url")
    suspend fun createPhotoUploadUrl(
        @Path("placeId") placeId: Long,
        @Body request: PlaceReviewPhotoUploadUrlRequestDto,
    ): PlaceReviewPhotoUploadUrlDto

    @POST("api/v1/places/{placeId}/comments")
    suspend fun createReview(
        @Path("placeId") placeId: Long,
        @Body request: PlaceReviewCreateRequestDto,
    ): PlaceReviewDto

    @PATCH("api/v1/places/{placeId}/comments/{reviewId}")
    suspend fun updateReview(
        @Path("placeId") placeId: Long,
        @Path("reviewId") reviewId: Long,
        @Body request: PlaceReviewUpdateRequestDto,
    ): PlaceReviewDto

    @DELETE("api/v1/places/{placeId}/comments/{reviewId}")
    suspend fun deleteReview(
        @Path("placeId") placeId: Long,
        @Path("reviewId") reviewId: Long,
    )
}
