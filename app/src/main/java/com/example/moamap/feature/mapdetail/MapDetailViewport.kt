package com.example.moamap.feature.mapdetail

import androidx.compose.runtime.Immutable
import kotlin.math.floor

/**
 * 경계에 더하는 여유. 폭·높이의 비율이다.
 *
 * [viewportBounds] 가 pitch 와 bearing 을 무시하기 때문에 필요하다. 3D 로 기울이면 실제
 * 보이는 영역이 사다리꼴로 넓어져, 여유가 없으면 지평선 쪽 마커가 잘못 잘린다. 화면을
 * 조금 움직였을 때 마커가 곧바로 사라지는 것도 이 여유가 막는다.
 */
internal const val ViewportCullMargin = 0.3

/**
 * 컬링 경계를 다시 계산하는 중심 이동 간격(dp).
 *
 * 카메라 중심을 이 격자에 맞춰 내린 값으로 경계를 잡는다. 팬 도중 매 프레임 다시 세지 않게
 * 하려는 것이다. 그만큼 경계가 실제 화면에서 어긋나므로, 어긋남을 마진([ViewportCullMargin])
 * 이 덮을 수 있어야 한다.
 *
 * 도가 아니라 화면 거리(dp)로 잡는 게 핵심이다. 마진은 화면 크기의 비율, 곧 화면 거리다.
 * 도 단위 상수(0.001도, 약 100m)로 두었을 때는 확대하면 마커가 통째로 사라졌다. 어긋남만
 * 도 단위로 고정이라 확대할수록 화면에서 커졌기 때문이다. 393dp 폭 화면 기준으로 이랬다.
 *
 * ```
 * zoom | 어긋남   | 마진   | 화면 한복판 마커
 *   14 |   13.3dp | 58.9dp | 보임
 *   16 |   53.4dp | 58.9dp | 보임
 *   19 |  427.1dp | 58.9dp | 사라짐
 *   21 | 1708.5dp | 58.9dp | 사라짐
 * ```
 *
 * 화면 거리로 잡으면 어느 줌에서든 어긋남이 이 값으로 묶인다. 마진보다 넉넉히 작게 둔다 -
 * 위도는 메르카토르 때문에 화면 거리로 1.26배가 되니 그것까지 담아야 한다.
 *
 * 낮은 줌에서는 덤으로 재계산이 줄어든다. 0.001도는 zoom 10 에서 1.5dp 라, 손가락을 조금만
 * 움직여도 컬링과 클러스터링을 다시 돌렸다.
 */
internal const val CenterStepDp = 16.0

/**
 * 카메라 중심 좌표를 [CenterStepDp] 격자에 맞춰 내린다.
 *
 * 경도와 위도에 같은 함수를 쓴다. 위도 쪽 어긋남이 화면 거리로 1.26배가 되지만
 * ([degreesPerDp] 참고) 마진이 그걸 담을 만큼 넉넉하다.
 */
internal fun quantizeCenter(degrees: Double, zoom: Double): Double {
    val step = CenterStepDp * degreesPerDp(zoom)
    return floor(degrees / step) * step
}

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
