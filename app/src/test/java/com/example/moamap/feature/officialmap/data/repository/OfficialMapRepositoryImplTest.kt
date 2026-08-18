package com.example.moamap.feature.officialmap.data.repository

import com.example.moamap.core.network.model.PageResponse
import com.example.moamap.feature.collection.data.remote.CoverUploadUrlDto
import com.example.moamap.feature.collection.data.remote.CoverUploadUrlRequestDto
import com.example.moamap.feature.collection.data.remote.JoinByInviteCodeRequestDto
import com.example.moamap.feature.collection.data.remote.MapCreateRequestDto
import com.example.moamap.feature.collection.data.remote.MapDetailDto
import com.example.moamap.feature.collection.data.remote.MapMemberListDto
import com.example.moamap.feature.collection.data.remote.MapMemberRoleDto
import com.example.moamap.feature.collection.data.remote.MapMemberRoleUpdateDto
import com.example.moamap.feature.collection.data.remote.MapMemberRoleUpdateRequestDto
import com.example.moamap.feature.collection.data.remote.MapService
import com.example.moamap.feature.collection.data.remote.MapSummaryDto
import com.example.moamap.feature.collection.data.remote.MapUpdateRequestDto
import com.example.moamap.feature.officialmap.data.remote.OfficialMapDto
import com.example.moamap.feature.officialmap.data.remote.OfficialMapService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeOfficialMapService(
    private val content: List<OfficialMapDto>,
) : OfficialMapService {
    val calls = mutableListOf<Triple<Int?, Int?, String?>>()

    override suspend fun getOfficialMaps(
        page: Int?,
        size: Int?,
        sort: String?,
    ): PageResponse<OfficialMapDto> {
        calls += Triple(page, size, sort)
        return PageResponse(content = content, totalElements = content.size.toLong())
    }
}

/**
 * 이 저장소가 [MapService] 에서 쓰는 건 참여뿐이다. 나머지는 불리면 안 되는 것이라 막아 둔다 -
 * 빈 값을 돌려주면 잘못 부른 것이 조용히 지나간다.
 */
private class FakeMapService : MapService {
    val joinedMapIds = mutableListOf<Long>()
    var joinError: Exception? = null

    override suspend fun joinMap(mapId: Long): MapDetailDto {
        joinError?.let { throw it }
        joinedMapIds += mapId
        return MapDetailDto(id = mapId, joined = true)
    }

    override suspend fun getMaps(page: Int?, size: Int?, sort: String?) = notUsed()
    override suspend fun createMap(request: MapCreateRequestDto) = notUsed()
    override suspend fun createCoverUploadUrl(request: CoverUploadUrlRequestDto):
        CoverUploadUrlDto = notUsed()
    override suspend fun getMyMaps(type: String, page: Int?, size: Int?, sort: String?):
        PageResponse<MapSummaryDto> = notUsed()
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

    private fun notUsed(): Nothing = error("공식지도 저장소가 부를 일이 없는 호출이다")
}

class OfficialMapRepositoryImplTest {

    @Test
    fun `응답 content를 도메인 목록으로 옮긴다`() = runTest {
        val service = FakeOfficialMapService(
            listOf(
                OfficialMapDto(id = 6, name = "화장실 위치", memberCount = 1, placeCount = 5416),
                OfficialMapDto(id = 7, name = "무장애 여행"),
            )
        )

        val maps = OfficialMapRepositoryImpl(service, FakeMapService()).getOfficialMaps()

        assertEquals(2, maps.size)
        assertEquals("화장실 위치", maps[0].title)
        assertEquals(5416, maps[0].placeCount)
        assertEquals(7L, maps[1].id)
    }

    @Test
    fun `빈 목록이면 빈 목록을 돌려준다`() = runTest {
        val maps = OfficialMapRepositoryImpl(
            FakeOfficialMapService(emptyList()),
            FakeMapService(),
        ).getOfficialMaps()

        assertTrue(maps.isEmpty())
    }

    @Test
    fun `첫 페이지만 크기를 지정해 읽는다`() = runTest {
        val service = FakeOfficialMapService(emptyList())

        OfficialMapRepositoryImpl(service, FakeMapService()).getOfficialMaps()

        // 정렬 키가 스웨거에 없어 서버 기본 정렬을 따른다.
        assertEquals(listOf(Triple(null, 20, null)), service.calls)
    }

    @Test
    fun `참여는 커뮤니티 지도와 같은 API 를 쓴다`() = runTest {
        val mapService = FakeMapService()

        OfficialMapRepositoryImpl(FakeOfficialMapService(emptyList()), mapService).joinMap(6L)

        assertEquals(listOf(6L), mapService.joinedMapIds)
    }
}
