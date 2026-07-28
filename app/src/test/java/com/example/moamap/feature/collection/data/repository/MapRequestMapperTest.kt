package com.example.moamap.feature.collection.data.repository

import com.example.moamap.feature.collection.domain.model.MapVisibility
import com.example.moamap.feature.collection.domain.model.NewMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapRequestMapperTest {

    private fun newMap(
        name: String = "성수 카페 투어",
        description: String? = "주말에 다녀온 곳",
        visibility: MapVisibility = MapVisibility.Public,
        tags: List<String> = listOf("카페"),
    ) = NewMap(name = name, description = description, visibility = visibility, tags = tags)

    @Test
    fun `공개 지도는 PUBLIC 으로 보낸다`() {
        val request = newMap(visibility = MapVisibility.Public).toCreateRequest()

        assertEquals("PUBLIC", request.visibility)
    }

    @Test
    fun `프라이빗 지도는 PRIVATE 으로 보낸다`() {
        val request = newMap(visibility = MapVisibility.Private).toCreateRequest()

        assertEquals("PRIVATE", request.visibility)
    }

    @Test
    fun `커버 이미지는 생성 요청에 싣지 않는다`() {
        assertNull(newMap().toCreateRequest().imageUrl)
    }

    @Test
    fun `태그가 없으면 빈 배열 대신 아예 보내지 않는다`() {
        assertNull(newMap(tags = emptyList()).toCreateRequest().tags)
    }

    @Test
    fun `공백뿐인 설명은 안 쓴 것으로 본다`() {
        assertNull(newMap(description = "   ").toCreateRequest().description)
    }

    @Test
    fun `설명을 아예 비워도 그대로 비운다`() {
        assertNull(newMap(description = null).toCreateRequest().description)
    }

    @Test
    fun `이름 앞뒤 공백은 떼고 보낸다`() {
        assertEquals("성수 카페 투어", newMap(name = "  성수 카페 투어  ").toCreateRequest().name)
    }

}
