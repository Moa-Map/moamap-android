package com.example.moamap.feature.explore.data.remote

import com.example.moamap.core.network.model.PageResponse
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

    @GET("api/v1/places/pending")
    suspend fun getPendingPlaces(
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null,
        @Query("sort") sort: String? = null,
    ): PageResponse<PlaceDto>

    @GET("api/v1/places/{id}")
    suspend fun getPlace(@Path("id") id: Long): PlaceDto

    @PATCH("api/v1/places/{id}")
    suspend fun updatePlace(
        @Path("id") id: Long,
        @Body request: PlaceUpdateRequestDto,
    ): PlaceDto

    @DELETE("api/v1/places/{id}")
    suspend fun deletePlace(@Path("id") id: Long)

    @PATCH("api/v1/places/{id}/approve")
    suspend fun approvePlace(@Path("id") id: Long): PlaceDto

    @PATCH("api/v1/places/{id}/reject")
    suspend fun rejectPlace(@Path("id") id: Long): PlaceDto

    @POST("api/v1/places/instagram-extractions")
    suspend fun extractFromInstagram(
        @Body request: InstagramExtractRequestDto,
    ): List<PlaceCandidateDto>
}
