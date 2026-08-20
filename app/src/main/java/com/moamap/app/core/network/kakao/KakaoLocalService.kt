package com.moamap.app.core.network.kakao

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 카카오 로컬 API.
 *
 * 우리 서버에 장소 검색 엔드포인트가 없어 앱에서 직접 부른다. 인증은 REST 키를 헤더에
 * 싣는 방식이라 `@KakaoLocalClient` 로 한정한 별도 Retrofit 을 쓴다.
 *
 * 지도 상세(검색)와 모음(좌표 추천)이 함께 쓰므로 feature 가 아니라 여기에 둔다.
 */
interface KakaoLocalService {

    /**
     * 키워드로 장소를 찾는다.
     *
     * [x]/[y] 와 [radius] 를 함께 주면 그 반경 안으로 결과를 제한한다. `sort = "distance"`
     * 는 좌표가 있어야 의미가 있다 - 없이 보내면 카카오가 정확도순으로 되돌린다.
     *
     * 쿼터가 소진되면 인증 실패가 아니라 **200 에 빈 결과**로 온다. 검색이 계속 0건이면
     * 쿼터를 의심한다.
     */
    @GET("v2/local/search/keyword.json")
    suspend fun searchKeyword(
        @Query("query") query: String,
        @Query("size") size: Int? = null,
        @Query("x") x: String? = null,
        @Query("y") y: String? = null,
        @Query("radius") radius: Int? = null,
        @Query("sort") sort: String? = null,
    ): KakaoKeywordSearchDto

    /**
     * 좌표를 주소로 바꾼다.
     *
     * **[lng] 가 x 이고 [lat] 이 y 다.** 둘 다 문자열이라 바꿔 넣어도 컴파일이 되고,
     * 서울 좌표를 뒤집으면 중국 근처 주소가 돌아온다.
     */
    @GET("v2/local/geo/coord2address.json")
    suspend fun coordToAddress(
        @Query("x") lng: String,
        @Query("y") lat: String,
    ): KakaoCoordToAddressDto
}
