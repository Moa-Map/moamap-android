package com.moamap.app.feature.mapdetail.presentation.handover

import androidx.lifecycle.SavedStateHandle
import com.moamap.app.core.auth.FakeCurrentUserStore
import com.moamap.app.core.navigation.MoaMapRoute
import com.moamap.app.core.network.ApiException
import com.moamap.app.feature.mapdetail.domain.model.MapMember
import com.moamap.app.feature.mapdetail.domain.model.MapRole
import com.moamap.app.feature.mapdetail.domain.repository.MapMemberRepository
import com.moamap.app.feature.mapdetail.presentation.FakeMapDetailRepository
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

private class FakeHandoverRepository(
    var candidates: List<MapMember> = listOf(MapMember(2L, "이서연", null, MapRole.Admin)),
) : MapMemberRepository {

    var candidatesError: Exception? = null
    var handOverError: Exception? = null
    val candidateCalls = mutableListOf<Long>()
    val handOverCalls = mutableListOf<Pair<Long, Long>>()

    override suspend fun getMembers(mapId: Long): List<MapMember> = error("위임이 부를 일이 없다")

    override suspend fun grantAdmin(mapId: Long, userId: Long) = error("위임이 부를 일이 없다")

    override suspend fun getOwnerCandidates(mapId: Long): List<MapMember> {
        candidatesError?.let { throw it }
        candidateCalls += mapId
        return candidates
    }

    override suspend fun handOverAndLeave(mapId: Long, newOwnerId: Long) {
        handOverError?.let { throw it }
        handOverCalls += mapId to newOwnerId
    }
}

private class FakeRoleHistory : MapRoleHistory {
    val roles = mutableMapOf<Pair<Long, Long>, MapRole>()

    override suspend fun replace(userId: Long, mapId: Long, role: MapRole): MapRole? =
        roles.put(userId to mapId, role)
}

@OptIn(ExperimentalCoroutinesApi::class)
class OwnerHandoverTest {

    private val dispatcher = StandardTestDispatcher()
    private val handle get() = SavedStateHandle(mapOf(MoaMapRoute.OwnerHandover.ARG_MAP_ID to 7L))

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---------- 위임 안내 ----------

    @Test
    fun `관리자가 있으면 관리자에게 넘기라고 안내한다`() = runTest(dispatcher) {
        val viewModel = OwnerHandoverPromptViewModel(FakeHandoverRepository())

        viewModel.open(7L)
        advanceUntilIdle()

        assertEquals(HandoverPrompt.Ask(mapId = 7L, toAdmins = true), viewModel.uiState.value.prompt)
        assertTrue(handoverPromptMessage(toAdmins = true).contains("관리자에게"))
    }

    @Test
    fun `관리자가 없으면 멤버에게 넘기라고 안내한다`() = runTest(dispatcher) {
        val repository = FakeHandoverRepository(candidates = listOf(MapMember(3L, "박지훈", null, MapRole.Member)))
        val viewModel = OwnerHandoverPromptViewModel(repository)

        viewModel.open(7L)
        advanceUntilIdle()

        assertEquals(HandoverPrompt.Ask(mapId = 7L, toAdmins = false), viewModel.uiState.value.prompt)
        assertTrue(handoverPromptMessage(toAdmins = false).contains("멤버에게"))
    }

    @Test
    fun `넘길 사람이 없으면 혼자 남은 안내를 띄운다`() = runTest(dispatcher) {
        val viewModel = OwnerHandoverPromptViewModel(FakeHandoverRepository(candidates = emptyList()))

        viewModel.open(7L)
        advanceUntilIdle()

        assertEquals(HandoverPrompt.Alone, viewModel.uiState.value.prompt)
    }

