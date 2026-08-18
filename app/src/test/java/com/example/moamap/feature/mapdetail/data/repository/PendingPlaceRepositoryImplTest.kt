package com.example.moamap.feature.mapdetail.data.repository

import com.example.moamap.core.network.model.PageResponse
import com.example.moamap.feature.explore.data.remote.InstagramExtractRequestDto
import com.example.moamap.feature.explore.data.remote.MapShareExtractRequestDto
import com.example.moamap.feature.explore.data.remote.MapShareExtractResponseDto
import com.example.moamap.feature.explore.data.remote.PhotoUploadUrlDto
import com.example.moamap.feature.explore.data.remote.PhotoUploadUrlRequestDto
import com.example.moamap.feature.explore.data.remote.PlaceActivityDto
import com.example.moamap.feature.explore.data.remote.PlaceBulkCreateRequestDto
import com.example.moamap.feature.explore.data.remote.PlaceBulkCreateResponseDto
import com.example.moamap.feature.explore.data.remote.PlaceCandidateDto
import com.example.moamap.feature.explore.data.remote.PlaceCreateRequestDto
import com.example.moamap.feature.explore.data.remote.PlaceDto
import com.example.moamap.feature.explore.data.remote.PlaceService
import com.example.moamap.feature.explore.data.remote.PlaceUpdateRequestDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** 이 저장소가 [PlaceService] 에서 쓰는 건 승인 대기 조회와 승인·반려뿐이다. */
private class FakePlaceService(
    private val pages: List<PageResponse<PlaceDto>> = emptyList(),
) : PlaceService {

    val pendingCalls = mutableListOf<Triple<Long, Int?, Int?>>()
    val approved = mutableListOf<Long>()
    val rejected = mutableListOf<Long>()
    var pendingError: Exception? = null
    var actionError: Exception? = null

    override suspend fun getPendingPlaces(
        mapId: Long,
        page: Int?,
        size: Int?,
        sort: String?,
    ): PageResponse<PlaceDto> {
        pendingError?.let { throw it }
        pendingCalls += Triple(mapId, page, size)
        return pages.getOrElse(page ?: 0) { PageResponse(content = emptyList(), last = true) }
    }

    override suspend fun approvePlace(id: Long): PlaceDto {
        actionError?.let { throw it }
        approved += id
        return PlaceDto(id = id, status = "APPROVED")
    }

    override suspend fun rejectPlace(id: Long): PlaceDto {
        actionError?.let { throw it }
        rejected += id
        return PlaceDto(id = id, status = "REJECTED")
    }

    override suspend fun getPlaces(mapId: Long, page: Int?, size: Int?, sort: String?) =
        TODO("사용하지 않음")

    override suspend fun createPlace(request: PlaceCreateRequestDto): PlaceDto =
        TODO("사용하지 않음")

    override suspend fun createPhotoUploadUrls(
        request: PhotoUploadUrlRequestDto,
    ): List<PhotoUploadUrlDto> = TODO("사용하지 않음")

    override suspend fun getActivities(
        mapId: Long,
        page: Int?,
        size: Int?,
    ): PageResponse<PlaceActivityDto> = TODO("사용하지 않음")

    override suspend fun getPlace(id: Long) = TODO("사용하지 않음")
    override suspend fun updatePlace(id: Long, request: PlaceUpdateRequestDto) =
        TODO("사용하지 않음")

    override suspend fun deletePlace(id: Long) = TODO("사용하지 않음")
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

private fun page(vararg places: PlaceDto, last: Boolean) =
    PageResponse(content = places.toList(), last = last)

class PendingPlaceRepositoryImplTest {

    @Test
    fun `승인 대기 목록을 도메인으로 옮겨 돌려준다`() = runTest {
        val service = FakePlaceService(
            pages = listOf(
                page(
                    PlaceDto(id = 101, name = "성수 브루어리", createdBy = 3),
                    PlaceDto(id = 102, name = "연남 책방", createdBy = 4),
                    last = true,
                ),
            ),
        )

        val pending = PendingPlaceRepositoryImpl(service).getPendingPlaces(mapId = 7)

        assertEquals(listOf(101L, 102L), pending.map { it.id })
        assertEquals("성수 브루어리", pending[0].placeName)
        assertEquals(3L, pending[0].requesterId)
    }

    /** 서버가 필수로 받는 값이라 빠뜨리면 400 이 난다. */
    @Test
    fun `조회할 때 지도 식별자를 넘긴다`() = runTest {
        val service = FakePlaceService(pages = listOf(page(last = true)))

        PendingPlaceRepositoryImpl(service).getPendingPlaces(mapId = 7)

        assertEquals(7L, service.pendingCalls.single().first)
    }

    @Test
    fun `마지막 페이지까지 이어 받는다`() = runTest {
        val service = FakePlaceService(
            pages = listOf(
                page(PlaceDto(id = 1), last = false),
                page(PlaceDto(id = 2), last = true),
            ),
        )

        val pending = PendingPlaceRepositoryImpl(service).getPendingPlaces(mapId = 7)

        assertEquals(listOf(1L, 2L), pending.map { it.id })
        assertEquals(listOf(0, 1), service.pendingCalls.map { it.second })
    }

    @Test
    fun `수락하면 그 장소만 승인한다`() = runTest {
        val service = FakePlaceService()

        PendingPlaceRepositoryImpl(service).approve(placeId = 101)

        assertEquals(listOf(101L), service.approved)
        assertEquals(emptyList<Long>(), service.rejected)
    }

    @Test
    fun `거절하면 그 장소만 반려한다`() = runTest {
        val service = FakePlaceService()

        PendingPlaceRepositoryImpl(service).reject(placeId = 101)

        assertEquals(listOf(101L), service.rejected)
        assertEquals(emptyList<Long>(), service.approved)
    }

    @Test(expected = IllegalStateException::class)
    fun `목록 조회 실패는 그대로 올린다`() = runTest {
        val service = FakePlaceService().apply { pendingError = IllegalStateException("boom") }

        PendingPlaceRepositoryImpl(service).getPendingPlaces(mapId = 7)
    }

    @Test(expected = IllegalStateException::class)
    fun `승인 실패는 그대로 올린다`() = runTest {
        val service = FakePlaceService().apply { actionError = IllegalStateException("boom") }

        PendingPlaceRepositoryImpl(service).approve(placeId = 101)
    }
}
