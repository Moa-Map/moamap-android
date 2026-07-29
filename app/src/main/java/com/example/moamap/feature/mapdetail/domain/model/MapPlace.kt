package com.example.moamap.feature.mapdetail.domain.model

import androidx.compose.runtime.Immutable

/**
 * 지도에 등록된 장소 한 건.
 *
 * 지금은 지도 설명 화면의 미리보기(목록 몇 건 + 지도 위 마커)만 쓴다. 상세 화면의
 * 바텀시트·마커는 아직 목데이터라 이 모델을 쓰지 않는다.
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
)

/** 지도 설명 화면이 한 번에 받아오는 장소 목록. */
@Immutable
data class MapPlacePreview(
    val places: List<MapPlace> = emptyList(),
    /** 보여준 것 말고 더 있는지. 목록 아래 `더보기` 를 띄울지 정한다. */
    val hasMore: Boolean = false,
)
