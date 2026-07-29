package com.example.moamap.feature.collection.data.repository

import com.example.moamap.core.network.model.PageResponse
import com.example.moamap.feature.collection.domain.model.PlaceExtractionException
import com.example.moamap.feature.collection.instagram.CaptionExtractor
import com.example.moamap.feature.collection.instagram.CaptionResult
import com.example.moamap.feature.explore.data.remote.InstagramExtractRequestDto
import com.example.moamap.feature.explore.data.remote.PhotoUploadUrlDto
import com.example.moamap.feature.explore.data.remote.PhotoUploadUrlRequestDto
import com.example.moamap.feature.explore.data.remote.PlaceCandidateDto
import com.example.moamap.feature.explore.data.remote.PlaceCreateRequestDto
import com.example.moamap.feature.explore.data.remote.PlaceDto
import com.example.moamap.feature.explore.data.remote.PlaceService
import com.example.moamap.feature.explore.data.remote.PlaceUpdateRequestDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeCaptionExtractor(private val result: CaptionResult) : CaptionExtractor {

    var lastUrl: String? = null
        private set

    override suspend fun extract(rawUrl: String): CaptionResult {
        lastUrl = rawUrl
        return result
    }
}

/** 추출 API 외의 메서드는 이 테스트에서 쓰지 않는다. */
private class FakePlaceService(
    private val candidates: List<PlaceCandidateDto> = emptyList(),
) : PlaceService {

    var lastRequest: InstagramExtractRequestDto? = null
        private set

    override suspend fun extractFromInstagram(
        request: InstagramExtractRequestDto,
    ): List<PlaceCandidateDto> {
        lastRequest = request
        return candidates
    }

    override suspend fun getPlaces(mapId: Long, page: Int?, size: Int?, sort: String?) =
        TODO("사용하지 않음")

    override suspend fun createPlace(request: PlaceCreateRequestDto) = TODO("사용하지 않음")
    override suspend fun getPendingPlaces(page: Int?, size: Int?, sort: String?): PageResponse<PlaceDto> =
        TODO("사용하지 않음")

    override suspend fun getPlace(id: Long) = TODO("사용하지 않음")
    override suspend fun updatePlace(id: Long, request: PlaceUpdateRequestDto) = TODO("사용하지 않음")
    override suspend fun deletePlace(id: Long) = TODO("사용하지 않음")
    override suspend fun approvePlace(id: Long) = TODO("사용하지 않음")
    override suspend fun rejectPlace(id: Long) = TODO("사용하지 않음")
    override suspend fun createPhotoUploadUrls(
        request: PhotoUploadUrlRequestDto,
    ): List<PhotoUploadUrlDto> = TODO("사용하지 않음")
}

class PlaceImportRepositoryImplTest {

    private fun candidate(
        kakaoPlaceId: String? = "kakao-1",
        name: String? = "커피나무",
        address: String? = "서울 동작구 상도동 1",
        roadAddress: String? = "서울 동작구 상도로 369",
    ) = PlaceCandidateDto(
        kakaoPlaceId = kakaoPlaceId,
        name = name,
        address = address,
        roadAddress = roadAddress,
    )

    private fun repository(
        caption: CaptionResult = CaptionResult.Success("캡션 전문"),
        service: FakePlaceService = FakePlaceService(),
        extractor: FakeCaptionExtractor = FakeCaptionExtractor(caption),
    ) = PlaceImportRepositoryImpl(extractor, service)

    @Test
    fun `비공개 게시물이면 서버를 호출하지 않고 실패한다`() = runTest {
        val service = FakePlaceService()
        val repository = repository(caption = CaptionResult.Blocked, service = service)

        val error = runCatching { repository.extractPlaces("url") }.exceptionOrNull()

        assertTrue(error is PlaceExtractionException.CaptionBlocked)
        // 보낼 캡션이 없으므로 서버를 부를 이유가 없다.
        assertNull(service.lastRequest)
    }

    @Test
    fun `캡션을 읽지 못하면 링크 확인 안내로 실패한다`() = runTest {
        val repository = repository(caption = CaptionResult.Error("shortcode 없음"))

        val error = runCatching { repository.extractPlaces("url") }.exceptionOrNull()

        assertTrue(error is PlaceExtractionException.CaptionUnavailable)
    }

    @Test
    fun `인스타그램에 닿지 못하면 네트워크 안내로 실패한다`() = runTest {
        // 링크가 잘못된 것과 구분하지 않으면 통신 장애에 링크를 확인하라고 안내하게 된다.
        val repository = repository(caption = CaptionResult.NetworkError("timeout"))

        val error = runCatching { repository.extractPlaces("url") }.exceptionOrNull()

        assertTrue(error is PlaceExtractionException.CaptionNetworkError)
    }

    @Test
    fun `캡션 추출과 서버 요청이 같은 URL을 쓴다`() = runTest {
        val extractor = FakeCaptionExtractor(CaptionResult.Success("캡션"))
        val service = FakePlaceService(listOf(candidate()))

        repository(service = service, extractor = extractor)
            .extractPlaces("  https://www.instagram.com/reel/ABC/  ")

        assertEquals("https://www.instagram.com/reel/ABC/", extractor.lastUrl)
        assertEquals("https://www.instagram.com/reel/ABC/", service.lastRequest?.url)
    }

    @Test
    fun `추출한 캡션 전문을 URL과 함께 서버로 보낸다`() = runTest {
        val service = FakePlaceService(listOf(candidate()))

        repository(caption = CaptionResult.Success("성수 카페 추천"), service = service)
            .extractPlaces("https://www.instagram.com/reel/ABC/")

        assertEquals("성수 카페 추천", service.lastRequest?.description)
        assertEquals("https://www.instagram.com/reel/ABC/", service.lastRequest?.url)
    }

    @Test
    fun `도로명 주소를 우선하고 없으면 지번 주소를 쓴다`() = runTest {
        val service = FakePlaceService(
            listOf(
                candidate(kakaoPlaceId = "a", roadAddress = "서울 동작구 상도로 369"),
                candidate(kakaoPlaceId = "b", roadAddress = null, address = "서울 동작구 상도동 1"),
            ),
        )

        val places = repository(service = service).extractPlaces("url")

        assertEquals("서울 동작구 상도로 369", places[0].address)
        assertEquals("서울 동작구 상도동 1", places[1].address)
    }

    @Test
    fun `kakaoPlaceId가 없으면 순번으로 id를 만든다`() = runTest {
        // 서버 응답에 고유 id 가 없어 선택 상태를 구분하지 못하면 안 된다.
        val service = FakePlaceService(
            listOf(candidate(kakaoPlaceId = null), candidate(kakaoPlaceId = "")),
        )

        val places = repository(service = service).extractPlaces("url")

        assertEquals(listOf("candidate-0", "candidate-1"), places.map { it.id })
    }

    @Test
    fun `이름 없는 후보는 목록에서 걸러낸다`() = runTest {
        val service = FakePlaceService(
            listOf(candidate(name = "커피나무"), candidate(name = null), candidate(name = " ")),
        )

        val places = repository(service = service).extractPlaces("url")

        assertEquals(listOf("커피나무"), places.map { it.name })
    }
}
