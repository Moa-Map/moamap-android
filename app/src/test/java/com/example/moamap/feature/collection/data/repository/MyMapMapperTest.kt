package com.example.moamap.feature.collection.data.repository

import com.example.moamap.feature.collection.data.remote.MapSummaryDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MyMapMapperTest {

    private fun dto(
        id: Long = 1L,
        name: String? = "성수 카페 투어",
        imageUrl: String? = "https://cdn.example.com/map.jpg",
        type: String? = "COMMUNITY",
        memberCount: Int = 12,
        placeCount: Int = 5,
        personal: Boolean = false,
    ) = MapSummaryDto(
        id = id,
        name = name,
        imageUrl = imageUrl,
        type = type,
        memberCount = memberCount,
        placeCount = placeCount,
        personal = personal,
    )

    @Test
    fun `이름이 없으면 자리를 채운다`() {
        assertEquals("이름 없는 지도", dto(name = null).toMyMap().title)
    }

    @Test
    fun `공백뿐인 이름도 자리를 채운다`() {
        assertEquals("이름 없는 지도", dto(name = "   ").toMyMap().title)
    }

    @Test
    fun `공백뿐인 이미지 주소는 없는 것으로 본다`() {
        assertNull(dto(imageUrl = "  ").toMyMap().imageUrl)
    }

    @Test
    fun `이미지 주소가 아예 없어도 그대로 비운다`() {
        assertNull(dto(imageUrl = null).toMyMap().imageUrl)
    }

    @Test
    fun `공식 지도만 인증 배지를 단다`() {
        assertTrue(dto(type = "OFFICIAL").toMyMap().official)
        assertFalse(dto(type = "COMMUNITY").toMyMap().official)
        assertFalse(dto(type = "PRIVATE").toMyMap().official)
        assertFalse(dto(type = null).toMyMap().official)
    }

    @Test
    fun `개인 지도 여부를 그대로 옮긴다`() {
        assertTrue(dto(personal = true).toMyMap().personal)
        assertFalse(dto(personal = false).toMyMap().personal)
    }

    /** 서버가 필드를 빼고 주면 0·false 로 본다. 카드가 빈 줄로 보이지 않게 한다. */
    @Test
    fun `장소 수와 개인 지도 여부가 없으면 기본값으로 본다`() {
        val myMap = MapSummaryDto(id = 1L, name = "성수 카페 투어").toMyMap()

        assertEquals(0, myMap.placeCount)
        assertFalse(myMap.personal)
    }

    @Test
    fun `나머지 값은 그대로 옮긴다`() {
        val myMap = dto(
            id = 7L,
            name = "성수 카페 투어",
            memberCount = 2312,
            placeCount = 116,
        ).toMyMap()

        assertEquals(7L, myMap.id)
        assertEquals("성수 카페 투어", myMap.title)
        assertEquals(2312, myMap.memberCount)
        assertEquals(116, myMap.placeCount)
    }
}
