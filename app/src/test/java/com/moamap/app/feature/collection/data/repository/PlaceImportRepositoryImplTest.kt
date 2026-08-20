package com.moamap.app.feature.collection.data.repository

import android.net.Uri
import com.moamap.app.core.common.upload.PhotoSpec
import com.moamap.app.core.common.upload.PhotoUploader
import com.moamap.app.core.network.model.PageResponse
import com.moamap.app.feature.collection.domain.model.EditedPlace
import com.moamap.app.feature.collection.domain.model.ImportedPlace
import com.moamap.app.feature.collection.domain.model.PlaceEdit
import com.moamap.app.feature.collection.domain.model.PlaceExtractionException
import com.moamap.app.feature.collection.instagram.CaptionExtractor
import com.moamap.app.feature.collection.instagram.CaptionResult
import com.moamap.app.feature.explore.data.remote.InstagramExtractRequestDto
import com.moamap.app.feature.explore.data.remote.MapShareExtractRequestDto
import com.moamap.app.feature.explore.data.remote.MapShareExtractResponseDto
import com.moamap.app.feature.explore.data.remote.MapSharePlaceCandidateDto
import com.moamap.app.feature.explore.data.remote.PhotoUploadUrlDto
import com.moamap.app.feature.explore.data.remote.PhotoUploadUrlRequestDto
import com.moamap.app.feature.explore.data.remote.PlaceActivityDto
import com.moamap.app.feature.explore.data.remote.PlaceBulkCreateRequestDto
import com.moamap.app.feature.explore.data.remote.PlaceBulkCreateResponseDto
import com.moamap.app.feature.explore.data.remote.PlaceBulkResultDto
import com.moamap.app.feature.explore.data.remote.PlaceCandidateDto
import com.moamap.app.feature.explore.data.remote.PlaceCreateRequestDto
import com.moamap.app.feature.explore.data.remote.PlaceDto
import com.moamap.app.feature.explore.data.remote.PlaceService
import com.moamap.app.feature.explore.data.remote.PlaceUpdateRequestDto
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
    private val mapShare: MapShareExtractResponseDto = MapShareExtractResponseDto(),
) : PlaceService {

    var lastRequest: InstagramExtractRequestDto? = null
        private set

    var lastMapShareRequest: MapShareExtractRequestDto? = null
        private set

    /** 일괄 등록은 지도마다 한 번씩 나가므로 요청을 모두 모은다. */
    val bulkRequests = mutableListOf<PlaceBulkCreateRequestDto>()

    /** 요청 순서대로 돌려줄 응답. 모자라면 전건 CREATED 로 채운다. */
    var bulkResponses: List<PlaceBulkCreateResponseDto> = emptyList()

    override suspend fun createPlacesBulk(
        request: PlaceBulkCreateRequestDto,
    ): PlaceBulkCreateResponseDto {
        val index = bulkRequests.size
        bulkRequests += request
        return bulkResponses.getOrElse(index) {
            PlaceBulkCreateResponseDto(
                requested = request.places.size,
                created = request.places.size,
                skipped = 0,
                results = request.places.mapIndexed { i, item ->
                    PlaceBulkResultDto(index = i, name = item.name, status = "CREATED")
                },
            )
        }
    }

    override suspend fun extractFromInstagram(
        request: InstagramExtractRequestDto,
    ): List<PlaceCandidateDto> {
        lastRequest = request
        return candidates
    }

    override suspend fun extractFromMapShare(
        request: MapShareExtractRequestDto,
    ): MapShareExtractResponseDto {
        lastMapShareRequest = request
        return mapShare
    }

    override suspend fun getPlaces(mapId: Long, page: Int?, size: Int?, sort: String?) =
        TODO("사용하지 않음")

    override suspend fun createPlace(request: PlaceCreateRequestDto) = TODO("사용하지 않음")
    override suspend fun getPendingPlaces(
        mapId: Long,
        page: Int?,
        size: Int?,
        sort: String?,
    ): PageResponse<PlaceDto> = TODO("사용하지 않음")

    override suspend fun getPlace(id: Long) = TODO("사용하지 않음")
    override suspend fun updatePlace(id: Long, request: PlaceUpdateRequestDto) = TODO("사용하지 않음")
    override suspend fun deletePlace(id: Long) = TODO("사용하지 않음")
    override suspend fun approvePlace(id: Long) = TODO("사용하지 않음")
    override suspend fun rejectPlace(id: Long) = TODO("사용하지 않음")
    override suspend fun createPhotoUploadUrls(
        request: PhotoUploadUrlRequestDto,
    ): List<PhotoUploadUrlDto> = TODO("사용하지 않음")

    override suspend fun getActivities(
        mapId: Long,
        page: Int?,
        size: Int?,
    ): PageResponse<PlaceActivityDto> = TODO("사용하지 않음")
}

/**
 * 사진 경로는 `Uri` 가 필요해 JVM 테스트에서 만들 수 없다(모킹 라이브러리가 없다).
 *
 * 그래서 올리는 동작 자체는 검증하지 않고, 이미 올라간 주소를 넘겼을 때 요청에 실리는지와
 * 사진이 없을 때 아예 부르지 않는지만 본다. 이 가짜는 불려서는 안 된다.
 */
