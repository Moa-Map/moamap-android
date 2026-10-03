package com.moamap.app.feature.officialmap.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RestroomService {

    @GET("api/v1/maps/official/restrooms")
    suspend fun getRestrooms(
        @Query("swLat") swLat: Double,
        @Query("swLng") swLng: Double,
        @Query("neLat") neLat: Double,
        @Query("neLng") neLng: Double,
    ): RestroomListDto

    @GET("api/v1/maps/official/restrooms/{id}")
    suspend fun getRestroom(@Path("id") id: Long): RestroomDetailDto
}
