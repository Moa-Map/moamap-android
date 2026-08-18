package com.example.moamap.feature.collection.data.remote

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapMemberDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // map-service MapMemberListResponse. 봉투(ApiResponse)는 컨버터가 벗기므로 안쪽만 온다.
    private val listJson = """
        {"memberCount":3,
         "members":[
           {"userId":1,"nickname":"김도현","profileImageUrl":"https://cdn/1.jpg","role":"OWNER"},
           {"userId":2,"nickname":"이서연","profileImageUrl":null,"role":"ADMIN"},
           {"userId":3,"nickname":"박지훈","profileImageUrl":null,"role":"MEMBER"}
         ]}
    """.trimIndent()

    @Test
    fun `멤버 목록 응답을 역직렬화하면 인원수와 멤버가 채워진다`() {
        val list = json.decodeFromString<MapMemberListDto>(listJson)

        assertEquals(3, list.memberCount)
        assertEquals(3, list.members.size)
        assertEquals(1L, list.members[0].userId)
        assertEquals("김도현", list.members[0].nickname)
        assertEquals("https://cdn/1.jpg", list.members[0].profileImageUrl)
        assertEquals("OWNER", list.members[0].role)
        assertNull(list.members[1].profileImageUrl)
    }

    /** 서버가 나중에 필드를 더해도 앱이 터지지 않아야 한다. */
    @Test
    fun `모르는 필드가 있어도 역직렬화된다`() {
        val withExtra = """
            {"memberCount":1,
             "members":[{"userId":9,"nickname":"최유진","role":"MEMBER","joinedAt":"2026-08-18"}],
             "someNewField":"값"}
        """.trimIndent()

        val list = json.decodeFromString<MapMemberListDto>(withExtra)

        assertEquals(1, list.members.size)
        assertEquals(9L, list.members[0].userId)
    }

    /** 멤버가 하나도 없는 지도는 members 자체가 빠져 올 수 있다. */
    @Test
    fun `members 가 없으면 빈 목록이 된다`() {
        val list = json.decodeFromString<MapMemberListDto>("""{"memberCount":0}""")

        assertEquals(0, list.memberCount)
        assertEquals(emptyList<MapMemberSummaryDto>(), list.members)
    }

    @Test
    fun `역할 변경 요청은 역할만 담는다`() {
        val encoded = json.encodeToString(MapMemberRoleUpdateRequestDto(role = "ADMIN"))

        assertEquals("""{"role":"ADMIN"}""", encoded)
    }

    @Test
    fun `역할 변경 응답을 역직렬화하면 바뀐 역할을 얻는다`() {
        val updated = json.decodeFromString<MapMemberRoleUpdateDto>(
            """{"mapId":7,"userId":2,"role":"ADMIN"}""",
        )

        assertEquals(7L, updated.mapId)
        assertEquals(2L, updated.userId)
        assertEquals("ADMIN", updated.role)
    }
}
