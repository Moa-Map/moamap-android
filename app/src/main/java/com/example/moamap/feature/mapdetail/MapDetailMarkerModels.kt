package com.example.moamap.feature.mapdetail

import androidx.compose.runtime.Immutable
import com.example.moamap.feature.mapdetail.domain.model.MapPlace
import com.mapbox.geojson.Point

/** 장소도 위치도 없을 때 마지막으로 기대는 좌표. 숭실대 캠퍼스 중심이다. */
internal val MapDetailCenter: Point = Point.fromLngLat(126.9574, 37.4963)

/** 동네 몇 개가 한눈에 들어오는 배율. 처음 열었을 때 마커를 찾아다니지 않아도 된다. */
internal const val MapDetailDefaultZoom = 14.0

/** 3D 로 토글했을 때만 쓰는 기울기. 건물이 입체로 보이게 한다. */
internal const val MapDetailPitch = 55.0

/**
 * 지도에 사진 마커로 표시할 장소.
 *
 * 클러스터링이 Compose 람다 메모이제이션에 의존하므로 반드시 값 동등성을
 * 만족해야 한다. `@Immutable` 을 제거하면 안 된다.
 */
@Immutable
internal data class PlaceMarker(
    val placeId: Long,
    val name: String,
    val longitude: Double,
    val latitude: Double,
    /** 장소 사진. 없으면 마커에 플레이스홀더를 띄운다. */
    val photoUrl: String?,
)

internal fun MapPlace.toPlaceMarker(): PlaceMarker = PlaceMarker(
    placeId = id,
    name = name,
    longitude = longitude,
    latitude = latitude,
    photoUrl = photoUrl,
)

/** 미리보기 전용 마커. 프리뷰는 네트워크를 타지 않아 사진 자리는 플레이스홀더로 뜬다. */
internal val PreviewPlaceMarkers = listOf(
    PlaceMarker(
        placeId = 1L,
        name = "커피나무",
        longitude = 126.9570,
        latitude = 37.4960,
        photoUrl = null,
    ),
    PlaceMarker(
        placeId = 2L,
        name = "달빛정원",
        longitude = 126.9578,
        latitude = 37.4966,
        photoUrl = null,
    ),
)
