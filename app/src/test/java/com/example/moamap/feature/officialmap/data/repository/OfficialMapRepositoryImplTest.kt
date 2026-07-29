package com.example.moamap.feature.officialmap.data.repository

import com.example.moamap.core.network.model.PageResponse
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

class OfficialMapRepositoryImplTest {

    @Test
    fun `응답 content를 도메인 목록으로 옮긴다`() = runTest {
        val service = FakeOfficialMapService(
            listOf(
                OfficialMapDto(id = 6, name = "화장실 위치", memberCount = 1, placeCount = 5416),
                OfficialMapDto(id = 7, name = "무장애 여행"),
            )
        )

        val maps = OfficialMapRepositoryImpl(service).getOfficialMaps()

        assertEquals(2, maps.size)
        assertEquals("화장실 위치", maps[0].title)
        assertEquals(5416, maps[0].placeCount)
        assertEquals(7L, maps[1].id)
    }

    @Test
    fun `빈 목록이면 빈 목록을 돌려준다`() = runTest {
        val maps = OfficialMapRepositoryImpl(FakeOfficialMapService(emptyList())).getOfficialMaps()

        assertTrue(maps.isEmpty())
    }

    @Test
    fun `첫 페이지만 크기를 지정해 읽는다`() = runTest {
        val service = FakeOfficialMapService(emptyList())

        OfficialMapRepositoryImpl(service).getOfficialMaps()

        // 정렬 키가 스웨거에 없어 서버 기본 정렬을 따른다.
        assertEquals(listOf(Triple(null, 20, null)), service.calls)
    }
}
