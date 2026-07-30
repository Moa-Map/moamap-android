package com.example.moamap.feature.collection.presentation.placeimport

import androidx.lifecycle.SavedStateHandle
import com.example.moamap.core.navigation.MoaMapRoute
import com.example.moamap.core.network.ConnectionException
import com.example.moamap.feature.collection.domain.model.ImportedPlace
import com.example.moamap.feature.collection.domain.model.MapType
import com.example.moamap.feature.collection.domain.model.MyMap
import com.example.moamap.feature.collection.domain.model.NewMap
import com.example.moamap.feature.collection.domain.model.PlaceExtractionException
import com.example.moamap.feature.collection.domain.model.PlaceImportSource
import com.example.moamap.feature.collection.domain.model.PlaceSaveResult
import com.example.moamap.feature.collection.domain.repository.MapRepository
import com.example.moamap.feature.collection.domain.repository.PlaceImportRepository
import com.example.moamap.feature.collection.presentation.MyMapsState
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

private fun place(id: String, name: String, kakaoPlaceId: String? = id) = ImportedPlace(
    id = id,
    name = name,
    roadAddress = "서울 동작구 상도로 369",
    kakaoPlaceId = kakaoPlaceId,
    sourceType = "INSTAGRAM",
)

private val Places = listOf(
    place(id = "a", name = "커피나무"),
    place(id = "b", name = "블루보틀 성수"),
)

private val MyMaps = listOf(
    MyMap(11L, "성수 카페 투어", null, memberCount = 1, placeCount = 8, official = false, personal = false),
    MyMap(12L, "주말 데이트", null, memberCount = 3, placeCount = 0, official = false, personal = false),
)

private class FakeMapRepository : MapRepository {

    var failure: Throwable? = null
    var maps: List<MyMap> = MyMaps

    val calls = mutableListOf<MapType>()

    override suspend fun getMyMaps(type: MapType): List<MyMap> {
        calls += type
        failure?.let { throw it }
        return maps
    }

    override suspend fun uploadCoverImage(imageUri: String) = TODO("사용하지 않음")

    override suspend fun createMap(newMap: NewMap) = TODO("사용하지 않음")

    override suspend fun joinByInviteCode(inviteCode: String) = TODO("사용하지 않음")
}

private class FakePlaceImportRepository : PlaceImportRepository {

    var failure: Throwable? = null
    var places: List<ImportedPlace> = Places

    /** 값을 넣으면 완료될 때까지 추출이 매달린다. */
    var pending: CompletableDeferred<Unit>? = null

    var callCount: Int = 0
        private set

    var mapShareCallCount: Int = 0
        private set

    var coordinateCallCount: Int = 0
        private set

    var lastCoordinate: Pair<Double, Double>? = null
        private set

    var saveFailure: Throwable? = null
    var saveResult = PlaceSaveResult(created = 2, duplicate = 0, failed = 0)

    /** 값을 넣으면 완료될 때까지 저장이 매달린다. */
    var savePending: CompletableDeferred<Unit>? = null

    var savedMapIds: Set<Long>? = null
        private set

    var savedPlaces: List<ImportedPlace>? = null
        private set

    override suspend fun savePlaces(
        mapIds: Set<Long>,
        places: List<ImportedPlace>,
    ): PlaceSaveResult {
        savedMapIds = mapIds
        savedPlaces = places
        savePending?.await()
        saveFailure?.let { throw it }
        return saveResult
    }

    override suspend fun extractPlaces(url: String): List<ImportedPlace> {
        callCount++
        return extract()
    }

    override suspend fun extractMapSharePlaces(url: String): List<ImportedPlace> {
        mapShareCallCount++
        return extract()
    }

    override suspend fun findPlaceAtCoordinate(lat: Double, lng: Double): List<ImportedPlace> {
        coordinateCallCount++
        lastCoordinate = lat to lng
        return extract()
    }

    private suspend fun extract(): List<ImportedPlace> {
        pending?.await()
        failure?.let { throw it }
        return places
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PlaceImportViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakePlaceImportRepository()
    private val mapRepository = FakeMapRepository()

    private lateinit var viewModel: PlaceImportViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = viewModel(PlaceImportSource.Instagram)
    }

    private fun viewModel(source: PlaceImportSource, url: String = "") = PlaceImportViewModel(
        SavedStateHandle(
            mapOf(
                MoaMapRoute.PlaceImport.ARG_SOURCE to source.name,
                MoaMapRoute.PlaceImport.ARG_URL to url,
            ),
        ),
        repository,
        mapRepository,
    )

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun startExtraction() {
        viewModel.updateUrl("https://www.instagram.com/reel/ABC123/")
        viewModel.startExtraction()
    }

    @Test
    fun `공유로 들어오면 URL이 채워진 채로 시작한다`() = runTest(dispatcher) {
        val shared = viewModel(PlaceImportSource.MapShare, "https://naver.me/xAbCdEf")
        advanceUntilIdle()

        assertEquals("https://naver.me/xAbCdEf", shared.uiState.value.url)
        assertTrue(shared.uiState.value.canSearch)
        // 채우기만 한다. 검색은 사용자가 누른다.
        assertEquals(ExtractionState.Idle, shared.uiState.value.extraction)
        assertEquals(0, repository.mapShareCallCount)
    }

