package com.moamap.app.feature.collection.presentation.placeimport

import androidx.lifecycle.SavedStateHandle
import com.moamap.app.core.common.PLACE_ADDED_TOAST
import com.moamap.app.core.common.upload.ImageUploadException
import com.moamap.app.core.common.upload.MAX_PLACE_PHOTO_FILE_SIZE
import com.moamap.app.core.navigation.MoaMapRoute
import com.moamap.app.core.network.ApiException
import com.moamap.app.core.network.ConnectionException
import com.moamap.app.feature.collection.domain.model.ImportedPlace
import com.moamap.app.feature.collection.domain.model.MapShareImport
import com.moamap.app.feature.collection.domain.model.MapType
import com.moamap.app.feature.collection.domain.model.MyMap
import com.moamap.app.feature.collection.domain.model.NewMap
import com.moamap.app.feature.collection.domain.model.PlaceExtractionException
import com.moamap.app.feature.collection.domain.model.PlaceImportSource
import com.moamap.app.feature.collection.domain.model.PlaceSaveResult
import com.moamap.app.feature.collection.domain.repository.MapRepository
import com.moamap.app.feature.collection.domain.model.EditedPlace
import com.moamap.app.feature.collection.domain.repository.PlaceImportRepository
import com.moamap.app.feature.collection.presentation.MyMapsState
import com.moamap.app.feature.mapdetail.domain.model.MapRole
import com.moamap.app.feature.mapdetail.domain.model.PlaceCandidate
import com.moamap.app.feature.mapdetail.domain.repository.PlaceSearchRepository
import com.moamap.app.feature.mapdetail.presentation.addplace.PlaceSearchState
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

private class FakePlaceSearchRepository : PlaceSearchRepository {

    var results: List<PlaceCandidate> = emptyList()
    val queries = mutableListOf<String>()

    override suspend fun search(query: String): List<PlaceCandidate> {
        queries += query
        return results
    }
}

private class FakeMapRepository : MapRepository {

    var failure: Throwable? = null
    var maps: List<MyMap> = MyMaps

    /** 커뮤니티 탭 목록. 비워 두면 [maps] 를 그대로 준다. */
    var communityMaps: List<MyMap>? = null

    override suspend fun getMyMaps(type: MapType): List<MyMap> {
        failure?.let { throw it }
        return if (type == MapType.Community) communityMaps ?: maps else maps
    }

    override suspend fun updateMyMapOrder(type: MapType, mapIds: List<Long>) = TODO("사용하지 않음")

    override suspend fun uploadCoverImage(imageUri: String) = TODO("사용하지 않음")

    override suspend fun createMap(newMap: NewMap) = TODO("사용하지 않음")

    override suspend fun updateMap(
        mapId: Long,
        name: String,
        description: String?,
        imageUrl: String?,
        tags: List<String>,
    ) = TODO("사용하지 않음")
    override suspend fun joinByInviteCode(inviteCode: String) = TODO("사용하지 않음")

    override suspend fun getLeaveOutcome(mapId: Long) = TODO("사용하지 않음")

    override suspend fun leaveMap(mapId: Long) = TODO("사용하지 않음")
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

    var saveFailure: Throwable? = null
    var saveResult = PlaceSaveResult(created = 2, duplicate = 0, failed = 0)

    /** 값을 넣으면 완료될 때까지 저장이 매달린다. */
    var savePending: CompletableDeferred<Unit>? = null

    var savedMapIds: Set<Long>? = null
        private set

    var savedPlaces: List<EditedPlace>? = null
        private set

    var savedPhotoUrls: Map<String, List<String>>? = null
        private set

    /** 발급을 몇 번 불렀는지. 재시도 때 또 올리지 않는지 보는 데 쓴다. */
    var uploadCallCount: Int = 0
        private set

    var uploadResult: Map<String, List<String>> = emptyMap()

