package com.example.moamap.feature.collection.domain.model

/**
 * 모음 화면이 나눠 보는 지도 종류. 서버 `type` 파라미터와 1:1 로 대응한다.
 *
 * 로그인하면 기본으로 있는 개인 지도("나만의 지도")는 서버에 별도 종류가 없어 아직 다루지 못한다.
 * `PERSONAL` 이 추가되면 여기에 더한다.
 */
enum class MapType(val requestValue: String) {
    Community("COMMUNITY"),
    Private("PRIVATE"),
}
