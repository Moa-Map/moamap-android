package com.moamap.app.feature.officialmap.presentation

import com.moamap.app.feature.mapdetail.ViewportBounds
import com.moamap.app.feature.officialmap.domain.model.RestroomDetail
import com.moamap.app.feature.officialmap.domain.model.RestroomMarker
import com.moamap.app.feature.officialmap.domain.model.RestroomMarkers
import com.moamap.app.feature.officialmap.domain.repository.RestroomRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RestroomMapViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeRestroomRepository : RestroomRepository {
        val boundsCalls = mutableListOf<List<Double>>()
        var markers: () -> RestroomMarkers = { RestroomMarkers(emptyList(), truncated = false) }
        var detailCalls = 0
        var detail: (Long) -> RestroomDetail = { id -> sampleDetail(id) }

        override suspend fun getRestrooms(south: Double, west: Double, north: Double, east: Double): RestroomMarkers {
            boundsCalls += listOf(south, west, north, east)
            return markers()
        }

        override suspend fun getRestroom(id: Long): RestroomDetail {
            detailCalls++
            return detail(id)
        }
    }

    private val bounds = ViewportBounds(west = 126.9, south = 37.5, east = 127.0, north = 37.6)
    private val otherBounds = ViewportBounds(west = 127.0, south = 37.4, east = 127.1, north = 37.5)

    private fun ready(repository: FakeRestroomRepository, vararg ids: Long): RestroomMapViewModel {
        repository.markers = { RestroomMarkers(ids.map(::sampleMarker), truncated = false) }
        return RestroomMapViewModel(repository).also { viewModel ->
            viewModel.onCameraIdle(bounds)
            dispatcher.scheduler.advanceUntilIdle()
        }
    }

    @Test
    fun `카메라가 멈추면 보이는 범위를 남·서·북·동으로 넘겨 화장실을 읽는다`() {
        val repository = FakeRestroomRepository()

        val viewModel = ready(repository, 1L, 2L)

        assertEquals(listOf(listOf(37.5, 126.9, 37.6, 127.0)), repository.boundsCalls)
        assertEquals(listOf(1L, 2L), viewModel.uiState.value.restrooms.map { it.id })
        assertFalse(viewModel.uiState.value.truncated)
    }

    @Test
    fun `같은 범위로 다시 멈추면 다시 읽지 않는다`() {
        val repository = FakeRestroomRepository()
        val viewModel = ready(repository, 1L)

        viewModel.onCameraIdle(bounds.copy())
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.boundsCalls.size)
    }

    @Test
    fun `서버가 일부만 주면 확대 안내를 띄울 수 있게 표시한다`() {
        val repository = FakeRestroomRepository()
        repository.markers = { RestroomMarkers(listOf(sampleMarker(1)), truncated = true) }
        val viewModel = RestroomMapViewModel(repository)

        viewModel.onCameraIdle(bounds)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.truncated)
    }

    @Test
    fun `읽기에 실패하면 알리고, 같은 자리에서 다시 멈춰도 다시 읽는다`() {
        val repository = FakeRestroomRepository()
        var fail = true
        repository.markers = {
            if (fail) throw RuntimeException("boom")
            RestroomMarkers(listOf(sampleMarker(1)), truncated = false)
        }
        val viewModel = RestroomMapViewModel(repository)

        viewModel.onCameraIdle(bounds)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.loadFailed)

        fail = false
        viewModel.onCameraIdle(bounds)
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.loadFailed)
        assertEquals(listOf(1L), viewModel.uiState.value.restrooms.map { it.id })
    }

    @Test
    fun `마커를 누르면 카드가 뜨고 상세를 채운다`() {
        val repository = FakeRestroomRepository()
        val viewModel = ready(repository, 1L, 2L)

        viewModel.selectRestroom(2)
        assertEquals(RestroomDetailState.Loading, viewModel.uiState.value.detail)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(2L, viewModel.uiState.value.selected?.id)
        assertEquals(sampleDetail(2), (viewModel.uiState.value.detail as RestroomDetailState.Loaded).detail)
    }

    @Test
    fun `상세를 못 읽으면 실패로 두고, 같은 마커를 다시 누르면 다시 읽는다`() {
        val repository = FakeRestroomRepository()
        var fail = true
        repository.detail = { id -> if (fail) throw RuntimeException("boom") else sampleDetail(id) }
        val viewModel = ready(repository, 1L)

        viewModel.selectRestroom(1)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(RestroomDetailState.Failed, viewModel.uiState.value.detail)

        fail = false
        viewModel.selectRestroom(1)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, repository.detailCalls)
        assertTrue(viewModel.uiState.value.detail is RestroomDetailState.Loaded)
    }

    @Test
    fun `이미 읽은 마커를 다시 누르면 다시 읽지 않는다`() {
        val repository = FakeRestroomRepository()
        val viewModel = ready(repository, 1L)

        viewModel.selectRestroom(1)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.selectRestroom(1)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.detailCalls)
    }

    @Test
    fun `지도를 옮겨 고른 화장실이 목록에서 빠져도 카드는 남는다`() {
        val repository = FakeRestroomRepository()
        val viewModel = ready(repository, 1L)
        viewModel.selectRestroom(1)
        dispatcher.scheduler.advanceUntilIdle()

        repository.markers = { RestroomMarkers(listOf(sampleMarker(9)), truncated = false) }
        viewModel.onCameraIdle(otherBounds)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1L, viewModel.uiState.value.selected?.id)
    }

    @Test
    fun `빈 곳을 누르면 카드를 닫는다`() {
        val repository = FakeRestroomRepository()
        val viewModel = ready(repository, 1L)
        viewModel.selectRestroom(1)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.clearSelection()

        assertNull(viewModel.uiState.value.selected)
        assertNull(viewModel.uiState.value.detail)
    }
}

private fun sampleMarker(id: Long) = RestroomMarker(
    id = id, name = "화장실$id", latitude = 37.5, longitude = 126.9, category = "공중화장실",
)

private fun sampleDetail(id: Long) = RestroomDetail(
    id = id, name = "화장실$id", category = "공중화장실", address = "주소$id",
    openHours = "정시", openHoursDetail = null,
    maleToilet = 1, maleUrinal = 0, maleDisabledToilet = 0, maleDisabledUrinal = 0,
    maleChildToilet = 0, maleChildUrinal = 0,
    femaleToilet = 1, femaleDisabledToilet = 0, femaleChildToilet = 0,
    diaperTable = false, emergencyBell = false, entranceCctv = false,
    managerOrg = null, phone = null, dataRefDate = null,
)
