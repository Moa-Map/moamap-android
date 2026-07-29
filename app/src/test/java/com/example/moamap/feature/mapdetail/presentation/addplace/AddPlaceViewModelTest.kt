package com.example.moamap.feature.mapdetail.presentation.addplace

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.example.moamap.core.navigation.MoaMapRoute
import com.example.moamap.core.network.ApiException
import com.example.moamap.feature.collection.domain.model.MapType
import com.example.moamap.feature.mapdetail.domain.model.MapDetail
import com.example.moamap.feature.mapdetail.domain.model.MapRole
import com.example.moamap.feature.mapdetail.domain.model.NewPlace
import com.example.moamap.feature.mapdetail.domain.model.PlaceCandidate
import com.example.moamap.feature.mapdetail.domain.repository.PlaceAddRepository
import com.example.moamap.feature.mapdetail.domain.repository.PlaceSearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private fun candidate(id: String) = PlaceCandidate(
    kakaoPlaceId = id,
    name = "장소$id",
    address = "지번 $id",
    roadAddress = "도로명 $id",
    latitude = 37.5,
    longitude = 127.0,
    category = "음식점 > 카페",
    placeUrl = null,
)

private class FakeSearchRepository(
    private val responseDelayMillis: Long = 0L,
    var result: () -> List<PlaceCandidate> = { listOf(candidate("1")) },
) : PlaceSearchRepository {
    val queries = mutableListOf<String>()

    override suspend fun search(query: String): List<PlaceCandidate> {
        queries += query
        delay(responseDelayMillis)
        return result()
    }
}

