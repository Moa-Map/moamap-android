package com.moamap.app.feature.notification.presentation

import com.moamap.app.feature.notification.domain.model.NotificationKind
import com.moamap.app.feature.notification.domain.model.NotificationPage
import com.moamap.app.feature.notification.domain.model.UserNotification
import com.moamap.app.feature.notification.domain.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

private fun notification(id: Long, read: Boolean = false, kind: NotificationKind = NotificationKind.MapAdminAssigned) =
    UserNotification(id, kind, "알림$id", targetId = 10L, mapId = 10L, read = read, createdAtMillis = null)

private class FakeNotificationRepository(
    var pages: List<List<UserNotification>> = listOf(listOf(notification(1), notification(2))),
) : NotificationRepository {
    val requests = mutableListOf<Pair<Int, Int>>()
    val readIds = mutableListOf<Long>()
    var readAllCalls = 0
    var failure: Exception? = null

    override suspend fun getNotifications(page: Int, size: Int): NotificationPage {
        requests += page to size
        return NotificationPage(pages.getOrElse(page) { emptyList() }, isLast = page >= pages.lastIndex)
    }

    override suspend fun markRead(id: Long) {
        failure?.let { throw it }
        readIds += id
    }

    override suspend fun markAllRead() {
        failure?.let { throw it }
        readAllCalls++
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationListViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun loaded(repository: FakeNotificationRepository) = NotificationListViewModel(repository).also {
        dispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `열면 첫 페이지를 20개 받고 끝에 가까우면 다음 페이지를 붙인다`() = runTest {
        val repository = FakeNotificationRepository(
            pages = listOf(listOf(notification(1), notification(2)), listOf(notification(2), notification(3))),
        )
        val viewModel = loaded(repository)

        viewModel.loadMore()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(0 to NOTIFICATION_PAGE_SIZE, 1 to NOTIFICATION_PAGE_SIZE), repository.requests)
        // 페이지 경계가 밀려 다시 온 알림은 한 번만 보인다.
        assertEquals(listOf(1L, 2L, 3L), viewModel.uiState.value.notifications.map { it.id })
        assertTrue(viewModel.uiState.value.endReached)
    }

    @Test
    fun `누르면 바로 읽음으로 바꾸고 서버에 알리고 갈 곳을 준다`() = runTest {
        val repository = FakeNotificationRepository()
        val viewModel = loaded(repository)

        val destination = viewModel.open(viewModel.uiState.value.notifications.first())
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(NotificationDestination.Map(10L), destination)
        assertEquals(listOf(1L), repository.readIds)
        assertEquals(listOf(true, false), viewModel.uiState.value.notifications.map { it.read })
    }

    @Test
    fun `이미 읽은 알림은 다시 알리지 않는다`() = runTest {
        val repository = FakeNotificationRepository(pages = listOf(listOf(notification(1, read = true))))
        val viewModel = loaded(repository)

        viewModel.open(viewModel.uiState.value.notifications.first())
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(repository.readIds.isEmpty())
    }

    @Test
    fun `갈 곳이 없는 알림도 읽음으로 바꾼다`() = runTest {
        val repository = FakeNotificationRepository(
            pages = listOf(listOf(notification(1, kind = NotificationKind.InquiryAnswered))),
        )
        val viewModel = loaded(repository)

        assertNull(viewModel.open(viewModel.uiState.value.notifications.first()))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(1L), repository.readIds)
    }

    @Test
    fun `모두 읽음은 전부 읽음으로 바꾸고 실패하면 되돌린다`() = runTest {
        val repository = FakeNotificationRepository()
        val viewModel = loaded(repository)

        viewModel.markAllRead()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, repository.readAllCalls)
        assertTrue(viewModel.uiState.value.notifications.all { it.read })

        val failing = FakeNotificationRepository().apply { failure = IOException("끊김") }
        val failed = loaded(failing)
        failed.markAllRead()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf(false, false), failed.uiState.value.notifications.map { it.read })
    }
}