    /** 발급 전 검증에 걸린 경우를 흉내 낸다. */
    var uploadFailure: Throwable? = null

    override suspend fun uploadPhotos(
        mapId: Long,
        places: List<EditedPlace>,
    ): Map<String, List<String>> {
        uploadCallCount++
        uploadFailure?.let { throw it }
        return uploadResult
    }

    override suspend fun savePlaces(
        mapIds: Set<Long>,
        places: List<EditedPlace>,
        photoUrls: Map<String, List<String>>,
    ): PlaceSaveResult {
        savedMapIds = mapIds
        savedPlaces = places
        savedPhotoUrls = photoUrls
        savePending?.await()
        saveFailure?.let { throw it }
        return saveResult
    }

    override suspend fun extractPlaces(url: String): List<ImportedPlace> {
        callCount++
        return extract()
    }

    /** 외부 지도에서 위치를 못 찾은 장소 이름. */
    var unmatchedNames: List<String> = emptyList()

    override suspend fun extractMapSharePlaces(url: String): MapShareImport {
        mapShareCallCount++
        return MapShareImport(extract(), unmatchedNames)
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
    private val searchRepository = FakePlaceSearchRepository()

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
        searchRepository,
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
        assertFalse(viewModel.uiState.value.placesFromSearch)
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

    /** 시안 「양식 오류」(이슈 #380). 버튼 없이 키보드 엔터로 시작한다. */
    @Test
    fun `엔터를 누르면 형식이 맞는 링크만 불러오고 링크만 남긴다`() = runTest(dispatcher) {
        viewModel.updateUrl("이 릴스 봐 https://www.instagram.com/reel/ABC123/")

        assertTrue(viewModel.submitUrl())
        advanceUntilIdle()

        assertEquals("https://www.instagram.com/reel/ABC123/", viewModel.uiState.value.url)
        assertEquals(1, repository.callCount)
        assertFalse(viewModel.uiState.value.linkInvalid)
    }

    @Test
    fun `형식이 틀린 링크는 불러오지 않고 입력칸 오류를 띄운다`() = runTest(dispatcher) {
        viewModel.updateUrl("link")

        assertFalse(viewModel.submitUrl())
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.linkInvalid)
        assertEquals(0, repository.callCount)
        assertEquals(ExtractionState.Idle, viewModel.uiState.value.extraction)
    }

    @Test
    fun `다른 흐름의 링크도 형식 오류로 본다`() = runTest(dispatcher) {
        // 인스타그램 흐름에 지도 링크를 넣었다.
        viewModel.updateUrl("https://naver.me/xAbCdEf")

        assertFalse(viewModel.submitUrl())

        assertTrue(viewModel.uiState.value.linkInvalid)
    }

    @Test
    fun `다시 입력하면 입력칸 오류가 사라진다`() = runTest(dispatcher) {
        viewModel.updateUrl("link")
        viewModel.submitUrl()

        viewModel.updateUrl("link2")

        assertFalse(viewModel.uiState.value.linkInvalid)
    }

    @Test
    fun `빈 칸에서 엔터는 아무것도 하지 않는다`() = runTest(dispatcher) {
        assertFalse(viewModel.submitUrl())

        assertFalse(viewModel.uiState.value.linkInvalid)
        assertEquals(0, repository.callCount)
    }