private open class FakeAddRepository(
    private val responseDelayMillis: Long = 0L,
    /** 올린 사진 주소. 재업로드 방지를 검증하려면 비어 있지 않아야 한다. */
    private val uploadResult: List<String> = emptyList(),
) : PlaceAddRepository {
    val calls = mutableListOf<String>()
    var lastNewPlace: NewPlace? = null
        private set

    override suspend fun uploadPhotos(mapId: Long, photos: List<Uri>): List<String> {
        calls += "uploadPhotos(${photos.size})"
        delay(responseDelayMillis)
        return uploadResult
    }

    override suspend fun addPlace(mapId: Long, newPlace: NewPlace) {
        calls += "addPlace"
        lastNewPlace = newPlace
        delay(responseDelayMillis)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AddPlaceViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun communityMap(role: MapRole = MapRole.Owner) = MapDetail(
        id = 7L,
        title = "지도",
        description = null,
        imageUrl = null,
        ownerName = null,
        type = MapType.Community,
        role = role,
        tags = emptyList(),
        memberCount = 1,
        placeCount = 0,
        joined = true,
    )

    private fun viewModel(
        search: PlaceSearchRepository = FakeSearchRepository(),
        add: PlaceAddRepository = FakeAddRepository(),
    ) = AddPlaceViewModel(
        savedStateHandle = SavedStateHandle(mapOf(MoaMapRoute.MapDetail.ARG_MAP_ID to 7L)),
        searchRepository = search,
        addRepository = add,
    )

    // ---------- 검색 ----------

    @Test
    fun `열면 검색 단계이고 아직 부르지 않는다`() = runTest {
        val search = FakeSearchRepository()

        val viewModel = viewModel(search = search)
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isFormStep)
        assertEquals(PlaceSearchState.Idle, viewModel.uiState.value.search)
        assertTrue(search.queries.isEmpty())
    }

    @Test
    fun `연달아 입력하면 마지막 것만 검색한다`() = runTest {
        val search = FakeSearchRepository()
        val viewModel = viewModel(search = search)

        viewModel.updateQuery("스")
        viewModel.updateQuery("스타")
        viewModel.updateQuery("스타벅스")
        dispatcher.scheduler.advanceUntilIdle()

        // 글자마다 부르면 카카오 쿼터를 태운다.
        assertEquals(listOf("스타벅스"), search.queries)
    }

    @Test
    fun `검색어를 지우면 처음 상태로 돌아간다`() = runTest {
        val search = FakeSearchRepository()
        val viewModel = viewModel(search = search)

        viewModel.updateQuery("스타벅스")
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.updateQuery("")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(PlaceSearchState.Idle, viewModel.uiState.value.search)
        assertEquals(listOf("스타벅스"), search.queries)
    }

    @Test
    fun `검색에 실패하면 안내가 뜨고 다시 시도할 수 있다`() = runTest {
        var fail = true
        val search = FakeSearchRepository(
            result = { if (fail) throw RuntimeException("boom") else listOf(candidate("1")) },
        )
        val viewModel = viewModel(search = search)

        viewModel.updateQuery("카페")
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.search is PlaceSearchState.Error)

        fail = false
        viewModel.retrySearch()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.search is PlaceSearchState.Success)
    }

    // ---------- 단계 이동 ----------

    @Test
    fun `후보를 고르면 등록 폼으로 넘어간다`() = runTest {
        val viewModel = viewModel()

        viewModel.selectCandidate(candidate("1"))

        assertTrue(viewModel.uiState.value.isFormStep)
        assertEquals("1", viewModel.uiState.value.selected?.kakaoPlaceId)
    }

    @Test
    fun `검색으로 돌아가면 검색 결과는 남고 폼만 비워진다`() = runTest {
        val viewModel = viewModel()
        viewModel.updateQuery("카페")
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.selectCandidate(candidate("1"))
        viewModel.updateTagInput("성수 ")
        viewModel.updateMemo("메모")
        viewModel.backToSearch()

        assertFalse(viewModel.uiState.value.isFormStep)
        assertTrue(viewModel.uiState.value.search is PlaceSearchState.Success)
        assertEquals("카페", viewModel.uiState.value.query)
        assertEquals(emptyList<String>(), viewModel.uiState.value.tags)
        assertEquals("", viewModel.uiState.value.memo)
    }

    // ---------- 태그 ----------

    @Test
    fun `태그를 넣고 지운다`() = runTest {
        val viewModel = viewModel()
        viewModel.selectCandidate(candidate("1"))

        viewModel.updateTagInput("성수 ")
        viewModel.updateTagInput("카페 ")
        assertEquals(listOf("성수", "카페"), viewModel.uiState.value.tags)

        viewModel.removeTag("성수")
        assertEquals(listOf("카페"), viewModel.uiState.value.tags)
    }

    @Test
    fun `입력창이 비었을 때만 백스페이스가 마지막 태그를 지운다`() = runTest {
        val viewModel = viewModel()
        viewModel.selectCandidate(candidate("1"))
        viewModel.updateTagInput("성수 ")
        viewModel.updateTagInput("카")

        viewModel.removeLastTagIfInputEmpty()
        assertEquals(listOf("성수"), viewModel.uiState.value.tags)

        viewModel.updateTagInput("")
        viewModel.removeLastTagIfInputEmpty()
        assertEquals(emptyList<String>(), viewModel.uiState.value.tags)
    }

    // ---------- 등록 ----------

    @Test
    fun `등록하면 폼에 적은 값이 그대로 넘어간다`() = runTest {
        val add = FakeAddRepository()
        val viewModel = viewModel(add = add)
        viewModel.selectCandidate(candidate("1"))
        viewModel.updateTagInput("성수 ")
        viewModel.updateMemo("주말에 가기 좋아요")

        viewModel.submit(communityMap())
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("uploadPhotos(0)", "addPlace"), add.calls)
        assertEquals(listOf("성수"), add.lastNewPlace?.tags)
        assertEquals("주말에 가기 좋아요", add.lastNewPlace?.memo)
        assertEquals("1", add.lastNewPlace?.candidate?.kakaoPlaceId)
    }

    @Test
    fun `바로 등록되는 지도와 승인 대기 지도의 안내가 다르다`() = runTest {
        val owner = viewModel()
        owner.selectCandidate(candidate("1"))
        owner.submit(communityMap(MapRole.Owner))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("장소를 추가했어요", owner.uiState.value.addedMessage)

        val member = viewModel()
        member.selectCandidate(candidate("1"))
        member.submit(communityMap(MapRole.Member))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("추가 요청을 보냈어요", member.uiState.value.addedMessage)
    }

    @Test
    fun `요청이 도는 동안 다시 눌러도 한 번만 등록한다`() = runTest {
        val add = FakeAddRepository(responseDelayMillis = 100L)
        val viewModel = viewModel(add = add)
        viewModel.selectCandidate(candidate("1"))

        viewModel.submit(communityMap())
        dispatcher.scheduler.runCurrent()
        viewModel.submit(communityMap())
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, add.calls.count { call -> call == "addPlace" })
    }

    @Test
    fun `장소를 고르지 않으면 등록하지 않는다`() = runTest {
        val add = FakeAddRepository()
        val viewModel = viewModel(add = add)

        viewModel.submit(communityMap())
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(add.calls.isEmpty())
    }

    @Test
    fun `사진 업로드가 실패하면 등록하지 않는다`() = runTest {
        val add = object : FakeAddRepository() {
            override suspend fun uploadPhotos(mapId: Long, photos: List<Uri>): List<String> =
                throw RuntimeException("boom")
        }
        val viewModel = viewModel(add = add)
        viewModel.selectCandidate(candidate("1"))

        viewModel.submit(communityMap())
        dispatcher.scheduler.advanceUntilIdle()

        // 사진이 빠진 채로 등록되면 사용자가 알 수 없다.
        assertTrue(add.calls.none { call -> call == "addPlace" })
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.addedMessage)
    }

    @Test
    fun `시트를 다시 열면 처음 상태로 돌아간다`() = runTest {
        // ViewModel 이 지도 상세에 매여 살아남아, 지우지 않으면 직전 폼이 그대로 보인다.
        val viewModel = viewModel()
        viewModel.updateQuery("카페")
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.selectCandidate(candidate("1"))
        viewModel.updateTagInput("성수 ")
        viewModel.updateMemo("메모")

        viewModel.reset()

        val state = viewModel.uiState.value
        assertFalse(state.isFormStep)
        assertEquals("", state.query)
        assertEquals(PlaceSearchState.Idle, state.search)
        assertEquals(emptyList<String>(), state.tags)
        assertEquals("", state.memo)
        assertNull(state.addedMessage)
    }

    @Test
    fun `등록만 실패했으면 다시 눌러도 사진을 또 올리지 않는다`() = runTest {
        // 올린 사진은 지울 방법이 없어, 재시도마다 올리면 고아 파일이 쌓인다.
        var failAdd = true
        val add = object : FakeAddRepository(uploadResult = listOf("https://img/1.jpg")) {
            override suspend fun addPlace(mapId: Long, newPlace: NewPlace) {
                if (failAdd) throw RuntimeException("boom")
                super.addPlace(mapId, newPlace)
            }
        }
        val viewModel = viewModel(add = add)
        viewModel.selectCandidate(candidate("1"))

        viewModel.submit(communityMap())
        dispatcher.scheduler.advanceUntilIdle()

        failAdd = false
        viewModel.submit(communityMap())
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, add.calls.count { call -> call.startsWith("uploadPhotos") })
        assertNotNull(viewModel.uiState.value.addedMessage)
    }

    @Test
    fun `이미 있는 장소면 그렇다고 알려준다`() = runTest {
        // 실기기에서 확인한 응답이다. "추가하지 못했어요" 로 묶으면 왜 안 되는지 알 수 없어
        // 같은 장소를 계속 다시 시도하게 된다.
        val add = object : FakeAddRepository() {
            override suspend fun addPlace(mapId: Long, newPlace: NewPlace): Unit =
                throw ApiException(
                    code = "PLACE_010",
                    status = 409,
                    serverMessage = "해당 지도에 이미 등록된 장소입니다.",
                )
        }
        val viewModel = viewModel(add = add)
        viewModel.selectCandidate(candidate("1"))

        viewModel.submit(communityMap())
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("이미 이 지도에 있는 장소예요", viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.addedMessage)
    }

    @Test
    fun `모르는 실패는 원인을 단정하지 않는다`() = runTest {
        val add = object : FakeAddRepository() {
            override suspend fun addPlace(mapId: Long, newPlace: NewPlace): Unit =
                throw ApiException(code = "COMMON_005", status = 500, serverMessage = "서버 오류")
        }
        val viewModel = viewModel(add = add)
        viewModel.selectCandidate(candidate("1"))

        viewModel.submit(communityMap())
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("장소를 추가하지 못했어요", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `등록에 실패하면 시트를 닫지 않고 입력이 남는다`() = runTest {
        val add = object : FakeAddRepository() {
            override suspend fun addPlace(mapId: Long, newPlace: NewPlace) =
                throw RuntimeException("boom")
        }
        val viewModel = viewModel(add = add)
        viewModel.selectCandidate(candidate("1"))
        viewModel.updateTagInput("성수 ")

        viewModel.submit(communityMap())
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.addedMessage)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertEquals(listOf("성수"), viewModel.uiState.value.tags)
        assertFalse(viewModel.uiState.value.submitting)

        viewModel.consumeErrorMessage()
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
