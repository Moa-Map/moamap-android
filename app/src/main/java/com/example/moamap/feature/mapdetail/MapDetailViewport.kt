package com.example.moamap.feature.mapdetail

import androidx.compose.runtime.Immutable

/**
 * 경계에 더하는 여유. 폭·높이의 비율이다.
 *
 * [viewportBounds] 가 pitch 와 bearing 을 무시하기 때문에 필요하다. 3D 로 기울이면 실제
 * 보이는 영역이 사다리꼴로 넓어져, 여유가 없으면 지평선 쪽 마커가 잘못 잘린다. 화면을
 * 조금 움직였을 때 마커가 곧바로 사라지는 것도 이 여유가 막는다.
 */
internal const val ViewportCullMargin = 0.3

/**
 * 컬링에 쓰는 사각 경계.
 *
 * Mapbox `CoordinateBounds` 를 쓰지 않는다. 그쪽은 네이티브 메서드라 JVM 테스트에서
 * 만들 수가 없다.
 */
@Immutable
internal data class ViewportBounds(
    val west: Double,
    val south: Double,
    val east: Double,
    val north: Double,
)

/**
 * 카메라와 뷰 크기로 화면에 담기는 경계를 되돌린다.
 *
 * pitch 와 bearing 은 셈에 넣지 않는다. [marginRatio] 가 그 오차를 흡수한다.
 *
 * 날짜변경선을 걸치는 경우는 다루지 않는다. 국내 서비스라 나오지 않는다.
 */
internal fun viewportBounds(
    centerLongitude: Double,
    centerLatitude: Double,
    zoom: Double,
    widthDp: Double,
    heightDp: Double,
    marginRatio: Double = ViewportCullMargin,
): ViewportBounds {
    val halfWidth = widthDp / 2.0 * (1.0 + marginRatio)
    val halfHeight = heightDp / 2.0 * (1.0 + marginRatio)

    val centerX = worldPixelX(centerLongitude, zoom)
    val centerY = worldPixelY(centerLatitude, zoom)

    return ViewportBounds(
        west = longitudeAtWorldPixelX(centerX - halfWidth, zoom),
        east = longitudeAtWorldPixelX(centerX + halfWidth, zoom),
        // 월드 픽셀 y 는 아래로 갈수록 커진다. 위도와 반대다.
        north = latitudeAtWorldPixelY(centerY - halfHeight, zoom),
        south = latitudeAtWorldPixelY(centerY + halfHeight, zoom),
    )
}

/**
 * 경계 안의 마커만 남긴다.
 *
 * 클러스터링 앞에 둔다. 화면 밖 마커까지 클러스터가 되면 그 수만큼 `ViewAnnotation`
 * (안드로이드 View)이 지도 위에 올라간다. 장소가 수백 개인 지도에서 그대로 두면 화면이
 * 버틴다.
 *
 * 입력 순서를 지킨다. `clusterMarkers` 의 결정성이, 나아가 `MarkerCluster.id` 안정성이
 * 여기에 달려 있다.
 */
internal fun cullToViewport(
    markers: List<PlaceMarker>,
    bounds: ViewportBounds,
): List<PlaceMarker> = markers.filter { marker ->
    marker.longitude in bounds.west..bounds.east &&
        marker.latitude in bounds.south..bounds.north
}
