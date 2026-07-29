package com.example.moamap.feature.mapdetail.data.repository

import android.net.Uri
import com.example.moamap.core.network.model.PageResponse
import com.example.moamap.feature.explore.data.remote.InstagramExtractRequestDto
import com.example.moamap.feature.explore.data.remote.MapShareExtractRequestDto
import com.example.moamap.feature.explore.data.remote.MapShareExtractResponseDto
import com.example.moamap.feature.explore.data.remote.PhotoUploadUrlDto
import com.example.moamap.feature.explore.data.remote.PhotoUploadUrlRequestDto
import com.example.moamap.feature.explore.data.remote.PlaceBulkCreateRequestDto
import com.example.moamap.feature.explore.data.remote.PlaceBulkCreateResponseDto
import com.example.moamap.feature.explore.data.remote.PlaceCandidateDto
import com.example.moamap.feature.explore.data.remote.PlaceCreateRequestDto
import com.example.moamap.feature.explore.data.remote.PlaceDto
import com.example.moamap.feature.explore.data.remote.PlaceService
import com.example.moamap.feature.explore.data.remote.PlaceUpdateRequestDto
import com.example.moamap.feature.mapdetail.domain.model.NewPlace
import com.example.moamap.feature.mapdetail.domain.model.PlaceCandidate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 등록 요청만 받아 두는 가짜. 나머지는 이 테스트에서 쓰지 않는다. */
private class RecordingPlaceService : PlaceService {

    var lastCreate: PlaceCreateRequestDto? = null
        private set
    var lastUploadRequest: PhotoUploadUrlRequestDto? = null
        private set
    var uploadUrls: List<PhotoUploadUrlDto> = emptyList()

    override suspend fun createPlace(request: PlaceCreateRequestDto): PlaceDto {
        lastCreate = request
        return PlaceDto()
    }

    override suspend fun createPhotoUploadUrls(
        request: PhotoUploadUrlRequestDto,
    ): List<PhotoUploadUrlDto> {
        lastUploadRequest = request
        return uploadUrls
    }

    override suspend fun getPlaces(mapId: Long, page: Int?, size: Int?, sort: String?) =
        TODO("사용하지 않음")

    override suspend fun getPendingPlaces(page: Int?, size: Int?, sort: String?): PageResponse<PlaceDto> =
        TODO("사용하지 않음")

    override suspend fun getPlace(id: Long) = TODO("사용하지 않음")
    override suspend fun updatePlace(id: Long, request: PlaceUpdateRequestDto) = TODO("사용하지 않음")
    override suspend fun deletePlace(id: Long) = TODO("사용하지 않음")
    override suspend fun approvePlace(id: Long) = TODO("사용하지 않음")
    override suspend fun rejectPlace(id: Long) = TODO("사용하지 않음")
    override suspend fun extractFromInstagram(
        request: InstagramExtractRequestDto,
    ): List<PlaceCandidateDto> = TODO("사용하지 않음")

    override suspend fun createPlacesBulk(
        request: PlaceBulkCreateRequestDto,
    ): PlaceBulkCreateResponseDto = TODO("사용하지 않음")

    override suspend fun extractFromMapShare(
        request: MapShareExtractRequestDto,
    ): MapShareExtractResponseDto = TODO("사용하지 않음")
}

/**
 * 사진 경로는 `Uri` 가 필요해 JVM 테스트에서 만들 수 없다(모킹 라이브러리가 없다).
 * 사진이 없는 경로만 검증하므로 이 가짜는 불려서는 안 된다.
 */
private class UnusedPhotoUploader : PhotoUploader {
    override suspend fun inspect(uri: Uri) = TODO("사진 없는 경로만 검증한다")
    override suspend fun upload(uploadUrl: String, photo: PhotoSpec) =
        TODO("사진 없는 경로만 검증한다")
}

class PlaceAddRepositoryImplTest {

