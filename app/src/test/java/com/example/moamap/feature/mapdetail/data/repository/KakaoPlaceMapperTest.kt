package com.example.moamap.feature.mapdetail.data.repository

import com.example.moamap.feature.mapdetail.data.remote.KakaoPlaceDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KakaoPlaceMapperTest {

    /** 실제 카카오 응답에서 가져온 값이다. */
    private fun starbucks(
        id: String = "76206032",
        placeName: String = "스타벅스 더북한산점",
        addressName: String? = "서울 은평구 진관동 277-11",
        roadAddressName: String? = "서울 은평구 대서문길 24-11",
        x: String = "126.947556601185",
        y: String = "37.6554573891378",
        categoryName: String? = "음식점 > 카페 > 커피전문점 > 스타벅스",
        placeUrl: String? = "http://place.map.kakao.com/76206032",
    ) = KakaoPlaceDto(
        id = id,
        placeName = placeName,
        addressName = addressName,
        roadAddressName = roadAddressName,
        x = x,
        y = y,
        categoryName = categoryName,
        placeUrl = placeUrl,
    )

    @Test
    fun `x는 경도이고 y는 위도다`() {
        val candidate = starbucks().toPlaceCandidate()

        // 뒤집으면 서울(위도 37, 경도 127)이 중국 근처가 된다.
        assertEquals(37.6554573891378, candidate?.latitude)
        assertEquals(126.947556601185, candidate?.longitude)
    }

    @Test
    fun `좌표가 숫자가 아니면 후보로 쓰지 않는다`() {
        assertNull(starbucks(x = "").toPlaceCandidate())
        assertNull(starbucks(y = "없음").toPlaceCandidate())
    }

    @Test
    fun `좌표가 지구 밖이면 후보로 쓰지 않는다`() {
        assertNull(starbucks(x = "999").toPlaceCandidate())
        assertNull(starbucks(y = "-91").toPlaceCandidate())
    }

    @Test
    fun `NaN 이나 Infinity 도 걸러낸다`() {
        // toDoubleOrNull 이 이 둘을 읽어 내므로 범위 검사가 없으면 통과해 버린다.
        assertNull(starbucks(x = "NaN").toPlaceCandidate())
        assertNull(starbucks(y = "Infinity").toPlaceCandidate())
    }

    @Test
    fun `id가 없으면 후보로 쓰지 않는다`() {
        // kakaoPlaceId 는 서버 등록 필수값이다.
        assertNull(starbucks(id = "").toPlaceCandidate())
    }

    @Test
    fun `이름이 없으면 후보로 쓰지 않는다`() {
        assertNull(starbucks(placeName = "").toPlaceCandidate())
    }

    @Test
    fun `지번과 도로명 주소를 둘 다 옮긴다`() {
        val candidate = starbucks().toPlaceCandidate()

        assertEquals("서울 은평구 진관동 277-11", candidate?.address)
        assertEquals("서울 은평구 대서문길 24-11", candidate?.roadAddress)
    }

    @Test
    fun `보여줄 주소는 도로명을 우선한다`() {
        assertEquals(
            "서울 은평구 대서문길 24-11",
            starbucks().toPlaceCandidate()?.displayAddress,
        )
    }

    @Test
    fun `도로명이 없으면 지번 주소로 보여준다`() {
        assertEquals(
            "서울 은평구 진관동 277-11",
            starbucks(roadAddressName = " ").toPlaceCandidate()?.displayAddress,
        )
    }

    @Test
    fun `주소가 둘 다 없으면 보여줄 주소가 빈 문자열이다`() {
        val candidate = starbucks(addressName = null, roadAddressName = null).toPlaceCandidate()

        assertEquals("", candidate?.displayAddress)
    }

    @Test
    fun `분류 경로를 그대로 옮긴다`() {
        // 서버에 보내는 category 다. 그룹 코드는 18개 그룹 밖 장소에서 비어 와 쓰지 않는다.
        assertEquals(
            "음식점 > 카페 > 커피전문점 > 스타벅스",
            starbucks().toPlaceCandidate()?.category,
        )
        assertNull(starbucks(categoryName = " ").toPlaceCandidate()?.category)
    }

    @Test
    fun `카카오맵 주소를 옮긴다`() {
        assertEquals(
            "http://place.map.kakao.com/76206032",
            starbucks().toPlaceCandidate()?.placeUrl,
        )
        assertNull(starbucks(placeUrl = "").toPlaceCandidate()?.placeUrl)
    }
}
