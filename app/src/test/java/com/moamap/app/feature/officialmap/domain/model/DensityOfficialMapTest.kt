package com.moamap.app.feature.officialmap.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DensityOfficialMapTest {

    @Test
    fun `서버 공식지도 이름이 유동인구 지도면 유동인구 화면으로 연다`() {
        assertTrue(isDensityOfficialMap(official = true, title = "유동인구 지도"))
    }

    @Test
    fun `공식지도가 아니면 이름이 같아도 일반 지도다`() {
        // 사용자가 커뮤니티 지도 이름을 똑같이 지을 수 있다.
        assertFalse(isDensityOfficialMap(official = false, title = "유동인구 지도"))
    }

    @Test
    fun `다른 공식지도는 일반 지도로 연다`() {
        assertFalse(isDensityOfficialMap(official = true, title = "공중화장실 지도"))
        assertFalse(isDensityOfficialMap(official = true, title = "실시간 유동인구 지도"))
    }
}
