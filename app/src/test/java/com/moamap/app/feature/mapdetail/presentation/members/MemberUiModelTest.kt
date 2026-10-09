package com.moamap.app.feature.mapdetail.presentation.members

import com.moamap.app.feature.mapdetail.domain.model.MapMember
import com.moamap.app.feature.mapdetail.domain.model.MapRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MemberUiModelTest {

    private fun member(role: MapRole) = MapMember(
        id = 1L,
        name = "박지훈",
        imageUrl = null,
        role = role,
    )

    @Test
    fun `멤버를 카드 모델로 옮긴다`() {
        val ui = MapMember(
            id = 4L,
            name = "최유진",
            imageUrl = "https://cdn/4.jpg",
            role = MapRole.Owner,
            placeCount = 3,
        ).toUiModel()

        assertEquals(4L, ui.id)
        assertEquals("최유진", ui.name)
        assertEquals("https://cdn/4.jpg", ui.imageUrl)
        assertEquals(MemberRole.Owner, ui.role)
        assertEquals(3L, ui.placeCount)
    }

    @Test
    fun `역할을 그대로 옮긴다`() {
        assertEquals(MemberRole.Owner, member(MapRole.Owner).toUiModel().role)
        assertEquals(MemberRole.Admin, member(MapRole.Admin).toUiModel().role)
        assertEquals(MemberRole.Member, member(MapRole.Member).toUiModel().role)
    }

    /**
     * 목록에 실린 사람은 어떻게든 그 지도의 멤버다. 역할 자리가 비어 오더라도 카드를 빼거나
     * 빈 배지를 그리지 않고 일반으로 본다 - 권한 부여 버튼도 그 자리에 붙어야 한다.
     */
    @Test
    fun `역할이 없으면 일반 멤버로 본다`() {
        assertEquals(MemberRole.Member, member(MapRole.None).toUiModel().role)
    }

    @Test
    fun `사진이 없으면 비운다`() {
        assertNull(member(MapRole.Member).toUiModel().imageUrl)
    }

    // ---------- 내 카드 나누기 ----------

    private val members = listOf(
        MemberUiModel(1L, "방장", null, MemberRole.Owner),
        MemberUiModel(2L, "관리자", null, MemberRole.Admin),
        MemberUiModel(3L, "멤버", null, MemberRole.Member),
    )

    @Test
    fun `커뮤니티 지도에서는 내 카드를 떼고 나머지는 순서 그대로 둔다`() {
        val (me, others) = members.splitMine(myId = 2L, display = MemberRoleDisplay.All)!!

        assertEquals(2L, me.id)
        assertEquals(listOf(1L, 3L), others.map { it.id })
    }

    @Test
    fun `다른 멤버가 없어도 나눈다`() {
        val (me, others) = members.take(1).splitMine(myId = 1L, display = MemberRoleDisplay.All)!!

        assertEquals(1L, me.id)
        assertEquals(emptyList<MemberUiModel>(), others)
    }

    @Test
    fun `프라이빗 지도는 나누지 않는다`() {
        assertNull(members.splitMine(myId = 1L, display = MemberRoleDisplay.OwnerOnly))
    }

    @Test
    fun `내 id 를 모르거나 목록에 없으면 나누지 않는다`() {
        assertNull(members.splitMine(myId = null, display = MemberRoleDisplay.All))
        assertNull(members.splitMine(myId = 9L, display = MemberRoleDisplay.All))
    }
}
