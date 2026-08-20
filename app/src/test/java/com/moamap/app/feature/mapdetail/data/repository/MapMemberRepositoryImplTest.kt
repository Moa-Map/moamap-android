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
import com.moamap.app.feature.collection.data.remote.MapMemberSummaryDto
import com.moamap.app.feature.collection.data.remote.MapService
import com.moamap.app.feature.collection.data.remote.MapSummaryDto
import com.moamap.app.feature.collection.data.remote.MapUpdateRequestDto
import com.moamap.app.feature.mapdetail.domain.model.MapRole
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** 이 저장소가 [MapService] 에서 쓰는 건 멤버 목록과 역할 변경뿐이다. 나머지는 막아 둔다. */
private class FakeMapService(
    private val members: List<MapMemberSummaryDto> = emptyList(),
) : MapService {

    val memberCalls = mutableListOf<Long>()
    val roleCalls = mutableListOf<Triple<Long, Long, String>>()
    var membersError: Exception? = null
    var roleError: Exception? = null

    override suspend fun getMembers(mapId: Long): MapMemberListDto {
        membersError?.let { throw it }
        memberCalls += mapId
        return MapMemberListDto(memberCount = members.size, members = members)
    }

    override suspend fun updateMemberRole(
        mapId: Long,
        userId: Long,
        request: MapMemberRoleUpdateRequestDto,
    ): MapMemberRoleUpdateDto {
        roleError?.let { throw it }
        roleCalls += Triple(mapId, userId, request.role)
        return MapMemberRoleUpdateDto(mapId = mapId, userId = userId, role = request.role)
    }

    override suspend fun getMaps(page: Int?, size: Int?, sort: String?) = notUsed()
    override suspend fun createMap(request: MapCreateRequestDto) = notUsed()
    override suspend fun createCoverUploadUrl(request: CoverUploadUrlRequestDto):
        CoverUploadUrlDto = notUsed()
    override suspend fun getMyMaps(type: String, page: Int?, size: Int?, sort: String?):
        PageResponse<MapSummaryDto> = notUsed()
    override suspend fun joinByInviteCode(request: JoinByInviteCodeRequestDto) = notUsed()
    override suspend fun getMap(mapId: Long): MapDetailDto = notUsed()
    override suspend fun updateMap(mapId: Long, request: MapUpdateRequestDto) = notUsed()
    override suspend fun deleteMap(mapId: Long) = notUsed()
    override suspend fun joinMap(mapId: Long): MapDetailDto = notUsed()
    override suspend fun leaveMap(mapId: Long) = notUsed()
    override suspend fun getMemberRole(mapId: Long, userId: Long): MapMemberRoleDto = notUsed()

    private fun notUsed(): Nothing = error("멤버 저장소가 부를 일이 없는 호출이다")
}

class MapMemberRepositoryImplTest {

    @Test
    fun `멤버 목록을 도메인으로 옮겨 돌려준다`() = runTest {
        val service = FakeMapService(
            members = listOf(
                MapMemberSummaryDto(userId = 1, nickname = "김도현", role = "OWNER"),
                MapMemberSummaryDto(userId = 2, nickname = "이서연", role = "MEMBER"),
            ),
        )

        val members = MapMemberRepositoryImpl(service).getMembers(mapId = 7)

        assertEquals(listOf(7L), service.memberCalls)
        assertEquals(listOf(1L, 2L), members.map { it.id })
        assertEquals("김도현", members[0].name)
        assertEquals(MapRole.Owner, members[0].role)
        assertEquals(MapRole.Member, members[1].role)
    }

    /** 서버가 준 순서를 그대로 둔다. 화면이 다시 줄 세우지 않는다. */
    @Test
    fun `서버 순서를 바꾸지 않는다`() = runTest {
        val service = FakeMapService(
            members = listOf(
                MapMemberSummaryDto(userId = 3, nickname = "박지훈", role = "MEMBER"),
                MapMemberSummaryDto(userId = 1, nickname = "김도현", role = "OWNER"),
            ),
        )

        val members = MapMemberRepositoryImpl(service).getMembers(mapId = 7)

        assertEquals(listOf(3L, 1L), members.map { it.id })
    }

    @Test
    fun `관리자 권한을 주면 ADMIN 으로 보낸다`() = runTest {
        val service = FakeMapService()

        MapMemberRepositoryImpl(service).grantAdmin(mapId = 7, userId = 2)

        assertEquals(listOf(Triple(7L, 2L, "ADMIN")), service.roleCalls)
    }

    @Test(expected = IllegalStateException::class)
    fun `목록 조회 실패는 그대로 올린다`() = runTest {
        val service = FakeMapService().apply { membersError = IllegalStateException("boom") }

        MapMemberRepositoryImpl(service).getMembers(mapId = 7)
    }

    @Test(expected = IllegalStateException::class)
    fun `역할 변경 실패는 그대로 올린다`() = runTest {
        val service = FakeMapService().apply { roleError = IllegalStateException("boom") }

        MapMemberRepositoryImpl(service).grantAdmin(mapId = 7, userId = 2)
    }
}
