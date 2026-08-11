package com.example.moamap.feature.mapdetail.domain.model

/** 활동 내역의 종류. 서버가 내려주는 세 가지다. */
enum class MapActivityType {
    PlaceAdded,
    PlaceRemoved,

    /** 후기 작성. 프라이빗 지도에만 내려온다. */
    ReviewCreated,
}

/**
 * 지도 활동 내역 한 건.
 *
 * 서버가 `places`·`place_reviews` 의 시각 컬럼에서 이벤트를 역산해 주는 구조라 **로그
 * 고유 식별자가 없다.** 화면이 목록 키를 따로 만든다.
 *
 * 이름·사진·장소명이 모두 nullable 이다. 탈퇴한 사용자나 프로필 조회 실패를 서버가 빈 값으로
 * 내려보내고, 그런 건이라도 "언제 무슨 일이 있었는지" 는 남겨야 해서 버리지 않는다.
 */
data class MapActivity(
    val type: MapActivityType,
    /** 발생 시각(epoch millis). 서버 값이 없거나 못 읽으면 null 이다. */
    val occurredAtMillis: Long?,
    val actorName: String?,
    val actorImageUrl: String?,
    val placeId: Long?,
    val placeName: String?,
    /** 후기 작성 로그의 별점. 다른 종류에는 없다. */
    val rating: Int?,
)