    @Test
    fun `모음 탭으로 들어오면 URL이 비어 있다`() = runTest(dispatcher) {
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.url)
        assertFalse(viewModel.uiState.value.canSearch)
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
        viewModel.togglePlace(Places[1].id)

        val gate = CompletableDeferred<Unit>()
        repository.pending = gate
        viewModel.startExtraction()
        advanceUntilIdle()
        viewModel.cancelExtraction()

        // 취소한 사용자가 결과를 잃고 URL 입력부터 다시 하게 두면 안 된다.
        assertEquals(Places, viewModel.uiState.value.places)
        assertEquals(listOf(Places[1]), viewModel.uiState.value.selectedPlaces)

        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `재시도가 실패해도 보고 있던 목록과 선택이 유지된다`() = runTest(dispatcher) {
        startExtraction()
        advanceUntilIdle()
        viewModel.togglePlace(Places[1].id)

        repository.failure = ConnectionException(IOException("boom"))
        viewModel.startExtraction()
        advanceUntilIdle()

        // 한 번 실패했다는 이유로 처음부터 다시 하게 만들면 안 된다.
        assertEquals(Places, viewModel.uiState.value.places)
        assertEquals(listOf(Places[1]), viewModel.uiState.value.selectedPlaces)
        assertEquals("네트워크에 연결할 수 없어요", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `장소는 여러 개 선택할 수 있고 다시 누르면 해제된다`() = runTest(dispatcher) {
        startExtraction()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.canProceed)

        viewModel.togglePlace(Places[0].id)
        viewModel.togglePlace(Places[1].id)

        assertEquals(Places, viewModel.uiState.value.selectedPlaces)
        assertTrue(viewModel.uiState.value.canProceed)

        viewModel.togglePlace(Places[0].id)

        assertEquals(listOf(Places[1]), viewModel.uiState.value.selectedPlaces)
    }

    @Test
    fun `선택한 장소는 고른 순서가 아니라 목록 순서로 나온다`() = runTest(dispatcher) {
        startExtraction()
        advanceUntilIdle()

        viewModel.togglePlace(Places[1].id)
        viewModel.togglePlace(Places[0].id)

        // 지도 선택 화면이 이 순서 그대로 노란 카드에 나열한다.
        assertEquals(Places, viewModel.uiState.value.selectedPlaces)
    }

    @Test
    fun `재시도하면 골랐던 장소가 모두 초기화된다`() = runTest(dispatcher) {
        startExtraction()
        advanceUntilIdle()
        viewModel.togglePlace(Places[0].id)
        viewModel.togglePlace(Places[1].id)

        viewModel.startExtraction()

        assertEquals(emptySet<String>(), viewModel.uiState.value.selectedPlaceIds)
        assertEquals(ExtractionState.Loading, viewModel.uiState.value.extraction)

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.extraction is ExtractionState.Success)
    }

    @Test
    fun `인스타그램으로 들어오면 인스타그램 추출을 부른다`() = runTest(dispatcher) {
        startExtraction()
        advanceUntilIdle()

        assertEquals(1, repository.callCount)
        assertEquals(0, repository.mapShareCallCount)
    }

    @Test
    fun `외부 지도로 들어오면 공유 링크 추출을 부른다`() = runTest(dispatcher) {
        viewModel = viewModel(PlaceImportSource.MapShare)

        startExtraction()
        advanceUntilIdle()

        assertEquals(1, repository.mapShareCallCount)
        assertEquals(0, repository.callCount)
    }

    @Test
    fun `외부 지도는 가져온 장소를 모두 고른 채로 시작한다`() = runTest(dispatcher) {
        // 리스트를 통째로 가져오는 것이라 빼고 싶은 것만 해제하게 한다.
        viewModel = viewModel(PlaceImportSource.MapShare)

        startExtraction()
        advanceUntilIdle()

        assertEquals(Places, viewModel.uiState.value.selectedPlaces)
        assertTrue(viewModel.uiState.value.canProceed)
    }

    @Test
    fun `인스타그램은 아무것도 고르지 않은 채로 시작한다`() = runTest(dispatcher) {
        startExtraction()
        advanceUntilIdle()

        assertEquals(emptySet<String>(), viewModel.uiState.value.selectedPlaceIds)
        assertFalse(viewModel.uiState.value.canProceed)
    }

    @Test
    fun `지도는 여러 개 선택할 수 있고 다시 누르면 해제된다`() = runTest(dispatcher) {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.toggleMap(MyMaps[0].id)
        viewModel.toggleMap(MyMaps[1].id)

        assertEquals(setOf(MyMaps[0].id, MyMaps[1].id), viewModel.uiState.value.selectedMapIds)
        assertTrue(viewModel.uiState.value.canSave)

        viewModel.toggleMap(MyMaps[0].id)

        assertEquals(setOf(MyMaps[1].id), viewModel.uiState.value.selectedMapIds)
    }

