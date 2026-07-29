package com.example.moamap.feature.mapdetail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapDetailViewportTest {

    private fun marker(id: Long, longitude: Double, latitude: Double) = PlaceMarker(
        placeId = id,
        name = "장소$id",
        longitude = longitude,
        latitude = latitude,
        photoUrl = null,
    )

    @Test
    fun `월드 픽셀 투영을 되돌리면 원래 좌표가 나온다`() {
        val zoom = 14.0
        val longitude = 126.9574
        val latitude = 37.4963

        val backLongitude = longitudeAtWorldPixelX(worldPixelX(longitude, zoom), zoom)
        val backLatitude = latitudeAtWorldPixelY(worldPixelY(latitude, zoom), zoom)

        assertEquals(longitude, backLongitude, 1e-9)
        assertEquals(latitude, backLatitude, 1e-9)
    }

    @Test
    fun `경계는 화면 중심을 감싼다`() {
        val bounds = viewportBounds(
            centerLongitude = 126.9574,
            centerLatitude = 37.4963,
            zoom = 14.0,
            widthDp = 393.0,
            heightDp = 852.0,
            marginRatio = 0.0,
        )

        assertTrue(bounds.west < 126.9574)
        assertTrue(bounds.east > 126.9574)
        assertTrue(bounds.south < 37.4963)
        assertTrue(bounds.north > 37.4963)
        // 세로가 더 길므로 위도 폭이 경도 폭보다 넓다.
        assertTrue(bounds.north - bounds.south > bounds.east - bounds.west)
    }

    @Test
    fun `마진은 경계를 사방으로 넓힌다`() {
        val plain = viewportBounds(126.9574, 37.4963, 14.0, 393.0, 852.0, marginRatio = 0.0)
        val padded = viewportBounds(126.9574, 37.4963, 14.0, 393.0, 852.0, marginRatio = 0.3)

        assertTrue(padded.west < plain.west)
        assertTrue(padded.east > plain.east)
        assertTrue(padded.south < plain.south)
        assertTrue(padded.north > plain.north)
    }

    @Test
    fun `경계 안의 마커만 남는다`() {
        val bounds = ViewportBounds(west = 126.0, south = 37.0, east = 127.0, north = 38.0)
        val markers = listOf(
            marker(1L, 126.5, 37.5),
            marker(2L, 128.0, 37.5),
            marker(3L, 126.5, 39.0),
            marker(4L, 126.9, 37.9),
        )

        val kept = cullToViewport(markers, bounds)

        assertEquals(listOf(1L, 4L), kept.map { it.placeId })
    }

    @Test
    fun `경계선 위의 마커는 남긴다`() {
        // 경계를 스치는 마커를 지우면 스크롤 중에 깜빡인다.
        val bounds = ViewportBounds(west = 126.0, south = 37.0, east = 127.0, north = 38.0)

        val kept = cullToViewport(listOf(marker(1L, 126.0, 38.0)), bounds)

        assertEquals(1, kept.size)
    }

    @Test
    fun `입력 순서를 지킨다`() {
        // 클러스터 id 안정성이 입력 순서에 달려 있다.
        val bounds = ViewportBounds(west = 126.0, south = 37.0, east = 127.0, north = 38.0)
        val markers = listOf(marker(3L, 126.1, 37.1), marker(1L, 126.2, 37.2))

        assertEquals(listOf(3L, 1L), cullToViewport(markers, bounds).map { it.placeId })
    }
}
