package com.moamap.app.feature.explore.data.repository

import com.moamap.app.core.network.model.PageResponse
import com.moamap.app.feature.collection.domain.model.MapType
import com.moamap.app.feature.collection.domain.model.MyMap
import com.moamap.app.feature.collection.domain.model.NewMap
import com.moamap.app.feature.collection.domain.repository.MapRepository
import com.moamap.app.feature.explore.data.remote.CommunityMapDto
import com.moamap.app.feature.explore.data.remote.CommunityMapService
import com.moamap.app.feature.explore.domain.model.CommunityMapSort
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private class AllMapsService(private val maps: List<CommunityMapDto>) : CommunityMapService {

    val allCalls = mutableListOf<Pair<String?, String?>>()

    override suspend fun getAllCommunityMaps(tag: String?, sort: String?, page: Int?, size: Int?) =
        PageResponse(content = maps, page = page ?: 0, last = true).also { allCalls += tag to sort }

    override suspend fun getCommunityMaps(tag: String?, sort: String?, page: Int?, size: Int?) =
        TODO("전체보기는 참여한 지도를 빼는 목록을 쓰지 않는다")

    override suspend fun searchMaps(keyword: String, sort: String?, page: Int?, size: Int?) =
        TODO("사용하지 않음")
}

private class JoinedMapsRepository(private val joinedIds: List<Long>) : MapRepository {

    val asked = mutableListOf<MapType>()

    override suspend fun getMyMaps(type: MapType): List<MyMap> {
        asked += type
        return joinedIds.map { id -> MyMap(id, "지도$id", null, 1, 0, official = false, personal = false) }
    }

    override suspend fun updateMyMapOrder(type: MapType, mapIds: List<Long>) = TODO("사용하지 않음")
    override suspend fun uploadCoverImage(imageUri: String) = TODO("사용하지 않음")
    override suspend fun createMap(newMap: NewMap) = TODO("사용하지 않음")
    override suspend fun updateMap(
        mapId: Long,
        name: String,
        description: String?,
        imageUrl: String?,
        tags: List<String>,
    ) = TODO("사용하지 않음")
    override suspend fun joinByInviteCode(inviteCode: String) = TODO("사용하지 않음")
    override suspend fun getLeaveOutcome(mapId: Long) = TODO("사용하지 않음")
    override suspend fun leaveMap(mapId: Long) = TODO("사용하지 않음")
}

class CommunityMapRepositoryImplTest {

    /** 로그인 없이 받은 목록이라 서버의 joined 는 늘 false 다. 내 커뮤니티 지도로 다시 채운다. */
    @Test
    fun `전체보기 목록은 참여한 지도도 받고 내 지도로 참여 여부를 채운다`() = runTest {
        val service = AllMapsService(
            listOf(CommunityMapDto(id = 1, joined = false), CommunityMapDto(id = 2, joined = false)),
        )
        val myMaps = JoinedMapsRepository(joinedIds = listOf(2L, 9L))
        val repository = CommunityMapRepositoryImpl(service, myMaps)

        val page = repository.getAllCommunityMaps(tag = "카페", sort = CommunityMapSort.LATEST, page = 0, size = 20)

        assertEquals(listOf(1L to false, 2L to true), page.maps.map { it.id to it.joined })
        assertEquals(listOf<Pair<String?, String?>>("카페" to "LATEST"), service.allCalls)
        assertEquals(listOf(MapType.Community), myMaps.asked)
    }
}
