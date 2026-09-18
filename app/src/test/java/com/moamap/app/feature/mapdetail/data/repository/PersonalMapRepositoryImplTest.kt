package com.moamap.app.feature.mapdetail.data.repository

import com.moamap.app.core.network.model.PageResponse
import com.moamap.app.feature.collection.data.remote.CoverUploadUrlDto
import com.moamap.app.feature.collection.data.remote.CoverUploadUrlRequestDto
import com.moamap.app.feature.collection.data.remote.JoinByInviteCodeRequestDto
import com.moamap.app.feature.collection.data.remote.MapCreateRequestDto
import com.moamap.app.feature.collection.data.remote.MapDetailDto
import com.moamap.app.feature.collection.data.remote.MapMemberListDto
import com.moamap.app.feature.collection.data.remote.MapMemberRoleDto
import com.moamap.app.feature.collection.data.remote.MapMemberRoleUpdateDto
import com.moamap.app.feature.collection.data.remote.MapMemberRoleUpdateRequestDto
import com.moamap.app.feature.collection.data.remote.MapService
import com.moamap.app.feature.collection.data.remote.MapSummaryDto
import com.moamap.app.feature.collection.data.remote.MapUpdateRequestDto
import com.moamap.app.feature.explore.data.remote.InstagramExtractRequestDto
import com.moamap.app.feature.explore.data.remote.MapShareExtractRequestDto
import com.moamap.app.feature.explore.data.remote.MapShareExtractResponseDto
import com.moamap.app.feature.explore.data.remote.PendingPlaceDto
import com.moamap.app.feature.explore.data.remote.PhotoUploadUrlDto
import com.moamap.app.feature.explore.data.remote.PhotoUploadUrlRequestDto
import com.moamap.app.feature.explore.data.remote.PlaceActivityDto
import com.moamap.app.feature.explore.data.remote.PlaceBulkCreateRequestDto
import com.moamap.app.feature.explore.data.remote.PlaceBulkCreateResponseDto
import com.moamap.app.feature.explore.data.remote.PlaceCandidateDto
import com.moamap.app.feature.explore.data.remote.PlaceCreateRequestDto
import com.moamap.app.feature.explore.data.remote.PlaceDto
import com.moamap.app.feature.explore.data.remote.PlaceService
import com.moamap.app.feature.explore.data.remote.PlaceUpdateRequestDto
import com.moamap.app.feature.mapdetail.domain.repository.PersonalMapNotFoundException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** 장소 한 건을 돌려주고 등록 요청을 받아 둔다. */
private class CopyingPlaceService(private val place: PlaceDto) : PlaceService {

    val created = mutableListOf<PlaceCreateRequestDto>()
    val readIds = mutableListOf<Long>()

    override suspend fun getPlace(id: Long): PlaceDto {
        readIds += id
        return place
    }

    override suspend fun createPlace(request: PlaceCreateRequestDto): PlaceDto {
        created += request
        return PlaceDto()
    }

    override suspend fun getPlaces(mapId: Long, page: Int?, size: Int?, sort: String?) =
        TODO("사용하지 않음")

    override suspend fun getPendingPlaces(
        mapId: Long,
        page: Int?,
        size: Int?,
        sort: String?,
    ): PageResponse<PendingPlaceDto> = TODO("사용하지 않음")

    override suspend fun getActivities(
        mapId: Long,
        page: Int?,
        size: Int?,
    ): PageResponse<PlaceActivityDto> = TODO("사용하지 않음")

    override suspend fun createPhotoUploadUrls(
        request: PhotoUploadUrlRequestDto,
    ): List<PhotoUploadUrlDto> = TODO("사용하지 않음")

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

/** 내 프라이빗 지도 목록을 페이지로 나눠 돌려준다. */
private class PagedMyMapService(private val pages: List<List<MapSummaryDto>>) : MapService {

    val requests = mutableListOf<Pair<String, Int?>>()

    override suspend fun getMyMaps(
        type: String,
        page: Int?,
        size: Int?,
        sort: String?,
    ): PageResponse<MapSummaryDto> {
        requests += type to page
        val index = page ?: 0
        return PageResponse(
            content = pages.getOrElse(index) { emptyList() },
            page = index,
            last = index >= pages.lastIndex,
        )
    }

