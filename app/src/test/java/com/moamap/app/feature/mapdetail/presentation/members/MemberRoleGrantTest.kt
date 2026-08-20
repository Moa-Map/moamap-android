package com.moamap.app.feature.mapdetail.presentation.members

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MemberRoleGrantTest {

    private fun member(role: MemberRole) = MemberUiModel(
        id = 1L,
        name = "박지훈",
        imageUrl = null,
        role = role,
    )

    /** 방장·관리자는 이미 권한이 있다. 시안에서도 일반 카드에만 버튼이 붙는다. */
    @Test
    fun `일반 멤버에게만 권한 부여 버튼을 붙인다`() {
        assertTrue(member(MemberRole.Member).canGrantRole(grantEnabled = true))
        assertFalse(member(MemberRole.Admin).canGrantRole(grantEnabled = true))
        assertFalse(member(MemberRole.Owner).canGrantRole(grantEnabled = true))
    }

    /** 프라이빗 지도이거나 방장이 아니면 아무에게도 붙지 않는다. */
    @Test
    fun `권한 위임을 할 수 없으면 누구에게도 붙지 않는다`() {
        MemberRole.entries.forEach { role ->
            assertFalse(member(role).canGrantRole(grantEnabled = false))
        }
    }
}
