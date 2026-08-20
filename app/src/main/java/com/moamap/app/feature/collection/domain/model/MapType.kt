package com.moamap.app.feature.collection.domain.model

/**
 * 지도 종류. 서버 `type` 과 1:1 로 대응한다.
 *
 * 모음 화면이 탭으로 나눠 보는 건 이 중 [Community] 와 [Private] 뿐이다. [Official] 은
 * 사용자가 만들 수 없는 공공데이터 지도라 모음에 걸리지 않고 공식지도 탭으로만 들어온다.
 * 탭 목록을 이 enum 순회로 만들지 않는 이유가 그것이다 - `CollectionScreen.MapTypeTabs` 참고.
 *
 * 로그인하면 기본으로 있는 개인 지도("나만의 지도")는 서버에 별도 종류가 없어 아직 다루지 못한다.
 * `PERSONAL` 이 추가되면 여기에 더한다.
 */
enum class MapType(val requestValue: String) {
    Community("COMMUNITY"),
    Private("PRIVATE"),
    Official("OFFICIAL"),
}
