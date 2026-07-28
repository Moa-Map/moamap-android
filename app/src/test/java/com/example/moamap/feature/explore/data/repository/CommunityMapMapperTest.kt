package com.example.moamap.feature.explore.data.repository

import com.example.moamap.feature.explore.data.remote.CommunityMapDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CommunityMapMapperTest {

    @Test
    fun `DTO 값을 그대로 도메인으로 옮긴다`() {
        val map = CommunityMapDto(
            id = 7L,
            name = "서울 팝업스토어 맵",
            imageUrl = "https://cdn.example.com/map.png",
            tags = listOf("맛집", "데이트코스"),
            memberCount = 2312,
            joined = true,
        ).toDomain()

        assertEquals(7L, map.id)
        assertEquals("서울 팝업스토어 맵", map.title)
        assertEquals("https://cdn.example.com/map.png", map.imageUrl)
        assertEquals(listOf("맛집", "데이트코스"), map.hashtags)
        assertEquals(2312, map.memberCount)
        assertEquals(true, map.joined)
    }

    @Test
    fun `이름이 비어 있으면 자리를 채운다`() {
        assertEquals("이름 없는 지도", CommunityMapDto(name = null).toDomain().title)
        assertEquals("이름 없는 지도", CommunityMapDto(name = "  ").toDomain().title)
    }

    @Test
    fun `빈 이미지 URL은 기본 이미지를 쓰도록 null로 만든다`() {
        assertNull(CommunityMapDto(imageUrl = null).toDomain().imageUrl)
        assertNull(CommunityMapDto(imageUrl = "").toDomain().imageUrl)
        assertNull(CommunityMapDto(imageUrl = "   ").toDomain().imageUrl)
    }

    @Test
    fun `빈 태그는 해시태그에서 걸러낸다`() {
        val map = CommunityMapDto(tags = listOf("맛집", "", "  ", "데이트")).toDomain()

        assertEquals(listOf("맛집", "데이트"), map.hashtags)
    }
}
