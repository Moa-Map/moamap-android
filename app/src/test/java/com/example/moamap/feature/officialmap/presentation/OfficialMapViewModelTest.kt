package com.example.moamap.feature.officialmap.presentation

import com.example.moamap.feature.officialmap.domain.model.OfficialMap
import com.example.moamap.feature.officialmap.domain.repository.OfficialMapRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
class OfficialMapViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sampleMap(id: Long) = OfficialMap(
        id = id,
        title = "지도$id",
        description = "설명$id",
        memberCount = 1,
        placeCount = 5416,
        joined = false,
    )

    /**
     * [responseDelayMillis] 를 두면 요청이 진행 중인 상태를 만들 수 있다. 재조회 중에도
     * 보던 목록이 남는지 보려면 앞선 요청이 실제로 매달려 있어야 한다.
     */
    private class FakeRepository(
        val responseDelayMillis: Long = 0L,
        var result: () -> List<OfficialMap> = { emptyList() },
    ) : OfficialMapRepository {
        var callCount = 0
        val joinedMapIds = mutableListOf<Long>()
        var joinDelayMillis = 0L
        var joinError: Exception? = null

        override suspend fun getOfficialMaps(): List<OfficialMap> {
            callCount++
            delay(responseDelayMillis)
            return result()
        }

        override suspend fun joinMap(mapId: Long) {
            delay(joinDelayMillis)
            joinError?.let { throw it }
            joinedMapIds += mapId
        }
    }

    @Test
    fun `만들어지기만 하면 조회하지 않는다`() = runTest {
        val repository = FakeRepository()

        val viewModel = OfficialMapViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        // 화면이 보일 때 refresh 가 첫 조회를 겸한다. init 에서도 읽으면 요청이 두 번 나간다.
        assertEquals(0, repository.callCount)
        assertEquals(OfficialMapsState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `로드에 성공하면 Success 상태가 된다`() = runTest {
        val repository = FakeRepository { listOf(sampleMap(6L)) }

        val viewModel = OfficialMapViewModel(repository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is OfficialMapsState.Success)
        assertEquals(1, (state as OfficialMapsState.Success).maps.size)
        assertEquals("지도6", state.maps[0].title)
    }

    @Test
    fun `빈 목록도 Success 로 둔다`() = runTest {
        val viewModel = OfficialMapViewModel(FakeRepository()).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is OfficialMapsState.Success)
        assertTrue((state as OfficialMapsState.Success).maps.isEmpty())
    }

    @Test
    fun `실패하면 Error 상태가 되고 retry로 복구한다`() = runTest {
        var fail = true
        val repository = FakeRepository {
            if (fail) throw RuntimeException("boom") else listOf(sampleMap(6L))
        }
        val viewModel = OfficialMapViewModel(repository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value is OfficialMapsState.Error)

        fail = false
        viewModel.retry()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is OfficialMapsState.Success)
    }

    @Test
    fun `오류 메시지로 예외 메시지를 노출하지 않는다`() = runTest {
        val repository = FakeRepository { throw RuntimeException("[500] COMMON_005: 내부 오류") }

        val viewModel = OfficialMapViewModel(repository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is OfficialMapsState.Error)
        assertEquals("공식지도를 불러오지 못했어요", (state as OfficialMapsState.Error).message)
    }

    @Test
    fun `돌아와서 다시 읽는 동안에는 보던 목록이 남는다`() = runTest {
        val repository = FakeRepository(responseDelayMillis = 100L) { listOf(sampleMap(6L)) }
        val viewModel = OfficialMapViewModel(repository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.refresh()
        dispatcher.scheduler.runCurrent()

        // Loading 으로 되돌리면 돌아올 때마다 목록이 사라졌다 나타난다.
        assertTrue(viewModel.uiState.value is OfficialMapsState.Success)
    }

    @Test
    fun `새로고침이 실패해도 보던 목록을 지우지 않는다`() = runTest {
        var fail = false
        val repository = FakeRepository {
            if (fail) throw RuntimeException("boom") else listOf(sampleMap(6L))
        }
        val viewModel = OfficialMapViewModel(repository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()

        fail = true
        viewModel.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is OfficialMapsState.Success)
    }

    @Test
    fun `진행 중인 요청이 취소돼도 오류로 새지 않는다`() = runTest {
        val repository = FakeRepository(responseDelayMillis = 100L) { listOf(sampleMap(6L)) }
        val viewModel = OfficialMapViewModel(repository).apply { refresh() }

        // 첫 요청을 매달아 둔 채로 재시도를 걸어 취소를 만든다.
        dispatcher.scheduler.advanceTimeBy(50L)
        viewModel.retry()
        dispatcher.scheduler.runCurrent()
        assertEquals(OfficialMapsState.Loading, viewModel.uiState.value)

        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is OfficialMapsState.Success)
        assertEquals(2, repository.callCount)
    }

    @Test
    fun `참여에 성공하면 목록을 다시 읽는다`() = runTest {
        val repository = FakeRepository { listOf(sampleMap(6L)) }
        val viewModel = OfficialMapViewModel(repository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, repository.callCount)

        viewModel.join(6L)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(6L), repository.joinedMapIds)
        // 참여로 멤버 수도 함께 늘어 목록이 낡는다.
        assertEquals(2, repository.callCount)
    }

    @Test
    fun `참여를 마치고 다시 읽는 동안에도 보던 목록이 남는다`() = runTest {
        val repository = FakeRepository(responseDelayMillis = 100L) { listOf(sampleMap(6L)) }
        val viewModel = OfficialMapViewModel(repository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.join(6L)
        dispatcher.scheduler.runCurrent()

        // 참여 뒤 재조회가 Loading 으로 되돌리면 카드가 통째로 깜빡인다.
        assertTrue(viewModel.uiState.value is OfficialMapsState.Success)
    }

    @Test
    fun `연달아 눌러도 참여 요청은 한 번만 나간다`() = runTest {
        val repository = FakeRepository { listOf(sampleMap(6L)) }
            .apply { joinDelayMillis = 100L }
        val viewModel = OfficialMapViewModel(repository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.join(6L)
        dispatcher.scheduler.runCurrent()
        viewModel.join(6L)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(6L), repository.joinedMapIds)
    }

    @Test
    fun `참여에 실패해도 보던 목록을 지우지 않는다`() = runTest {
        val repository = FakeRepository { listOf(sampleMap(6L)) }
            .apply { joinError = RuntimeException("boom") }
        val viewModel = OfficialMapViewModel(repository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.join(6L)
        dispatcher.scheduler.advanceUntilIdle()

        // 실패는 로그로만 남는다. 버튼이 "참여하기" 인 채로 남아 다시 누를 수 있다.
        val state = viewModel.uiState.value
        assertTrue(state is OfficialMapsState.Success)
        assertEquals(1, (state as OfficialMapsState.Success).maps.size)
    }
}
