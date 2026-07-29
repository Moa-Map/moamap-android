package com.example.moamap.feature.mapdetail.presentation

import androidx.lifecycle.SavedStateHandle
import com.example.moamap.core.navigation.MoaMapRoute
import com.example.moamap.feature.collection.domain.model.MapType
import com.example.moamap.feature.mapdetail.domain.model.MapDetailAction
import com.example.moamap.feature.mapdetail.domain.model.MapRole
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: FakeMapDetailRepository) = MapDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf(MoaMapRoute.MapDetail.ARG_MAP_ID to 1L)),
        repository = repository,
    )

    @Test
    fun `열면 지도를 한 번 읽는다`() = runTest {
        val repository = FakeMapDetailRepository(
            map = { testMap(joined = true, role = MapRole.Member, placeCount = 32) },
        )

        val viewModel = viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("getMapDetail"), repository.calls)
        assertEquals("지도1", viewModel.uiState.value.title)
        assertEquals("멤버", viewModel.uiState.value.roleBadge)
        assertEquals(MapDetailAction.Leave, viewModel.uiState.value.action)
        assertEquals(32, viewModel.uiState.value.placeCount)
        assertTrue(viewModel.uiState.value.canAddPlace)
    }

    @Test
    fun `조회에 실패하면 Error 가 되고 retry 로 복구한다`() = runTest {
        var fail = true
        val repository = FakeMapDetailRepository(
            map = { if (fail) throw RuntimeException("boom") else testMap() },
        )
        val viewModel = viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.map is MapLoadState.Error)

        fail = false
        viewModel.retry()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.map is MapLoadState.Success)
    }

    @Test
    fun `참여하면 화면에 남고 나가기로 바뀐다`() = runTest {
        var joined = false
        val repository = FakeMapDetailRepository(
            map = { testMap(joined = joined, role = if (joined) MapRole.Member else MapRole.None) },
        )
        val viewModel = viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(MapDetailAction.Join, viewModel.uiState.value.action)
        assertFalse(viewModel.uiState.value.canAddPlace)

        joined = true
        viewModel.join()
        dispatcher.scheduler.advanceUntilIdle()

        // 참여 뒤에는 역할과 인원이 함께 바뀌므로 상세를 다시 읽는다.
        assertEquals(listOf("getMapDetail", "joinMap", "getMapDetail"), repository.calls)
        assertEquals(MapDetailAction.Leave, viewModel.uiState.value.action)
        assertTrue(viewModel.uiState.value.canAddPlace)
        assertFalse(viewModel.uiState.value.left)
    }

    @Test
    fun `나가면 이전 화면으로 돌아갈 신호를 낸다`() = runTest {
        val repository = FakeMapDetailRepository(
            map = { testMap(joined = true, role = MapRole.Member) },
        )
        val viewModel = viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.leave()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("getMapDetail", "leaveMap"), repository.calls)
        assertTrue(viewModel.uiState.value.left)
    }

    @Test
    fun `프라이빗 지도에 만든 사람 혼자면 나가기가 지도 삭제다`() = runTest {
        val repository = FakeMapDetailRepository(
            map = {
                testMap(
                    type = MapType.Private,
                    role = MapRole.Owner,
                    joined = true,
                    memberCount = 1,
                )
            },
        )
        val viewModel = viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(MapDetailAction.Leave, viewModel.uiState.value.action)

        viewModel.leave()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("getMapDetail", "deleteMap"), repository.calls)
        assertTrue(viewModel.uiState.value.left)
    }

    @Test
    fun `요청이 도는 동안 다시 눌러도 한 번만 나간다`() = runTest {
        val repository = FakeMapDetailRepository(responseDelayMillis = 100L)
        val viewModel = viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.join()
        dispatcher.scheduler.runCurrent()
        viewModel.join()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.calls.count { call -> call == "joinMap" })
    }

    @Test
    fun `참여에 실패하면 안내가 뜨고 화면을 떠나지 않는다`() = runTest {
        val repository = object : FakeMapDetailRepository() {
            override suspend fun joinMap(mapId: Long) = throw RuntimeException("boom")
        }
        val viewModel = viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.join()
        dispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.left)
        assertFalse(viewModel.uiState.value.actionInProgress)

        viewModel.consumeErrorMessage()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `나가기에 실패해도 화면을 떠나지 않는다`() = runTest {
        val repository = object : FakeMapDetailRepository(
            map = { testMap(joined = true, role = MapRole.Member) },
        ) {
            override suspend fun leaveMap(mapId: Long) = throw RuntimeException("boom")
        }
        val viewModel = viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.leave()
        dispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.left)
    }
}
