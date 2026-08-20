package com.moamap.app.feature.explore.data.repository

import com.moamap.app.feature.explore.data.remote.CommunityMapDto
import com.moamap.app.feature.explore.data.remote.MapRecommendationDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class CommunityMapMapperTest {

    /** `NetworkModule.provideJson()` 과 같은 설정. 응답을 읽는 경로를 그대로 재현한다. */
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun `DTO 값을 그대로 도메인으로 옮긴다`() {
        val map = CommunityMapDto(
            id = 7L,
            name = "서울 팝업스토어 맵",
            imageUrl = "https://cdn.example.com/map.png",
            tags = listOf("맛집", "데이트코스"),
            memberCount = 2312,
            placeCount = 116,
            joined = true,
        ).toDomain()

        assertEquals(7L, map.id)
        assertEquals("서울 팝업스토어 맵", map.title)
        assertEquals("https://cdn.example.com/map.png", map.imageUrl)
        assertEquals(listOf("맛집", "데이트코스"), map.hashtags)
        assertEquals(2312, map.memberCount)
        assertEquals(116, map.placeCount)
        assertEquals(true, map.joined)
    }

    /**
     * 서버가 필드를 빼고 주면 0 으로 본다. 카드는 그때도 "0곳" 을 그린다.
     *
     * 생성자 기본값이 아니라 **응답을 읽는 경로**를 확인해야 의미가 있어 JSON 으로 넣는다.
     */
    @Test
    fun `장소 수가 응답에 없으면 0으로 본다`() {
        val map = json
            .decodeFromString<CommunityMapDto>("""{"id":1,"name":"서울 팝업스토어 맵"}""")
            .toDomain()

        assertEquals(0, map.placeCount)
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

    @Test
    fun `추천 DTO 값을 그대로 도메인으로 옮긴다`() {
        val map = MapRecommendationDto(
            id = 7L,
            name = "서울 팝업스토어 맵",
            imageUrl = "https://cdn.example.com/map.png",
            tags = listOf("맛집", "데이트코스"),
            memberCount = 2312,
            reason = "관심 태그 #맛집와 비슷해요",
        ).toDomain()

        assertEquals(7L, map.id)
        assertEquals("서울 팝업스토어 맵", map.title)
        assertEquals("https://cdn.example.com/map.png", map.imageUrl)
        assertEquals(listOf("맛집", "데이트코스"), map.hashtags)
        assertEquals(2312, map.memberCount)
    }

    /**
     * 서버가 이미 참여했거나 직접 만든 지도를 추천에서 빼고 준다. 그래서 참여 여부를 실어
     * 보내지 않고, 카드를 누르면 소개 화면으로 가는 것이 맞다.
     */
    @Test
    fun `추천 지도는 아직 참여하지 않은 것으로 본다`() {
        assertFalse(MapRecommendationDto(id = 1L).toDomain().joined)
    }

    /** 추천 카드는 장소 수를 그리지 않아 서버도 주지 않는다. */
    @Test
    fun `추천 지도의 장소 수는 0이다`() {
        assertEquals(0, MapRecommendationDto(id = 1L).toDomain().placeCount)
    }

    @Test
    fun `추천 지도도 이름이 비면 자리를 채우고 빈 이미지는 null로 만든다`() {
        val map = MapRecommendationDto(name = "  ", imageUrl = "   ").toDomain()

        assertEquals("이름 없는 지도", map.title)
        assertNull(map.imageUrl)
    }

    /**
     * 응답에 `description`·`type` 이 함께 오지만 어느 화면도 쓰지 않아 DTO 에 없다.
     * 생성자 기본값이 아니라 **응답을 읽는 경로**를 확인해야 의미가 있어 JSON 으로 넣는다.
     */
    @Test
    fun `추천 응답의 모르는 필드는 무시한다`() {
        val map = json.decodeFromString<MapRecommendationDto>(
            """{"id":1,"name":"서울 팝업스토어 맵","description":"설명","type":"COMMUNITY"}""",
        ).toDomain()

        assertEquals(1L, map.id)
        assertEquals("서울 팝업스토어 맵", map.title)
    }
}
