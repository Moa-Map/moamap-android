package com.moamap.app.feature.mapdetail.data.repository

import com.moamap.app.core.network.model.PageResponse
import com.moamap.app.feature.mapdetail.data.remote.MapPostDto
import com.moamap.app.feature.mapdetail.data.remote.MapPostService
import com.moamap.app.feature.mapdetail.domain.model.MapPostSort
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private data class PostsCall(val mapId: Long, val page: Int?, val size: Int?, val sort: String?)

private class FakeMapPostService(
    private val response: PageResponse<MapPostDto> = PageResponse(),
) : MapPostService {

    val calls = mutableListOf<PostsCall>()

    override suspend fun getPosts(
        mapId: Long,
        page: Int?,
        size: Int?,
        sort: String?,
    ): PageResponse<MapPostDto> {
        calls += PostsCall(mapId, page, size, sort)
        return response
    }
}

class MapPostRepositoryImplTest {

    @Test
    fun `지도와 페이지와 한 번에 받을 수를 넘긴다`() = runTest {
        val service = FakeMapPostService()

        MapPostRepositoryImpl(service).getPosts(mapId = 10, page = 2, sort = MapPostSort.Latest)

        val call = service.calls.single()
        assertEquals(10L, call.mapId)
        assertEquals(2, call.page)
        assertEquals(POST_PAGE_SIZE, call.size)
    }

    /** 최신순도 빼지 않고 보낸다. 서버 기본값이 바뀌어도 화면의 선택과 어긋나지 않는다. */
    @Test
    fun `정렬을 서버 형식으로 바꿔 보낸다`() = runTest {
        val service = FakeMapPostService()
        val repository = MapPostRepositoryImpl(service)

        repository.getPosts(mapId = 10, page = 0, sort = MapPostSort.Latest)
        repository.getPosts(mapId = 10, page = 0, sort = MapPostSort.Oldest)

        assertEquals(listOf("createdAt,desc", "createdAt,asc"), service.calls.map { it.sort })
    }

    @Test
    fun `응답을 도메인으로 옮기고 마지막 페이지 여부를 넘긴다`() = runTest {
        val service = FakeMapPostService(
            PageResponse(content = listOf(MapPostDto(id = 1), MapPostDto(id = 2)), last = false),
        )

        val page = MapPostRepositoryImpl(service).getPosts(mapId = 10, page = 0, sort = MapPostSort.Latest)

        assertEquals(listOf(1L, 2L), page.posts.map { it.id })
        assertFalse(page.isLast)
    }

    /** 서버가 `last` 를 잘못 내려도 스크롤할 때마다 헛조회가 반복되지 않게 한다. */
    @Test
    fun `빈 페이지는 마지막으로 본다`() = runTest {
        val service = FakeMapPostService(PageResponse(content = emptyList(), last = false))

        val page = MapPostRepositoryImpl(service).getPosts(mapId = 10, page = 3, sort = MapPostSort.Latest)

        assertTrue(page.isLast)
    }
}
