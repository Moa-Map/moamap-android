package com.example.moamap.feature.mapdetail.domain.model

import com.example.moamap.feature.collection.domain.model.MapType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 상단 배지·액션 규칙.
 *
 * 서버 제약 두 가지가 그대로 규칙이 된다.
 * - `POST /maps/{mapId}/join` 은 공개 지도 전용
 * - `DELETE /maps/{mapId}/members/me` 는 OWNER 의 탈퇴를 거절
 */
class MapDetailRuleTest {

    private fun map(
        type: MapType,
        role: MapRole,
        joined: Boolean,
        memberCount: Int = 1,
        personal: Boolean = false,
    ) = MapDetail(
        id = 1L,
        title = "지도",
        description = null,
        imageUrl = null,
        ownerName = null,
        type = type,
        role = role,
        tags = emptyList(),
        memberCount = memberCount,
        placeCount = 0,
        joined = joined,
        personal = personal,
    )

    // ---------- 역할 배지 ----------

    @Test
    fun `커뮤니티 지도는 역할에 맞는 배지를 보여준다`() {
        assertEquals("방장", map(MapType.Community, MapRole.Owner, joined = true).roleBadge)
        assertEquals("관리자", map(MapType.Community, MapRole.Admin, joined = true).roleBadge)
        assertEquals("멤버", map(MapType.Community, MapRole.Member, joined = true).roleBadge)
    }

    @Test
    fun `참여하지 않은 커뮤니티 지도는 배지가 없다`() {
        assertNull(map(MapType.Community, MapRole.None, joined = false).roleBadge)
    }

    @Test
    fun `프라이빗 지도는 역할이 무엇이든 배지가 없다`() {
        // 프라이빗은 역할 개념이 없는 지도다. 서버가 만든 사람을 OWNER 로 내려줘도 띄우지 않는다.
        assertNull(map(MapType.Private, MapRole.Owner, joined = true).roleBadge)
        assertNull(map(MapType.Private, MapRole.Member, joined = true).roleBadge)
    }

    // ---------- 우측 상단 액션 ----------

    @Test
    fun `참여하지 않은 커뮤니티 지도는 참여하기다`() {
        assertEquals(
            MapDetailAction.Join,
            map(MapType.Community, MapRole.None, joined = false).topBarAction,
        )
    }

    @Test
    fun `방장이 아닌 커뮤니티 멤버는 나갈 수 있다`() {
        assertEquals(
            MapDetailAction.Leave,
            map(MapType.Community, MapRole.Member, joined = true).topBarAction,
        )
        assertEquals(
            MapDetailAction.Leave,
            map(MapType.Community, MapRole.Admin, joined = true).topBarAction,
        )
    }

    @Test
    fun `커뮤니티 방장은 나가기가 비활성이다`() {
        // 서버가 OWNER 의 탈퇴를 거절한다. 눌러도 에러만 난다.
        assertEquals(
            MapDetailAction.LeaveDisabled,
            map(MapType.Community, MapRole.Owner, joined = true, memberCount = 1).topBarAction,
        )
    }

    @Test
    fun `프라이빗 멤버는 나갈 수 있다`() {
        assertEquals(
            MapDetailAction.Leave,
            map(MapType.Private, MapRole.Member, joined = true, memberCount = 3).topBarAction,
        )
    }

    @Test
    fun `프라이빗 지도에 만든 사람 혼자 남으면 나갈 수 있다`() {
        val map = map(MapType.Private, MapRole.Owner, joined = true, memberCount = 1)

        assertEquals(MapDetailAction.Leave, map.topBarAction)
        // 이때 나가기는 지도를 없애는 것과 같아 삭제로 처리한다.
        assertTrue(map.leavingDeletesMap)
    }

    @Test
    fun `프라이빗 지도에 다른 멤버가 남아 있으면 만든 사람은 나갈 수 없다`() {
        // 소유권 이전 API 가 없어 프론트에서 할 수 있는 게 없다.
        val map = map(MapType.Private, MapRole.Owner, joined = true, memberCount = 3)

        assertEquals(MapDetailAction.LeaveDisabled, map.topBarAction)
        // 남의 지도까지 날아가면 안 된다. 삭제 판단이 인원 수를 스스로 확인해야 한다.
        assertFalse(map.leavingDeletesMap)
    }

    @Test
    fun `참여하지 않은 프라이빗 지도는 삭제 대상이 아니다`() {
        assertFalse(map(MapType.Private, MapRole.Owner, joined = false).leavingDeletesMap)
    }

    @Test
    fun `참여하지 않은 프라이빗 지도는 아무 액션도 없다`() {
        // 프라이빗은 초대 코드로만 합류한다. 참여하기를 띄울 수 없다.
        assertEquals(
            MapDetailAction.None,
            map(MapType.Private, MapRole.None, joined = false).topBarAction,
        )
    }

    @Test
    fun `나만의 지도는 나가기를 띄우지 않는다`() {
        // 서버가 PRIVATE·OWNER·혼자로 내려주므로 `프라이빗에 혼자 남은 방장` 과 모양이 같다.
        // personal 을 먼저 보지 않으면 나가기가 뜨고, 그 나가기는 삭제로 처리된다.
        val map = map(MapType.Private, MapRole.Owner, joined = true, personal = true)

        assertEquals(MapDetailAction.None, map.topBarAction)
        assertFalse(map.leavingDeletesMap)
    }

    @Test
    fun `커뮤니티 지도에서 나가는 것은 지도 삭제가 아니다`() {
        assertFalse(map(MapType.Community, MapRole.Owner, joined = true).leavingDeletesMap)
        assertFalse(map(MapType.Community, MapRole.Member, joined = true).leavingDeletesMap)
        assertFalse(map(MapType.Private, MapRole.Member, joined = true).leavingDeletesMap)
    }

    // ---------- 역할 문자열 ----------

    @Test
    fun `모르는 역할 문자열은 권한 없음으로 본다`() {
        assertEquals(MapRole.Owner, MapRole.from("OWNER"))
        assertEquals(MapRole.Admin, MapRole.from("ADMIN"))
        assertEquals(MapRole.Member, MapRole.from("MEMBER"))
        assertEquals(MapRole.None, MapRole.from("NONE"))
        assertEquals(MapRole.None, MapRole.from(null))
        assertEquals(MapRole.None, MapRole.from("VIEWER"))
    }
}
