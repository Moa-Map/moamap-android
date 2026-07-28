package com.example.moamap.feature.collection.presentation.placeimport

import com.example.moamap.core.network.ConnectionException
import com.example.moamap.feature.collection.domain.model.ImportedPlace
import com.example.moamap.feature.collection.domain.model.PlaceExtractionException
import com.example.moamap.feature.collection.domain.repository.PlaceImportRepository
import kotlinx.coroutines.CompletableDeferred
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
import java.io.IOException

private val Places = listOf(
    ImportedPlace(id = "a", name = "커피나무", address = "서울 동작구 상도로 369"),
    ImportedPlace(id = "b", name = "블루보틀 성수", address = "서울 성동구 아차산로 7"),
)

private class FakePlaceImportRepository : PlaceImportRepository {

    var failure: Throwable? = null
    var places: List<ImportedPlace> = Places

    /** 값을 넣으면 완료될 때까지 추출이 매달린다. */
    var pending: CompletableDeferred<Unit>? = null

    var callCount: Int = 0
        private set

    override suspend fun extractPlaces(url: String): List<ImportedPlace> {
        callCount++
        pending?.await()
        failure?.let { throw it }
        return places
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PlaceImportViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakePlaceImportRepository()

    private lateinit var viewModel: PlaceImportViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = PlaceImportViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun startExtraction() {
        viewModel.updateUrl("https://www.instagram.com/reel/ABC123/")
        viewModel.startExtraction()
    }

    @Test
    fun `URL이 비어 있으면 추출을 시작하지 않는다`() = runTest(dispatcher) {
        viewModel.startExtraction()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.canSearch)
        assertEquals(0, repository.callCount)
        assertEquals(ExtractionState.Idle, viewModel.uiState.value.extraction)
    }

    @Test
    fun `추출에 성공하면 장소 목록이 나온다`() = runTest(dispatcher) {
        startExtraction()
        assertEquals(ExtractionState.Loading, viewModel.uiState.value.extraction)

        advanceUntilIdle()

        assertEquals(Places, viewModel.uiState.value.places)
    }

    @Test
    fun `앞뒤 공백을 지운 URL로 요청한다`() = runTest(dispatcher) {
        viewModel.updateUrl("  https://www.instagram.com/reel/ABC123/  ")
        viewModel.startExtraction()
        advanceUntilIdle()

        assertEquals(Places, viewModel.uiState.value.places)
    }

    @Test
    fun `캡션을 읽지 못하면 그 이유를 그대로 안내한다`() = runTest(dispatcher) {
        repository.failure = PlaceExtractionException.CaptionBlocked()

        startExtraction()
        advanceUntilIdle()

        assertEquals("비공개 게시물이라 장소를 가져올 수 없어요", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `서버에 닿지 못하면 네트워크 안내를 낸다`() = runTest(dispatcher) {
        repository.failure = ConnectionException(IOException("boom"))

        startExtraction()
        advanceUntilIdle()

        assertEquals("네트워크에 연결할 수 없어요", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `안내를 소비하면 초기 상태로 돌아간다`() = runTest(dispatcher) {
        repository.failure = PlaceExtractionException.CaptionUnavailable()
        startExtraction()
        advanceUntilIdle()

        viewModel.consumeError()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(ExtractionState.Idle, viewModel.uiState.value.extraction)
    }

    @Test
    fun `추출을 취소하면 결과가 나중에 도착하지 않는다`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        repository.pending = gate

        startExtraction()
        advanceUntilIdle()
        viewModel.cancelExtraction()

        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(ExtractionState.Idle, viewModel.uiState.value.extraction)
    }

    @Test
    fun `재시도를 취소하면 보고 있던 목록으로 되돌아간다`() = runTest(dispatcher) {
        startExtraction()
        advanceUntilIdle()
        viewModel.selectPlace(Places[1].id)

        val gate = CompletableDeferred<Unit>()
        repository.pending = gate
        viewModel.startExtraction()
        advanceUntilIdle()
        viewModel.cancelExtraction()

        // 취소한 사용자가 결과를 잃고 URL 입력부터 다시 하게 두면 안 된다.
        assertEquals(Places, viewModel.uiState.value.places)
        assertEquals(Places[1], viewModel.uiState.value.selectedPlace)

        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `장소는 하나만 선택되고 다시 고르면 교체된다`() = runTest(dispatcher) {
        startExtraction()
        advanceUntilIdle()

        viewModel.selectPlace(Places[0].id)
        assertEquals(Places[0], viewModel.uiState.value.selectedPlace)
        assertTrue(viewModel.uiState.value.canProceed)

        viewModel.selectPlace(Places[1].id)
        assertEquals(Places[1], viewModel.uiState.value.selectedPlace)
    }

    @Test
    fun `재시도하면 골랐던 장소가 초기화된다`() = runTest(dispatcher) {
        startExtraction()
        advanceUntilIdle()
        viewModel.selectPlace(Places[0].id)

        viewModel.startExtraction()

        assertNull(viewModel.uiState.value.selectedPlaceId)
        assertEquals(ExtractionState.Loading, viewModel.uiState.value.extraction)

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.extraction is ExtractionState.Success)
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
}
