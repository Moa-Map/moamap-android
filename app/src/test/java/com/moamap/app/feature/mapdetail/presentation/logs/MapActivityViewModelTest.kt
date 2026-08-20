package com.moamap.app.feature.mapdetail.presentation.logs

import androidx.lifecycle.SavedStateHandle
import com.moamap.app.core.navigation.MoaMapRoute
import com.moamap.app.core.network.ApiException
import com.moamap.app.core.network.ConnectionException
import com.moamap.app.feature.mapdetail.domain.model.MapActivity
import com.moamap.app.feature.mapdetail.domain.model.MapActivityType
import com.moamap.app.feature.mapdetail.domain.repository.MapActivityRepository
import com.moamap.app.feature.mapdetail.presentation.NETWORK_ERROR_MESSAGE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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

private const val MAP_ID = 7L

private fun testActivity(placeName: String) = MapActivity(
    type = MapActivityType.PlaceAdded,
    occurredAtMillis = null,
    actorName = "김도현",
    actorImageUrl = null,
    placeId = 1L,
    placeName = placeName,
    rating = null,
)

private class FakeMapActivityRepository(
    var activities: List<MapActivity> = emptyList(),
    var error: Throwable? = null,
) : MapActivityRepository {

    val calls = mutableListOf<Long>()

    override suspend fun getActivities(mapId: Long): List<MapActivity> {
        calls += mapId
        error?.let { failure -> throw failure }
        return activities
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MapActivityViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: MapActivityRepository) = MapActivityViewModel(
        savedStateHandle = SavedStateHandle(mapOf(MoaMapRoute.MapDetail.ARG_MAP_ID to MAP_ID)),
        repository = repository,
    )

    /** 장소 탭만 보고 나가는 사용자에게 활동 내역 왕복을 물리지 않는다. */
    @Test
    fun `만들어지기만 해서는 읽지 않는다`() = runTest {
        val repository = FakeMapActivityRepository()

        viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun `로그 탭을 처음 열면 읽는다`() = runTest {
        val repository = FakeMapActivityRepository(activities = listOf(testActivity("어니언 성수")))
        val viewModel = viewModel(repository)

        viewModel.loadOnce()
        assertTrue(viewModel.uiState.value.loading)

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(MAP_ID), repository.calls)
        assertFalse(viewModel.uiState.value.loading)
        assertEquals(1, viewModel.uiState.value.activities.size)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `탭을 오가도 다시 읽지 않는다`() = runTest {
        val repository = FakeMapActivityRepository()
        val viewModel = viewModel(repository)

        viewModel.loadOnce()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.loadOnce()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(MAP_ID), repository.calls)
    }

    @Test
    fun `재시도는 실패한 뒤에도 다시 읽는다`() = runTest {
        val repository = FakeMapActivityRepository(error = IOException("끊김"))
        val viewModel = viewModel(repository)

        viewModel.loadOnce()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(ACTIVITY_LOAD_FAILED_MESSAGE, viewModel.uiState.value.errorMessage)

        repository.error = null
        repository.activities = listOf(testActivity("대림창고"))
        viewModel.retry()
        assertTrue(viewModel.uiState.value.loading)
        assertNull(viewModel.uiState.value.errorMessage)

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(MAP_ID, MAP_ID), repository.calls)
        assertEquals(1, viewModel.uiState.value.activities.size)
    }

    /** 프라이빗 지도를 기웃거린 경우다. 통신 문제로 오해하면 재시도만 반복하게 된다. */
    @Test
    fun `멤버가 아니면 그 사유를 적는다`() = runTest {
        val repository = FakeMapActivityRepository(
            error = ApiException(code = "PLACE_002", status = 403, serverMessage = "해당 지도의 멤버가 아닙니다."),
        )
        val viewModel = viewModel(repository)

        viewModel.loadOnce()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ACTIVITY_NOT_MEMBER_MESSAGE, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `연결 실패는 네트워크 안내로 가른다`() = runTest {
        val repository = FakeMapActivityRepository(error = ConnectionException(IOException("타임아웃")))
        val viewModel = viewModel(repository)

        viewModel.loadOnce()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(NETWORK_ERROR_MESSAGE, viewModel.uiState.value.errorMessage)
    }

    /** 서버 메시지(`[500] COMMON_005: ...`)를 그대로 노출하지 않는다. */
    @Test
    fun `그 밖의 서버 실패는 한 문구로 묶는다`() = runTest {
        val repository = FakeMapActivityRepository(
            error = ApiException(code = "COMMON_005", status = 500, serverMessage = "서버 오류"),
        )
        val viewModel = viewModel(repository)

        viewModel.loadOnce()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ACTIVITY_LOAD_FAILED_MESSAGE, viewModel.uiState.value.errorMessage)
    }
}