    @Test
    fun `등록에 필요한 값이 없는 후보는 고를 수 없다`() = runTest(dispatcher) {
        // 서버가 kakaoPlaceId 를 등록 키로 요구해서, 없는 후보는 저장까지 갈 수 없다.
        repository.places = listOf(place(id = "candidate-0", name = "이름만 있는 곳", kakaoPlaceId = null))
        startExtraction()
        advanceUntilIdle()

        viewModel.togglePlace("candidate-0")

        assertEquals(emptySet<String>(), viewModel.uiState.value.selectedPlaceIds)
        assertFalse(viewModel.uiState.value.canProceed)
    }

    @Test
    fun `외부 지도를 전부 고를 때도 등록할 수 없는 후보는 빼둔다`() = runTest(dispatcher) {
        repository.places = listOf(Places[0], place(id = "candidate-1", name = "미매칭", kakaoPlaceId = null))
        viewModel = viewModel(PlaceImportSource.MapShare)

        startExtraction()
        advanceUntilIdle()

        assertEquals(setOf("a"), viewModel.uiState.value.selectedPlaceIds)
    }

    @Test
    fun `저장하면 고른 지도와 장소를 넘긴다`() = runTest(dispatcher) {
        startExtraction()
        advanceUntilIdle()
        viewModel.togglePlace(Places[1].id)
        viewModel.toggleMap(MyMaps[0].id)

        viewModel.savePlaces()
        advanceUntilIdle()

        assertEquals(setOf(MyMaps[0].id), repository.savedMapIds)
        assertEquals(listOf(Places[1]), repository.savedPlaces)
    }

    @Test
    fun `저장에 성공하면 결과가 상태에 남는다`() = runTest(dispatcher) {
        repository.saveResult = PlaceSaveResult(created = 1, duplicate = 1, failed = 0)
        startExtraction()
        advanceUntilIdle()
        viewModel.togglePlace(Places[0].id)
        viewModel.toggleMap(MyMaps[0].id)

        viewModel.savePlaces()
        advanceUntilIdle()

        assertEquals(
            PlaceSaveResult(created = 1, duplicate = 1, failed = 0),
            viewModel.uiState.value.saveResult,
        )
        assertFalse(viewModel.uiState.value.saving)
    }

    @Test
    fun `한 곳도 등록되지 않으면 흐름을 닫지 않고 안내한다`() = runTest(dispatcher) {
        repository.saveResult = PlaceSaveResult(created = 0, duplicate = 2, failed = 0)
        startExtraction()
        advanceUntilIdle()
        viewModel.togglePlace(Places[0].id)
        viewModel.toggleMap(MyMaps[0].id)

        viewModel.savePlaces()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.saveResult)
        assertEquals("이미 저장되어 있는 장소예요", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `저장하는 동안에는 다시 저장하지 않는다`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        repository.savePending = gate
        startExtraction()
        advanceUntilIdle()
        viewModel.togglePlace(Places[0].id)
        viewModel.toggleMap(MyMaps[0].id)

        viewModel.savePlaces()
        advanceUntilIdle()

        // 두 번 눌러 같은 장소가 두 번 등록되면 안 된다.
        assertTrue(viewModel.uiState.value.saving)
        assertFalse(viewModel.uiState.value.canSave)

        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `저장에 실패하면 안내를 내고 다시 시도할 수 있다`() = runTest(dispatcher) {
        repository.saveFailure = ConnectionException(IOException("boom"))
        startExtraction()
        advanceUntilIdle()
        viewModel.togglePlace(Places[0].id)
        viewModel.toggleMap(MyMaps[0].id)

        viewModel.savePlaces()
        advanceUntilIdle()

        assertEquals("네트워크에 연결할 수 없어요", viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.saveResult)
        assertFalse(viewModel.uiState.value.saving)
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `고른 지도가 없으면 저장하지 않는다`() = runTest(dispatcher) {
        startExtraction()
        advanceUntilIdle()
        viewModel.togglePlace(Places[0].id)

        viewModel.savePlaces()
        advanceUntilIdle()

        assertNull(repository.savedMapIds)
    }

    @Test
    fun `저장할 지도 목록은 내 프라이빗 지도만 읽는다`() = runTest(dispatcher) {
        advanceUntilIdle()

        // 커뮤니티 지도는 남이 만든 것이라 여기에 저장할 수 없다.
        assertEquals(listOf(MapType.Private), mapRepository.calls)
        assertEquals(MyMapsState.Success(MyMaps), viewModel.uiState.value.targetMaps)
    }

    @Test
    fun `지도 목록을 읽지 못하면 안내를 내고 다시 읽을 수 있다`() = runTest(dispatcher) {
        mapRepository.failure = ConnectionException(IOException("boom"))
        viewModel = viewModel(PlaceImportSource.Instagram)
        advanceUntilIdle()

        assertEquals(
            MyMapsState.Error("지도 목록을 불러오지 못했어요"),
            viewModel.uiState.value.targetMaps,
        )

        mapRepository.failure = null
        viewModel.retryLoadMaps()
        advanceUntilIdle()

        assertEquals(MyMapsState.Success(MyMaps), viewModel.uiState.value.targetMaps)
    }
}
