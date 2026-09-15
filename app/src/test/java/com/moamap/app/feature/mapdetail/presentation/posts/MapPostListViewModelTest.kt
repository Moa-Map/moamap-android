package com.moamap.app.feature.mapdetail.presentation.posts

import androidx.lifecycle.SavedStateHandle
import com.moamap.app.core.navigation.MoaMapRoute
import com.moamap.app.feature.mapdetail.domain.model.MapPost
import com.moamap.app.feature.mapdetail.domain.model.MapPostPage
import com.moamap.app.feature.mapdetail.domain.model.MapPostSort
import com.moamap.app.feature.mapdetail.domain.repository.MapPostRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

private fun post(id: Long) = MapPost(
    id = id,
    authorId = 2,
    content = "글$id",
    imageUrls = emptyList(),
    placeNames = emptyList(),
    createdAtMillis = null,
)

private data class PostsRequest(val page: Int, val sort: MapPostSort)

/** 정렬마다 페이지 목록을 들고 있다가 요청한 페이지를 돌려준다. */
private class FakeMapPostRepository(
    var pages: Map<MapPostSort, List<MapPostPage>> = mapOf(
        MapPostSort.Latest to listOf(MapPostPage(listOf(post(1), post(2)), isLast = true)),
    ),
) : MapPostRepository {

    val requests = mutableListOf<PostsRequest>()

    /** 이 페이지를 요청하면 실패한다. */
    var failingPage: Int? = null

    /** 열어 두면 조회가 여기서 멈춘다. 응답이 늦게 도착하는 상황을 만들 때 쓴다. */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun getPosts(mapId: Long, page: Int, sort: MapPostSort): MapPostPage {
        requests += PostsRequest(page, sort)
        gate?.await()
        if (page == failingPage) throw RuntimeException("boom")
        return pages[sort]?.getOrNull(page) ?: MapPostPage(emptyList(), isLast = true)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MapPostListViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: MapPostRepository) = MapPostListViewModel(
        savedStateHandle = SavedStateHandle(mapOf(MoaMapRoute.MapDetail.ARG_MAP_ID to 7L)),
        repository = repository,
    )

    /** 로그 탭을 열 때 읽는다. 장소 탭만 보는 사용자는 게시물을 받지 않는다. */
    @Test
    fun `만들어지기만 하면 조회하지 않는다`() = runTest {
        val repository = FakeMapPostRepository()

        viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(repository.requests.isEmpty())
    }

    @Test
    fun `로그 탭을 열면 최신순 첫 페이지를 읽는다`() = runTest {
        val repository = FakeMapPostRepository()

        val viewModel = viewModel(repository).apply { loadOnce() }
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(PostsRequest(0, MapPostSort.Latest)), repository.requests)
        val state = viewModel.uiState.value
        assertFalse(state.loading)
        assertEquals(listOf(1L, 2L), state.posts.map { it.id })
        assertTrue(state.endReached)
    }

    @Test
    fun `탭을 오갈 때마다 다시 읽지 않는다`() = runTest {
        val repository = FakeMapPostRepository()

        viewModel(repository).apply {
            loadOnce()
            loadOnce()
        }
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.requests.size)
    }

    @Test
    fun `끝에 닿으면 다음 페이지를 이어 붙인다`() = runTest {
        val repository = FakeMapPostRepository(
            mapOf(
                MapPostSort.Latest to listOf(
                    MapPostPage(listOf(post(1), post(2)), isLast = false),
                    MapPostPage(listOf(post(3)), isLast = true),
                ),
            ),
        )
        val viewModel = viewModel(repository).apply { loadOnce() }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.loadMore()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(0, 1), repository.requests.map { it.page })
        assertEquals(listOf(1L, 2L, 3L), viewModel.uiState.value.posts.map { it.id })
        assertTrue(viewModel.uiState.value.endReached)
    }

    @Test
    fun `마지막 페이지를 받은 뒤에는 더 부르지 않는다`() = runTest {
        val repository = FakeMapPostRepository()
        val viewModel = viewModel(repository).apply { loadOnce() }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.loadMore()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.requests.size)
    }

    /** 그때 다음 페이지를 받으면 첫 페이지 없이 둘째 페이지만 화면에 붙는다. */
    @Test
    fun `첫 페이지를 받는 중에는 다음 페이지를 부르지 않는다`() = runTest {
        val repository = FakeMapPostRepository().apply { gate = CompletableDeferred() }
        val viewModel = viewModel(repository).apply { loadOnce() }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.loadMore()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(0), repository.requests.map { it.page })
    }

    /** 받는 사이 새 글이 올라오면 페이지 경계가 밀려 앞 페이지의 글이 한 번 더 온다. */
    @Test
    fun `다음 페이지에 이미 받은 글이 섞이면 한 번만 둔다`() = runTest {
        val repository = FakeMapPostRepository(
            mapOf(
                MapPostSort.Latest to listOf(
                    MapPostPage(listOf(post(1), post(2)), isLast = false),
                    MapPostPage(listOf(post(2), post(3)), isLast = true),
                ),
            ),
        )
        val viewModel = viewModel(repository).apply { loadOnce() }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.loadMore()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(1L, 2L, 3L), viewModel.uiState.value.posts.map { it.id })
    }

    @Test
    fun `정렬을 바꾸면 첫 페이지부터 다시 읽는다`() = runTest {
        val repository = FakeMapPostRepository(
            mapOf(
                MapPostSort.Latest to listOf(MapPostPage(listOf(post(2), post(1)), isLast = false)),
                MapPostSort.Oldest to listOf(MapPostPage(listOf(post(1), post(2)), isLast = true)),
            ),
        )
        val viewModel = viewModel(repository).apply { loadOnce() }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.selectSort(MapPostSort.Oldest)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(PostsRequest(0, MapPostSort.Oldest), repository.requests.last())
        val state = viewModel.uiState.value
        assertEquals(MapPostSort.Oldest, state.sort)
        assertEquals(listOf(1L, 2L), state.posts.map { it.id })
        assertTrue(state.endReached)
    }

    @Test
    fun `같은 정렬을 다시 누르면 다시 읽지 않는다`() = runTest {
        val repository = FakeMapPostRepository()
        val viewModel = viewModel(repository).apply { loadOnce() }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.selectSort(MapPostSort.Latest)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.requests.size)
    }

    /** 이전 정렬의 다음 페이지가 늦게 도착해 새 목록 뒤에 붙으면 안 된다. */
    @Test
    fun `다음 페이지를 받는 중에 정렬을 바꾸면 그 응답은 버린다`() = runTest {
        val repository = FakeMapPostRepository(
            mapOf(
                MapPostSort.Latest to listOf(
                    MapPostPage(listOf(post(2)), isLast = false),
                    MapPostPage(listOf(post(1)), isLast = true),
                ),
                MapPostSort.Oldest to listOf(MapPostPage(listOf(post(9)), isLast = true)),
            ),
        )
        val viewModel = viewModel(repository).apply { loadOnce() }
        dispatcher.scheduler.advanceUntilIdle()

        val gate = CompletableDeferred<Unit>()
        repository.gate = gate
        viewModel.loadMore()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.selectSort(MapPostSort.Oldest)
        gate.complete(Unit)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(9L), viewModel.uiState.value.posts.map { it.id })
    }

    @Test
    fun `첫 페이지를 못 읽으면 안내를 남기고 재시도하면 다시 읽는다`() = runTest {
        val repository = FakeMapPostRepository().apply { failingPage = 0 }
        val viewModel = viewModel(repository).apply { loadOnce() }
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(POST_LOAD_FAILED_MESSAGE, viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.posts.isEmpty())

        repository.failingPage = null
        viewModel.retry()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(listOf(1L, 2L), viewModel.uiState.value.posts.map { it.id })
    }

    /** 이미 받은 카드는 그대로 두고 끝에만 다시 시도를 붙인다. */
    @Test
    fun `다음 페이지를 못 읽으면 받은 목록은 두고 실패만 표시한다`() = runTest {
        val repository = FakeMapPostRepository(
            mapOf(
                MapPostSort.Latest to listOf(
                    MapPostPage(listOf(post(1), post(2)), isLast = false),
                    MapPostPage(listOf(post(3)), isLast = true),
                ),
            ),
        ).apply { failingPage = 1 }
        val viewModel = viewModel(repository).apply { loadOnce() }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.loadMore()
        dispatcher.scheduler.advanceUntilIdle()

        val failed = viewModel.uiState.value
        assertTrue(failed.loadMoreFailed)
        assertFalse(failed.loadingMore)
        assertNull(failed.errorMessage)
        assertEquals(listOf(1L, 2L), failed.posts.map { it.id })

        repository.failingPage = null
        viewModel.loadMore()
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.loadMoreFailed)
        assertEquals(listOf(1L, 2L, 3L), viewModel.uiState.value.posts.map { it.id })
    }
}
