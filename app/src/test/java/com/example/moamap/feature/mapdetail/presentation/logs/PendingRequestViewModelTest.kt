package com.example.moamap.feature.mapdetail.presentation.logs

import androidx.lifecycle.SavedStateHandle
import com.example.moamap.core.navigation.MoaMapRoute
import com.example.moamap.feature.mapdetail.domain.model.PendingPlace
import com.example.moamap.feature.mapdetail.domain.repository.PendingPlaceRepository
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

private fun pending(id: Long) = PendingPlace(
    id = id,
    placeName = "장소$id",
    requesterId = 3L,
    requesterName = null,
    requesterImageUrl = null,
    requestedAtMillis = null,
)

private class FakePendingPlaceRepository(
    var pending: List<PendingPlace> = listOf(pending(101), pending(102)),
) : PendingPlaceRepository {

    val listCalls = mutableListOf<Long>()
    val approved = mutableListOf<Long>()
    val rejected = mutableListOf<Long>()
    var listError: Exception? = null
    var actionError: Exception? = null

    override suspend fun getPendingPlaces(mapId: Long): List<PendingPlace> {
        listError?.let { throw it }
        listCalls += mapId
        return pending
    }

    override suspend fun approve(placeId: Long) {
        actionError?.let { throw it }
        approved += placeId
    }

    override suspend fun reject(placeId: Long) {
        actionError?.let { throw it }
        rejected += placeId
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PendingRequestViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: PendingPlaceRepository) = PendingRequestViewModel(
        savedStateHandle = SavedStateHandle(mapOf(MoaMapRoute.MapDetail.ARG_MAP_ID to 7L)),
        repository = repository,
    )

    /** 로그 탭을 열 때 읽는다. 장소 탭만 보는 사용자는 요청을 받지 않는다. */
    @Test
    fun `만들어지기만 하면 조회하지 않는다`() = runTest {
        val repository = FakePendingPlaceRepository()

        viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(repository.listCalls.isEmpty())
    }

    @Test
    fun `로그 탭을 열면 요청을 읽는다`() = runTest {
        val repository = FakePendingPlaceRepository()

        val viewModel = viewModel(repository).apply { loadOnce() }
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(7L), repository.listCalls)
        assertFalse(viewModel.uiState.value.loading)
        assertEquals(listOf(101L, 102L), viewModel.uiState.value.requests.map { it.id })
    }

    @Test
    fun `탭을 오갈 때마다 다시 읽지 않는다`() = runTest {
        val repository = FakePendingPlaceRepository()

        viewModel(repository).apply {
            loadOnce()
            loadOnce()
        }
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.listCalls.size)
    }

    @Test
    fun `조회에 실패하면 메시지를 남긴다`() = runTest {
        val repository = FakePendingPlaceRepository()
            .apply { listError = RuntimeException("boom") }

        val viewModel = viewModel(repository).apply { loadOnce() }
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.loading)
        assertTrue(viewModel.uiState.value.requests.isEmpty())
        assertEquals(PENDING_LOAD_FAILED_MESSAGE, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `재시도하면 다시 읽는다`() = runTest {
        val repository = FakePendingPlaceRepository()
            .apply { listError = RuntimeException("boom") }
        val viewModel = viewModel(repository).apply { loadOnce() }
        dispatcher.scheduler.advanceUntilIdle()

        repository.listError = null
        viewModel.retry()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(2, viewModel.uiState.value.requests.size)
    }

    @Test
    fun `수락하면 그 요청만 목록에서 뺀다`() = runTest {
        val repository = FakePendingPlaceRepository()
        val viewModel = viewModel(repository).apply { loadOnce() }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.approve(placeId = 101L)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(101L), repository.approved)
        assertEquals(listOf(102L), viewModel.uiState.value.requests.map { it.id })
        assertFalse(viewModel.uiState.value.processing)
    }

    @Test
    fun `거절하면 그 요청만 목록에서 뺀다`() = runTest {
        val repository = FakePendingPlaceRepository()
        val viewModel = viewModel(repository).apply { loadOnce() }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.reject(placeId = 102L)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(102L), repository.rejected)
        assertEquals(listOf(101L), viewModel.uiState.value.requests.map { it.id })
    }

    /** 수락한 장소는 지도에 새로 떠야 한다. 화면이 이 값을 보고 장소를 다시 읽는다. */
    @Test
    fun `수락하면 갱신 신호를 올린다`() = runTest {
        val repository = FakePendingPlaceRepository()
        val viewModel = viewModel(repository).apply { loadOnce() }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.approve(placeId = 101L)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.approvedCount)
    }

    /** 거절한 장소는 지도에 올라가지 않는다. 장소 목록을 다시 읽을 이유가 없다. */
    @Test
    fun `거절은 갱신 신호를 올리지 않는다`() = runTest {
        val repository = FakePendingPlaceRepository()
        val viewModel = viewModel(repository).apply { loadOnce() }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.reject(placeId = 101L)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.approvedCount)
    }

    /** 버튼을 연달아 눌러도 서버에는 한 번만 간다. */
    @Test
    fun `처리가 끝나기 전에는 다시 부르지 않는다`() = runTest {
        val repository = FakePendingPlaceRepository()
        val viewModel = viewModel(repository).apply { loadOnce() }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.approve(placeId = 101L)
        viewModel.approve(placeId = 102L)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(101L), repository.approved)
    }

    @Test
    fun `처리에 실패하면 목록을 그대로 두고 알린다`() = runTest {
        val repository = FakePendingPlaceRepository()
        val viewModel = viewModel(repository).apply { loadOnce() }
        dispatcher.scheduler.advanceUntilIdle()

        repository.actionError = RuntimeException("boom")
        viewModel.approve(placeId = 101L)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(101L, 102L), viewModel.uiState.value.requests.map { it.id })
        assertEquals(0, viewModel.uiState.value.approvedCount)
        assertNotNull(viewModel.uiState.value.actionErrorMessage)
        assertFalse(viewModel.uiState.value.processing)
    }

    /**
     * 요청 목록은 활동 내역 위에 얹히는 곁가지라 자기 자리에 오류를 띄울 곳이 없다.
     * 스낵바로 한 번 알리고 지운다.
     */
    @Test
    fun `조회 실패 메시지도 소비하면 사라진다`() = runTest {
        val repository = FakePendingPlaceRepository()
            .apply { listError = RuntimeException("boom") }
        val viewModel = viewModel(repository).apply { loadOnce() }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.consumeLoadError()

        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `알림 메시지는 소비하면 사라진다`() = runTest {
        val repository = FakePendingPlaceRepository()
        val viewModel = viewModel(repository).apply { loadOnce() }
        dispatcher.scheduler.advanceUntilIdle()

        repository.actionError = RuntimeException("boom")
        viewModel.approve(placeId = 101L)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.consumeActionError()

        assertNull(viewModel.uiState.value.actionErrorMessage)
    }
}
