package com.moamap.app.feature.mapdetail

import com.moamap.app.feature.mapdetail.domain.model.MapPlace
import com.mapbox.geojson.Point
import org.junit.Assert.assertEquals
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
}
