package com.moamap.app.feature.mapdetail.domain.model

import androidx.compose.runtime.Immutable

/**
 * 카카오에서 찾은 장소 후보. 아직 우리 지도에 없다.
 *
 * 지도에 이미 등록된 장소는 [MapPlace] 다. 둘은 성격이 달라 모델을 나눈다 - 후보는
 * 우리 서버가 준 값이 아니고 id 도 카카오 것이다.
 */
@Immutable
data class PlaceCandidate(
    /** 카카오 장소 id. 서버 등록 필수값이라 비어 있으면 후보로 쓰지 않는다. */
    val kakaoPlaceId: String,
    val name: String,
    /** 지번 주소. 서버에 그대로 보낸다. */
    val address: String?,
    /** 도로명 주소. 서버에 그대로 보낸다. */
    val roadAddress: String?,
    val latitude: Double,
    val longitude: Double,
    /** `"음식점 > 카페"` 같은 분류 경로. 서버에 그대로 보낸다. */
    val category: String?,
    /** 카카오맵 웹페이지. 서버의 `sourceUrl` 로 보낸다. */
    val placeUrl: String?,
) {
    /** 카드에 한 줄로 보여줄 주소. 도로명을 우선한다. 둘 다 없으면 빈 문자열. */
    val displayAddress: String
        get() = roadAddress?.takeIf { it.isNotBlank() }
            ?: address?.takeIf { it.isNotBlank() }
            ?: ""
}

/** 등록 폼이 모은 값. */
@Immutable
data class NewPlace(
    val candidate: PlaceCandidate,
    val tags: List<String>,
    val memo: String?,
    /** 업로드가 끝난 뒤의 접근 URL. 사진을 붙이지 않았으면 빈 목록. */
    val photoUrls: List<String>,
)
