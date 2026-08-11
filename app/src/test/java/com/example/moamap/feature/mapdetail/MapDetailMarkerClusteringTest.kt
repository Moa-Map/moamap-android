package com.example.moamap.feature.mapdetail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 두 샘플 마커가 따로 보이는 줌.
 *
 * 화면 기본 줌([MapDetailDefaultZoom])과 엮지 않는다. 이 테스트가 보는 건 거리에 따라
 * 묶이고 갈라지는 규칙이지 화면이 어떤 배율로 열리느냐가 아니다.
 */
private const val SeparatedZoom = 16.5

/** 두 샘플 마커가 하나로 묶이는 줌. */
private const val MergedZoom = 15.0

class MapDetailMarkerClusteringTest {

    @Test
    fun `충분히 확대하면 두 샘플 마커가 각각 표시된다`() {
        val clusters = clusterMarkers(PreviewPlaceMarkers, zoom = SeparatedZoom)

        assertEquals(2, clusters.size)
        assertTrue(clusters.all { cluster -> cluster.isSingle })
    }

    @Test
    fun `줌을 낮추면 두 마커가 하나의 클러스터로 묶인다`() {
        val clusters = clusterMarkers(PreviewPlaceMarkers, zoom = MergedZoom)

        assertEquals(1, clusters.size)
        assertEquals(2, clusters.first().members.size)
    }

    @Test
    fun `샘플 마커의 화면 거리는 갈라지는 줌에서 임계값보다 충분히 크다`() {
        val distance = screenDistanceDp(
            first = PreviewPlaceMarkers[0],
            second = PreviewPlaceMarkers[1],
            zoom = SeparatedZoom,
        )

        assertTrue("실제 거리=$distance", distance in 140.0..150.0)
        assertTrue(distance > ClusterThresholdDp)
    }

    @Test
    fun `클러스터 앵커는 시드 좌표 그대로다`() {
        val cluster = clusterMarkers(PreviewPlaceMarkers, zoom = MergedZoom).first()
        val anchor = cluster.anchorPoint()

        // 평균(126.9574, 37.4963)이 아니라 시드인 첫 구성원의 좌표다.
        assertEquals(PreviewPlaceMarkers[0].longitude, anchor.longitude(), 1e-9)
        assertEquals(PreviewPlaceMarkers[0].latitude, anchor.latitude(), 1e-9)
    }

    @Test
    fun `앵커끼리는 임계값보다 가까워지지 않는다`() {
        // 겹쳐서 아래 깔린 마커를 누를 수 없던 문제가 이 성질이 깨져 생겼다.
        // A - B 는 임계값 아래라 묶이고, C 는 A 로부터 임계값 위라 따로 남는 배치다.
        val zoom = 16.0
        val baseLongitude = 127.0
        fun longitudeOffsetBy(dp: Double) =
            longitudeAtWorldPixelX(worldPixelX(baseLongitude, zoom) + dp, zoom)

        val markers = listOf(
            PlaceMarker(1L, "A", baseLongitude, 37.5, null),
            PlaceMarker(2L, "B", longitudeOffsetBy(70.0), 37.5, null),
            PlaceMarker(3L, "C", longitudeOffsetBy(80.0), 37.5, null),
        )

        val clusters = clusterMarkers(markers, zoom)
        assertEquals(2, clusters.size)

        val gap = kotlin.math.abs(
            worldPixelX(clusters[0].anchorPoint().longitude(), zoom) -
                worldPixelX(clusters[1].anchorPoint().longitude(), zoom),
        )
        assertTrue("앵커 간격=$gap", gap >= ClusterThresholdDp)
    }

    @Test
    fun `클러스터 id는 구성이 같으면 동일하다`() {
        val first = clusterMarkers(PreviewPlaceMarkers, zoom = MergedZoom).first()
        val second = clusterMarkers(PreviewPlaceMarkers, zoom = MergedZoom).first()

        assertEquals(first.id, second.id)
        assertEquals(PreviewPlaceMarkers.first().placeId, first.id)
    }

    /**
     * ViewAnnotation 재사용의 근거.
     *
     * 묶이든 갈라지든 시드의 id 를 쓰는 묶음이 남아야, 지도 위의 View 와 그 안에 걸린
     * 사진이 버려지지 않고 이어진다.
     */
    @Test
    fun `묶이고 갈라져도 시드 마커의 클러스터 id는 남는다`() {
        val seedId = PreviewPlaceMarkers.first().placeId

        val separated = clusterMarkers(PreviewPlaceMarkers, zoom = SeparatedZoom)
        val merged = clusterMarkers(PreviewPlaceMarkers, zoom = MergedZoom)

        assertTrue(separated.any { cluster -> cluster.id == seedId })
        assertTrue(merged.any { cluster -> cluster.id == seedId })
    }

    /** id 는 Compose key 로 쓰이므로 같은 화면 안에서 유일해야 한다. */
    @Test
    fun `같은 화면 안에서 클러스터 id는 겹치지 않는다`() {
        val markers = GridMarkers

        listOf(13.0, 14.0, 15.0, 16.0, 17.0).forEach { zoom ->
            val ids = clusterMarkers(markers, zoom = zoom).map { cluster -> cluster.id }

            assertEquals("zoom=$zoom", ids.size, ids.toSet().size)
        }
    }

    /**
     * 투영을 미리 계산하고 거리를 제곱끼리 견주는 최적화가, 쌍마다
     * [screenDistanceDp] 를 부르던 원래 셈과 같은 묶음을 내는지 본다.
     */
    @Test
    fun `투영을 미리 계산해도 묶이는 결과는 그대로다`() {
        listOf(13.0, 14.0, 15.0, 16.0, 17.0).forEach { zoom ->
            val actual = clusterMarkers(GridMarkers, zoom = zoom).map { it.members }
            val expected = clusterMarkersByPairDistance(GridMarkers, zoom).map { it.members }

            assertEquals("zoom=$zoom", expected, actual)
        }
    }

    @Test
    fun `마커가 없으면 빈 목록을 반환한다`() {
        assertTrue(clusterMarkers(emptyList(), zoom = SeparatedZoom).isEmpty())
    }
}

/** 여러 줌에서 묶였다 갈라졌다 하도록 촘촘히 깔아 둔 마커들. */
private val GridMarkers = List(48) { index ->
    PlaceMarker(
        placeId = index.toLong(),
        name = "장소 $index",
        longitude = 126.9500 + index % 8 * 0.00037,
        latitude = 37.4900 + index / 8 * 0.00041,
        photoUrl = null,
    )
}

/** 최적화 이전의 셈법. 쌍마다 투영해 [screenDistanceDp] 로 잰다. */
private fun clusterMarkersByPairDistance(
    markers: List<PlaceMarker>,
    zoom: Double,
): List<MarkerCluster> {
    val remaining = markers.toMutableList()
    val clusters = mutableListOf<MarkerCluster>()

    while (remaining.isNotEmpty()) {
        val seed = remaining.removeAt(0)
        val members = mutableListOf(seed)
        val candidates = remaining.iterator()

        while (candidates.hasNext()) {
            val candidate = candidates.next()
            if (screenDistanceDp(seed, candidate, zoom) <= ClusterThresholdDp) {
                members += candidate
                candidates.remove()
            }
        }

        clusters += MarkerCluster(members = members)
    }

    return clusters
}