private class UnusedPhotoUploader : PhotoUploader {
    override suspend fun inspect(uri: Uri) = TODO("사진 없는 경로만 검증한다")
    override suspend fun upload(uploadUrl: String, photo: PhotoSpec) =
        TODO("사진 없는 경로만 검증한다")
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
    ) = PlaceImportRepositoryImpl(extractor, service, UnusedPhotoUploader())

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

        assertEquals("서울 동작구 상도로 369", places[0].displayAddress)
        assertEquals("서울 동작구 상도동 1", places[1].displayAddress)
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

    // --- 지도 공유 링크 ---

    private fun shared(
        kakaoPlaceId: String? = "kakao-1",
        name: String? = "커피나무",
        address: String? = "서울 동작구 상도동 1",
        roadAddress: String? = "서울 동작구 상도로 369",
    ) = MapSharePlaceCandidateDto(
        kakaoPlaceId = kakaoPlaceId,
        name = name,
        address = address,
        roadAddress = roadAddress,
    )

    private fun mapShareService(
        matched: List<MapSharePlaceCandidateDto> = listOf(shared()),
    ) = FakePlaceService(mapShare = MapShareExtractResponseDto(matched = matched))

    @Test
    fun `공유 링크는 캡션을 읽지 않고 그대로 서버로 보낸다`() = runTest {
        val extractor = FakeCaptionExtractor(CaptionResult.Success("캡션"))
        val service = mapShareService()

        repository(service = service, extractor = extractor)
            .extractMapSharePlaces("  https://naver.me/xAbC1234  ")

        assertEquals("https://naver.me/xAbC1234", service.lastMapShareRequest?.url)
        // 서버가 링크를 직접 열어보므로 앱이 긁을 캡션이 없다.
        assertNull(extractor.lastUrl)
    }

    @Test
    fun `matched 를 장소 목록으로 바꾼다`() = runTest {
        val service = mapShareService(
            matched = listOf(
                shared(kakaoPlaceId = "a", roadAddress = "서울 동작구 상도로 369"),
                shared(kakaoPlaceId = "b", roadAddress = null, address = "서울 동작구 상도동 1"),
            ),
        )

        val places = repository(service = service).extractMapSharePlaces("url")

        assertEquals(listOf("a", "b"), places.map { it.id })
        assertEquals("서울 동작구 상도로 369", places[0].displayAddress)
        assertEquals("서울 동작구 상도동 1", places[1].displayAddress)
    }

    @Test
    fun `공유 링크에서도 이름 없는 항목은 걸러낸다`() = runTest {
        val service = mapShareService(
            matched = listOf(shared(name = "커피나무"), shared(name = null), shared(name = " ")),
        )

        val places = repository(service = service).extractMapSharePlaces("url")

        assertEquals(listOf("커피나무"), places.map { it.name })
    }

    @Test
    fun `공유 링크에서도 kakaoPlaceId 가 없으면 순번으로 id 를 만든다`() = runTest {
        val service = mapShareService(
            matched = listOf(shared(kakaoPlaceId = null), shared(kakaoPlaceId = "")),
        )

        val places = repository(service = service).extractMapSharePlaces("url")

        assertEquals(listOf("candidate-0", "candidate-1"), places.map { it.id })
    }

    // --- 일괄 등록 ---

    private fun importedPlace(
        id: String = "kakao-1",
        name: String = "커피나무",
    ) = ImportedPlace(
        id = id,
        name = name,
        address = "서울 동작구 상도동 1",
        roadAddress = "서울 동작구 상도로 369",
        lat = 37.5,
        lng = 127.0,
        category = "음식점 > 카페",
        description = "메모",
        kakaoPlaceId = id,
        sourceType = "INSTAGRAM",
        sourceUrl = "https://www.instagram.com/reel/ABC/",
    )

    /** 화면이 편집값을 만드는 방식과 같다. 손대지 않은 장소는 원래 메모가 그대로 실린다. */
    private fun edited(place: ImportedPlace, edit: PlaceEdit? = null) =
        EditedPlace(place, edit ?: PlaceEdit(memo = place.description.orEmpty()))

    @Test
    fun `고른 지도마다 한 번씩 일괄 등록을 부른다`() = runTest {
        // 서버가 요청 하나에 지도 하나만 받는다.
        val service = FakePlaceService()

        repository(service = service).savePlaces(
            mapIds = setOf(11L, 12L),
            places = listOf(edited(importedPlace("a")), edited(importedPlace("b"))),
            photoUrls = emptyMap(),
        )

        assertEquals(listOf(11L, 12L), service.bulkRequests.map { it.mapId })
        assertEquals(listOf(2, 2), service.bulkRequests.map { it.places.size })
    }

    @Test
    fun `등록 요청에 장소 값을 그대로 담는다`() = runTest {
        val service = FakePlaceService()

        repository(service = service)
            .savePlaces(setOf(11L), listOf(edited(importedPlace("kakao-9"))), emptyMap())

        val item = service.bulkRequests.single().places.single()
        assertEquals("커피나무", item.name)
        assertEquals("서울 동작구 상도동 1", item.address)
        assertEquals("서울 동작구 상도로 369", item.roadAddress)
        assertEquals(37.5, item.lat, 0.0)
        assertEquals(127.0, item.lng, 0.0)
        assertEquals("음식점 > 카페", item.category)
        assertEquals("kakao-9", item.kakaoPlaceId)
        assertEquals("INSTAGRAM", item.sourceType)
        assertEquals("https://www.instagram.com/reel/ABC/", item.sourceUrl)
        assertEquals("메모", item.description)
    }

    @Test
    fun `한 번에 보낼 수 있는 수를 넘으면 나눠 보낸다`() = runTest {
        // 서버가 요청당 100 개로 제한한다.
        val service = FakePlaceService()
        val places = (1..101).map { index -> edited(importedPlace("kakao-$index")) }

        repository(service = service).savePlaces(setOf(11L), places, emptyMap())

        assertEquals(listOf(100, 1), service.bulkRequests.map { it.places.size })
    }

    @Test
    fun `여러 지도의 결과를 상태별로 합산한다`() = runTest {
        val service = FakePlaceService()
        service.bulkResponses = listOf(
            bulkResponse("CREATED", "DUPLICATE"),
            bulkResponse("CREATED", "FAILED"),
        )

        val result = repository(service = service).savePlaces(
            mapIds = setOf(11L, 12L),
            places = listOf(edited(importedPlace("a")), edited(importedPlace("b"))),
            photoUrls = emptyMap(),
        )

        assertEquals(2, result.created)
        assertEquals(1, result.duplicate)
        assertEquals(1, result.failed)
    }

    @Test
    fun `편집한 태그와 메모를 등록 요청에 담는다`() = runTest {
        val service = FakePlaceService()
        val place = importedPlace("kakao-9")

        repository(service = service).savePlaces(
            mapIds = setOf(11L),
            places = listOf(edited(place, PlaceEdit(tags = listOf("성수", "카페"), memo = "창가 자리"))),
            photoUrls = emptyMap(),
        )

        val item = service.bulkRequests.single().places.single()
        assertEquals(listOf("성수", "카페"), item.tags)
        // 편집 화면의 메모가 곧 설명이다. 원래 값("메모")을 덮어쓴다.
        assertEquals("창가 자리", item.description)
    }

    @Test
    fun `붙인 것이 없으면 빈 값 대신 보내지 않는다`() = runTest {
        // 빈 목록이나 빈 문자열을 보내면 서버가 "지우라는 뜻"으로 받을 수 있다.
        val service = FakePlaceService()

        repository(service = service).savePlaces(
            mapIds = setOf(11L),
            places = listOf(edited(importedPlace("kakao-9"), PlaceEdit())),
            photoUrls = emptyMap(),
        )

        val item = service.bulkRequests.single().places.single()
        assertNull(item.tags)
        assertNull(item.description)
        assertNull(item.photoUrls)
    }

    @Test
    fun `올려 둔 사진 주소를 그 장소의 요청에만 담는다`() = runTest {
        val service = FakePlaceService()

        repository(service = service).savePlaces(
            mapIds = setOf(11L),
            places = listOf(edited(importedPlace("a")), edited(importedPlace("b"))),
            photoUrls = mapOf("a" to listOf("https://cdn/1.jpg", "https://cdn/2.jpg")),
        )

        val items = service.bulkRequests.single().places
        assertEquals(listOf("https://cdn/1.jpg", "https://cdn/2.jpg"), items[0].photoUrls)
        assertNull(items[1].photoUrls)
    }

    @Test
    fun `같은 사진 주소를 고른 지도 모두에 그대로 쓴다`() = runTest {
        // 같은 파일이라 지도 수만큼 올릴 이유가 없다.
        val service = FakePlaceService()

        repository(service = service).savePlaces(
            mapIds = setOf(11L, 12L),
            places = listOf(edited(importedPlace("a"))),
            photoUrls = mapOf("a" to listOf("https://cdn/1.jpg")),
        )

        assertEquals(2, service.bulkRequests.size)
        service.bulkRequests.forEach { request ->
            assertEquals(listOf("https://cdn/1.jpg"), request.places.single().photoUrls)
        }
    }

    @Test
    fun `사진을 붙이지 않았으면 발급을 부르지 않는다`() = runTest {
        // FakePlaceService 의 발급은 TODO 라 불리면 터진다. 그게 이 테스트의 검증이다.
        val service = FakePlaceService()

        val urls = repository(service = service).uploadPhotos(
            mapId = 11L,
            places = listOf(edited(importedPlace("a")), edited(importedPlace("b"))),
        )

        assertTrue(urls.isEmpty())
    }

    private fun bulkResponse(vararg statuses: String) = PlaceBulkCreateResponseDto(
        requested = statuses.size,
        created = statuses.count { it == "CREATED" },
        skipped = statuses.count { it != "CREATED" },
        results = statuses.mapIndexed { index, status ->
            PlaceBulkResultDto(index = index, name = "장소$index", status = status)
        },
    )
}
