package com.moamap.app.feature.mapdetail.presentation.members

import com.moamap.app.feature.collection.domain.model.MapType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 멤버 카드 이름 옆 역할 뱃지. 커뮤니티 지도의 방장·관리자에게만 붙는다. */
class MemberRoleTagTest {

    private fun member(role: MemberRole) = MemberUiModel(1L, "김도현", null, role)

    @Test
    fun `지도 종류마다 역할을 드러내는 범위가 다르다`() {
        assertEquals(MemberRoleDisplay.All, memberRoleDisplayOf(MapType.Community))
        // 프라이빗은 역할이 나뉘지 않아 공식지도처럼 드러내지 않는다.
        assertEquals(MemberRoleDisplay.None, memberRoleDisplayOf(MapType.Private))
        assertEquals(MemberRoleDisplay.None, memberRoleDisplayOf(MapType.Official))
    }

    @Test
    fun `커뮤니티 지도는 방장과 관리자에게 뱃지를 붙인다`() {
        assertEquals(MemberRole.Owner, member(MemberRole.Owner).tag(MemberRoleDisplay.All))
        assertEquals(MemberRole.Admin, member(MemberRole.Admin).tag(MemberRoleDisplay.All))
        assertNull(member(MemberRole.Member).tag(MemberRoleDisplay.All))
    }

    @Test
    fun `프라이빗과 공식지도는 뱃지를 붙이지 않는다`() {
        MemberRole.entries.forEach { role ->
            assertNull(member(role).tag(MemberRoleDisplay.None))
        }
    }
}
