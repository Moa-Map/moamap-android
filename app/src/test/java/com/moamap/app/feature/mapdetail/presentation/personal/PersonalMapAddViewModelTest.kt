package com.moamap.app.feature.mapdetail.presentation.personal

import com.moamap.app.core.network.ApiException
import com.moamap.app.core.network.ConnectionException
import com.moamap.app.feature.mapdetail.domain.repository.PersonalMapNotFoundException
import com.moamap.app.feature.mapdetail.domain.repository.PersonalMapRepository
import com.moamap.app.feature.mapdetail.presentation.NETWORK_ERROR_MESSAGE
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakePersonalMapRepository(
    private val delayMillis: Long = 0L,
    var error: Exception? = null,
) : PersonalMapRepository {

    val added = mutableListOf<Long>()

    override suspend fun addPlace(placeId: Long) {
        delay(delayMillis)
        error?.let { throw it }
        added += placeId
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PersonalMapAddViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `추가하면 연 장소를 담고 완료를 알린다`() = runTest {
        val repository = FakePersonalMapRepository()
        val viewModel = PersonalMapAddViewModel(repository)
        viewModel.open(placeId = 7L)

        viewModel.add()
        assertTrue(viewModel.uiState.value.adding)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(7L), repository.added)
        assertFalse(viewModel.uiState.value.adding)
        assertEquals(PERSONAL_MAP_ADDED_MESSAGE, viewModel.uiState.value.message)
        assertFalse(viewModel.uiState.value.failed)
    }

    @Test
    fun `장소를 열지 않았으면 아무것도 하지 않는다`() = runTest {
        val repository = FakePersonalMapRepository()
        val viewModel = PersonalMapAddViewModel(repository)

        viewModel.add()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(repository.added.isEmpty())
    }

    @Test
    fun `추가하는 중에는 다시 받지 않는다`() = runTest {
        val repository = FakePersonalMapRepository(delayMillis = 100L)
        val viewModel = PersonalMapAddViewModel(repository)
        viewModel.open(placeId = 7L)

        viewModel.add()
        viewModel.add()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(7L), repository.added)
    }

    @Test
    fun `실패 이유마다 안내를 가른다`() = runTest {
        val cases = listOf(
            ApiException(code = "PLACE_010", status = 409, serverMessage = "dup") to
                PERSONAL_MAP_DUPLICATE_MESSAGE,
            PersonalMapNotFoundException() to PERSONAL_MAP_NOT_FOUND_MESSAGE,
            ConnectionException(RuntimeException("offline")) to NETWORK_ERROR_MESSAGE,
            RuntimeException("boom") to PERSONAL_MAP_ADD_FAILED_MESSAGE,
        )

        cases.forEach { (error, expected) ->
            val viewModel = PersonalMapAddViewModel(FakePersonalMapRepository(error = error))
            viewModel.open(placeId = 7L)

            viewModel.add()
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(expected, viewModel.uiState.value.message)
            assertTrue(viewModel.uiState.value.failed)
        }
    }

    @Test
    fun `다른 장소를 열면 앞 장소의 안내를 지우고 늦은 결과도 버린다`() = runTest {
        val repository = FakePersonalMapRepository(delayMillis = 100L)
        val viewModel = PersonalMapAddViewModel(repository)
        viewModel.open(placeId = 7L)
        viewModel.add()

        viewModel.open(placeId = 8L)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(8L, viewModel.uiState.value.placeId)
        assertNull(viewModel.uiState.value.message)
        assertFalse(viewModel.uiState.value.adding)
    }

    @Test
    fun `같은 장소를 다시 열면 안내를 그대로 둔다`() = runTest {
        val viewModel = PersonalMapAddViewModel(FakePersonalMapRepository())
        viewModel.open(placeId = 7L)
        viewModel.add()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.open(placeId = 7L)

        assertEquals(PERSONAL_MAP_ADDED_MESSAGE, viewModel.uiState.value.message)
    }

    @Test
    fun `닫으면 비운다`() = runTest {
        val viewModel = PersonalMapAddViewModel(FakePersonalMapRepository())
        viewModel.open(placeId = 7L)

        viewModel.close()

        assertNull(viewModel.uiState.value.placeId)
    }
}
