package com.moamap.app.feature.mapdetail

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import com.mapbox.geojson.Point
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMapComposable
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.animation.MapAnimationOptions
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

internal val LocationPermissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

internal fun hasLocationPermission(context: Context): Boolean =
    LocationPermissions.any { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

/** 「내 위치」 버튼 안내. 지도 상세와 화장실 지도가 같이 쓴다. */
internal const val LOCATION_PERMISSION_DENIED_MESSAGE = "위치 권한이 없어 현재 위치를 찾을 수 없어요"
internal const val MY_LOCATION_NOT_FOUND_MESSAGE = "현재 위치를 찾지 못했어요"

/**
 * 「내 위치」 버튼으로 찾은 [point] 로 카메라를 옮긴다. 지도 상세와 화장실 지도가 같이 쓴다.
 *
 * pitch 는 건드리지 않는다. 2D 로 보던 사람의 지도가 버튼 하나에 기울어지면 안 된다.
 */
internal fun MapViewportState.easeToMyLocation(point: Point) {
    easeTo(
        cameraOptions {
            center(point)
            zoom(myLocationZoom(cameraState?.zoom))
        },
        MapAnimationOptions.mapAnimationOptions { duration(600L) },
    )
}
