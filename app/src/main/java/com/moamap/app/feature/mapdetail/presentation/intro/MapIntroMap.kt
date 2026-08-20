package com.moamap.app.feature.mapdetail.presentation.intro

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.feature.mapdetail.MapDetailCenter
import com.moamap.app.feature.mapdetail.domain.model.MapPlace
import com.mapbox.geojson.Point
import com.mapbox.maps.ViewAnnotationAnchor
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.extension.compose.rememberMapState
import com.mapbox.maps.extension.compose.style.BooleanValue
import com.mapbox.maps.extension.compose.style.standard.LightPresetValue
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import com.mapbox.maps.extension.compose.style.standard.rememberStandardStyleState
import com.mapbox.maps.plugin.gestures.generated.GesturesSettings
import com.mapbox.maps.viewannotation.annotationAnchor
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions

/** 장소들이 한눈에 들어오도록 잡는 줌. 상세(16.5)보다 한 단계 넓게 본다. */
private const val IntroMapZoom = 14.0

/**
 * 설명 화면의 지도 미리보기.
 *
 * 상세와 같은 Mapbox 스타일을 쓰되 조작할 수 없다. 이 영역은 탭하면 상세로 넘어가는
 * 자리이고, 세로 스크롤 안에 들어 있어 제스처를 살려 두면 스크롤 도중 지도가 끌려간다.
 *
 * 3D 를 끄는 이유도 같다 - 기울인 시점은 돌려볼 수 있을 때나 쓸모가 있다.
 */
@Composable
internal fun MapIntroMap(
    places: List<MapPlace>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val center = places.centerPoint()
    // 장소를 늦게 받아오므로, 중심이 바뀌면 카메라를 새 좌표로 다시 잡아야 한다.
    val mapViewportState = rememberMapViewportState(key = center.toJson()) {
        setCameraOptions {
            center(center)
            zoom(IntroMapZoom)
        }
    }
    val mapState = rememberMapState {
        gesturesSettings = GesturesSettings {
            scrollEnabled = false
            pinchToZoomEnabled = false
            pinchScrollEnabled = false
            rotateEnabled = false
            pitchEnabled = false
            quickZoomEnabled = false
            doubleTapToZoomInEnabled = false
            doubleTouchToZoomOutEnabled = false
        }
    }
    val standardStyleState = rememberStandardStyleState {
        configurationsState.lightPreset = LightPresetValue.DAY
        configurationsState.show3dObjects = BooleanValue(false)
    }

    Box(modifier = modifier) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState,
            mapState = mapState,
            style = { MapboxStandardStyle(standardStyleState = standardStyleState) },
        ) {
            places.forEach { place ->
                key(place.id) {
                    ViewAnnotation(
                        options = viewAnnotationOptions {
                            geometry(Point.fromLngLat(place.longitude, place.latitude))
                            allowOverlap(true)
                            annotationAnchor { anchor(ViewAnnotationAnchor.CENTER) }
                        },
                    ) {
                        MapIntroMarker()
                    }
                }
            }
        }

        // 지도 전체가 상세로 가는 탭 영역이다. 마커를 눌러도 같은 곳으로 간다.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick),
        )
    }
}

/** 장소 위치를 알리는 점. 사진 마커는 상세에서 쓰고 여기는 위치만 보이면 된다. */
@Composable
private fun MapIntroMarker(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(MoaMapPrimitiveColors.White)
            .padding(3.dp)
            .clip(CircleShape)
            .background(MoaMapPrimitiveColors.Blue500),
    )
}

/** 장소가 없으면 상세 화면과 같은 기본 좌표를 쓴다. 빈 바다를 보여줄 수는 없다. */
private fun List<MapPlace>.centerPoint(): Point =
    if (isEmpty()) {
        MapDetailCenter
    } else {
        Point.fromLngLat(
            sumOf { place -> place.longitude } / size,
            sumOf { place -> place.latitude } / size,
        )
    }
