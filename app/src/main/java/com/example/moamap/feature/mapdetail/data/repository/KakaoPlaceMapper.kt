package com.example.moamap.feature.mapdetail.data.repository

import com.example.moamap.feature.mapdetail.data.remote.KakaoPlaceDto
import com.example.moamap.feature.mapdetail.domain.model.PlaceCandidate

/**
 * 카카오 검색 결과를 후보로 옮긴다.
 *
 * **`x` 가 경도이고 `y` 가 위도다.** 둘 다 문자열로 와서 바꿔 넣어도 컴파일이 되고 값도
 * 들어간다. 서울은 위도 37, 경도 127 이라 뒤집으면 마커가 중국 근처에 선다.
 *
 * 등록 필수값([kakaoPlaceId] 와 좌표)이 없는 항목은 후보로 삼지 않는다. 목록에 남겨 두면
 * 고른 뒤 등록 단계에서야 막혀 사용자가 이유를 알 수 없다.
 *
 * @return 등록할 수 없는 항목이면 null
 */
fun KakaoPlaceDto.toPlaceCandidate(): PlaceCandidate? {
    if (id.isBlank() || placeName.isBlank()) return null

    val longitude = x.toDoubleOrNull() ?: return null
    val latitude = y.toDoubleOrNull() ?: return null

    return PlaceCandidate(
        kakaoPlaceId = id,
        name = placeName,
        address = addressName?.takeIf { it.isNotBlank() },
        roadAddress = roadAddressName?.takeIf { it.isNotBlank() },
        latitude = latitude,
        longitude = longitude,
        category = categoryName?.takeIf { it.isNotBlank() },
        placeUrl = placeUrl?.takeIf { it.isNotBlank() },
    )
}
