package com.moamap.app.feature.mapdetail

import androidx.compose.runtime.Composable
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMapComposable
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location

/**
 * 지도 위 내 위치. 다른 지도 앱처럼 파란 점에 휴대폰이 향한 방향을 함께 그리고, 내가 움직이면 따라
 * 움직인다(10-06 사용자 결정 - 지도 라이브러리 기본 그림). 지도를 그리는 모든 화면이 `MapboxMap` 안에서 부른다.
 *
 * 위치 권한이 있을 때만 켠다([enabled]). 권한이 없으면 위치를 받을 수 없어 그릴 것도 없다. 위치는 지도가
 * 화면에 있는 동안만 받는다 - 지도 라이브러리가 화면 수명에 맞춰 멈추고 다시 켠다.
 */
@Composable
@MapboxMapComposable
internal fun MyLocationPuck(enabled: Boolean) {
    MapEffect(enabled) { mapView ->
        mapView.location.updateSettings {
            this.enabled = enabled
            locationPuck = createDefault2DPuck(withBearing = true)
            puckBearingEnabled = true
            puckBearing = PuckBearing.HEADING
        }
    }
}