    override suspend fun getMaps(page: Int?, size: Int?, sort: String?) = notUsed()
    override suspend fun createMap(request: MapCreateRequestDto) = notUsed()
    override suspend fun createCoverUploadUrl(request: CoverUploadUrlRequestDto):
        CoverUploadUrlDto = notUsed()
    override suspend fun joinMap(mapId: Long): MapDetailDto = notUsed()
    override suspend fun joinByInviteCode(request: JoinByInviteCodeRequestDto) = notUsed()
    override suspend fun getMap(mapId: Long) = notUsed()
    override suspend fun updateMap(mapId: Long, request: MapUpdateRequestDto) = notUsed()
    override suspend fun deleteMap(mapId: Long): Unit = notUsed()
    override suspend fun leaveMap(mapId: Long): Unit = notUsed()
    override suspend fun getMemberRole(mapId: Long, userId: Long): MapMemberRoleDto = notUsed()
    override suspend fun getMembers(mapId: Long): MapMemberListDto = notUsed()
    override suspend fun updateMemberRole(
        mapId: Long,
        userId: Long,
        request: MapMemberRoleUpdateRequestDto,
    ): MapMemberRoleUpdateDto = notUsed()

    private fun notUsed(): Nothing = error("나만의 지도 저장소가 부를 일이 없는 호출이다")
}

class PersonalMapRepositoryImplTest {

    private val place = PlaceDto(
        id = 7L,
        name = "스타벅스 더북한산점",
        address = "서울 은평구 진관동 277-11",
        roadAddress = "서울 은평구 대서문길 24-11",
        lat = 37.65,
        lng = 126.94,
        category = "음식점 > 카페",
        kakaoPlaceId = "76206032",
        sourceType = "INSTAGRAM",
        sourceUrl = "https://instagram.com/p/1",
        description = "뷰 맛집",
        mapId = 3L,
        tags = listOf("카페", "뷰"),
        photoUrls = listOf("https://img/1.jpg", "https://img/2.jpg"),
    )

    @Test
    fun `장소를 다시 읽어 나만의 지도에 그대로 등록한다`() = runTest {
        val placeService = CopyingPlaceService(place)
        val mapService = PagedMyMapService(
            listOf(listOf(MapSummaryDto(id = 11L), MapSummaryDto(id = 12L, personal = true))),
        )

        PersonalMapRepositoryImpl(placeService, mapService).addPlace(placeId = 7L)

        assertEquals(listOf(7L), placeService.readIds)
        assertEquals(listOf("PRIVATE" to 0), mapService.requests)
        val request = placeService.created.single()
        assertEquals(12L, request.mapId)
        assertEquals("스타벅스 더북한산점", request.name)
        assertEquals("서울 은평구 진관동 277-11", request.address)
        assertEquals("서울 은평구 대서문길 24-11", request.roadAddress)
        assertEquals(37.65, request.lat, 0.0)
        assertEquals(126.94, request.lng, 0.0)
        assertEquals("음식점 > 카페", request.category)
        assertEquals("76206032", request.kakaoPlaceId)
        assertEquals("INSTAGRAM", request.sourceType)
        assertEquals("https://instagram.com/p/1", request.sourceUrl)
        assertEquals("뷰 맛집", request.description)
        assertEquals(listOf("카페", "뷰"), request.tags)
        assertEquals(listOf("https://img/1.jpg", "https://img/2.jpg"), request.photoUrls)
    }

    @Test
    fun `나만의 지도가 뒤 페이지에 있으면 넘겨 가며 찾는다`() = runTest {
        val placeService = CopyingPlaceService(place)
        val mapService = PagedMyMapService(
            listOf(listOf(MapSummaryDto(id = 11L)), listOf(MapSummaryDto(id = 21L, personal = true))),
        )

        PersonalMapRepositoryImpl(placeService, mapService).addPlace(placeId = 7L)

        assertEquals(listOf("PRIVATE" to 0, "PRIVATE" to 1), mapService.requests)
        assertEquals(21L, placeService.created.single().mapId)
    }

    @Test
    fun `나만의 지도가 없으면 등록하지 않고 알린다`() = runTest {
        val placeService = CopyingPlaceService(place)
        val mapService = PagedMyMapService(listOf(listOf(MapSummaryDto(id = 11L))))

        val error = runCatching {
            PersonalMapRepositoryImpl(placeService, mapService).addPlace(placeId = 7L)
        }.exceptionOrNull()

        assertTrue(error is PersonalMapNotFoundException)
        assertTrue(placeService.created.isEmpty())
    }

    @Test
    fun `비어 있는 선택값은 요청에 싣지 않는다`() {
        val request = PlaceDto(
            id = 1L,
            name = "이름",
            address = " ",
            roadAddress = "",
            kakaoPlaceId = "1",
            sourceType = null,
            description = "",
            photoUrls = listOf(" "),
        ).toCopyRequest(mapId = 5L)

        assertNull(request.address)
        assertNull(request.roadAddress)
        assertNull(request.description)
        assertNull(request.tags)
        assertNull(request.photoUrls)
        assertEquals("KAKAO_SEARCH", request.sourceType)
    }

    @Test
    fun `카카오 장소 id 가 없으면 요청을 만들지 않는다`() {
        assertThrows(IllegalStateException::class.java) {
            PlaceDto(id = 1L, name = "이름", kakaoPlaceId = " ").toCopyRequest(mapId = 5L)
        }
    }
}
