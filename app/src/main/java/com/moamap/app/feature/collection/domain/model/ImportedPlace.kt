package com.moamap.app.feature.collection.domain.model

/**
 * 링크에서 뽑아낸 장소 후보.
 *
 * 서버 응답에는 고유 id 가 없어 [id] 는 `kakaoPlaceId` 를 쓰고, 그마저 없으면 순번으로 채운다.
 * 화면에서 어떤 장소를 골랐는지 구분하는 용도다.
 *
 * 나머지 값은 그대로 일괄 등록 요청이 된다. 후보를 받은 뒤 다시 조회하지 않으므로
 * 등록에 필요한 값을 여기서 전부 들고 있어야 한다.
 */
data class ImportedPlace(
    val id: String,
    val name: String,
    /** 지번 주소. */
    val address: String? = null,
    val roadAddress: String? = null,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val category: String? = null,
    /** 공유 리스트에 사용자가 적어둔 메모. */
    val description: String? = null,
    /** 서버가 등록 키로 쓴다. 매칭에 실패한 후보는 비어 있다. */
    val kakaoPlaceId: String? = null,
    /** `INSTAGRAM`, `NAVER_MAP`, `KAKAO_MAP`, `GOOGLE_MAP`. */
    val sourceType: String = "",
    val sourceUrl: String? = null,
) {
    /** 카드에 보여줄 주소. 도로명이 익숙하니 먼저 쓰고 없으면 지번으로 대체한다. */
    val displayAddress: String
        get() = roadAddress?.takeIf { it.isNotBlank() } ?: address.orEmpty()

    /**
     * 지도에 등록할 수 있는 후보인지.
     *
     * 서버가 `kakaoPlaceId` 를 필수로 요구한다. 없는 후보는 골라도 등록에서 거절당하므로
     * 애초에 고르지 못하게 한다.
     */
    val savable: Boolean
        get() = !kakaoPlaceId.isNullOrBlank()
}

/**
 * 일괄 등록 결과.
 *
 * 서버가 건별로 부분 성공시키므로 요청 하나가 통째로 성공하거나 실패하지 않는다.
 * 고른 지도가 여러 개면 지도별 결과를 모두 합친 값이다.
 */
data class PlaceSaveResult(
    val created: Int,
    /** 그 지도에 이미 있던 장소. */
    val duplicate: Int,
    val failed: Int,
)