    private val candidate = PlaceCandidate(
        kakaoPlaceId = "76206032",
        name = "스타벅스 더북한산점",
        address = "서울 은평구 진관동 277-11",
        roadAddress = "서울 은평구 대서문길 24-11",
        latitude = 37.6554573891378,
        longitude = 126.947556601185,
        category = "음식점 > 카페",
        placeUrl = "http://place.map.kakao.com/76206032",
    )

    private fun repository(service: PlaceService) =
        PlaceAddRepositoryImpl(placeService = service, uploader = UnusedPhotoUploader())

    private fun newPlace(
        tags: List<String> = emptyList(),
        memo: String? = null,
        photoUrls: List<String> = emptyList(),
    ) = NewPlace(candidate = candidate, tags = tags, memo = memo, photoUrls = photoUrls)

    @Test
    fun `카카오 검색으로 들어온 장소임을 표시한다`() = runTest {
        val service = RecordingPlaceService()

        repository(service).addPlace(mapId = 7L, newPlace = newPlace())

        assertEquals("KAKAO_SEARCH", service.lastCreate?.sourceType)
    }

    @Test
    fun `후보의 좌표와 식별자를 그대로 보낸다`() = runTest {
        val service = RecordingPlaceService()

        repository(service).addPlace(mapId = 7L, newPlace = newPlace())

        val request = service.lastCreate
        assertEquals(7L, request?.mapId)
        assertEquals("76206032", request?.kakaoPlaceId)
        assertEquals(37.6554573891378, request?.lat)
        assertEquals(126.947556601185, request?.lng)
        assertEquals("서울 은평구 진관동 277-11", request?.address)
        assertEquals("서울 은평구 대서문길 24-11", request?.roadAddress)
        assertEquals("음식점 > 카페", request?.category)
        assertEquals("http://place.map.kakao.com/76206032", request?.sourceUrl)
    }

    @Test
    fun `태그가 비면 보내지 않는다`() = runTest {
        val service = RecordingPlaceService()

        repository(service).addPlace(mapId = 7L, newPlace = newPlace(tags = emptyList()))

        assertNull(service.lastCreate?.tags)
    }

    @Test
    fun `태그가 있으면 그대로 보낸다`() = runTest {
        val service = RecordingPlaceService()

        repository(service).addPlace(mapId = 7L, newPlace = newPlace(tags = listOf("성수", "카페")))

        assertEquals(listOf("성수", "카페"), service.lastCreate?.tags)
    }

    @Test
    fun `메모가 공백뿐이면 보내지 않는다`() = runTest {
        val service = RecordingPlaceService()

        repository(service).addPlace(mapId = 7L, newPlace = newPlace(memo = "   "))

        assertNull(service.lastCreate?.description)
    }

    @Test
    fun `메모를 설명으로 보낸다`() = runTest {
        val service = RecordingPlaceService()

        repository(service).addPlace(mapId = 7L, newPlace = newPlace(memo = "주말에 가기 좋아요"))

        assertEquals("주말에 가기 좋아요", service.lastCreate?.description)
    }

    @Test
    fun `사진이 없으면 보내지 않는다`() = runTest {
        val service = RecordingPlaceService()

        repository(service).addPlace(mapId = 7L, newPlace = newPlace())

        assertNull(service.lastCreate?.photoUrls)
    }

    @Test
    fun `올린 사진 주소를 그대로 보낸다`() = runTest {
        val service = RecordingPlaceService()

        repository(service).addPlace(
            mapId = 7L,
            newPlace = newPlace(photoUrls = listOf("https://img/1.jpg")),
        )

        assertEquals(listOf("https://img/1.jpg"), service.lastCreate?.photoUrls)
    }

    @Test
    fun `사진이 없으면 발급을 부르지 않는다`() = runTest {
        val service = RecordingPlaceService()

        val urls = repository(service).uploadPhotos(mapId = 7L, photos = emptyList())

        assertTrue(urls.isEmpty())
        assertNull(service.lastUploadRequest)
    }
}
