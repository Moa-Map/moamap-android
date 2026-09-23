package com.moamap.app.feature.mapdetail.presentation.intro

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.moamap.app.feature.mapdetail.InitialCamera
import com.moamap.app.feature.mapdetail.MapDetailCenter
import com.moamap.app.feature.mapdetail.PlaceMarkerAnnotations
import com.moamap.app.feature.mapdetail.rememberMarkerClusters
import com.moamap.app.feature.mapdetail.toPlaceMarker
import com.moamap.app.feature.mapdetail.initialCamera
import com.moamap.app.feature.mapdetail.domain.model.MapPlace
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.rememberMapState
import com.mapbox.maps.extension.compose.style.BooleanValue
import com.mapbox.maps.extension.compose.style.standard.LightPresetValue
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import com.mapbox.maps.extension.compose.style.standard.rememberStandardStyleState
import com.mapbox.maps.plugin.gestures.generated.GesturesSettings

/** 장소가 하나뿐이거나 없을 때 쓰는 줌. 여러 곳이면 전부 담기도록 따로 맞춘다. */
private const val IntroMapZoom = 14.0

/** 마커가 가장자리에 붙지 않도록 카메라에 두는 여백. */
private val IntroFitPadding = 32.dp

/**
 * 설명 화면의 지도 미리보기.
 *
 * 참여 전에도 이 지도에 무엇이 모여 있는지 볼 수 있어야 해서 이동·확대를 연다. 회전과 기울기는
 * 닫아 둔다 - 돌려 볼 일이 없고, 한번 기울면 되돌릴 버튼이 이 화면에 없다.
 *
 * 지도를 누르면 상세로 가던 동작은 없앴다. 이제 지도를 끄는 손짓과 부딪힌다 - 상세로 가는 길은
 * 지도 위의 「미리보기」 버튼이다.
 *
 * 마커는 상세와 같은 사진 마커를 쓰고, 겹치면 상세처럼 묶어 보여준다. 누르는 동작만 없다 -
 * 참여 전에는 장소 상세를 열지 않는다.
 */
@Composable
internal fun MapIntroMap(
    places: List<MapPlace>,
    modifier: Modifier = Modifier,
) {
    val mapViewportState = rememberMapViewportState()
    val mapState = rememberMapState {
        gesturesSettings = GesturesSettings {
            rotateEnabled = false
            pitchEnabled = false
        }
    }
    val standardStyleState = rememberStandardStyleState {
        configurationsState.lightPreset = LightPresetValue.DAY
        configurationsState.show3dObjects = BooleanValue(false)
    }

    val fitPadding = with(LocalDensity.current) {
        val padding = IntroFitPadding.toPx().toDouble()
        EdgeInsets(padding, padding, padding, padding)
    }

    /** 장소는 지도가 붙은 뒤에 도착한다. 처음 한 번만 맞추고, 그다음은 사용자가 움직인 대로 둔다. */
    var cameraSettled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(places) {
        if (cameraSettled || places.isEmpty()) return@LaunchedEffect

        when (val camera = initialCamera(places, deviceLocation = null)) {
            is InitialCamera.Fit -> mapViewportState.setCameraOptions(
                mapViewportState.cameraForCoordinates(
                    coordinates = camera.points,
                    coordinatesPadding = fitPadding,
                    maxZoom = IntroMapZoom,
                ),
            )

            is InitialCamera.Center -> mapViewportState.setCameraOptions {
                center(camera.point)
                zoom(IntroMapZoom)
            }
        }
        cameraSettled = true
    }

    // 장소가 하나도 없으면 맞출 대상이 없다. 기본 좌표를 잡아 빈 지도라도 자리를 지킨다.
    LaunchedEffect(Unit) {
        if (places.isEmpty()) {
            mapViewportState.setCameraOptions {
                center(MapDetailCenter)
                zoom(IntroMapZoom)
            }
        }
    }

    val markers = remember(places) { places.map { place -> place.toPlaceMarker() } }

    BoxWithConstraints(modifier = modifier) {
        val widthDp = maxWidth.value.toDouble()
        val heightDp = maxHeight.value.toDouble()
        val clusters = rememberMarkerClusters(
            mapViewportState = mapViewportState,
            markers = markers,
            widthDp = widthDp,
            heightDp = heightDp,
        )

        MapboxMap(
            modifier = Modifier.matchParentSize(),
            mapViewportState = mapViewportState,
            mapState = mapState,
            // 축척과 나침반을 띄우지 않는다. 로고와 저작권 표시는 약관상 남긴다.
            compass = {},
            scaleBar = {},
            style = { MapboxStandardStyle(standardStyleState = standardStyleState) },
        ) {
            PlaceMarkerAnnotations(
                clusters = clusters,
                // 참여 전에는 장소 상세도, 묶음 펼치기도 열지 않는다. 그릴 뿐이다.
                onMarkerClick = {},
                onClusterClick = {},
            )
        }
    }
}
