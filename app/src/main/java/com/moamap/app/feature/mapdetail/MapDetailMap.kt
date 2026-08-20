package com.moamap.app.feature.mapdetail

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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

// 중심 양자화는 컬링 마진과 짝지어 봐야 해서 MapDetailViewport 에 있다. quantizeCenter 참고.

/**
 * 클러스터링에 넣을 카메라 값을 양자화해 담은 것.
 *
 * `derivedStateOf` 는 리컴포지션만 막지 람다 실행은 막지 못한다. 값이 달라졌는지
 * 알아내려면 돌려 보는 수밖에 없어서다. 그래서 컬링과 클러스터링을 그 안에 두면
 * 팬·줌 매 프레임마다 그 비용을 치른다. 여기에는 floor 몇 번짜리 이 값만 두고,
 * 비싼 셈은 이 값이 실제로 달라질 때만 도는 `remember` 로 넘긴다.
 *
 * 카메라가 아직 없으면 중심이 null 이다. 그때는 컬링을 건너뛴다.
 */
@Immutable
private data class ClusterCameraKey(
    val zoom: Double,
    val centerLongitude: Double?,
    val centerLatitude: Double?,
)

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

        // 카메라를 바디에서 직접 읽으면 팬·줌·회전 매 프레임마다 리컴포지션된다.
        // derivedStateOf 로 감싸 결과가 실제로 달라질 때만 리컴포지션되게 한다.
        // 여기 담기는 일은 floor 몇 번뿐이다. 이 람다는 매 프레임 도니까.
        val cameraKey by remember(mapViewportState) {
            derivedStateOf {
                val camera = mapViewportState.cameraState
                val center = camera?.center
                val zoom = floor((camera?.zoom ?: MapDetailDefaultZoom) / ClusterZoomStep) *
                    ClusterZoomStep
                ClusterCameraKey(
                    zoom = zoom,
                    // 경계도 이 줌으로 계산한다. 양자화에 쓰는 줌이 어긋나면 안 된다.
                    centerLongitude = center?.let { point ->
                        quantizeCenter(point.longitude(), zoom)
                    },
                    centerLatitude = center?.let { point ->
                        quantizeCenter(point.latitude(), zoom)
                    },
                )
            }
        }

        // 양자화한 카메라가 실제로 한 칸 움직였을 때만 다시 센다.
        val clusters = remember(markers, widthDp, heightDp, cameraKey) {
            val longitude = cameraKey.centerLongitude
            val latitude = cameraKey.centerLatitude
            val visible = if (longitude == null || latitude == null) {
                markers
            } else {
                cullToViewport(
                    markers = markers,
                    bounds = viewportBounds(
                        centerLongitude = longitude,
                        centerLatitude = latitude,
                        zoom = cameraKey.zoom,
                        widthDp = widthDp,
                        heightDp = heightDp,
                    ),
                )
            }

            clusterMarkers(visible, cameraKey.zoom)
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
