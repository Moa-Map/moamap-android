package com.example.moamap.feature.mapdetail

import androidx.compose.runtime.Composable
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

@Composable
internal fun MapDetailMap(
    mapViewportState: MapViewportState,
    markers: List<PlaceMarker>,
    is3d: Boolean,
    onMarkerClick: (Long) -> Unit,
    onClusterClick: (MarkerCluster) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rawZoom = mapViewportState.cameraState?.zoom ?: MapDetailDefaultZoom
    val clusterZoom = floor(rawZoom / ClusterZoomStep) * ClusterZoomStep
    val clusters = remember(markers, clusterZoom) { clusterMarkers(markers, clusterZoom) }

    // 3D/2D 토글에 반응하도록, 초기화 블록이 아니라 매 리컴포지션마다 값을 반영한다.
    val standardStyleState = rememberStandardStyleState()
    standardStyleState.configurationsState.show3dObjects = BooleanValue(is3d)
    standardStyleState.configurationsState.lightPreset = LightPresetValue.DAY

    MapboxMap(
        modifier = modifier,
        mapViewportState = mapViewportState,
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
