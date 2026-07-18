package com.example.moamap.feature.officialmap.presentation

import com.example.moamap.feature.officialmap.domain.model.DensityArea
import com.example.moamap.feature.officialmap.domain.repository.FootTrafficRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DensityMapViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sampleArea(code: String) = DensityArea(
        code = code, name = "지역$code", lat = 37.5, lng = 126.9,
        boundaryGeoJson = null, congestion = null,
    )

    private class FakeRepository(
        var result: () -> List<DensityArea>,
    ) : FootTrafficRepository {
        override suspend fun getDensityAreas(): List<DensityArea> = result()
    }

    @Test
    fun `로드 성공 시 Success 상태가 된다`() = runTest {
        val viewModel = DensityMapViewModel(FakeRepository { listOf(sampleArea("A")) })

        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DensityMapUiState.Success)
        assertEquals(1, (state as DensityMapUiState.Success).areas.size)
    }

    @Test
    fun `로드 실패 시 Error 상태가 되고 retry로 복구한다`() = runTest {
        var fail = true
        val repository = FakeRepository {
            if (fail) throw RuntimeException("boom") else listOf(sampleArea("A"))
        }
        val viewModel = DensityMapViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value is DensityMapUiState.Error)

        fail = false
        viewModel.retry()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is DensityMapUiState.Success)
    }

    @Test
    fun `같은 지역을 다시 선택하면 해제된다`() = runTest {
        val viewModel = DensityMapViewModel(FakeRepository { listOf(sampleArea("A")) })
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.selectArea("A")
        assertEquals("A", (viewModel.uiState.value as DensityMapUiState.Success).selectedCode)

        viewModel.selectArea("A")
        assertEquals(null, (viewModel.uiState.value as DensityMapUiState.Success).selectedCode)
    }
}