    @Test
    fun `게시물을 읽지 못하면 아래 안내 대신 입력칸 오류를 띄운다`() = runTest(dispatcher) {
        repository.failure = PlaceExtractionException.CaptionUnavailable()

        startExtraction()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.linkInvalid)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(ExtractionState.Idle, viewModel.uiState.value.extraction)
    }

    @Test
    fun `서버가 지원하지 않는 공유 링크라고 하면 입력칸 오류를 띄운다`() = runTest(dispatcher) {
        val mapShare = viewModel(PlaceImportSource.MapShare)
        repository.failure = ApiException(status = 400, code = "PLACE_015", serverMessage = "지원하지 않는 공유 링크입니다.")
        mapShare.updateUrl("https://naver.me/xAbCdEf")

        assertTrue(mapShare.submitUrl())
        advanceUntilIdle()

        assertTrue(mapShare.uiState.value.linkInvalid)
        assertNull(mapShare.uiState.value.errorMessage)
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
        repository.failure = PlaceExtractionException.CaptionBlocked()
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
        assertEquals(listOf(Places[1]), repository.savedPlaces?.map { it.place })
        // 프라이빗 지도라 바로 들어갔다.
        assertEquals(PLACE_ADDED_TOAST, viewModel.uiState.value.doneToast)
    }

    @Test
    fun `편집한 태그와 메모를 저장 요청에 실어 보낸다`() = runTest(dispatcher) {
        startExtraction()
        advanceUntilIdle()
        viewModel.togglePlace(Places[0].id)
        viewModel.toggleMap(MyMaps[0].id)
        viewModel.updateEditTags(Places[0].id, listOf("성수", "카페"))
        viewModel.updateEditMemo(Places[0].id, "창가 자리")

        viewModel.savePlaces()
        advanceUntilIdle()

        val entry = repository.savedPlaces?.single()
        assertEquals(listOf("성수", "카페"), entry?.edit?.tags)
        assertEquals("창가 자리", entry?.edit?.memo)
    }

    @Test
    fun `편집하지 않은 장소는 가져온 메모를 그대로 넘긴다`() = runTest(dispatcher) {
        // 외부 지도로 가져온 장소는 공유 리스트에 적힌 메모가 이미 들어 있다.
        repository.places = listOf(Places[0].copy(description = "리스트 메모"))
        startExtraction()
        advanceUntilIdle()
        viewModel.togglePlace(Places[0].id)
        viewModel.toggleMap(MyMaps[0].id)

        viewModel.savePlaces()
        advanceUntilIdle()

        assertEquals("리스트 메모", repository.savedPlaces?.single()?.edit?.memo)
    }

    @Test
    fun `올린 사진 주소를 저장 요청에 함께 넘긴다`() = runTest(dispatcher) {
        repository.uploadResult = mapOf(Places[0].id to listOf("https://cdn/1.jpg"))
        startExtraction()
        advanceUntilIdle()
        viewModel.togglePlace(Places[0].id)
        viewModel.toggleMap(MyMaps[0].id)

        viewModel.savePlaces()
        advanceUntilIdle()

        assertEquals(
            mapOf(Places[0].id to listOf("https://cdn/1.jpg")),
            repository.savedPhotoUrls,
        )
    }

    @Test
    fun `저장에 실패해 다시 시도해도 사진을 또 올리지 않는다`() = runTest(dispatcher) {
        // 올린 사진을 지울 API 가 없어, 시도할 때마다 올리면 고아 파일이 그만큼 쌓인다.
        repository.uploadResult = mapOf(Places[0].id to listOf("https://cdn/1.jpg"))
        repository.saveFailure = ConnectionException(RuntimeException("timeout"))
        startExtraction()
        advanceUntilIdle()
        viewModel.togglePlace(Places[0].id)
        viewModel.toggleMap(MyMaps[0].id)

        viewModel.savePlaces()
        advanceUntilIdle()
        viewModel.consumeError()
        repository.saveFailure = null
        viewModel.savePlaces()
        advanceUntilIdle()

        assertEquals(1, repository.uploadCallCount)
        assertEquals(
            mapOf(Places[0].id to listOf("https://cdn/1.jpg")),
            repository.savedPhotoUrls,
        )
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

    /** "저장하지 못했어요" 로 뭉개면 사진을 바꿔야 한다는 것을 알 수 없다. */
    @Test
    fun `사진이 한도를 넘으면 그 이유를 그대로 알린다`() = runTest(dispatcher) {
        repository.uploadFailure = ImageUploadException.TooLarge(MAX_PLACE_PHOTO_FILE_SIZE)
        startExtraction()
        advanceUntilIdle()
        viewModel.togglePlace(Places[0].id)
        viewModel.toggleMap(MyMaps[0].id)

        viewModel.savePlaces()
        advanceUntilIdle()

        assertEquals("사진 크기는 5MB 이하여야 해요", viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.saveResult)
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

    /** 시안 「장소 가져오기 - 지도 선택」(이슈 #386·#390). 커뮤니티 지도는 방장만 넣는다. 두 흐름이 같다. */
    @Test
    fun `나만의 지도를 맨 위에 두고 내가 방장인 커뮤니티 지도를 덧붙인다`() = runTest(dispatcher) {
        val personal = MyMap(1L, "나만의 지도", null, memberCount = 1, placeCount = 0, official = false, personal = true)
        val owned = MyMap(21L, "내가 만든", null, 3, 0, official = false, personal = false, role = MapRole.Owner)
        val joined = MyMap(22L, "참여한", null, 3, 0, official = false, personal = false, role = MapRole.Member)
        val official = MyMap(23L, "공식", null, 9, 0, official = true, personal = false, role = MapRole.Owner)
        mapRepository.maps = MyMaps + personal
        mapRepository.communityMaps = listOf(joined, owned, official)

        for (source in PlaceImportSource.entries) {
            viewModel = viewModel(source)
            advanceUntilIdle()

            val maps = (viewModel.uiState.value.targetMaps as MyMapsState.Success).maps
            assertEquals(source.name, listOf(1L, 11L, 12L, 21L), maps.map { it.id })
        }
    }

    @Test
    fun `외부 지도에서 위치를 못 찾은 장소 이름을 남긴다`() = runTest(dispatcher) {
        repository.unmatchedNames = listOf("나고미칸", "이름 없는 카페")
        viewModel = viewModel(PlaceImportSource.MapShare)

        viewModel.updateUrl("https://naver.me/xAbCdEf")
        viewModel.startExtraction()
        advanceUntilIdle()

        assertEquals(Places, viewModel.uiState.value.places)
        assertEquals(listOf("나고미칸", "이름 없는 카페"), viewModel.uiState.value.unmatchedPlaceNames)
    }

    @Test
    fun `직접 검색에서 고른 장소는 고른 장소 하나로 이어진다`() = runTest(dispatcher) {
        val candidate = PlaceCandidate(
            kakaoPlaceId = "777",
            name = "커피나무",
            address = "서울 동작구 상도동 1",
            roadAddress = "서울 동작구 상도로 369",
            latitude = 37.49,
            longitude = 126.95,
            category = "음식점 > 카페",
            placeUrl = "http://place.map.kakao.com/777",
        )
        searchRepository.results = listOf(candidate)

        viewModel.updateSearchQuery("커피나무")
        advanceUntilIdle()
        assertEquals(listOf("커피나무"), searchRepository.queries)
        assertEquals(PlaceSearchState.Success(listOf(candidate)), viewModel.uiState.value.search)

        viewModel.selectSearchedPlace(candidate)

        val place = viewModel.uiState.value.selectedPlaces.single()
        assertEquals("777", place.kakaoPlaceId)
        assertEquals("KAKAO_SEARCH", place.sourceType)
        assertEquals(37.49, place.lat, 0.0)
        assertTrue(viewModel.uiState.value.placesFromSearch)
    }

    @Test
    fun `불러오기 오류는 다시 입력할 때까지 남는다`() = runTest(dispatcher) {
        repository.failure = ConnectionException(IOException("boom"))
        startExtraction()
        advanceUntilIdle()
        assertEquals("네트워크에 연결할 수 없어요", viewModel.uiState.value.errorMessage)

        viewModel.updateUrl("https://www.instagram.com/reel/XYZ/")

        assertNull(viewModel.uiState.value.errorMessage)
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
