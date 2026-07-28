package com.example.moamap.feature.collection.presentation

import com.example.moamap.feature.collection.domain.model.MapType
import com.example.moamap.feature.collection.domain.model.MyMap
import com.example.moamap.feature.collection.domain.model.NewMap
import com.example.moamap.feature.collection.domain.repository.MapRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var repository: FakeMapRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeMapRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = CollectionViewModel(repository)

    /** 화면이 처음 보이는 시점까지 진행시킨다. 첫 조회는 `refresh()` 가 겸한다. */
    private fun TestScope.startedViewModel(): CollectionViewModel {
        val viewModel = createViewModel()
        viewModel.refresh()
        advanceUntilIdle()
        return viewModel
    }

    private fun myMap(id: Long) = MyMap(
        id = id,
        title = "지도$id",
        imageUrl = null,
        memberCount = 1,
        official = false,
    )

    /**
     * 호출될 때마다 어떤 탭을 물었는지 기록한다.
     *
     * [delayMillis] 를 두면 요청이 진행 중인 상태를 만들 수 있다. 취소나 "요청 중에도 기존
     * 목록 유지" 를 검증하려면 요청이 실제로 매달려 있어야 한다.
     */
    private class FakeMapRepository(
        var delayMillis: Long = 0L,
        var result: (MapType) -> List<MyMap> = { emptyList() },
    ) : MapRepository {

        val calls = mutableListOf<MapType>()

        override suspend fun getMyMaps(type: MapType): List<MyMap> {
            calls += type
            delay(delayMillis)
            return result(type)
        }

        override suspend fun createMap(newMap: NewMap): Long = TODO("사용하지 않음")
    }

    @Test
    fun `화면이 보이기 전에는 읽지 않는다`() = runTest(dispatcher) {
        createViewModel()
        advanceUntilIdle()

        // init 에서도 읽으면 화면이 뜰 때 같은 요청이 두 번 나간다.
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun `화면이 보이면 선택된 탭만 읽는다`() = runTest(dispatcher) {
        val viewModel = startedViewModel()

        assertEquals(listOf(MapType.Community), repository.calls)
        assertEquals(MapType.Community, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun `받은 목록이 상태에 담긴다`() = runTest(dispatcher) {
        repository.result = { listOf(myMap(1), myMap(2)) }

        val viewModel = startedViewModel()

        val state = viewModel.uiState.value.community
        assertTrue(state is MyMapsState.Success)
        assertEquals(listOf(1L, 2L), (state as MyMapsState.Success).maps.map { it.id })
    }

    @Test
    fun `탭을 바꾸면 그 탭을 읽는다`() = runTest(dispatcher) {
        val viewModel = startedViewModel()

        viewModel.selectTab(MapType.Private)
        advanceUntilIdle()

        assertEquals(listOf(MapType.Community, MapType.Private), repository.calls)
        assertEquals(MapType.Private, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun `이미 읽은 탭으로 돌아가면 다시 읽지 않는다`() = runTest(dispatcher) {
        val viewModel = startedViewModel()
        viewModel.selectTab(MapType.Private)
        advanceUntilIdle()

        viewModel.selectTab(MapType.Community)
        advanceUntilIdle()

        // 커뮤니티 1회 + 프라이빗 1회. 돌아왔다고 다시 부르지 않는다.
        assertEquals(listOf(MapType.Community, MapType.Private), repository.calls)
    }

    @Test
    fun `같은 탭을 다시 고르면 아무 일도 하지 않는다`() = runTest(dispatcher) {
        val viewModel = startedViewModel()

        viewModel.selectTab(MapType.Community)
        advanceUntilIdle()

        assertEquals(listOf(MapType.Community), repository.calls)
    }

    @Test
    fun `실패하면 안내를 보여주고 다시 시도할 수 있다`() = runTest(dispatcher) {
        repository.result = { throw IOException("서버 오류") }

        val viewModel = startedViewModel()

        val failed = viewModel.uiState.value.community
        assertTrue(failed is MyMapsState.Error)
        assertEquals("지도 목록을 불러오지 못했어요", (failed as MyMapsState.Error).message)

        repository.result = { listOf(myMap(1)) }
        viewModel.retry()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.community is MyMapsState.Success)
    }

    @Test
    fun `새로고침은 현재 탭만 다시 읽는다`() = runTest(dispatcher) {
        val viewModel = startedViewModel()
        viewModel.selectTab(MapType.Private)
        advanceUntilIdle()
        repository.calls.clear()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(listOf(MapType.Private), repository.calls)
    }

    @Test
    fun `새로고침하면 보고 있지 않던 탭도 다음에 고를 때 새로 읽는다`() = runTest(dispatcher) {
        val viewModel = startedViewModel()
        viewModel.selectTab(MapType.Private)
        advanceUntilIdle()
        viewModel.selectTab(MapType.Community)
        advanceUntilIdle()
        repository.calls.clear()

        // 지도를 만들고 커뮤니티 탭으로 돌아온 상황. 프라이빗 목록도 낡았다.
        viewModel.refresh()
        advanceUntilIdle()
        viewModel.selectTab(MapType.Private)
        advanceUntilIdle()

        assertEquals(listOf(MapType.Community, MapType.Private), repository.calls)
    }

    @Test
    fun `새로고침 중에도 보고 있던 목록이 사라지지 않는다`() = runTest(dispatcher) {
        repository.result = { listOf(myMap(1)) }
        val viewModel = startedViewModel()

        repository.delayMillis = 1_000
        viewModel.refresh()

        // 응답이 오기 전. Loading 으로 되돌리면 목록이 깜빡인다.
        val during = viewModel.uiState.value.community
        assertTrue(during is MyMapsState.Success)
        assertEquals(listOf(1L), (during as MyMapsState.Success).maps.map { it.id })

        advanceUntilIdle()
    }

    @Test
    fun `새로고침이 실패해도 이미 받은 목록은 남긴다`() = runTest(dispatcher) {
        repository.result = { listOf(myMap(1)) }
        val viewModel = startedViewModel()

        repository.result = { throw IOException("서버 오류") }
        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value.community
        assertTrue(state is MyMapsState.Success)
        assertEquals(listOf(1L), (state as MyMapsState.Success).maps.map { it.id })
    }

    @Test
    fun `첫 조회가 실패한 뒤 새로고침하면 안내를 보여준다`() = runTest(dispatcher) {
        repository.result = { throw IOException("서버 오류") }
        val viewModel = startedViewModel()

        viewModel.refresh()
        advanceUntilIdle()

        // 보여줄 목록이 없으면 실패를 숨기지 않는다.
        assertTrue(viewModel.uiState.value.community is MyMapsState.Error)
    }

    @Test
    fun `같은 탭을 연달아 읽으면 앞선 요청은 버린다`() = runTest(dispatcher) {
        repository.delayMillis = 1_000
        repository.result = { listOf(myMap(1)) }

        val viewModel = createViewModel()
        viewModel.refresh()
        // 첫 요청이 시작돼 응답을 기다리는 중이다.
        advanceTimeBy(500)

        repository.result = { listOf(myMap(2)) }
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(listOf(MapType.Community, MapType.Community), repository.calls)
        // 늦게 도착한 앞선 요청이 최신 결과를 덮어쓰면 안 된다.
        val state = viewModel.uiState.value.community
        assertEquals(listOf(2L), (state as MyMapsState.Success).maps.map { it.id })
    }
}
