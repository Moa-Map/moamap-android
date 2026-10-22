package com.moamap.app.feature.collection

import com.moamap.app.feature.collection.domain.model.MyMap
import com.moamap.app.feature.mapdetail.domain.model.MapRole
import org.junit.Assert.assertEquals
import org.junit.Test

class CommunityUiModelTest {

    private fun myMap(official: Boolean, role: MapRole) = MyMap(
        id = 1L,
        title = "성수 카페 투어",
        imageUrl = null,
        memberCount = 1,
        placeCount = 0,
        official = official,
        personal = false,
        role = role,
    )

    @Test
    fun `커뮤니티 지도는 목록의 내 역할로 뱃지를 단다`() {
        assertEquals(MapRole.Owner, myMap(official = false, role = MapRole.Owner).toCommunityUiModel().role)
        assertEquals(MapRole.Admin, myMap(official = false, role = MapRole.Admin).toCommunityUiModel().role)
    }

    @Test
    fun `공식지도는 역할이 있어도 뱃지를 달지 않는다`() {
        assertEquals(MapRole.None, myMap(official = true, role = MapRole.Owner).toCommunityUiModel().role)
    }
}
