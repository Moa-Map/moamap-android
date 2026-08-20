package com.moamap.app.core.network.kakao

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** GET v2/local/search/keyword.json 응답 */
@Serializable
data class KakaoKeywordSearchDto(
    val documents: List<KakaoPlaceDto> = emptyList(),
)

/**
 * 카카오 로컬 검색 결과 한 건.
 *
 * 서버가 주는 필드 중 장소 등록에 필요한 것만 선언한다.
 * (`ignoreUnknownKeys = true` 라 나머지는 무시된다.)
 *
 * 사진은 오지 않는다. 카카오 키워드 검색은 [placeUrl](카카오맵 웹페이지)까지만 준다.
 */
@Serializable
data class KakaoPlaceDto(
    /** 장소 고유 id. 우리 서버의 `kakaoPlaceId` 로 그대로 쓴다. */
    val id: String = "",
    @SerialName("place_name") val placeName: String = "",
    @SerialName("address_name") val addressName: String? = null,
    @SerialName("road_address_name") val roadAddressName: String? = null,
    /** 경도. 숫자가 아니라 문자열로 온다. */
    val x: String = "",
    /** 위도. 숫자가 아니라 문자열로 온다. */
    val y: String = "",
    /** `"음식점 > 카페 > 커피전문점"` 같은 분류 경로. 항상 채워져 온다. */
    @SerialName("category_name") val categoryName: String? = null,
    @SerialName("place_url") val placeUrl: String? = null,
)

/** GET v2/local/geo/coord2address.json 응답 */
@Serializable
data class KakaoCoordToAddressDto(
    val documents: List<KakaoCoordAddressDocumentDto> = emptyList(),
)

/**
 * 좌표 하나에 대한 주소.
 *
 * 도로명이 없는 곳(산·논밭)에서는 [roadAddress] 가 null 로 오므로 [address] 로 대체한다.
 */
@Serializable
data class KakaoCoordAddressDocumentDto(
    @SerialName("road_address") val roadAddress: KakaoAddressNameDto? = null,
    val address: KakaoAddressNameDto? = null,
)

@Serializable
data class KakaoAddressNameDto(
    @SerialName("address_name") val addressName: String = "",
)
