package com.moamap.app.feature.mapdetail.domain.model

import androidx.compose.runtime.Immutable

/**
 * 지도에 등록된 장소 한 건.
 *
 * 지도 설명 화면의 미리보기, 상세 화면의 마커와 바텀시트 목록이 모두 이 모델을 쓴다.
 */
@Immutable
data class MapPlace(
    val id: Long,
    val name: String,
    /** 도로명 주소를 우선 쓰고, 없으면 지번 주소로 대신한다. 둘 다 없으면 빈 문자열. */
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val photoUrl: String?,
    /** 등록자가 적은 한 줄 설명. 안 적었으면 빈 문자열. */
    val description: String = "",
    /** 카카오 분류 경로. `"음식점 > 카페 > 커피전문점"` 처럼 온다. 마커 아이콘과 카테고리 필터가 쓴다. */
    val category: String = "",
    /** 장소를 등록할 때 사람이 단 태그. 장소 상세의 노란 칩이다. */
    val tags: List<String> = emptyList(),
    /** 인스타그램 링크로 가져온 장소의 원본 게시물 주소. 그 밖의 장소는 null 이다. */
    val instagramUrl: String? = null,
    /** 댓글 수. */
    val reviewCount: Int = 0,
    /** 카카오 장소 id. 카카오맵으로 열 때 쓴다. 서버 필수값이라 보통 채워져 온다. */
    val kakaoPlaceId: String = "",
    /** 하트를 누른 사람 수. */
    val likeCount: Int = 0,
    /** 내가 하트를 눌러 둔 상태인지. 서버가 요청한 사람 기준으로 내려준다. */
    val liked: Boolean = false,
)

/** 하트를 누르거나 취소한 뒤 서버가 확정한 상태. */
data class PlaceLike(val liked: Boolean, val likeCount: Int)

/** 지도 설명 화면이 한 번에 받아오는 장소 목록. */
@Immutable
data class MapPlacePreview(
    val places: List<MapPlace> = emptyList(),
    /** 보여준 것 말고 더 있는지. 목록 아래 `더보기` 를 띄울지 정한다. */
    val hasMore: Boolean = false,
)
