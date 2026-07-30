package com.example.moamap.feature.collection

import com.example.moamap.core.network.kakao.KakaoAddressNameDto
import com.example.moamap.core.network.kakao.KakaoCoordAddressDocumentDto
import com.example.moamap.core.network.kakao.KakaoCoordToAddressDto
import com.example.moamap.core.network.kakao.KakaoKeywordSearchDto
import com.example.moamap.core.network.kakao.KakaoLocalService
import com.example.moamap.core.network.kakao.KakaoPlaceDto
import com.example.moamap.feature.collection.data.repository.CoordinatePlaceFinder
import com.example.moamap.feature.collection.domain.model.PlaceExtractionException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** 호출 인자를 기록하는 fake. 이 저장소는 mocking 라이브러리를 쓰지 않는다. */
private class FakeKakaoLocalService(
    private val addressResponse: KakaoCoordToAddressDto = KakaoCoordToAddressDto(),
    private val keywordResponse: KakaoKeywordSearchDto = KakaoKeywordSearchDto(),
) : KakaoLocalService {

    var lastQuery: String? = null
    var lastX: String? = null
    var lastY: String? = null
    var lastRadius: Int? = null
    var lastSort: String? = null

    override suspend fun searchKeyword(
        query: String,
        size: Int?,
        x: String?,
        y: String?,
        radius: Int?,
        sort: String?,
    ): KakaoKeywordSearchDto {
        lastQuery = query
        lastX = x
        lastY = y
        lastRadius = radius
        lastSort = sort
        return keywordResponse
    }

    override suspend fun coordToAddress(lng: String, lat: String): KakaoCoordToAddressDto =
        addressResponse
}

class CoordinatePlaceFinderTest {

    private val soongsil = KakaoPlaceDto(
        id = "26338954",
        placeName = "숭실대학교",
        addressName = "서울 동작구 상도동 511",
        roadAddressName = "서울 동작구 상도로 369",
        x = "126.9574",
        y = "37.4963",
        categoryName = "교육,학문 > 학교 > 대학교",
    )

    private fun addressOf(road: String?, jibun: String?) = KakaoCoordToAddressDto(
        documents = listOf(
            KakaoCoordAddressDocumentDto(
                roadAddress = road?.let { KakaoAddressNameDto(it) },
                address = jibun?.let { KakaoAddressNameDto(it) },
            ),
        ),
    )

    @Test
    fun `좌표의 주소로 검색해 첫 결과를 돌려준다`() = runTest {
        val service = FakeKakaoLocalService(
            addressResponse = addressOf("서울 동작구 상도로 369", "서울 동작구 상도동 511"),
            keywordResponse = KakaoKeywordSearchDto(documents = listOf(soongsil)),
        )

        val places = CoordinatePlaceFinder(service).find(lat = 37.4963, lng = 126.9574)

        val place = places.single()
        assertEquals("숭실대학교", place.name)
        assertEquals("26338954", place.kakaoPlaceId)
        assertEquals(37.4963, place.lat, 0.00001)
        assertEquals(126.9574, place.lng, 0.00001)
        assertEquals("KAKAO_SEARCH", place.sourceType)
    }

    @Test
    fun `검색은 좌표와 거리순 정렬을 함께 보낸다`() = runTest {
        // 같은 주소 문자열이 전국에 여러 개 있을 수 있다. 좌표를 함께 보내야
        // 사용자가 서 있는 곳의 장소가 첫 번째로 온다.
        val service = FakeKakaoLocalService(
            addressResponse = addressOf("서울 동작구 상도로 369", null),
            keywordResponse = KakaoKeywordSearchDto(documents = listOf(soongsil)),
        )

        CoordinatePlaceFinder(service).find(lat = 37.4963, lng = 126.9574)

        assertEquals("서울 동작구 상도로 369", service.lastQuery)
        assertEquals("126.9574", service.lastX)
        assertEquals("37.4963", service.lastY)
        assertEquals(1000, service.lastRadius)
        assertEquals("distance", service.lastSort)
    }

    @Test
    fun `도로명이 없으면 지번으로 검색한다`() = runTest {
        val service = FakeKakaoLocalService(
            addressResponse = addressOf(null, "서울 동작구 상도동 산65"),
            keywordResponse = KakaoKeywordSearchDto(documents = listOf(soongsil)),
        )

        CoordinatePlaceFinder(service).find(lat = 37.4963, lng = 126.9574)

        assertEquals("서울 동작구 상도동 산65", service.lastQuery)
    }

    // 아래 세 개는 `= runTest` 를 쓰지 않는다. assertThrows 블록 안에서 runTest 를 돌려야
    // 하는데, runTest 안에서 runTest 를 다시 열면 테스트 스케줄러가 중첩되어 깨진다.

    @Test
    fun `주소를 얻지 못하면 예외를 던진다`() {
        val service = FakeKakaoLocalService(addressResponse = KakaoCoordToAddressDto())

        assertThrows(PlaceExtractionException.NoPlaceAtCoordinate::class.java) {
            runTest { CoordinatePlaceFinder(service).find(lat = 37.4963, lng = 126.9574) }
        }
    }

    @Test
    fun `검색 결과가 없으면 예외를 던진다`() {
        val service = FakeKakaoLocalService(
            addressResponse = addressOf("서울 동작구 상도로 369", null),
            keywordResponse = KakaoKeywordSearchDto(documents = emptyList()),
        )

        assertThrows(PlaceExtractionException.NoPlaceAtCoordinate::class.java) {
            runTest { CoordinatePlaceFinder(service).find(lat = 37.4963, lng = 126.9574) }
        }
    }

    @Test
    fun `등록 키가 없는 결과는 장소로 삼지 않는다`() {
        // kakaoPlaceId 가 없으면 서버가 등록을 거절한다. 목록에 올려도 사용자가 할 수 있는 일이 없다.
        val service = FakeKakaoLocalService(
            addressResponse = addressOf("서울 동작구 상도로 369", null),
            keywordResponse = KakaoKeywordSearchDto(documents = listOf(soongsil.copy(id = ""))),
        )

        assertThrows(PlaceExtractionException.NoPlaceAtCoordinate::class.java) {
            runTest { CoordinatePlaceFinder(service).find(lat = 37.4963, lng = 126.9574) }
        }
    }
}
