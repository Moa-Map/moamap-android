package com.example.moamap.feature.mapdetail

import androidx.compose.runtime.Immutable
import com.mapbox.geojson.Point
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

/** Mapbox 타일 한 변의 크기(dp). 월드 크기 = TileSizeDp * 2^zoom */
private const val TileSizeDp = 512.0

/** 웹 메르카토르 위도 한계. 이 밖에서는 투영이 발산한다. */
private const val MaxMercatorLatitude = 85.05112878

/** 마커 버블 지름(56dp) + 최소 여백(16dp). 이보다 가까우면 하나로 묶는다. */
internal const val ClusterThresholdDp = 72.0

/** 경도를 해당 줌의 월드 픽셀 x 로 투영한다. */
internal fun worldPixelX(longitude: Double, zoom: Double): Double =
    (longitude + 180.0) / 360.0 * TileSizeDp * 2.0.pow(zoom)

/** 위도를 해당 줌의 월드 픽셀 y 로 투영한다. */
internal fun worldPixelY(latitude: Double, zoom: Double): Double {
    val clamped = latitude.coerceIn(-MaxMercatorLatitude, MaxMercatorLatitude)
    val radians = clamped * PI / 180.0
    val mercator = ln(tan(PI / 4.0 + radians / 2.0))
    return (1.0 - mercator / PI) / 2.0 * TileSizeDp * 2.0.pow(zoom)
}

/** [worldPixelX] 의 역. 화면 모서리 픽셀을 경도로 되돌릴 때 쓴다. */
internal fun longitudeAtWorldPixelX(x: Double, zoom: Double): Double =
    x / (TileSizeDp * 2.0.pow(zoom)) * 360.0 - 180.0

/** [worldPixelY] 의 역. 화면 모서리 픽셀을 위도로 되돌릴 때 쓴다. */
internal fun latitudeAtWorldPixelY(y: Double, zoom: Double): Double {
    val mercator = (1.0 - 2.0 * y / (TileSizeDp * 2.0.pow(zoom))) * PI
    return (2.0 * atan(exp(mercator)) - PI / 2.0) * 180.0 / PI
}

/** 두 마커가 화면에서 얼마나 떨어져 보이는지(dp). */
internal fun screenDistanceDp(
    first: PlaceMarker,
    second: PlaceMarker,
    zoom: Double,
): Double = hypot(
    worldPixelX(first.longitude, zoom) - worldPixelX(second.longitude, zoom),
    worldPixelY(first.latitude, zoom) - worldPixelY(second.latitude, zoom),
)

/**
 * 화면 거리 기준으로 가까워서 하나로 묶인 마커 묶음.
 */
@Immutable
internal data class MarkerCluster(
    val members: List<PlaceMarker>,
) {
    /**
     * ViewAnnotation 의 Compose key.
     *
     * 시드(첫 구성원)의 placeId 를 쓴다. 구성 전체를 이어붙이면 하나라도 붙거나 떨어질
     * 때마다 key 가 달라져, 지도 위의 ViewAnnotation(안드로이드 View)이 통째로 버려지고
     * 새로 만들어진다. 안에 걸린 사진 요청도 함께 처음부터 다시 시작한다.
     *
     * [clusterMarkers] 가 입력 순서를 지키는 그리디라, 두 묶음이 합쳐지면 앞선 시드가
     * 그대로 시드가 되고 갈라지면 원래 시드가 남는다. 그래서 통합·분리를 거쳐도 묶음
     * 하나는 key 를 유지하고, 그 View 와 사진이 살아남는다.
     *
     * 마커는 한 묶음에만 속하므로 같은 화면 안에서 값이 겹치지 않는다.
     */
    val id: Long = members.first().placeId

    val isSingle: Boolean get() = members.size == 1
}

/** 클러스터를 지도에 고정할 좌표. 구성원의 평균 위치를 쓴다. */
internal fun MarkerCluster.anchorPoint(): Point = Point.fromLngLat(
    members.sumOf { member -> member.longitude } / members.size,
    members.sumOf { member -> member.latitude } / members.size,
)

/**
 * 화면 거리 기준 그리디 클러스터링.
 *
 * 입력 순서를 유지하므로 같은 입력이면 항상 같은 결과가 나온다. 이 결정성이
 * [MarkerCluster.id] 안정성의 근거이고, 나아가 ViewAnnotation 재생성을 막는다.
 *
 * 투영을 마커마다 한 번만 한다. [screenDistanceDp] 를 쌍마다 부르면 pow·ln·tan 이
 * 쌍의 수만큼, 즉 마커 수의 제곱으로 늘어난다. 거리도 제곱끼리 견주어 sqrt 를 뺀다.
 * 묶는 기준과 결과는 그대로다.
 *
 * 카메라 pitch 와 bearing 은 무시한다. 기울인 화면에서 지평선 쪽 마커는 실제보다
 * 가깝게 보이지만, 이번 스파이크 규모에서는 무시할 수 있는 오차다.
 */
internal fun clusterMarkers(
    markers: List<PlaceMarker>,
    zoom: Double,
    thresholdDp: Double = ClusterThresholdDp,
): List<MarkerCluster> {
    val xs = DoubleArray(markers.size) { index -> worldPixelX(markers[index].longitude, zoom) }
    val ys = DoubleArray(markers.size) { index -> worldPixelY(markers[index].latitude, zoom) }
    // 묶인 마커를 리스트에서 지우는 대신 표시만 한다. 지우면 뒤쪽이 통째로 밀린다.
    val taken = BooleanArray(markers.size)
    val thresholdSquared = thresholdDp * thresholdDp
    val clusters = mutableListOf<MarkerCluster>()

    for (seed in markers.indices) {
        if (taken[seed]) continue
        taken[seed] = true
        val members = mutableListOf(markers[seed])

        for (candidate in seed + 1 until markers.size) {
            if (taken[candidate]) continue
            val dx = xs[seed] - xs[candidate]
            val dy = ys[seed] - ys[candidate]
            if (dx * dx + dy * dy <= thresholdSquared) {
                taken[candidate] = true
                members += markers[candidate]
            }
        }

        clusters += MarkerCluster(members = members)
    }

    return clusters
}
