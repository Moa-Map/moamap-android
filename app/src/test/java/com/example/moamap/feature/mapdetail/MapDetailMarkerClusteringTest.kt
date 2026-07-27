package com.example.moamap.feature.mapdetail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapDetailMarkerClusteringTest {

    @Test
    fun `초기 줌에서는 두 샘플 마커가 각각 표시된다`() {
        val clusters = clusterMarkers(SamplePlaceMarkers, zoom = MapDetailDefaultZoom)

        assertEquals(2, clusters.size)
        assertTrue(clusters.all { cluster -> cluster.isSingle })
    }

    @Test
    fun `줌을 낮추면 두 마커가 하나의 클러스터로 묶인다`() {
        val clusters = clusterMarkers(SamplePlaceMarkers, zoom = 15.0)

        assertEquals(1, clusters.size)
        assertEquals(2, clusters.first().members.size)
    }

    @Test
    fun `샘플 마커의 초기 줌 화면 거리는 임계값보다 충분히 크다`() {
        val distance = screenDistanceDp(
            first = SamplePlaceMarkers[0],
            second = SamplePlaceMarkers[1],
            zoom = MapDetailDefaultZoom,
        )

        assertTrue("실제 거리=$distance", distance in 140.0..150.0)
        assertTrue(distance > ClusterThresholdDp)
    }

    @Test
    fun `클러스터 중심은 구성원의 평균 좌표다`() {
        val cluster = clusterMarkers(SamplePlaceMarkers, zoom = 15.0).first()
        val anchor = cluster.anchorPoint()

        assertEquals(126.9574, anchor.longitude(), 1e-6)
        assertEquals(37.4963, anchor.latitude(), 1e-6)
    }

    @Test
    fun `클러스터 id는 구성이 같으면 동일하다`() {
        val first = clusterMarkers(SamplePlaceMarkers, zoom = 15.0).first()
        val second = clusterMarkers(SamplePlaceMarkers, zoom = 15.0).first()

        assertEquals(first.id, second.id)
        assertEquals("1-2", first.id)
    }

    @Test
    fun `마커가 없으면 빈 목록을 반환한다`() {
        assertTrue(clusterMarkers(emptyList(), zoom = MapDetailDefaultZoom).isEmpty())
    }
}
