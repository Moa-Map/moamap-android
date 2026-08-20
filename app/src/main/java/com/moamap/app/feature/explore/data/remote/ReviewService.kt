package com.moamap.app.feature.explore.data.remote

import com.moamap.app.core.network.model.PageResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ReviewService {

    @GET("api/v1/places/{placeId}/reviews")
    suspend fun getReviews(
        @Path("placeId") placeId: Long,
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null,
        @Query("sort") sort: String? = null,
    ): PageResponse<PlaceReviewDto>

    @POST("api/v1/places/{placeId}/reviews")
    suspend fun createReview(
        @Path("placeId") placeId: Long,
        @Body request: PlaceReviewCreateRequestDto,
    ): PlaceReviewDto

    @PATCH("api/v1/places/{placeId}/reviews/{reviewId}")
    suspend fun updateReview(
        @Path("placeId") placeId: Long,
        @Path("reviewId") reviewId: Long,
        @Body request: PlaceReviewUpdateRequestDto,
    ): PlaceReviewDto

    @DELETE("api/v1/places/{placeId}/reviews/{reviewId}")
    suspend fun deleteReview(
        @Path("placeId") placeId: Long,
        @Path("reviewId") reviewId: Long,
    )
}
