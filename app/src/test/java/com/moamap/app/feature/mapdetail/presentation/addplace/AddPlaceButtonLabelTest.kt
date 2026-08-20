package com.moamap.app.feature.mapdetail.presentation.addplace

import com.moamap.app.feature.collection.domain.model.MapType
import com.moamap.app.feature.mapdetail.domain.model.MapDetail
import com.moamap.app.feature.mapdetail.domain.model.MapRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 권한별 버튼 글씨.
 *
 * 실제 등록 상태(APPROVED/PENDING)는 서버가 정한다. 클라이언트는 글씨와 완료 안내만 고른다.
 */
class AddPlaceButtonLabelTest {

    private fun map(type: MapType, role: MapRole) = MapDetail(
        id = 1L,
        title = "지도",
        description = null,
        imageUrl = null,
        ownerName = null,
        type = type,
        role = role,
        tags = emptyList(),
        memberCount = 1,
        placeCount = 0,
        joined = true,
        personal = false,
        inviteCode = null,
    )

    @Test
    fun `프라이빗 지도는 역할과 무관하게 바로 추가한다`() {
        // 권한 분류가 없는 지도다.
        listOf(MapRole.Owner, MapRole.Admin, MapRole.Member).forEach { role ->
            val map = map(MapType.Private, role)

            assertTrue(map.addsPlaceDirectly)
            assertEquals("추가하기", addPlaceButtonLabel(map))
        }
    }

    @Test
    fun `커뮤니티 방장과 관리자는 바로 추가한다`() {
        listOf(MapRole.Owner, MapRole.Admin).forEach { role ->
            val map = map(MapType.Community, role)

            assertTrue(map.addsPlaceDirectly)
            assertEquals("추가하기", addPlaceButtonLabel(map))
        }
    }

    @Test
    fun `커뮤니티 일반 멤버는 추가 요청을 보낸다`() {
        val map = map(MapType.Community, MapRole.Member)

        assertFalse(map.addsPlaceDirectly)
        assertEquals("추가 요청 보내기", addPlaceButtonLabel(map))
    }

    @Test
    fun `완료 안내가 등록과 승인 대기로 갈린다`() {
        // 승인 대기는 목록에 나타나지 않는다. 안내가 없으면 사라진 것처럼 보인다.
        assertEquals(
            "장소를 추가했어요",
            addPlaceDoneMessage(map(MapType.Community, MapRole.Owner)),
        )
        assertEquals(
            "추가 요청을 보냈어요",
            addPlaceDoneMessage(map(MapType.Community, MapRole.Member)),
        )
        assertEquals(
            "장소를 추가했어요",
            addPlaceDoneMessage(map(MapType.Private, MapRole.Member)),
        )
    }
}
