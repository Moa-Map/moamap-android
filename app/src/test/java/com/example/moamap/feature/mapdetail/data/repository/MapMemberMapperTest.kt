package com.example.moamap.feature.mapdetail.data.repository

import com.example.moamap.feature.collection.data.remote.MapMemberSummaryDto
import com.example.moamap.feature.mapdetail.domain.model.MapRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapMemberMapperTest {

    @Test
    fun `멤버 응답을 도메인으로 옮긴다`() {
        val member = MapMemberSummaryDto(
            userId = 3,
            nickname = "박지훈",
            profileImageUrl = "https://cdn/3.jpg",
            role = "ADMIN",
        ).toMapMember()

        assertEquals(3L, member.id)
        assertEquals("박지훈", member.name)
        assertEquals("https://cdn/3.jpg", member.imageUrl)
        assertEquals(MapRole.Admin, member.role)
    }

    /** 닉네임 자리가 비면 카드 이름 줄이 빈 줄로 보인다. */
    @Test
    fun `닉네임이 비면 대체 문구를 쓴다`() {
        val blank = MapMemberSummaryDto(userId = 1, nickname = "   ", role = "MEMBER")
        val missing = MapMemberSummaryDto(userId = 2, nickname = null, role = "MEMBER")

        assertEquals(UNKNOWN_MEMBER, blank.toMapMember().name)
        assertEquals(UNKNOWN_MEMBER, missing.toMapMember().name)
    }

    /** 빈 문자열을 그대로 넘기면 이미지 로더가 깨진 아이콘을 그린다. */
    @Test
    fun `프로필 주소가 비면 null 로 본다`() {
        val member = MapMemberSummaryDto(userId = 1, nickname = "김도현", profileImageUrl = "  ")
            .toMapMember()

        assertNull(member.imageUrl)
    }

    @Test
    fun `모르는 역할은 권한이 없는 쪽으로 본다`() {
        val member = MapMemberSummaryDto(userId = 1, nickname = "김도현", role = "SUPERVISOR")
            .toMapMember()

        assertEquals(MapRole.None, member.role)
    }
}
