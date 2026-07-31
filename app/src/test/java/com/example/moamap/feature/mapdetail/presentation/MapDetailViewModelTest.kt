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
    fun `열면 지도와 장소를 한 번씩 읽는다`() = runTest {
        val repository = FakeMapDetailRepository(
            map = { testMap(joined = true, role = MapRole.Member, placeCount = 32) },
            allPlaces = { listOf(testPlace(1L), testPlace(2L)) },
        )

        val viewModel = viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("getMapDetail", "getPlaces"), repository.calls)
        assertEquals(listOf(1L, 2L), viewModel.uiState.value.places.map { place -> place.id })
        assertEquals("지도1", viewModel.uiState.value.title)
        assertEquals("멤버", viewModel.uiState.value.roleBadge)
        assertEquals(MapDetailAction.Leave, viewModel.uiState.value.action)
        assertEquals(32, viewModel.uiState.value.placeCount)
        assertTrue(viewModel.uiState.value.canAddPlace)
    }

    @Test
    fun `공식지도는 참여 중이어도 장소를 더할 수 없다`() = runTest {
        // 공공데이터를 옮겨 온 지도라 사용자가 넣은 장소가 섞이면 출처를 가릴 수 없다.
        val repository = FakeMapDetailRepository(
            map = { testMap(type = MapType.Official, role = MapRole.Member, joined = true) },
        )
        val viewModel = viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.canAddPlace)
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
    fun `refresh 는 화면을 채워 둔 채로 다시 읽는다`() = runTest {
        // 후기를 남긴 뒤의 갱신이다. 잠깐이라도 Loading 이 되면 상단바 제목과 후기 입력창이 깜빡인다.
        val repository = FakeMapDetailRepository(
            responseDelayMillis = 100L,
            map = { testMap(joined = true, placeCount = 3) },
        )
        val viewModel = viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.refresh()
        dispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value.map is MapLoadState.Success)
        assertTrue(viewModel.uiState.value.canAddPlace)
        assertEquals("지도1", viewModel.uiState.value.title)

        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.map is MapLoadState.Success)
    }

    @Test
    fun `retry 는 다시 읽는 동안 Loading 으로 되돌린다`() = runTest {
        val repository = FakeMapDetailRepository(responseDelayMillis = 100L)
        val viewModel = viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.retry()
        dispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value.map is MapLoadState.Loading)
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
        assertEquals(
            listOf("getMapDetail", "getPlaces", "joinMap", "getMapDetail", "getPlaces"),
            repository.calls,
        )
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

        assertEquals(listOf("getMapDetail", "getPlaces", "leaveMap"), repository.calls)
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

        assertEquals(listOf("getMapDetail", "getPlaces", "deleteMap"), repository.calls)
        assertTrue(viewModel.uiState.value.left)
    }

    @Test
    fun `프라이빗 지도에 참여했으면 초대 코드가 상태에 실린다`() = runTest {
        val repository = FakeMapDetailRepository(
            map = {
                testMap(
                    type = MapType.Private,
                    role = MapRole.Member,
                    joined = true,
                    memberCount = 3,
                    inviteCode = "VH4YXZ",
                )
            },
        )
        val viewModel = viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        // 상세 응답에 실려 온 값을 그대로 쓴다. 코드를 따로 조회하지 않는다.
        assertEquals("VH4YXZ", viewModel.uiState.value.inviteCode)
        assertEquals(listOf("getMapDetail", "getPlaces"), repository.calls)
    }

    @Test
    fun `커뮤니티 지도에서는 초대 코드가 없다`() = runTest {
        val repository = FakeMapDetailRepository(
            map = { testMap(joined = true, role = MapRole.Member, inviteCode = "VH4YXZ") },
        )
        val viewModel = viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.inviteCode)
    }

    @Test
    fun `요청이 도는 동안 다시 눌러도 한 번만 참여한다`() = runTest {
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
    fun `요청이 도는 동안 다시 눌러도 한 번만 나간다`() = runTest {
        val repository = FakeMapDetailRepository(
            responseDelayMillis = 100L,
            map = { testMap(joined = true, role = MapRole.Member) },
        )
        val viewModel = viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.leave()
        dispatcher.scheduler.runCurrent()
        viewModel.leave()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.calls.count { call -> call == "leaveMap" })
    }

    @Test
    fun `나가기가 막힌 지도에서는 요청을 보내지 않는다`() = runTest {
        // 커뮤니티 방장은 서버가 탈퇴를 거절한다. 화면도 비활성이지만 여기서 한 번 더 막는다.
        val repository = FakeMapDetailRepository(
            map = { testMap(joined = true, role = MapRole.Owner) },
        )
        val viewModel = viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(MapDetailAction.LeaveDisabled, viewModel.uiState.value.action)

        viewModel.leave()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("getMapDetail", "getPlaces"), repository.calls)
        assertFalse(viewModel.uiState.value.left)
    }

    @Test
    fun `조회 중에 지워진 안내는 응답이 와도 되살아나지 않는다`() = runTest {
        val repository = object : FakeMapDetailRepository(responseDelayMillis = 100L) {
            override suspend fun joinMap(mapId: Long) = throw RuntimeException("boom")
        }
        val viewModel = viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.join()
        dispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)

        // 재조회가 도는 동안 스낵바가 떠서 안내를 소비한다.
        viewModel.retry()
        dispatcher.scheduler.runCurrent()
        viewModel.consumeErrorMessage()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
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

    @Test
    fun `장소 조회가 실패해도 지도는 뜬다`() = runTest {
        val repository = FakeMapDetailRepository(
            map = { testMap(joined = true, role = MapRole.Member) },
            allPlaces = { error("장소 조회 실패") },
        )

        val viewModel = viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        // 마커 한 겹 때문에 화면 전체를 못 여는 게 더 나쁘다.
        assertTrue(viewModel.uiState.value.map is MapLoadState.Success)
        assertNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.places.isEmpty())
    }

    @Test
    fun `지도 조회가 실패해도 받아 둔 장소는 남는다`() = runTest {
        var failMap = false
        val repository = FakeMapDetailRepository(
            map = { if (failMap) error("지도 조회 실패") else testMap(joined = true) },
            allPlaces = { listOf(testPlace(1L)) },
        )

        val viewModel = viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        failMap = true
        viewModel.retry()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.map is MapLoadState.Error)
        assertEquals(1, viewModel.uiState.value.places.size)
    }
}
