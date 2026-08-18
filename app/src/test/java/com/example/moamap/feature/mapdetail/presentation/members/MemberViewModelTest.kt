package com.example.moamap.feature.mapdetail.presentation.members

import androidx.lifecycle.SavedStateHandle
import com.example.moamap.core.navigation.MoaMapRoute
import com.example.moamap.feature.mapdetail.domain.model.MapMember
import com.example.moamap.feature.mapdetail.domain.model.MapRole
import com.example.moamap.feature.mapdetail.domain.repository.MapMemberRepository
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

private open class FakeMapMemberRepository(
    var members: List<MapMember> = listOf(
        MapMember(1L, "김도현", null, MapRole.Owner),
        MapMember(3L, "박지훈", null, MapRole.Member),
    ),
) : MapMemberRepository {

    val memberCalls = mutableListOf<Long>()
    val grantCalls = mutableListOf<Pair<Long, Long>>()
    var membersError: Exception? = null
    var grantError: Exception? = null

    override suspend fun getMembers(mapId: Long): List<MapMember> {
        membersError?.let { throw it }
        memberCalls += mapId
        return members
    }

    override suspend fun grantAdmin(mapId: Long, userId: Long) {
        grantError?.let { throw it }
        grantCalls += mapId to userId
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MemberViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: MapMemberRepository) = MemberViewModel(
        savedStateHandle = SavedStateHandle(mapOf(MoaMapRoute.MapDetail.ARG_MAP_ID to 7L)),
        repository = repository,
    )

    /** 시트를 열 때 읽는다. 장소 탭만 보는 사용자는 멤버를 받지 않는다. */
    @Test
    fun `만들어지기만 하면 조회하지 않는다`() = runTest {
        val repository = FakeMapMemberRepository()

        viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(repository.memberCalls.isEmpty())
    }

    @Test
    fun `시트를 열면 멤버를 읽는다`() = runTest {
        val repository = FakeMapMemberRepository()

        val viewModel = viewModel(repository).apply { loadOnce() }
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(7L), repository.memberCalls)
        assertFalse(viewModel.uiState.value.loading)
        assertEquals(listOf(1L, 3L), viewModel.uiState.value.members.map { it.id })
        assertEquals(MemberRole.Owner, viewModel.uiState.value.members[0].role)
    }

    /** 시트를 여닫을 때마다 다시 받지 않는다. */
    @Test
    fun `여러 번 열어도 한 번만 읽는다`() = runTest {
        val repository = FakeMapMemberRepository()

        viewModel(repository).apply {
            loadOnce()
            loadOnce()
        }
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.memberCalls.size)
    }

    @Test
    fun `조회에 실패하면 메시지를 남긴다`() = runTest {
        val repository = FakeMapMemberRepository()
            .apply { membersError = RuntimeException("boom") }

        val viewModel = viewModel(repository).apply { loadOnce() }
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.loading)
        assertTrue(viewModel.uiState.value.members.isEmpty())
        assertEquals(MEMBER_LOAD_FAILED_MESSAGE, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `재시도하면 다시 읽는다`() = runTest {
        val repository = FakeMapMemberRepository()
            .apply { membersError = RuntimeException("boom") }
        val viewModel = viewModel(repository).apply { loadOnce() }
        dispatcher.scheduler.advanceUntilIdle()

        repository.membersError = null
        viewModel.retry()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(2, viewModel.uiState.value.members.size)
    }

    @Test
    fun `권한을 주면 그 사람만 관리자가 된다`() = runTest {
        val repository = FakeMapMemberRepository()
        val viewModel = viewModel(repository).apply { loadOnce() }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.grantAdmin(userId = 3L)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(7L to 3L), repository.grantCalls)
        val members = viewModel.uiState.value.members
        assertEquals(MemberRole.Owner, members.first { it.id == 1L }.role)
        assertEquals(MemberRole.Admin, members.first { it.id == 3L }.role)
        assertFalse(viewModel.uiState.value.granting)
    }

    /** 버튼을 연달아 눌러도 서버에는 한 번만 간다. */
    @Test
    fun `권한 부여가 끝나기 전에는 다시 부르지 않는다`() = runTest {
        val repository = FakeMapMemberRepository()
        val viewModel = viewModel(repository).apply { loadOnce() }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.grantAdmin(userId = 3L)
        viewModel.grantAdmin(userId = 3L)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.grantCalls.size)
    }

    @Test
    fun `권한 부여에 실패하면 목록을 그대로 두고 알린다`() = runTest {
        val repository = FakeMapMemberRepository()
        val viewModel = viewModel(repository).apply { loadOnce() }
        dispatcher.scheduler.advanceUntilIdle()

        repository.grantError = RuntimeException("boom")
        viewModel.grantAdmin(userId = 3L)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            MemberRole.Member,
            viewModel.uiState.value.members.first { it.id == 3L }.role,
        )
        assertNotNull(viewModel.uiState.value.grantErrorMessage)
        assertFalse(viewModel.uiState.value.granting)
    }

    /** 스낵바를 한 번 띄우고 나면 지운다. 화면을 되돌아올 때 다시 뜨면 안 된다. */
    @Test
    fun `알림 메시지는 소비하면 사라진다`() = runTest {
        val repository = FakeMapMemberRepository()
        val viewModel = viewModel(repository).apply { loadOnce() }
        dispatcher.scheduler.advanceUntilIdle()

        repository.grantError = RuntimeException("boom")
        viewModel.grantAdmin(userId = 3L)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.consumeGrantError()

        assertNull(viewModel.uiState.value.grantErrorMessage)
    }
}
