package com.example.moamap.feature.mapdetail

import androidx.compose.runtime.Immutable
import com.mapbox.geojson.Point
import kotlin.math.PI
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
 *
 * [id] 는 ViewAnnotation 의 Compose key 로 쓰이므로 구성이 바뀔 때만 바뀌어야 한다.
 */
@Immutable
internal data class MarkerCluster(
    val members: List<PlaceMarker>,
) {
    val id: String = members.joinToString(separator = "-") { member -> member.placeId.toString() }

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
 * 카메라 pitch 와 bearing 은 무시한다. 기울인 화면에서 지평선 쪽 마커는 실제보다
 * 가깝게 보이지만, 이번 스파이크 규모에서는 무시할 수 있는 오차다.
 */
internal fun clusterMarkers(
    markers: List<PlaceMarker>,
    zoom: Double,
    thresholdDp: Double = ClusterThresholdDp,
): List<MarkerCluster> {
    val remaining = markers.toMutableList()
    val clusters = mutableListOf<MarkerCluster>()

    while (remaining.isNotEmpty()) {
        val seed = remaining.removeAt(0)
        val members = mutableListOf(seed)
        val candidates = remaining.iterator()

        while (candidates.hasNext()) {
            val candidate = candidates.next()
            if (screenDistanceDp(seed, candidate, zoom) <= thresholdDp) {
                members += candidate
                candidates.remove()
            }
        }

        clusters += MarkerCluster(members = members)
    }

    return clusters
}
