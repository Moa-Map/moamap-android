package com.moamap.app.feature.officialmap.data.repository

import com.moamap.app.feature.officialmap.data.remote.RestroomDetailDto
import com.moamap.app.feature.officialmap.data.remote.RestroomMarkerDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class RestroomMapperTest {

    @Test
    fun `이름이 비어 오면 기본 이름을 채운다`() {
        val marker = RestroomMarkerDto(id = 1, name = " ", lat = 37.5, lng = 126.9, category = "").toDomain()

        assertEquals("이름 없는 화장실", marker.name)
        assertNull(marker.category)
    }

    @Test
    fun `도로명 주소가 없으면 지번 주소를 쓴다`() {
        val detail = RestroomDetailDto(id = 1, roadAddress = "", lotAddress = "서울특별시 중구 정동 5-5").toDomain()

        assertEquals("서울특별시 중구 정동 5-5", detail.address)
    }

    @Test
    fun `비어 온 칸 수는 0, 편의시설은 없음으로 본다`() {
        val detail = RestroomDetailDto(id = 1).toDomain()

        assertEquals(0, detail.maleToilet)
        assertEquals(0, detail.femaleChildToilet)
        assertFalse(detail.diaperTable)
        assertNull(detail.address)
    }
}
