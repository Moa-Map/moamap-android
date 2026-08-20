package com.moamap.app.feature.collection.data.repository

import com.moamap.app.feature.collection.domain.model.MapVisibility
import com.moamap.app.feature.collection.domain.model.NewMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapRequestMapperTest {

    private fun newMap(
        name: String = "성수 카페 투어",
        description: String? = "주말에 다녀온 곳",
        visibility: MapVisibility = MapVisibility.Public,
        tags: List<String> = listOf("카페"),
        imageUrl: String? = null,
    ) = NewMap(
        name = name,
        description = description,
        visibility = visibility,
        tags = tags,
        imageUrl = imageUrl,
    )

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
    fun `올려둔 커버 주소를 생성 요청에 그대로 싣는다`() {
        val request = newMap(imageUrl = "https://cdn/cover.jpg").toCreateRequest()

        assertEquals("https://cdn/cover.jpg", request.imageUrl)
    }

    @Test
    fun `사진을 안 골랐으면 커버를 비운다`() {
        assertNull(newMap(imageUrl = null).toCreateRequest().imageUrl)
    }

    /** 빈 문자열이 흘러들어가면 서버가 빈 imageUrl 을 저장한다. */
    @Test
    fun `공백뿐인 커버 주소는 안 쓴 것으로 본다`() {
        assertNull(newMap(imageUrl = "  ").toCreateRequest().imageUrl)
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
