package com.moamap.app.feature.mapdetail

import org.junit.Assert.assertEquals
import org.junit.Test

class KakaoMapLinkTest {

    @Test
    fun `카카오맵 앱과 웹 주소는 장소 id 로 만든다`() {
        assertEquals("kakaomap://place?id=76206032", kakaoMapAppUrl("76206032"))
        assertEquals("https://place.map.kakao.com/76206032", kakaoMapWebUrl("76206032"))
    }

    @Test
    fun `이름 검색 주소는 한글과 공백을 경로에 맞게 인코딩한다`() {
        assertEquals(
            "https://map.kakao.com/link/search/%EC%B2%AD%EB%85%84%EB%8B%A4%EB%B0%A9%20%EB%B6%80%EC%B2%9C",
            kakaoMapSearchUrl(" 청년다방 부천 "),
        )
    }
}
