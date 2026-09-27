package com.moamap.app.feature.mapdetail.presentation.members

import com.moamap.app.feature.collection.domain.model.MapType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 멤버 카드 이름 옆 태그. 일반 멤버에게는 어느 지도에서도 붙지 않는다. */
class MemberRoleTagTest {

    private fun member(role: MemberRole) = MemberUiModel(1L, "김도현", null, role)

    @Test
    fun `지도 종류마다 역할을 드러내는 범위가 다르다`() {
        assertEquals(MemberRoleDisplay.All, memberRoleDisplayOf(MapType.Community))
        assertEquals(MemberRoleDisplay.OwnerOnly, memberRoleDisplayOf(MapType.Private))
        assertEquals(MemberRoleDisplay.None, memberRoleDisplayOf(MapType.Official))
    }

    @Test
    fun `커뮤니티 지도는 방장과 관리자에게 태그를 붙인다`() {
        assertEquals(MemberRole.Owner, member(MemberRole.Owner).tag(MemberRoleDisplay.All))
        assertEquals(MemberRole.Admin, member(MemberRole.Admin).tag(MemberRoleDisplay.All))
        assertNull(member(MemberRole.Member).tag(MemberRoleDisplay.All))
    }

    /** 프라이빗 지도의 방장은 역할이 아니라 지도를 만든 사람이라는 표시다. */
    @Test
    fun `프라이빗 지도는 만든 사람에게만 방장 태그를 붙인다`() {
        assertEquals(MemberRole.Owner, member(MemberRole.Owner).tag(MemberRoleDisplay.OwnerOnly))
        assertNull(member(MemberRole.Admin).tag(MemberRoleDisplay.OwnerOnly))
        assertNull(member(MemberRole.Member).tag(MemberRoleDisplay.OwnerOnly))
    }

    @Test
    fun `공식지도는 태그를 붙이지 않는다`() {
        MemberRole.entries.forEach { role ->
            assertNull(member(role).tag(MemberRoleDisplay.None))
        }
    }
}
