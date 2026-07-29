package com.example.moamap.feature.mapdetail.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 카카오 로컬 API.
 *
 * 우리 서버에 장소 검색 엔드포인트가 없어 앱에서 직접 부른다. 인증은 REST 키를 헤더에
 * 싣는 방식이라 `@KakaoLocalClient` 로 한정한 별도 Retrofit 을 쓴다.
 */
interface KakaoLocalService {

    /**
     * 키워드로 장소를 찾는다.
     *
     * 쿼터가 소진되면 인증 실패가 아니라 **200 에 빈 결과**로 온다. 검색이 계속 0건이면
     * 쿼터를 의심한다.
     */
    @GET("v2/local/search/keyword.json")
    suspend fun searchKeyword(
        @Query("query") query: String,
        @Query("size") size: Int? = null,
    ): KakaoKeywordSearchDto
}
