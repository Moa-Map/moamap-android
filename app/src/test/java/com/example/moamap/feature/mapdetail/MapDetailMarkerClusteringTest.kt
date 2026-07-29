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
    fun `클러스터 중심은 구성원의 평균 좌표다`() {
        val cluster = clusterMarkers(PreviewPlaceMarkers, zoom = MergedZoom).first()
        val anchor = cluster.anchorPoint()

        assertEquals(126.9574, anchor.longitude(), 1e-6)
        assertEquals(37.4963, anchor.latitude(), 1e-6)
    }

    @Test
    fun `클러스터 id는 구성이 같으면 동일하다`() {
        val first = clusterMarkers(PreviewPlaceMarkers, zoom = MergedZoom).first()
        val second = clusterMarkers(PreviewPlaceMarkers, zoom = MergedZoom).first()

        assertEquals(first.id, second.id)
        assertEquals("1-2", first.id)
    }

    @Test
    fun `마커가 없으면 빈 목록을 반환한다`() {
        assertTrue(clusterMarkers(emptyList(), zoom = SeparatedZoom).isEmpty())
    }
}