    @Test
    fun `후보를 못 읽으면 팝업 없이 알린다`() = runTest(dispatcher) {
        val repository = FakeHandoverRepository().apply { candidatesError = IllegalStateException("boom") }
        val viewModel = OwnerHandoverPromptViewModel(repository)

        viewModel.open(7L)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.prompt)
        assertEquals(HANDOVER_CHECK_FAILED_MESSAGE, viewModel.uiState.value.errorMessage)
    }

    // ---------- 방장 위임 화면 ----------

    @Test
    fun `고르기 전에는 넘길 수 없고 고르면 넘기고 나간다`() = runTest(dispatcher) {
        val repository = FakeHandoverRepository()
        val viewModel = OwnerHandoverViewModel(handle, repository, FakeMapDetailRepository())
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.canSubmit)
        assertTrue(viewModel.uiState.value.toAdmins)

        viewModel.handOver()
        advanceUntilIdle()
        assertTrue(repository.handOverCalls.isEmpty())

        viewModel.select(2L)
        viewModel.handOver()
        advanceUntilIdle()

        assertEquals(listOf(7L to 2L), repository.handOverCalls)
        assertTrue(viewModel.uiState.value.handedOver)
    }

    @Test
    fun `후보가 아니라는 답이 오면 알리고 목록을 다시 받는다`() = runTest(dispatcher) {
        val repository = FakeHandoverRepository()
        val viewModel = OwnerHandoverViewModel(handle, repository, FakeMapDetailRepository())
        advanceUntilIdle()
        viewModel.select(2L)
        // 그사이 고른 사람이 나갔다.
        repository.handOverError = ApiException(code = "MAP_028", status = 400, serverMessage = "후보 아님")
        repository.candidates = listOf(MapMember(3L, "박지훈", null, MapRole.Member))

        viewModel.handOver()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(HANDOVER_NOT_CANDIDATE_MESSAGE, state.notice)
        assertEquals(listOf(3L), state.candidates.map { it.id })
        assertNull(state.selectedId)
        assertFalse(state.handedOver)
    }

    @Test
    fun `후보를 못 읽으면 다시 시도할 수 있다`() = runTest(dispatcher) {
        val repository = FakeHandoverRepository().apply { candidatesError = IllegalStateException("boom") }
        val viewModel = OwnerHandoverViewModel(handle, repository, FakeMapDetailRepository())
        advanceUntilIdle()
        assertEquals(HANDOVER_LOAD_FAILED_MESSAGE, viewModel.uiState.value.errorMessage)

        repository.candidatesError = null
        viewModel.retry()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(listOf(2L), viewModel.uiState.value.candidates.map { it.id })
    }

    // ---------- 새 방장 팝업 ----------

    @Test
    fun `멤버나 관리자였다가 방장이 됐을 때만 알린다`() {
        assertTrue(becameOwner(previous = MapRole.Member, current = MapRole.Owner))
        assertTrue(becameOwner(previous = MapRole.Admin, current = MapRole.Owner))
        // 처음 보는 지도(직접 만든 지도)와 원래 방장이던 지도는 알리지 않는다.
        assertFalse(becameOwner(previous = null, current = MapRole.Owner))
        assertFalse(becameOwner(previous = MapRole.Owner, current = MapRole.Owner))
        assertFalse(becameOwner(previous = MapRole.Member, current = MapRole.Admin))
    }

    @Test
    fun `지도에서 방장으로 바뀐 것을 보면 한 번 띄운다`() = runTest(dispatcher) {
        val history = FakeRoleHistory()
        val viewModel = NewOwnerNoticeViewModel(handle, FakeCurrentUserStore(initial = 1L), history)

        viewModel.onRoleLoaded(joined = true, role = MapRole.Member)
        advanceUntilIdle()
        assertFalse(viewModel.visible.value)

        viewModel.onRoleLoaded(joined = true, role = MapRole.Owner)
        advanceUntilIdle()
        assertTrue(viewModel.visible.value)

        viewModel.dismiss()
        viewModel.onRoleLoaded(joined = true, role = MapRole.Owner)
        advanceUntilIdle()
        assertFalse(viewModel.visible.value)
    }
}
