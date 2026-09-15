package com.moamap.app.feature.mapdetail.presentation.posts

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.moamap.app.core.navigation.MoaMapRoute
import com.moamap.app.feature.mapdetail.domain.model.MapPostPage
import com.moamap.app.feature.mapdetail.domain.model.MapPostSort
import com.moamap.app.feature.mapdetail.domain.model.NewMapPost
import com.moamap.app.feature.mapdetail.domain.repository.MapPostRepository
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
import java.util.TimeZone

/** 최신순 페이지를 차례로 돌려준다. 달력은 늘 최신순으로 받는다. */
private class FakeCalendarRepository(
    var pages: List<MapPostPage> = emptyList(),
) : MapPostRepository {

    val requests = mutableListOf<Pair<Int, MapPostSort>>()
    var failingPage: Int? = null

    override suspend fun getPosts(mapId: Long, page: Int, sort: MapPostSort): MapPostPage {
        requests += page to sort
        if (page == failingPage) throw RuntimeException("boom")
        return pages.getOrNull(page) ?: MapPostPage(emptyList(), isLast = true)
    }

    override suspend fun uploadPhotos(mapId: Long, photos: List<Uri>): List<String> = TODO("달력은 올리지 않는다")

    override suspend fun createPost(mapId: Long, post: NewMapPost) = TODO("달력은 올리지 않는다")
}

