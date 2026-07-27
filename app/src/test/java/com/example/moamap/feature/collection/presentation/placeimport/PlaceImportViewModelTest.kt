package com.example.moamap.feature.collection.presentation.placeimport

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
class PlaceImportViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var viewModel: PlaceImportViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = PlaceImportViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun extractSuccessfully() {
        viewModel.updateUrl("https://www.instagram.com/reel/ABC123/")
        viewModel.startExtraction()
    }

    @Test
    fun `URL이 비어 있으면 검색할 수 없다`() {
        assertFalse(viewModel.uiState.value.canSearch)

        viewModel.updateUrl("  ")

        assertFalse(viewModel.uiState.value.canSearch)
    }

    @Test
    fun `URL을 입력하면 검색할 수 있다`() {
        viewModel.updateUrl("https://www.instagram.com/reel/ABC123/")

        assertTrue(viewModel.uiState.value.canSearch)
    }

    @Test
    fun `URL이 비어 있으면 추출을 시작하지 않는다`() = runTest(dispatcher) {
        viewModel.startExtraction()
        advanceUntilIdle()

        assertEquals(ExtractionState.Idle, viewModel.uiState.value.extraction)
    }

    @Test
    fun `추출을 시작하면 로딩을 거쳐 장소 목록이 나온다`() = runTest(dispatcher) {
        extractSuccessfully()

        assertEquals(ExtractionState.Loading, viewModel.uiState.value.extraction)

        advanceUntilIdle()

        val extraction = viewModel.uiState.value.extraction
        assertTrue(extraction is ExtractionState.Success)
        assertTrue((extraction as ExtractionState.Success).places.isNotEmpty())
    }

    @Test
    fun `추출을 취소하면 초기 상태로 돌아가고 결과가 나중에 도착하지 않는다`() = runTest(dispatcher) {
        extractSuccessfully()

        viewModel.cancelExtraction()
        assertEquals(ExtractionState.Idle, viewModel.uiState.value.extraction)

        // 취소한 작업이 살아 있었다면 지연이 끝나면서 Success 로 바뀔 것이다.
        advanceUntilIdle()

        assertEquals(ExtractionState.Idle, viewModel.uiState.value.extraction)
    }

    @Test
    fun `장소는 하나만 선택되고 다시 고르면 교체된다`() = runTest(dispatcher) {
        extractSuccessfully()
        advanceUntilIdle()
        val places = viewModel.uiState.value.places

        viewModel.selectPlace(places[0].id)
        assertEquals(places[0], viewModel.uiState.value.selectedPlace)
        assertTrue(viewModel.uiState.value.canProceed)

        viewModel.selectPlace(places[1].id)
        assertEquals(places[1], viewModel.uiState.value.selectedPlace)
    }

    @Test
    fun `장소를 고르지 않으면 다음 단계로 갈 수 없다`() = runTest(dispatcher) {
        extractSuccessfully()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.canProceed)
    }

    @Test
    fun `재시도하면 골랐던 장소가 초기화된다`() = runTest(dispatcher) {
        extractSuccessfully()
        advanceUntilIdle()
        viewModel.selectPlace(viewModel.uiState.value.places.first().id)

        viewModel.startExtraction()

        assertNull(viewModel.uiState.value.selectedPlaceId)
        assertEquals(ExtractionState.Loading, viewModel.uiState.value.extraction)
    }

    @Test
    fun `지도는 여러 개 선택할 수 있고 다시 누르면 해제된다`() {
        val maps = viewModel.uiState.value.targetMaps
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.toggleMap(maps[0].id)
        viewModel.toggleMap(maps[1].id)

        assertEquals(setOf(maps[0].id, maps[1].id), viewModel.uiState.value.selectedMapIds)
        assertTrue(viewModel.uiState.value.canSave)

        viewModel.toggleMap(maps[0].id)

        assertEquals(setOf(maps[1].id), viewModel.uiState.value.selectedMapIds)
    }

    @Test
    fun `지도를 모두 해제하면 저장할 수 없다`() {
        val map = viewModel.uiState.value.targetMaps.first()

        viewModel.toggleMap(map.id)
        viewModel.toggleMap(map.id)

        assertFalse(viewModel.uiState.value.canSave)
    }
}
