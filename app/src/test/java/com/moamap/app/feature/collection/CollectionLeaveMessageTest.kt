package com.moamap.app.feature.collection

import com.moamap.app.feature.collection.domain.model.MapType
import org.junit.Assert.assertEquals
import org.junit.Test

class CollectionLeaveMessageTest {

    @Test
    fun `커뮤니티 탭에서 나가면 커뮤니티 탭에서 사라진다고 알린다`() {
        assertEquals("나가면 커뮤니티 탭에서 지도가 사라집니다", collectionLeaveMessage(MapType.Community))
    }

    @Test
    fun `프라이빗 탭에서 나가면 사라진다고 알리고 초대 코드가 필요하다고 덧붙인다`() {
        assertEquals(
            "나가면 프라이빗 탭에서 지도가 사라집니다\n다시 참여하려면 초대 코드가 필요해요",
            collectionLeaveMessage(MapType.Private),
        )
    }
}
