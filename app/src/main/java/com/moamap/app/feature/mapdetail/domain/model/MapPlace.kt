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
    /**
     * 카카오 분류 경로. `"음식점 > 카페 > 커피전문점"` 처럼 온다.
     *
     * 경로 그대로 담는다. 화면에 띄울 땐 [categoryLabel] 로 마지막 토막만 꺼내 쓴다.
     */
    val category: String = "",
    /** 평균 평점. 아직 아무도 안 매겼으면 0.0 이다. */
    val rating: Double = 0.0,
    /** 댓글 수. */
    val reviewCount: Int = 0,
)

/**
 * 분류 경로에서 화면에 띄울 한 토막.
 *
 * 경로를 통째로 띄우면 상세 시트의 작은 알약을 넘겨 버린다. 가장 구체적인 마지막
 * 토막(`"커피전문점"`)이 그 자리에 맞고 사람이 읽기에도 낫다.
 */
val MapPlace.categoryLabel: String
    get() = category.split(">").lastOrNull()?.trim().orEmpty()

/**
 * 주소에서 지역 이름 한 토막.
 *
 * `"서울 성동구 성수이로 12"` 에서 `"성동구"` 를 꺼낸다. 상세 시트가 분류 알약 옆에
 * 지역을 함께 보여 주는데 서버에 그런 필드가 따로 없다.
 */
val MapPlace.areaLabel: String
    get() = address.split(" ").getOrNull(1).orEmpty()

/** 지도 설명 화면이 한 번에 받아오는 장소 목록. */
@Immutable
data class MapPlacePreview(
    val places: List<MapPlace> = emptyList(),
    /** 보여준 것 말고 더 있는지. 목록 아래 `더보기` 를 띄울지 정한다. */
    val hasMore: Boolean = false,
)
