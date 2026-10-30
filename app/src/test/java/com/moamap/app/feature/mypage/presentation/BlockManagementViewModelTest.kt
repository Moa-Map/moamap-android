package com.moamap.app.feature.mypage.presentation

import com.moamap.app.core.common.NETWORK_ERROR_MESSAGE
import com.moamap.app.core.common.UNBLOCKED_TOAST
import com.moamap.app.core.network.ConnectionException
import com.moamap.app.feature.mypage.data.remote.BlockedUserDto
import com.moamap.app.feature.mypage.data.repository.toBlockedUser
import com.moamap.app.feature.mypage.domain.BlockedUserStore
import com.moamap.app.feature.mypage.domain.model.BlockedUser
import com.moamap.app.feature.mypage.domain.repository.BlockRepository
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

private val Users = listOf(
    BlockedUser(1L, "김도현", null),
    BlockedUser(2L, "이서연", "https://example.com/2.jpg"),
)

private class FakeBlockRepository : BlockRepository {

    var users: List<BlockedUser> = Users
    var loadFailure: Throwable? = null
    var unblockFailure: Throwable? = null

    /** 값을 넣으면 완료될 때까지 해제가 매달린다. */
    var unblockPending: CompletableDeferred<Unit>? = null

    val unblocked = mutableListOf<Long>()

    override suspend fun getBlockedUsers(): List<BlockedUser> {
        loadFailure?.let { throw it }
        return users
    }

    override suspend fun block(userId: Long) = error("차단 관리는 차단하지 않는다")

    override suspend fun unblock(userId: Long) {
        unblocked += userId
        unblockPending?.await()
        unblockFailure?.let { throw it }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class BlockManagementViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeBlockRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `들어오면 차단 목록을 받는다`() = runTest(dispatcher) {
        val viewModel = BlockManagementViewModel(BlockedUserStore(repository))
        assertTrue(viewModel.uiState.value.loading)

        advanceUntilIdle()

        assertEquals(Users, viewModel.uiState.value.users)
        assertFalse(viewModel.uiState.value.empty)
    }

    @Test
    fun `차단한 사람이 없으면 빈 화면이다`() = runTest(dispatcher) {
        repository.users = emptyList()
        val viewModel = BlockManagementViewModel(BlockedUserStore(repository))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.empty)
    }

    @Test
    fun `목록을 못 받으면 안내를 띄우고 다시 받을 수 있다`() = runTest(dispatcher) {
        repository.loadFailure = ConnectionException(IOException("boom"))
        val viewModel = BlockManagementViewModel(BlockedUserStore(repository))
        advanceUntilIdle()

        assertEquals(NETWORK_ERROR_MESSAGE, viewModel.uiState.value.loadErrorMessage)
        assertFalse(viewModel.uiState.value.empty)

        repository.loadFailure = null
        viewModel.retry()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.loadErrorMessage)
        assertEquals(Users, viewModel.uiState.value.users)
    }

    @Test
    fun `해제하면 목록에서 빼고 토스트를 띄운다`() = runTest(dispatcher) {
        val viewModel = BlockManagementViewModel(BlockedUserStore(repository))
        advanceUntilIdle()

        viewModel.unblock(1L)
        advanceUntilIdle()

        assertEquals(listOf(1L), repository.unblocked)
        assertEquals(listOf(Users[1]), viewModel.uiState.value.users)
        assertEquals(UNBLOCKED_TOAST, viewModel.uiState.value.toastMessage)

        viewModel.consumeToast()
        assertNull(viewModel.uiState.value.toastMessage)
    }

    @Test
    fun `해제에 실패하면 목록은 그대로 두고 안내만 띄운다`() = runTest(dispatcher) {
        repository.unblockFailure = ConnectionException(IOException("boom"))
        val viewModel = BlockManagementViewModel(BlockedUserStore(repository))
        advanceUntilIdle()

        viewModel.unblock(1L)
        advanceUntilIdle()

        assertEquals(Users, viewModel.uiState.value.users)
        assertEquals(NETWORK_ERROR_MESSAGE, viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.toastMessage)
        assertNull(viewModel.uiState.value.unblockingUserId)
    }

    @Test
    fun `해제 요청이 나가 있는 동안 다른 해제는 보내지 않는다`() = runTest(dispatcher) {
        repository.unblockPending = CompletableDeferred()
        val viewModel = BlockManagementViewModel(BlockedUserStore(repository))
        advanceUntilIdle()

        viewModel.unblock(1L)
        advanceUntilIdle()
        viewModel.unblock(2L)

        assertEquals(1L, viewModel.uiState.value.unblockingUserId)
        assertEquals(listOf(1L), repository.unblocked)

        repository.unblockPending?.complete(Unit)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.unblockingUserId)
    }

    @Test
    fun `서버 응답의 빈 값은 화면에 맞게 바꾼다`() {
        val user = BlockedUserDto(userId = 7L, nickname = null, profileImageUrl = " ").toBlockedUser()

        assertEquals(BlockedUser(7L, "", null), user)
    }
}
