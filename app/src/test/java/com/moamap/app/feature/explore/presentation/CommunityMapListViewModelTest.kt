package com.moamap.app.feature.explore.presentation

import com.moamap.app.feature.explore.domain.model.CommunityMap
import com.moamap.app.feature.explore.domain.model.CommunityMapPage
import com.moamap.app.feature.explore.domain.model.CommunityMapSort
import com.moamap.app.feature.explore.domain.repository.CommunityMapRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CommunityMapListViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private data class Call(val tag: String?, val sort: CommunityMapSort, val page: Int, val size: Int)

    private fun map(id: Long, vararg tags: String) = CommunityMap(
        id = id,
        title = "지도$id",
        imageUrl = null,
        hashtags = tags.toList(),
        memberCount = 0,
        placeCount = 0,
        joined = false,
    )

    private fun page(ids: LongRange, isLast: Boolean) =
        CommunityMapPage(maps = ids.map { id -> map(id) }, isLast = isLast)

    /** 호출 조건을 기록하고 [result] 가 만든 페이지를 돌려준다. */
    private class FakeRepository(
        val responseDelayMillis: Long = 0L,
        var result: (Call) -> CommunityMapPage,
    ) : CommunityMapRepository {
        val calls = mutableListOf<Call>()

        override suspend fun getCommunityMaps(
            tag: String?,
            sort: CommunityMapSort,
            page: Int,
            size: Int,
        ): CommunityMapPage {
            val call = Call(tag, sort, page, size)
            calls += call
            delay(responseDelayMillis)
            return result(call)
        }

        /** 전체보기는 참여한 지도까지 받는 목록을 쓴다. 조건 기록은 [getCommunityMaps] 와 같은 자리에 남긴다. */
        override suspend fun getAllCommunityMaps(
            tag: String?,
            sort: CommunityMapSort,
            page: Int,
            size: Int,
        ): CommunityMapPage = getCommunityMaps(tag, sort, page, size)

        /** 검색 요청. 목록 요청([calls])과 따로 센다. */
        val searches = mutableListOf<Pair<String, Int>>()
        var searchResult: (String) -> CommunityMapPage = { CommunityMapPage(maps = emptyList(), isLast = true) }

        override suspend fun searchMaps(keyword: String, page: Int, size: Int): CommunityMapPage {
            searches += keyword to page
            delay(responseDelayMillis)
            return searchResult(keyword)
        }
    }

    private fun loaded(repository: FakeRepository) = CommunityMapListViewModel(repository).apply {
        refresh()
        dispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `처음 보이면 전체·인기순 첫 20개를 읽는다`() = runTest {
        val repository = FakeRepository { page(1L..3L, isLast = true) }

        val viewModel = loaded(repository)

        assertEquals(listOf(Call(null, CommunityMapSort.POPULAR, 0, COMMUNITY_MAP_PAGE_SIZE)), repository.calls)
        val state = viewModel.uiState.value
        assertFalse(state.loading)
        assertEquals(3, state.maps.size)
        assertTrue(state.endReached)
    }

    @Test
    fun `칩은 거르지 않은 첫 목록의 태그를 많이 쓰인 순으로 만든다`() = runTest {
        val repository = FakeRepository {
            CommunityMapPage(
                maps = listOf(map(1L, "카페", "데이트"), map(2L, "데이트", "산책"), map(3L, "데이트", "카페")),
                isLast = true,
            )
        }

        val viewModel = loaded(repository)

        assertEquals(listOf("데이트", "카페", "산책"), viewModel.uiState.value.tags)
    }

    @Test
    fun `칩을 고르면 그 태그로 처음부터 다시 읽고 칩 줄은 그대로 둔다`() = runTest {
        val repository = FakeRepository { call ->
            if (call.tag == null) {
                CommunityMapPage(listOf(map(1L, "카페"), map(2L, "데이트")), isLast = true)
            } else {
                CommunityMapPage(listOf(map(1L, "카페")), isLast = true)
            }
        }
        val viewModel = loaded(repository)

        viewModel.selectTag("카페")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(Call("카페", CommunityMapSort.POPULAR, 0, COMMUNITY_MAP_PAGE_SIZE), repository.calls.last())
        assertEquals("카페", viewModel.uiState.value.selectedTag)
        // 고른 태그의 지도만 왔다고 칩을 다시 만들면 「카페」만 남는다.
        assertEquals(listOf("카페", "데이트"), viewModel.uiState.value.tags)
    }

    @Test
    fun `전체를 누르면 태그 없이 다시 읽는다`() = runTest {
        val repository = FakeRepository { page(1L..1L, isLast = true) }
        val viewModel = loaded(repository)
        viewModel.selectTag("카페")
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.selectTag(null)
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.selectedTag)
        assertEquals(Call(null, CommunityMapSort.POPULAR, 0, COMMUNITY_MAP_PAGE_SIZE), repository.calls.last())
    }

    @Test
    fun `정렬을 바꾸면 고른 태그 그대로 처음부터 다시 읽는다`() = runTest {
        val repository = FakeRepository { page(1L..1L, isLast = true) }
        val viewModel = loaded(repository)
        viewModel.selectTag("카페")
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.selectSort(CommunityMapSort.LATEST)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(Call("카페", CommunityMapSort.LATEST, 0, COMMUNITY_MAP_PAGE_SIZE), repository.calls.last())
    }

    @Test
    fun `같은 칩이나 정렬을 다시 누르면 다시 읽지 않는다`() = runTest {
        val repository = FakeRepository { page(1L..1L, isLast = true) }
        val viewModel = loaded(repository)

        viewModel.selectTag(null)
        viewModel.selectSort(CommunityMapSort.POPULAR)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.calls.size)
    }

    @Test
    fun `끝에 닿으면 다음 페이지를 이어 붙이고 겹친 지도는 뺀다`() = runTest {
        val repository = FakeRepository { call ->
            // 받는 사이 새 지도가 생겨 둘째 페이지 첫 칸에 첫 페이지 마지막(20)이 한 번 더 온다.
            if (call.page == 0) page(1L..20L, isLast = false) else page(20L..25L, isLast = true)
        }
        val viewModel = loaded(repository)

        viewModel.loadMore()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(Call(null, CommunityMapSort.POPULAR, 1, COMMUNITY_MAP_PAGE_SIZE), repository.calls.last())
        val state = viewModel.uiState.value
        assertEquals((1L..25L).toList(), state.maps.map { it.id })
        assertTrue(state.endReached)

        // 마지막 페이지까지 받았으면 더 부르지 않는다.
        viewModel.loadMore()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, repository.calls.size)
    }

    @Test
    fun `다음 페이지를 못 받으면 받은 목록은 두고 실패만 표시한다`() = runTest {
        var fail = true
        val repository = FakeRepository { call ->
            when {
                call.page == 0 -> page(1L..20L, isLast = false)
                fail -> throw RuntimeException("boom")
                else -> page(21L..22L, isLast = true)
            }
        }
        val viewModel = loaded(repository)

        viewModel.loadMore()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.loadMoreFailed)
        assertEquals(20, viewModel.uiState.value.maps.size)
        assertNull(viewModel.uiState.value.errorMessage)

        fail = false
        viewModel.loadMore()
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.loadMoreFailed)
        assertEquals(22, viewModel.uiState.value.maps.size)
    }

    @Test
    fun `첫 페이지를 못 받았으면 다음 페이지를 부르지 않고 재시도로 복구한다`() = runTest {
        var fail = true
        val repository = FakeRepository {
            if (fail) throw RuntimeException("boom") else page(1L..1L, isLast = true)
        }
        val viewModel = loaded(repository)
        assertTrue(viewModel.uiState.value.errorMessage != null)

        viewModel.loadMore()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, repository.calls.size)

        fail = false
        viewModel.retry()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(1, viewModel.uiState.value.maps.size)
    }

    @Test
    fun `지도에 들어갔다 돌아오면 받아 둔 만큼을 한 번에 다시 읽는다`() = runTest {
        val repository = FakeRepository { call ->
            when {
                // 다시 읽기: 두 페이지 분량을 한 번에.
                call.size == 2 * COMMUNITY_MAP_PAGE_SIZE -> page(1L..40L, isLast = false)
                call.page == 0 -> page(1L..20L, isLast = false)
                else -> page(21L..40L, isLast = false)
            }
        }
        val viewModel = loaded(repository)
        viewModel.loadMore()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(Call(null, CommunityMapSort.POPULAR, 0, 2 * COMMUNITY_MAP_PAGE_SIZE), repository.calls.last())
        assertEquals(40, viewModel.uiState.value.maps.size)

        // 다시 읽은 뒤에도 이어서 셋째 페이지를 부른다.
        viewModel.loadMore()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, repository.calls.last().page)
    }

    @Test
    fun `다시 읽기가 실패해도 보던 목록을 지우지 않는다`() = runTest {
        var fail = false
        val repository = FakeRepository {
            if (fail) throw RuntimeException("boom") else page(1L..3L, isLast = true)
        }
        val viewModel = loaded(repository)

        fail = true
        viewModel.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.maps.size)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `칩을 연달아 바꾸면 늦게 온 이전 응답은 버리고 마지막 선택만 남는다`() = runTest {
        val repository = FakeRepository(responseDelayMillis = 100L) { call ->
            when (call.tag) {
                "카페" -> CommunityMapPage(listOf(map(1L, "카페")), isLast = true)
                "데이트" -> CommunityMapPage(listOf(map(2L, "데이트")), isLast = true)
                else -> CommunityMapPage(listOf(map(1L, "카페"), map(2L, "데이트")), isLast = true)
            }
        }
        val viewModel = loaded(repository)

        viewModel.selectTag("카페")
        dispatcher.scheduler.advanceTimeBy(50L)
        viewModel.selectTag("데이트")
        // 취소된 「카페」 요청이 깨어나도 오류로 새면 안 된다.
        dispatcher.scheduler.runCurrent()
        assertNull(viewModel.uiState.value.errorMessage)

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("데이트", viewModel.uiState.value.selectedTag)
        assertEquals(listOf(2L), viewModel.uiState.value.maps.map { it.id })
    }

    // ---------- 검색 ----------

    @Test
    fun `입력을 멈추고 잠시 뒤에 앞뒤 공백을 지운 검색어로 찾는다`() = runTest {
        val repository = FakeRepository { page(1L..3L, isLast = true) }
        repository.searchResult = { CommunityMapPage(listOf(map(9L)), isLast = true) }
        val viewModel = loaded(repository)

        viewModel.updateQuery(" 카페 ")
        dispatcher.scheduler.advanceTimeBy(SEARCH_DEBOUNCE_MILLIS - 1)
        dispatcher.scheduler.runCurrent()
        assertTrue(repository.searches.isEmpty())

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("카페" to 0), repository.searches)
        assertEquals("카페", viewModel.uiState.value.searchKeyword)
        assertEquals(listOf(9L), viewModel.uiState.value.maps.map { it.id })
    }

    @Test
    fun `연달아 입력하면 마지막 검색어로 한 번만 찾는다`() = runTest {
        val repository = FakeRepository { page(1L..3L, isLast = true) }
        val viewModel = loaded(repository)

        viewModel.updateQuery("카")
        dispatcher.scheduler.advanceTimeBy(100L)
        viewModel.updateQuery("카페")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("카페" to 0), repository.searches)
    }

    @Test
    fun `키보드 검색 버튼은 기다리지 않고 찾고 같은 검색어를 두 번 찾지 않는다`() = runTest {
        val repository = FakeRepository { page(1L..3L, isLast = true) }
        val viewModel = loaded(repository)

        viewModel.updateQuery("카페")
        viewModel.searchNow()
        dispatcher.scheduler.runCurrent()
        assertEquals(listOf("카페" to 0), repository.searches)

        viewModel.searchNow()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, repository.searches.size)
    }

    @Test
    fun `검색어를 다 지우면 검색 전 태그와 정렬의 목록을 다시 읽는다`() = runTest {
        val repository = FakeRepository { page(1L..3L, isLast = true) }
        val viewModel = loaded(repository)
        viewModel.selectTag("카페")
        viewModel.selectSort(CommunityMapSort.LATEST)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.updateQuery("데이트")
        dispatcher.scheduler.advanceUntilIdle()
        repository.calls.clear()

        viewModel.updateQuery("")
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.searchKeyword)
        assertEquals("카페", state.selectedTag)
        assertEquals(CommunityMapSort.LATEST, state.sort)
        assertEquals(listOf(Call("카페", CommunityMapSort.LATEST, 0, COMMUNITY_MAP_PAGE_SIZE)), repository.calls)
    }

    @Test
    fun `검색 결과도 끝에 닿으면 다음 페이지를 검색으로 받는다`() = runTest {
        val repository = FakeRepository { page(1L..3L, isLast = true) }
        repository.searchResult = { CommunityMapPage(listOf(map(9L)), isLast = false) }
        val viewModel = loaded(repository)
        viewModel.updateQuery("카페")
        dispatcher.scheduler.advanceUntilIdle()
        repository.calls.clear()

        viewModel.loadMore()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("카페" to 0, "카페" to 1), repository.searches)
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun `검색 결과로는 태그 칩을 만들지 않는다`() = runTest {
        val repository = FakeRepository { CommunityMapPage(listOf(map(1L, "산책")), isLast = true) }
        repository.searchResult = { CommunityMapPage(listOf(map(9L, "카페")), isLast = true) }
        val viewModel = loaded(repository)

        viewModel.updateQuery("카페")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("산책"), viewModel.uiState.value.tags)
    }

    @Test
    fun `검색어는 서버 한도까지만 받는다`() = runTest {
        val viewModel = loaded(FakeRepository { page(1L..3L, isLast = true) })

        viewModel.updateQuery("가".repeat(SEARCH_KEYWORD_MAX_LENGTH + 5))

        assertEquals(SEARCH_KEYWORD_MAX_LENGTH, viewModel.uiState.value.query.length)
        dispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `태그가 많으면 많이 쓰인 순으로 상한까지만 칩이 된다`() {
        val maps = (1..(MAX_TAG_CHIPS + 5)).map { index -> map(index.toLong(), "태그$index") } +
            map(100L, "태그3")

        val tags = tagsByFrequency(maps)

        assertEquals(MAX_TAG_CHIPS, tags.size)
        // 두 번 나온 「태그3」이 맨 앞, 나머지는 한 번씩이라 먼저 나온 순서대로.
        assertEquals(listOf("태그3", "태그1", "태그2", "태그4"), tags.take(4))
    }
}
