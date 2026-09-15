package com.moamap.app.feature.mapdetail.data.repository

import com.moamap.app.feature.mapdetail.data.remote.MapPostDto
import com.moamap.app.feature.mapdetail.data.remote.MapPostPlaceTagDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapPostMapperTest {

    @Test
    fun `게시물 응답을 도메인으로 옮긴다`() {
        val post = MapPostDto(
            id = 7,
            mapId = 10,
            userId = 2,
            content = "성수 카페 다녀왔어요",
            imageUrls = listOf("https://img/1.jpg", "https://img/2.jpg"),
            placeTags = listOf(
                MapPostPlaceTagDto(placeId = 5, name = "블루보틀 성수점"),
                MapPostPlaceTagDto(placeId = 6, name = "연남 책방"),
            ),
            createdAt = "2026-09-15T09:30:00",
        ).toMapPost()

        assertEquals(7L, post.id)
        assertEquals(2L, post.authorId)
        assertEquals("성수 카페 다녀왔어요", post.content)
        assertEquals(listOf("https://img/1.jpg", "https://img/2.jpg"), post.imageUrls)
        assertEquals(listOf("블루보틀 성수점", "연남 책방"), post.placeNames)
        assertEquals(parseServerDateTime("2026-09-15T09:30:00"), post.createdAtMillis)
    }

    /** 남겨 두면 카드가 첫 사진 자리에 빈 이미지를, 장소 자리에 빈 알약을 그린다. */
    @Test
    fun `빈 사진 주소와 빈 장소 이름은 뺀다`() {
        val post = MapPostDto(
            imageUrls = listOf(" ", "https://img/2.jpg"),
            placeTags = listOf(
                MapPostPlaceTagDto(placeId = 5, name = null),
                MapPostPlaceTagDto(placeId = 6, name = "  "),
                MapPostPlaceTagDto(placeId = 7, name = " 연남 책방 "),
            ),
        ).toMapPost()

        assertEquals(listOf("https://img/2.jpg"), post.imageUrls)
        assertEquals(listOf("연남 책방"), post.placeNames)
    }

    @Test
    fun `본문이 없으면 빈 문자열이고 시각을 못 읽으면 비운다`() {
        val post = MapPostDto(content = null, createdAt = "어제").toMapPost()

        assertEquals("", post.content)
        assertNull(post.createdAtMillis)
    }
}
