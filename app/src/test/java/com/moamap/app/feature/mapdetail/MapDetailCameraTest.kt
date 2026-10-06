package com.moamap.app.feature.mapdetail

import com.moamap.app.feature.mapdetail.domain.model.MapPlace
import com.mapbox.geojson.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapDetailCameraTest {

    private fun place(id: Long, longitude: Double, latitude: Double) = MapPlace(
        id = id,
        name = "장소$id",
        address = "주소$id",
        latitude = latitude,
        longitude = longitude,
        photoUrl = null,
    )

    @Test
    fun `장소가 둘 이상이면 전부 담기게 맞춘다`() {
        val places = listOf(place(1L, 126.9, 37.4), place(2L, 127.1, 37.6))

        val camera = initialCamera(places, deviceLocation = Point.fromLngLat(129.0, 35.1))

        // 위치 권한이 있어도 장소가 우선이다. 먼 지도를 열었을 때 빈 화면이 뜨면 안 된다.
        val fit = camera as InitialCamera.Fit
        assertEquals(2, fit.points.size)
        assertEquals(126.9, fit.points[0].longitude(), 1e-9)
        assertEquals(37.6, fit.points[1].latitude(), 1e-9)
    }

    @Test
    fun `장소가 하나면 그 자리를 중심으로 둔다`() {
        val camera = initialCamera(listOf(place(1L, 126.9, 37.4)), deviceLocation = null)

        val center = camera as InitialCamera.Center
        assertEquals(126.9, center.point.longitude(), 1e-9)
        assertEquals(37.4, center.point.latitude(), 1e-9)
    }

    @Test
    fun `장소가 없으면 현재 위치를 쓴다`() {
        val camera = initialCamera(emptyList(), deviceLocation = Point.fromLngLat(129.0, 35.1))

        val center = camera as InitialCamera.Center
        assertEquals(129.0, center.point.longitude(), 1e-9)
        assertEquals(35.1, center.point.latitude(), 1e-9)
    }

    @Test
    fun `장소도 위치도 없으면 고정 좌표로 간다`() {
        val camera = initialCamera(emptyList(), deviceLocation = null)

        assertEquals(InitialCamera.Center(MapDetailCenter), camera)
    }

    @Test
    fun `좌표가 0인 장소는 빼고 센다`() {
        // 서버 기본값이 0.0 이라, 좌표가 비어 온 장소를 그대로 담으면 아프리카 앞바다까지
        // 화면에 넣으려 든다.
        val places = listOf(place(1L, 0.0, 0.0), place(2L, 126.9, 37.4))

        val camera = initialCamera(places, deviceLocation = null)

        val center = camera as InitialCamera.Center
        assertEquals(126.9, center.point.longitude(), 1e-9)
    }

    @Test
    fun `좌표가 모두 비면 위치로 넘어간다`() {
        val places = listOf(place(1L, 0.0, 0.0))

        val camera = initialCamera(places, deviceLocation = Point.fromLngLat(129.0, 35.1))

        assertEquals(InitialCamera.Center(Point.fromLngLat(129.0, 35.1)), camera)
    }

    @Test
    fun `카메라가 아직 없으면 기본 줌으로 간다`() {
        assertEquals(MapDetailDefaultZoom, myLocationZoom(null), 1e-9)
    }

    @Test
    fun `너무 멀리서 보고 있으면 기본 줌까지 끌어당긴다`() {
        // 전국이 보이는 줌에서는 카메라만 옮겨서는 내 위치로 왔다는 느낌이 나지 않는다.
        assertEquals(MapDetailDefaultZoom, myLocationZoom(6.0), 1e-9)
    }

    @Test
    fun `이미 더 확대해 봤으면 그 줌을 유지한다`() {
        assertEquals(17.5, myLocationZoom(17.5), 1e-9)
    }

    @Test
    fun `묶음은 한도에 닿기 전까지 확대한다`() {
        assertFalse(isClusterZoomMaxed(MapDetailDefaultZoom))
        assertFalse(isClusterZoomMaxed(19.5))
    }

    @Test
    fun `좌표가 같은 장소 묶음은 확대해도 안 갈라진다`() {
        val cluster = MarkerCluster(listOf(marker(1L, 126.95), marker(2L, 126.95)))

        assertFalse(cluster.splitsByZoom())
    }

    @Test
    fun `한도에서도 묶음 기준 안에 붙은 장소는 안 갈라진다`() {
        // 한도 줌에서 30dp - 서울에서 2m 쯤이다.
        val cluster = MarkerCluster(listOf(marker(1L, 126.95), marker(2L, 126.95 + 30 * degreesPerDp(ClusterMaxZoom))))

        assertFalse(cluster.splitsByZoom())
    }

    @Test
    fun `한도까지 확대해 갈라지면 확대한다`() {
        // 기본 줌에서는 묶이는 30m 남짓 떨어진 두 곳이다.
        val cluster = MarkerCluster(listOf(marker(1L, 126.95), marker(2L, 126.9504)))

        assertTrue(cluster.splitsByZoom())
    }

    @Test
    fun `같은 자리 둘과 떨어진 하나는 일부라도 갈라지니 확대한다`() {
        val cluster = MarkerCluster(listOf(marker(1L, 126.95), marker(2L, 126.95), marker(3L, 126.9504)))

        assertTrue(cluster.splitsByZoom())
    }

    @Test
    fun `한도에 닿은 묶음은 목록으로 연다`() {
        assertTrue(isClusterZoomMaxed(ClusterMaxZoom))
        // 손으로 더 확대해 봤어도 마찬가지다.
        assertTrue(isClusterZoomMaxed(21.0))
    }

    @Test
    fun `한도로 옮긴 카메라의 소수점 오차는 한도로 본다`() {
        // 아니면 눌러도 같은 자리로 다시 확대만 하고 상세는 영영 안 열린다.
        assertTrue(isClusterZoomMaxed(ClusterMaxZoom - 1e-6))
    }

    @Test
    fun `카메라가 아직 없으면 확대 쪽으로 보낸다`() {
        assertFalse(isClusterZoomMaxed(null))
    }

    // ---------- 장소로 옮길 때의 줌 ----------

    private fun marker(id: Long, longitude: Double, latitude: Double = 37.5) =
        PlaceMarker(placeId = id, name = "장소$id", longitude = longitude, latitude = latitude, photoUrl = null)

    /** [PlaceFocusZoom] 에서 기준 경도로부터 [dp] 만큼 떨어진 경도. */
    private fun longitudeAtDp(dp: Double) = 126.95 + dp * degreesPerDp(PlaceFocusZoom)

    @Test
    fun `멀리서 보고 있으면 기본 줌으로 당긴다`() {
        assertEquals(PlaceFocusZoom, placeFocusZoom(listOf(marker(1L, 126.95)), 1L, 12.0), 1e-9)
        assertEquals(PlaceFocusZoom, placeFocusZoom(listOf(marker(1L, 126.95)), 1L, null), 1e-9)
    }

    @Test
    fun `이미 더 확대해 봤으면 그 줌을 둔다`() {
        assertEquals(17.3, placeFocusZoom(listOf(marker(1L, 126.95)), 1L, 17.3), 1e-9)
    }

    @Test
    fun `이웃이 멀면 기본 줌 그대로다`() {
        val markers = listOf(marker(1L, 126.95), marker(2L, longitudeAtDp(200.0)))

        assertEquals(PlaceFocusZoom, placeFocusZoom(markers, 1L, null), 1e-9)
    }

    @Test
    fun `묶여 보이면 혼자 보일 때까지 확대한다`() {
        // 기본 줌에서 30dp - 묶음 기준(72dp) 안이다.
        val markers = listOf(marker(1L, 126.95), marker(2L, longitudeAtDp(30.0)), marker(3L, longitudeAtDp(400.0)))

        val zoom = placeFocusZoom(markers, 1L, null)

        // 지도가 묶음을 셈하는 줌(0.25 단위로 내림)에서 그 장소가 혼자여야 한다.
        val drawn = clusterMarkers(markers, kotlin.math.floor(zoom / ClusterZoomStep) * ClusterZoomStep)
        assertTrue(drawn.any { cluster -> cluster.isSingle && cluster.members.first().placeId == 1L })
        // 갈라지는 바로 그 칸이다. 한 칸 덜 확대하면 아직 묶인다.
        val before = clusterMarkers(markers, zoom - ClusterZoomStep)
        assertTrue(before.none { cluster -> cluster.isSingle && cluster.members.first().placeId == 1L })
    }

    @Test
    fun `좌표가 같은 장소와는 한도까지만 확대한다`() {
        val markers = listOf(marker(1L, 126.95), marker(2L, 126.95))

        assertEquals(ClusterMaxZoom, placeFocusZoom(markers, 1L, null), 1e-9)
    }

    @Test
    fun `아주 가까운 장소가 있어도 한도를 넘지 않는다`() {
        val markers = listOf(marker(1L, 126.95), marker(2L, 126.95 + 1e-7))

        assertEquals(ClusterMaxZoom, placeFocusZoom(markers, 1L, null), 1e-9)
    }

    @Test
    fun `지도에 없는 장소면 기본 규칙만 따른다`() {
        assertEquals(PlaceFocusZoom, placeFocusZoom(listOf(marker(2L, 126.95)), 1L, null), 1e-9)
    }
}
