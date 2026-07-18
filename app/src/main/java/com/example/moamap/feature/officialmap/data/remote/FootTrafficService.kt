package com.example.moamap.feature.officialmap.data.remote

import retrofit2.http.GET

interface FootTrafficService {

    @GET("map/official/foot-traffic/areas")
    suspend fun getAreas(): List<FootTrafficAreaDto>

    @GET("map/official/foot-traffic/congestion")
    suspend fun getCongestions(): List<CongestionDto>
}