@OptIn(ExperimentalCoroutinesApi::class)
class MapPostCalendarViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var originalTimeZone: TimeZone

    /** 날짜 경계를 고정한다. 기기 시간대에 따라 결과가 달라지면 안 된다. */
    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        TimeZone.setDefault(originalTimeZone)
    }

    private fun viewModel(repository: MapPostRepository) = MapPostCalendarViewModel(
        savedStateHandle = SavedStateHandle(mapOf(MoaMapRoute.MapDetail.ARG_MAP_ID to 7L)),
        repository = repository,
    )

    private val today = CalendarDay(2026, 8, 20)

    @Test
    fun `처음 열면 오늘이 있는 달을 보여주고 오늘을 고른다`() = runTest {
        val viewModel = viewModel(FakeCalendarRepository()).apply { open(today) }
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(CalendarMonth(2026, 8), viewModel.uiState.value.month)
        assertEquals(today, viewModel.uiState.value.selectedDay)
    }

    /** 이 달 1일보다 오래된 글이 나오면 그 뒤는 모두 이전 달이다. 더 받을 이유가 없다. */
    @Test
    fun `이 달보다 오래된 글이 나올 때까지만 최신순으로 이어 받는다`() = runTest {
        val repository = FakeCalendarRepository(
            listOf(
                MapPostPage(listOf(calendarPost(5, utcMillis(2026, 8, 20)), calendarPost(4, utcMillis(2026, 8, 10))), isLast = false),
                MapPostPage(listOf(calendarPost(3, utcMillis(2026, 8, 2)), calendarPost(2, utcMillis(2026, 7, 30))), isLast = false),
                MapPostPage(listOf(calendarPost(1, utcMillis(2026, 7, 1))), isLast = true),
            ),
        )

        val viewModel = viewModel(repository).apply { open(today) }
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(0 to MapPostSort.Latest, 1 to MapPostSort.Latest), repository.requests)
        assertEquals(listOf(5L, 4L, 3L, 2L), viewModel.uiState.value.posts.map { it.id })
        assertFalse(viewModel.uiState.value.loading)
    }

    @Test
    fun `이전 달로 가면 그 달 1일을 고르고 필요한 만큼 더 받는다`() = runTest {
        val repository = FakeCalendarRepository(
            listOf(
                MapPostPage(listOf(calendarPost(3, utcMillis(2026, 8, 5)), calendarPost(2, utcMillis(2026, 7, 25))), isLast = false),
                MapPostPage(listOf(calendarPost(1, utcMillis(2026, 6, 30))), isLast = false),
            ),
        )
        val viewModel = viewModel(repository).apply { open(today) }
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, repository.requests.size)

        viewModel.showPreviousMonth()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(CalendarMonth(2026, 7), viewModel.uiState.value.month)
        assertEquals(CalendarDay(2026, 7, 1), viewModel.uiState.value.selectedDay)
        assertEquals(listOf(0, 1), repository.requests.map { it.first })
        assertEquals(listOf(3L, 2L, 1L), viewModel.uiState.value.posts.map { it.id })
    }

    /** 뒤 달 글은 이미 받아 뒀다. 돌아올 때 다시 부르지 않는다. */
    @Test
    fun `이미 받아 둔 달로 돌아오면 다시 받지 않는다`() = runTest {
        val repository = FakeCalendarRepository(
            listOf(
                MapPostPage(listOf(calendarPost(2, utcMillis(2026, 8, 5)), calendarPost(1, utcMillis(2026, 6, 1))), isLast = false),
            ),
        )
        val viewModel = viewModel(repository).apply { open(today) }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.showPreviousMonth()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.showNextMonth()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.requests.size)
        assertEquals(CalendarMonth(2026, 8), viewModel.uiState.value.month)
    }

    @Test
    fun `마지막 페이지를 받았으면 이전 달로 가도 더 부르지 않는다`() = runTest {
        val repository = FakeCalendarRepository(
            listOf(MapPostPage(listOf(calendarPost(1, utcMillis(2026, 8, 5))), isLast = true)),
        )
        val viewModel = viewModel(repository).apply { open(today) }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.showPreviousMonth()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.requests.size)
        assertFalse(viewModel.uiState.value.loading)
    }

    @Test
    fun `두 번 열어도 한 번만 받는다`() = runTest {
        val repository = FakeCalendarRepository()
        viewModel(repository).apply {
            open(today)
            open(CalendarDay(2026, 1, 1))
        }
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.requests.size)
    }

    @Test
    fun `날짜를 고른다`() = runTest {
        val viewModel = viewModel(FakeCalendarRepository()).apply { open(today) }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.selectDay(CalendarDay(2026, 8, 3))

        assertEquals(CalendarDay(2026, 8, 3), viewModel.uiState.value.selectedDay)
    }

    @Test
    fun `못 받으면 안내를 남기고 재시도하면 이어 받는다`() = runTest {
        val repository = FakeCalendarRepository(
            listOf(MapPostPage(listOf(calendarPost(1, utcMillis(2026, 7, 1))), isLast = true)),
        ).apply { failingPage = 0 }
        val viewModel = viewModel(repository).apply { open(today) }
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(POST_LOAD_FAILED_MESSAGE, viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.loading)

        repository.failingPage = null
        viewModel.retry()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(listOf(1L), viewModel.uiState.value.posts.map { it.id })
    }

    /** 새 글은 목록 맨 앞에 붙어야 한다. 받아 둔 것을 버리고 처음부터 다시 받는다. */
    @Test
    fun `새 게시물이 올라오면 처음부터 다시 받는다`() = runTest {
        val repository = FakeCalendarRepository(
            listOf(MapPostPage(listOf(calendarPost(1, utcMillis(2026, 7, 1))), isLast = true)),
        )
        val viewModel = viewModel(repository).apply { open(today) }
        dispatcher.scheduler.advanceUntilIdle()

        repository.pages = listOf(
            MapPostPage(listOf(calendarPost(2, utcMillis(2026, 8, 20)), calendarPost(1, utcMillis(2026, 7, 1))), isLast = true),
        )
        viewModel.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(0, 0), repository.requests.map { it.first })
        assertEquals(listOf(2L, 1L), viewModel.uiState.value.posts.map { it.id })
    }

    @Test
    fun `달력을 열기 전에는 새로고침해도 받지 않는다`() = runTest {
        val repository = FakeCalendarRepository()

        viewModel(repository).refresh()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(repository.requests.isEmpty())
    }
}
