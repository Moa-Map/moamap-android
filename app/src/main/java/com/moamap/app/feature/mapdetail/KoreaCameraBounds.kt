package com.moamap.app.feature.mapdetail

import androidx.compose.runtime.Composable
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraBoundsOptions
import com.mapbox.maps.CoordinateBounds
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMapComposable

/**
 * 지도 화면이 벗어날 수 없는 국내 범위. 앱 대상이 국내라 전 세계로 나갈 일이 없다. iOS 와 같은 값이다.
 *
 * Mapbox 범위는 설명과 달리 카메라 중심이 아니라 화면에 보이는 영역 전체를 가둔다(11.24 실측). 화면이 범위보다
 * 커지려 하면 지도가 줌을 올려 버린다. 그래서 범위는 국토보다 넉넉하다.
 * - 남북은 360dp 세로로 긴 폰(20:9)이 국토 동서 폭을 다 담는 배율에서 화면 높이만큼(위도 17도쯤) 열어 둔다.
 * - 동서는 지도 소개 미리보기처럼 가로로 긴 작은 지도가 국토 남북을 다 담는 배율에서 화면 폭만큼 열어 둔다.
 */
internal val KoreaCameraSouthwest: Point = Point.fromLngLat(122.5, 26.9)
internal val KoreaCameraNortheast: Point = Point.fromLngLat(134.0, 44.0)

/** 지도 소개 미리보기(가로로 긴 작은 지도)에서 국토 남북이 다 들어오는 배율. 세로 폰은 범위 남북 폭에 막혀 덜 줄어든다. */
internal const val KoreaCameraMinZoom = 4.4

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
