package com.example.moamap.feature.officialmap.data.remote

import retrofit2.http.GET

interface FootTrafficService {

    @GET("api/v1/maps/official/foot-traffic/areas")
    suspend fun getAreas(): List<FootTrafficAreaDto>

    @GET("api/v1/maps/official/foot-traffic/congestion")
    suspend fun getCongestions(): List<CongestionDto>
}
