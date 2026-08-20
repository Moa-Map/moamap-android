package com.moamap.app.feature.officialmap.data.repository

import com.moamap.app.feature.officialmap.data.remote.OfficialMapDto
import org.junit.Assert.assertEquals
import org.junit.Test

class OfficialMapMapperTest {

    @Test
    fun `서버 응답을 그대로 옮긴다`() {
        val map = OfficialMapDto(
            id = 6,
            name = "화장실 위치",
            description = "공공데이터 기반 공중화장실 위치",
            type = "OFFICIAL",
            memberCount = 1,
            placeCount = 5416,
            joined = false,
        ).toOfficialMap()

        assertEquals(6L, map.id)
        assertEquals("화장실 위치", map.title)
        assertEquals("공공데이터 기반 공중화장실 위치", map.description)
        assertEquals(1, map.memberCount)
        assertEquals(5416, map.placeCount)
        assertEquals(false, map.joined)
    }

    @Test
    fun `이름이 없으면 자리를 채운다`() {
        assertEquals("이름 없는 지도", OfficialMapDto(name = null).toOfficialMap().title)
        assertEquals("이름 없는 지도", OfficialMapDto(name = "   ").toOfficialMap().title)
    }

    @Test
    fun `설명이 없으면 빈 문자열이다`() {
        assertEquals("", OfficialMapDto(description = null).toOfficialMap().description)
        assertEquals("", OfficialMapDto(description = "  ").toOfficialMap().description)
    }

    @Test
    fun `참여 여부가 유실되지 않는다`() {
        assertEquals(true, OfficialMapDto(joined = true).toOfficialMap().joined)
    }
}
