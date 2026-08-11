package com.example.moamap.feature.officialmap.data.remote

import com.example.moamap.core.network.model.PageResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface OfficialMapService {

    /** 공공데이터 기반 공식 지도 목록. 로그인 없이도 조회된다. */
    @GET("api/v1/maps/official")
    suspend fun getOfficialMaps(
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null,
        @Query("sort") sort: String? = null,
    ): PageResponse<OfficialMapDto>
}
