package com.example.moamap.feature.mapdetail

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.example.moamap.R
import com.mapbox.geojson.Point

/** 숭실대 캠퍼스 중심. 두 샘플 마커의 중점과 일치한다. */
internal val MapDetailCenter: Point = Point.fromLngLat(126.9574, 37.4963)

internal const val MapDetailDefaultZoom = 16.5

/** 3D 건물이 입체로 보이도록 카메라를 기울인다. */
internal const val MapDetailPitch = 55.0

/** 레퍼런스 디자인의 아이소메트릭 느낌을 위한 약간의 회전. */
internal const val MapDetailBearing = 20.0

/**
 * 지도에 사진 마커로 표시할 장소.
 *
 * 스파이크 단계라 좌표와 썸네일만 담는다. [placeId] 로 [SamplePlaces] 의
 * [PlaceUiModel] 과 느슨하게 연결되며, 마커를 탭하면 해당 장소 상세를 연다.
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
    @DrawableRes val thumbnailRes: Int,
)

/**
 * 숭실대 캠퍼스 기준 샘플 마커.
 *
 * 두 지점은 약 97m 떨어져 있어 초기 줌(16.5)에서는 따로 보이고
 * 줌 15.0 이하로 축소하면 하나의 Facepile 로 합쳐진다.
 */
internal val SamplePlaceMarkers = listOf(
    PlaceMarker(
        placeId = 1L,
        name = "커피나무",
        longitude = 126.9570,
        latitude = 37.4960,
        thumbnailRes = R.drawable.img_sample_place_1,
    ),
    PlaceMarker(
        placeId = 2L,
        name = "달빛정원",
        longitude = 126.9578,
        latitude = 37.4966,
        thumbnailRes = R.drawable.img_sample_place_2,
    ),
)
