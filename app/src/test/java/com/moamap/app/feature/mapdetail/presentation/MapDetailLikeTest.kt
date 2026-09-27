package com.moamap.app.feature.mapdetail.presentation

import androidx.lifecycle.SavedStateHandle
import com.moamap.app.core.navigation.MoaMapRoute
import com.moamap.app.feature.mapdetail.domain.model.MapRole
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

/** 장소 하트. 누르는 즉시 반영하고 서버 응답으로 확정한다. */
@OptIn(ExperimentalCoroutinesApi::class)
class MapDetailLikeTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun repository(joined: Boolean = true, liked: Boolean = false, likeCount: Int = 3) =
        FakeMapDetailRepository(
            responseDelayMillis = 100L,
            map = { testMap(joined = joined, role = MapRole.Member) },
            allPlaces = { listOf(testPlace(1L, liked = liked, likeCount = likeCount)) },
        )

    private fun viewModel(repository: FakeMapDetailRepository) = MapDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf(MoaMapRoute.MapDetail.ARG_MAP_ID to 1L)),
        repository = repository,
    )

    private fun MapDetailViewModel.place() = uiState.value.places.single()

    @Test
    fun `누르면 바로 바뀌고 서버 값으로 확정된다`() = runTest {
        val repository = repository()
        val viewModel = viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleLike(1L)
        dispatcher.scheduler.runCurrent()

        // 응답 전: 화면이 먼저 바뀐다.
        assertTrue(viewModel.place().liked)
        assertEquals(4, viewModel.place().likeCount)

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("likePlace"), repository.calls.filter { call -> "like" in call })
        assertTrue(viewModel.place().liked)
        assertEquals(10, viewModel.place().likeCount)
    }

    @Test
    fun `눌러 둔 하트를 다시 누르면 취소한다`() = runTest {
        val repository = repository(liked = true, likeCount = 5)
        val viewModel = viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleLike(1L)
        dispatcher.scheduler.runCurrent()

        assertFalse(viewModel.place().liked)
        assertEquals(4, viewModel.place().likeCount)

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("unlikePlace"), repository.calls.filter { call -> "like" in call })
    }

    @Test
    fun `실패하면 되돌리고 안내한다`() = runTest {
        val repository = repository().apply { likeFailure = RuntimeException("boom") }
        val viewModel = viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleLike(1L)
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.place().liked)
        assertEquals(3, viewModel.place().likeCount)
        assertEquals(LIKE_FAILED_MESSAGE, viewModel.uiState.value.errorMessage)
    }

    /** 서버가 멤버만 받아 준다. 보내 봐야 실패할 요청이다. */
    @Test
    fun `참여하지 않았으면 요청 없이 안내만 한다`() = runTest {
        val repository = repository(joined = false)
        val viewModel = viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleLike(1L)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(repository.calls.none { call -> "like" in call })
        assertFalse(viewModel.place().liked)
        assertEquals(LIKE_NEEDS_JOIN_MESSAGE, viewModel.uiState.value.errorMessage)
    }

    /** 연달아 보낸 요청의 응답 순서가 뒤바뀌면 화면이 서버와 다른 상태로 남는다. */
    @Test
    fun `응답을 기다리는 동안 같은 장소는 다시 눌리지 않는다`() = runTest {
        val repository = repository()
        val viewModel = viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleLike(1L)
        dispatcher.scheduler.runCurrent()
        viewModel.toggleLike(1L)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("likePlace"), repository.calls.filter { call -> "like" in call })
        assertTrue(viewModel.place().liked)
        assertNull(viewModel.uiState.value.errorMessage)

        // 응답이 오면 다시 누를 수 있다.
        viewModel.toggleLike(1L)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            listOf("likePlace", "unlikePlace"),
            repository.calls.filter { call -> "like" in call },
        )
    }
}
