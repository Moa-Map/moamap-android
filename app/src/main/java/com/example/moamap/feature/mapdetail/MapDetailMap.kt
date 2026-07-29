package com.example.moamap.feature.mapdetail

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.mapbox.maps.ViewAnnotationAnchor
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.extension.compose.style.BooleanValue
import com.mapbox.maps.extension.compose.style.standard.LightPresetValue
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import com.mapbox.maps.extension.compose.style.standard.rememberStandardStyleState
import com.mapbox.maps.viewannotation.annotationAnchor
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import kotlin.math.floor

/**
 * 클러스터를 다시 계산하는 줌 간격.
 *
 * 카메라 줌은 제스처 도중 매 프레임 바뀌므로 그대로 쓰면 클러스터 목록이
 * 프레임마다 새로 만들어진다. 0.25 단위로 양자화해 재계산을 눈에 띄는
 * 변화가 있을 때로 제한한다.
 */
private const val ClusterZoomStep = 0.25

/**
 * 경계를 다시 계산하는 좌표 간격(도).
 *
 * 줌과 같은 이유로 양자화한다. 약 100m 로, 컬링 마진([ViewportCullMargin])이 이보다
 * 훨씬 넓어 잘린 마커가 튀어나오지 않는다.
 */
private const val CenterStep = 0.001

@Composable
internal fun MapDetailMap(
    mapViewportState: MapViewportState,
    markers: List<PlaceMarker>,
    is3d: Boolean,
    onMarkerClick: (Long) -> Unit,
    onClusterClick: (MarkerCluster) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val widthDp = maxWidth.value.toDouble()
        val heightDp = maxHeight.value.toDouble()

        // derivedStateOf 로 감싸야 카메라가 바뀔 때마다가 아니라 결과가 실제로 달라질 때만
        // 이 컴포저블이 리컴포지션된다. cameraState 를 바디에서 직접 읽으면 팬/줌/회전 매
        // 프레임마다 리컴포지션된다.
        val clusters by remember(markers, widthDp, heightDp) {
            derivedStateOf {
                val camera = mapViewportState.cameraState
                val rawZoom = camera?.zoom ?: MapDetailDefaultZoom
                val zoom = floor(rawZoom / ClusterZoomStep) * ClusterZoomStep

                val center = camera?.center
                val visible = if (center == null) {
                    markers
                } else {
                    cullToViewport(
                        markers = markers,
                        bounds = viewportBounds(
                            centerLongitude = floor(center.longitude() / CenterStep) * CenterStep,
                            centerLatitude = floor(center.latitude() / CenterStep) * CenterStep,
                            zoom = zoom,
                            widthDp = widthDp,
                            heightDp = heightDp,
                        ),
                    )
                }

                clusterMarkers(visible, zoom)
            }
        }

        val standardStyleState = rememberStandardStyleState {
            configurationsState.lightPreset = LightPresetValue.DAY
            configurationsState.show3dObjects = BooleanValue(is3d)
        }
        LaunchedEffect(is3d) {
            standardStyleState.configurationsState.show3dObjects = BooleanValue(is3d)
        }

        MapboxMap(
            modifier = Modifier.matchParentSize(),
            mapViewportState = mapViewportState,
            // 축척과 나침반을 띄우지 않는다. 로고와 저작권 표시는 약관상 남긴다.
            compass = {},
            scaleBar = {},
            style = {
                MapboxStandardStyle(standardStyleState = standardStyleState)
            },
        ) {
            clusters.forEach { cluster ->
                key(cluster.id) {
                    ViewAnnotation(
                        options = viewAnnotationOptions {
                            geometry(cluster.anchorPoint())
                            allowOverlap(true)
                            annotationAnchor { anchor(ViewAnnotationAnchor.BOTTOM) }
                        },
                    ) {
                        if (cluster.isSingle) {
                            val marker = cluster.members.first()
                            PlacePhotoMarker(
                                marker = marker,
                                onClick = { onMarkerClick(marker.placeId) },
                            )
                        } else {
                            PlaceFacepileMarker(
                                cluster = cluster,
                                onClick = { onClusterClick(cluster) },
                            )
                        }
                    }
                }
            }
        }
    }
}
