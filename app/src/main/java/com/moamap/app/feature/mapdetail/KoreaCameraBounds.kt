package com.moamap.app.feature.mapdetail

import androidx.compose.runtime.Composable
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraBoundsOptions
import com.mapbox.maps.CoordinateBounds
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMapComposable

/**
 * 지도 화면이 벗어날 수 없는 범위. 앱 대상이 국내라 전 세계로 나갈 일은 없지만, 국토에 딱 맞추면 조금만 끌어도
 * 막혀 부자연스럽다(사용자 피드백). 그래서 중국 동부·일본·대만까지 담는 동아시아로 넉넉하게 연다. iOS 는 아직 좁은 값이다.
 *
 * Mapbox 범위는 설명과 달리 카메라 중심이 아니라 화면에 보이는 영역 전체를 가둔다(11.24 실측). 화면이 범위보다
 * 커지려 하면 지도가 줌을 올려 버린다.
 */
internal val KoreaCameraSouthwest: Point = Point.fromLngLat(110.0, 18.0)
internal val KoreaCameraNortheast: Point = Point.fromLngLat(152.0, 52.0)

/** 가장 멀리 축소하는 배율. 실제로는 화면이 범위를 넘지 않는 데서 먼저 멈춘다. */
internal const val KoreaCameraMinZoom = 3.0

/**
 * 카메라를 국내 범위([KoreaCameraSouthwest]~[KoreaCameraNortheast])와 최소 줌([KoreaCameraMinZoom]) 안에
 * 가둔다. 지도를 그리는 모든 화면이 `MapboxMap` 안에서 부른다.
 *
 * 범위 밖으로 가려는 카메라 이동(내 위치·마커 맞추기 등)은 따로 막지 않는다. 지도가 가장자리에서 멈춘다.
 */
@Composable
@MapboxMapComposable
internal fun KoreaCameraBounds() {
    MapEffect(Unit) { mapView ->
        mapView.mapboxMap.setBounds(
            CameraBoundsOptions.Builder()
                .bounds(CoordinateBounds(KoreaCameraSouthwest, KoreaCameraNortheast))
                .minZoom(KoreaCameraMinZoom)
                .build(),
        )
    }
}
