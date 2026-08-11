package com.example.moamap.feature.mapdetail.presentation.intro

import androidx.lifecycle.SavedStateHandle
import com.example.moamap.core.navigation.MoaMapRoute
import com.example.moamap.feature.mapdetail.domain.model.MapPlacePreview
import com.example.moamap.feature.mapdetail.presentation.FakeMapDetailRepository
import com.example.moamap.feature.mapdetail.presentation.MapLoadState
import com.example.moamap.feature.mapdetail.presentation.testMap
import com.example.moamap.feature.mapdetail.presentation.testPlace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapIntroViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: FakeMapDetailRepository) = MapIntroViewModel(
        savedStateHandle = SavedStateHandle(mapOf(MoaMapRoute.MapIntro.ARG_MAP_ID to 1L)),
        repository = repository,
    )

    @Test
    fun `만들어지기만 하면 조회하지 않는다`() = runTest {
        val repository = FakeMapDetailRepository()

        viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        // 화면이 보일 때 refresh 가 첫 조회를 겸한다.
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun `지도와 장소를 함께 읽는다`() = runTest {
        val repository = FakeMapDetailRepository(
            places = { MapPlacePreview(places = listOf(testPlace(1L)), hasMore = true) },
        )
        val viewModel = viewModel(repository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.map is MapLoadState.Success)
        assertEquals("지도1", viewModel.uiState.value.title)
        assertEquals(1, viewModel.uiState.value.places.places.size)
        assertTrue(viewModel.uiState.value.places.hasMore)
        // 보여줄 개수는 피그마의 4개다.
        assertTrue(repository.calls.contains("getPlacePreview($INTRO_PLACE_COUNT)"))
    }

    @Test
    fun `장소 조회만 실패해도 지도는 보여준다`() = runTest {
        val repository = object : FakeMapDetailRepository() {
            override suspend fun getPlacePreview(
                mapId: Long,
                visibleCount: Int,
            ): MapPlacePreview = throw RuntimeException("boom")
        }
        val viewModel = viewModel(repository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.map is MapLoadState.Success)
        assertTrue(viewModel.uiState.value.places.places.isEmpty())
    }

    @Test
    fun `지도 조회에 실패하면 Error 가 되고 retry 로 복구한다`() = runTest {
        var fail = true
        val repository = FakeMapDetailRepository(
            map = { if (fail) throw RuntimeException("boom") else testMap() },
        )
        val viewModel = viewModel(repository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.map is MapLoadState.Error)

        fail = false
        viewModel.retry()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.map is MapLoadState.Success)
    }

    @Test
    fun `돌아와서 다시 읽는 동안에는 보던 지도가 남는다`() = runTest {
        val repository = FakeMapDetailRepository(responseDelayMillis = 100L)
        val viewModel = viewModel(repository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.refresh()
        dispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value.map is MapLoadState.Success)
    }

    @Test
    fun `참여하면 상세로 넘어갈 신호를 낸다`() = runTest {
        val repository = FakeMapDetailRepository()
        val viewModel = viewModel(repository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.join()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(repository.calls.contains("joinMap"))
        assertTrue(viewModel.uiState.value.joined)
    }

    @Test
    fun `요청이 도는 동안 다시 눌러도 한 번만 참여한다`() = runTest {
        val repository = FakeMapDetailRepository(responseDelayMillis = 100L)
        val viewModel = viewModel(repository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.join()
        dispatcher.scheduler.runCurrent()
        viewModel.join()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.calls.count { call -> call == "joinMap" })
    }

    @Test
    fun `참여에 실패하면 안내가 뜨고 넘어가지 않는다`() = runTest {
        val repository = object : FakeMapDetailRepository() {
            override suspend fun joinMap(mapId: Long) = throw RuntimeException("boom")
        }
        val viewModel = viewModel(repository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.join()
        dispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.joined)
    }
}
