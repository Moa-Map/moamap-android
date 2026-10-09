package com.moamap.app.feature.collection.data.repository

import android.net.Uri
import com.moamap.app.core.common.upload.PhotoSpec
import com.moamap.app.core.common.upload.PhotoUploader
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
import com.moamap.app.feature.collection.data.remote.MapOrderUpdateRequestDto
import com.moamap.app.feature.collection.data.remote.MapService
import com.moamap.app.feature.collection.data.remote.MapSummaryDto
import com.moamap.app.feature.collection.data.remote.MapUpdateRequestDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 나가기·삭제를 기록하는 가짜. 상세 응답만 바꿔 가며 쓴다. */
private class LeaveRecordingMapService(var detail: MapDetailDto) : MapService {

    val calls = mutableListOf<String>()

    override suspend fun getMap(mapId: Long): MapDetailDto {
        calls += "getMap"
        return detail
    }

    override suspend fun deleteMap(mapId: Long) {
        calls += "deleteMap"
    }

    override suspend fun leaveMap(mapId: Long) {
        calls += "leaveMap"
    }

    override suspend fun getMaps(page: Int?, size: Int?, sort: String?): PageResponse<MapSummaryDto> =
        TODO("사용하지 않음")

    override suspend fun createMap(request: MapCreateRequestDto): MapDetailDto = TODO("사용하지 않음")

    override suspend fun createCoverUploadUrl(request: CoverUploadUrlRequestDto): CoverUploadUrlDto =
        TODO("사용하지 않음")

    override suspend fun getMyMaps(
        type: String,
        page: Int?,
        size: Int?,
        sort: String?,
    ): PageResponse<MapSummaryDto> = TODO("사용하지 않음")

    override suspend fun updateMyMapOrder(request: MapOrderUpdateRequestDto) = TODO("사용하지 않음")

    override suspend fun joinByInviteCode(request: JoinByInviteCodeRequestDto): MapDetailDto =
        TODO("사용하지 않음")

    override suspend fun updateMap(mapId: Long, request: MapUpdateRequestDto): MapDetailDto =
        TODO("사용하지 않음")

    override suspend fun joinMap(mapId: Long): MapDetailDto = TODO("사용하지 않음")

    override suspend fun getMemberRole(mapId: Long, userId: Long): MapMemberRoleDto =
        TODO("사용하지 않음")

    override suspend fun getMembers(mapId: Long): MapMemberListDto = TODO("사용하지 않음")

    override suspend fun updateMemberRole(
        mapId: Long,
        userId: Long,
        request: MapMemberRoleUpdateRequestDto,
    ): MapMemberRoleUpdateDto = TODO("사용하지 않음")
}

private class LeaveTestPhotoUploader : PhotoUploader {
    override suspend fun inspect(uri: Uri): PhotoSpec = TODO("사용하지 않음")
    override suspend fun upload(uploadUrl: String, photo: PhotoSpec) = TODO("사용하지 않음")
}

/** 모음 편집의 나가기. 지도 삭제가 걸린 갈래라 나가는 순간의 상태로 다시 판단하는지 본다. */
class MapRepositoryLeaveTest {

    private fun repository(service: MapService) = MapRepositoryImpl(service, LeaveTestPhotoUploader())

    private fun detail(type: String, role: String, memberCount: Int) = MapDetailDto(
        id = 1L,
        type = type,
        myRole = role,
        memberCount = memberCount,
        joined = true,
    )

    @Test
    fun `멤버인 지도는 나간다`() = runTest {
        val service = LeaveRecordingMapService(detail("COMMUNITY", "MEMBER", memberCount = 5))

        repository(service).leaveMap(1L)

        assertEquals(listOf("getMap", "leaveMap"), service.calls)
    }

    @Test
    fun `혼자 남은 프라이빗 지도의 방장이면 지도를 삭제한다`() = runTest {
        val service = LeaveRecordingMapService(detail("PRIVATE", "OWNER", memberCount = 1))

        repository(service).leaveMap(1L)

        assertEquals(listOf("getMap", "deleteMap"), service.calls)
    }

    /** 편집을 시작할 때는 혼자였어도, 그사이 누가 들어왔으면 남의 지도까지 지우면 안 된다. */
    @Test
    fun `그사이 다른 멤버가 들어왔으면 지우지 않고 실패한다`() = runTest {
        val service = LeaveRecordingMapService(detail("PRIVATE", "OWNER", memberCount = 2))

        val result = runCatching { repository(service).leaveMap(1L) }

        assertTrue(result.isFailure)
        assertEquals(listOf("getMap"), service.calls)
    }
}
