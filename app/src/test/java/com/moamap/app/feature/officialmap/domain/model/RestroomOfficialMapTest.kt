package com.moamap.app.feature.officialmap.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RestroomOfficialMapTest {

    @Test
    fun `서버 공식지도 이름이 공중화장실 지도면 화장실 화면으로 연다`() {
        assertTrue(isRestroomOfficialMap(official = true, title = "공중화장실 지도"))
    }

    @Test
    fun `공식지도가 아니면 이름이 같아도 일반 지도다`() {
        assertFalse(isRestroomOfficialMap(official = false, title = "공중화장실 지도"))
    }

    @Test
    fun `유동인구 지도나 다른 공식지도는 화장실 화면이 아니다`() {
        assertFalse(isRestroomOfficialMap(official = true, title = "유동인구 지도"))
        assertFalse(isRestroomOfficialMap(official = true, title = "화장실 지도"))
    }
}
