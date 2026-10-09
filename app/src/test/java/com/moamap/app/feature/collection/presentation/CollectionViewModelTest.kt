package com.moamap.app.feature.collection.presentation

import com.moamap.app.core.network.ApiException
import com.moamap.app.feature.collection.domain.model.MapType
import com.moamap.app.feature.collection.domain.model.MyMap
import com.moamap.app.feature.collection.domain.model.NewMap
import com.moamap.app.feature.collection.domain.repository.MapRepository
import com.moamap.app.feature.mapdetail.domain.model.LeaveOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
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

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var repository: FakeMapRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeMapRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = CollectionViewModel(repository)

    /** 화면이 처음 보이는 시점까지 진행시킨다. 첫 조회는 `refresh()` 가 겸한다. */
    private fun TestScope.startedViewModel(): CollectionViewModel {
        val viewModel = createViewModel()
        viewModel.refresh()
        advanceUntilIdle()
        return viewModel
    }

    private fun myMap(id: Long, personal: Boolean = false, official: Boolean = false) = MyMap(
        id = id,
        title = "지도$id",
        imageUrl = null,
        memberCount = 1,
        placeCount = 0,
        official = official,
        personal = personal,
    )

    /**
     * 호출될 때마다 어떤 탭을 물었는지 기록한다.
     *
     * [delayMillis] 를 두면 요청이 진행 중인 상태를 만들 수 있다. 취소나 "요청 중에도 기존
     * 목록 유지" 를 검증하려면 요청이 실제로 매달려 있어야 한다.
     */
    private class FakeMapRepository(
        var delayMillis: Long = 0L,
        var result: (MapType) -> List<MyMap> = { emptyList() },
        var joinResult: (String) -> Long = { JOINED_MAP_ID },
    ) : MapRepository {

        val calls = mutableListOf<MapType>()
        val joinedCodes = mutableListOf<String>()

        /** 순서 저장·나가기·목록 조회(공식지도 제외)가 일어난 차례. */
        val events = mutableListOf<String>()

        val orderUpdates = mutableListOf<Pair<MapType, List<Long>>>()
        var orderFailure: Throwable? = null

        override suspend fun updateMyMapOrder(type: MapType, mapIds: List<Long>) {
            delay(delayMillis)
            orderFailure?.let { throw it }
            orderUpdates += type to mapIds
            events += "save"
        }

        /**
         * 커뮤니티 탭을 읽을 때 함께 불리는 참여한 공식지도.
         *
         * 탭을 몇 번 읽었는지 보는 [calls] 에는 넣지 않고 [officialCalls] 로 따로 센다.
         */
        var officialResult: () -> List<MyMap> = { emptyList() }
        var officialCalls = 0

        override suspend fun getMyMaps(type: MapType): List<MyMap> {
            if (type == MapType.Official) {
                officialCalls++
                delay(delayMillis)
                return officialResult()
            }
            calls += type
            events += "load"
            delay(delayMillis)
            return result(type)
        }

        override suspend fun updateMap(
            mapId: Long,
            name: String,
            description: String?,
            imageUrl: String?,
            tags: List<String>,
        ) = TODO("사용하지 않음")
        override suspend fun joinByInviteCode(inviteCode: String): Long {
            joinedCodes += inviteCode
            delay(delayMillis)
            return joinResult(inviteCode)
        }

        override suspend fun uploadCoverImage(imageUri: String) = TODO("사용하지 않음")
        override suspend fun createMap(newMap: NewMap) = TODO("사용하지 않음")

        /** 지도별 나가기 결과. null 이면 방장처럼 나갈 수 없는 지도다. */
        var leaveOutcome: (Long) -> LeaveOutcome? = { LeaveOutcome.Leave }

        /** 나가기가 실패할 지도. */
        var leaveFailures: Set<Long> = emptySet()

        val outcomeChecks = mutableListOf<Long>()
        val leftMapIds = mutableListOf<Long>()

        override suspend fun getLeaveOutcome(mapId: Long): LeaveOutcome? {
            outcomeChecks += mapId
            delay(delayMillis)
            return leaveOutcome(mapId)
        }

        override suspend fun leaveMap(mapId: Long) {
            delay(delayMillis)
            if (mapId in leaveFailures) throw IOException("boom")
            leftMapIds += mapId
            events += "leave"
        }
    }

    private companion object {
        const val JOINED_MAP_ID = 77L
    }

    @Test
    fun `화면이 보이기 전에는 읽지 않는다`() = runTest(dispatcher) {
        createViewModel()
        advanceUntilIdle()

        // init 에서도 읽으면 화면이 뜰 때 같은 요청이 두 번 나간다.
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun `화면이 보이면 선택된 탭만 읽는다`() = runTest(dispatcher) {
        val viewModel = startedViewModel()

        assertEquals(listOf(MapType.Community), repository.calls)
        assertEquals(MapType.Community, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun `받은 목록이 상태에 담긴다`() = runTest(dispatcher) {
        repository.result = { listOf(myMap(1), myMap(2)) }

        val viewModel = startedViewModel()

        val state = viewModel.uiState.value.community
        assertTrue(state is MyMapsState.Success)
        assertEquals(listOf(1L, 2L), (state as MyMapsState.Success).maps.map { it.id })
    }

    @Test
    fun `커뮤니티 탭은 참여한 공식지도를 맨 위에 둔다`() = runTest(dispatcher) {
        repository.officialResult = { listOf(myMap(9)) }
        repository.result = { listOf(myMap(1), myMap(2)) }

        val viewModel = startedViewModel()

        val state = viewModel.uiState.value.community as MyMapsState.Success
        assertEquals(listOf(9L, 1L, 2L), state.maps.map { it.id })
        assertEquals(1, repository.officialCalls)
    }

    @Test
    fun `공식지도를 못 받아도 커뮤니티 지도는 보여준다`() = runTest(dispatcher) {
        repository.officialResult = { throw IOException("boom") }
        repository.result = { listOf(myMap(1)) }

        val viewModel = startedViewModel()

        val state = viewModel.uiState.value.community as MyMapsState.Success
        assertEquals(listOf(1L), state.maps.map { it.id })
    }

    @Test
    fun `프라이빗 탭에는 공식지도를 합치지 않는다`() = runTest(dispatcher) {
        repository.officialResult = { listOf(myMap(9)) }
        repository.result = { listOf(myMap(1)) }
        val viewModel = startedViewModel()

        viewModel.selectTab(MapType.Private)
        advanceUntilIdle()

        val state = viewModel.uiState.value.private as MyMapsState.Success
        assertEquals(listOf(1L), state.maps.map { it.id })
        // 커뮤니티 탭을 읽을 때 한 번뿐이다.
        assertEquals(1, repository.officialCalls)
    }

    @Test
    fun `탭을 바꾸면 그 탭을 읽는다`() = runTest(dispatcher) {
        val viewModel = startedViewModel()

        viewModel.selectTab(MapType.Private)
        advanceUntilIdle()

        assertEquals(listOf(MapType.Community, MapType.Private), repository.calls)
        assertEquals(MapType.Private, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun `이미 읽은 탭으로 돌아가면 다시 읽지 않는다`() = runTest(dispatcher) {
        val viewModel = startedViewModel()
        viewModel.selectTab(MapType.Private)
        advanceUntilIdle()

        viewModel.selectTab(MapType.Community)
        advanceUntilIdle()

        // 커뮤니티 1회 + 프라이빗 1회. 돌아왔다고 다시 부르지 않는다.
        assertEquals(listOf(MapType.Community, MapType.Private), repository.calls)
    }

    @Test
    fun `같은 탭을 다시 고르면 아무 일도 하지 않는다`() = runTest(dispatcher) {
        val viewModel = startedViewModel()

        viewModel.selectTab(MapType.Community)
        advanceUntilIdle()

        assertEquals(listOf(MapType.Community), repository.calls)
    }

    @Test
    fun `실패하면 안내를 보여주고 다시 시도할 수 있다`() = runTest(dispatcher) {
        repository.result = { throw IOException("서버 오류") }

        val viewModel = startedViewModel()

        val failed = viewModel.uiState.value.community
        assertTrue(failed is MyMapsState.Error)
        assertEquals("지도 목록을 불러오지 못했어요", (failed as MyMapsState.Error).message)

        repository.result = { listOf(myMap(1)) }
        viewModel.retry()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.community is MyMapsState.Success)
    }

    @Test
    fun `새로고침은 현재 탭만 다시 읽는다`() = runTest(dispatcher) {
        val viewModel = startedViewModel()
        viewModel.selectTab(MapType.Private)
        advanceUntilIdle()
        repository.calls.clear()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(listOf(MapType.Private), repository.calls)
    }

    @Test
    fun `새로고침하면 보고 있지 않던 탭도 다음에 고를 때 새로 읽는다`() = runTest(dispatcher) {
        val viewModel = startedViewModel()
        viewModel.selectTab(MapType.Private)
        advanceUntilIdle()
        viewModel.selectTab(MapType.Community)
        advanceUntilIdle()
        repository.calls.clear()

        // 지도를 만들고 커뮤니티 탭으로 돌아온 상황. 프라이빗 목록도 낡았다.
        viewModel.refresh()
        advanceUntilIdle()
        viewModel.selectTab(MapType.Private)
        advanceUntilIdle()

        assertEquals(listOf(MapType.Community, MapType.Private), repository.calls)
    }

    @Test
    fun `새로고침 중에도 보고 있던 목록이 사라지지 않는다`() = runTest(dispatcher) {
        repository.result = { listOf(myMap(1)) }
        val viewModel = startedViewModel()

        repository.delayMillis = 1_000
        viewModel.refresh()

        // 응답이 오기 전. Loading 으로 되돌리면 목록이 깜빡인다.
        val during = viewModel.uiState.value.community
        assertTrue(during is MyMapsState.Success)
        assertEquals(listOf(1L), (during as MyMapsState.Success).maps.map { it.id })

        advanceUntilIdle()
    }

    @Test
    fun `새로고침이 실패해도 이미 받은 목록은 남긴다`() = runTest(dispatcher) {
        repository.result = { listOf(myMap(1)) }
        val viewModel = startedViewModel()

        repository.result = { throw IOException("서버 오류") }
        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value.community
        assertTrue(state is MyMapsState.Success)
        assertEquals(listOf(1L), (state as MyMapsState.Success).maps.map { it.id })
    }

    @Test
    fun `첫 조회가 실패한 뒤 새로고침하면 안내를 보여준다`() = runTest(dispatcher) {
        repository.result = { throw IOException("서버 오류") }
        val viewModel = startedViewModel()

        viewModel.refresh()
        advanceUntilIdle()

        // 보여줄 목록이 없으면 실패를 숨기지 않는다.
        assertTrue(viewModel.uiState.value.community is MyMapsState.Error)
    }

    // ---------- 초대 코드로 합류 ----------

    @Test
    fun `초대 코드 입력은 영문 대문자와 숫자만 남긴다`() = runTest(dispatcher) {
        val viewModel = startedViewModel()
        viewModel.openJoinDialog()

        viewModel.updateInviteCode("#a1 b2-c3")

        assertEquals("A1B2C3", (viewModel.uiState.value.join as JoinState.Editing).code)
    }

    @Test
    fun `한글은 초대 코드로 받지 않는다`() = runTest(dispatcher) {
        val viewModel = startedViewModel()
        viewModel.openJoinDialog()

        // Char.isLetterOrDigit() 은 한글도 문자로 본다. 서버 코드는 ASCII 영숫자다.
        viewModel.updateInviteCode("ㅇㅇ가나다")

        assertEquals("", (viewModel.uiState.value.join as JoinState.Editing).code)
    }

    @Test
    fun `없는 초대 코드는 코드를 확인하라고 안내한다`() = runTest(dispatcher) {
        repository.joinResult = {
            throw ApiException(code = "MAP_007", status = 404, serverMessage = "유효하지 않은 초대 코드입니다.")
        }
        val viewModel = startedViewModel()
        viewModel.openJoinDialog()
        viewModel.updateInviteCode("ZZZZZZ")

        viewModel.join()
        advanceUntilIdle()

        val join = viewModel.uiState.value.join as JoinState.Editing
        assertEquals("코드를 다시 확인해주세요", join.errorMessage)
    }

    @Test
    fun `코드가 비어 있으면 제출할 수 없다`() = runTest(dispatcher) {
        val viewModel = startedViewModel()
        viewModel.openJoinDialog()

        viewModel.join()
        advanceUntilIdle()

        assertTrue(repository.joinedCodes.isEmpty())
    }

    @Test
    fun `합류에 성공하면 프라이빗 탭으로 옮기고 목록을 다시 읽는다`() = runTest(dispatcher) {
        val viewModel = startedViewModel()
        viewModel.openJoinDialog()
        viewModel.updateInviteCode("A1B2C3")
        repository.calls.clear()

        viewModel.join()
        advanceUntilIdle()

        assertEquals(listOf("A1B2C3"), repository.joinedCodes)
        assertEquals(JoinState.Hidden, viewModel.uiState.value.join)
        assertEquals(MapType.Private, viewModel.uiState.value.selectedTab)
        assertEquals(listOf(MapType.Private), repository.calls)
    }

    @Test
    fun `이미 참여한 지도는 그렇다고 안내한다`() = runTest(dispatcher) {
        repository.joinResult = {
            throw ApiException(code = "MAP_005", status = 409, serverMessage = "이미 참여한 지도입니다.")
        }
        val viewModel = startedViewModel()
        viewModel.openJoinDialog()
        viewModel.updateInviteCode("VH4YXZ")

        viewModel.join()
        advanceUntilIdle()

        val join = viewModel.uiState.value.join as JoinState.Editing
        assertEquals("이미 참여 중인 지도예요", join.errorMessage)
    }

    @Test
    fun `합류에 실패하면 모달을 열어둔 채 입력값을 남긴다`() = runTest(dispatcher) {
        repository.joinResult = { throw IOException("잘못된 코드") }
        val viewModel = startedViewModel()
        viewModel.openJoinDialog()
        viewModel.updateInviteCode("A1B2C3")

        viewModel.join()
        advanceUntilIdle()

        val join = viewModel.uiState.value.join
        assertTrue(join is JoinState.Editing)
        assertEquals("A1B2C3", (join as JoinState.Editing).code)
        assertEquals("지도에 참여하지 못했어요", join.errorMessage)
        assertFalse(join.submitting)
    }

    @Test
    fun `합류 중에는 다시 제출되지 않는다`() = runTest(dispatcher) {
        repository.delayMillis = 1_000
        val viewModel = startedViewModel()
        viewModel.openJoinDialog()
        viewModel.updateInviteCode("A1B2C3")

        viewModel.join()
        viewModel.join()
        advanceUntilIdle()

        assertEquals(1, repository.joinedCodes.size)
    }

    @Test
    fun `모달을 닫으면 상태가 사라진다`() = runTest(dispatcher) {
        val viewModel = startedViewModel()
        viewModel.openJoinDialog()
        viewModel.updateInviteCode("A1B2C3")

        viewModel.closeJoinDialog()

        assertEquals(JoinState.Hidden, viewModel.uiState.value.join)
    }

    @Test
    fun `같은 탭을 연달아 읽으면 앞선 요청은 버린다`() = runTest(dispatcher) {
        repository.delayMillis = 1_000
        repository.result = { listOf(myMap(1)) }

        val viewModel = createViewModel()
        viewModel.refresh()
        // 첫 요청이 시작돼 응답을 기다리는 중이다.
        advanceTimeBy(500)

        repository.result = { listOf(myMap(2)) }
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(listOf(MapType.Community, MapType.Community), repository.calls)
        // 늦게 도착한 앞선 요청이 최신 결과를 덮어쓰면 안 된다.
        val state = viewModel.uiState.value.community
        assertEquals(listOf(2L), (state as MyMapsState.Success).maps.map { it.id })
    }

    // ---------- 편집: 골라서 나가기 ----------

    /** 1: 멤버인 지도, 2: 방장인 지도, 3: 나만의 지도, 4: 멤버인 지도 */
    private fun TestScope.editingViewModel(): CollectionViewModel {
        repository.result = { listOf(myMap(1L), myMap(2L), myMap(3L, personal = true), myMap(4L)) }
        repository.leaveOutcome = { mapId -> if (mapId == 2L) null else LeaveOutcome.Leave }
        val viewModel = startedViewModel()
        viewModel.startEdit()
        advanceUntilIdle()
        return viewModel
    }

    @Test
    fun `편집을 시작하면 지도마다 나갈 수 있는지 확인한다`() = runTest(dispatcher) {
        val viewModel = editingViewModel()

        val edit = viewModel.uiState.value.edit!!
        assertEquals(LeaveEligibility.Allowed, edit.eligibilityOf(1L))
        assertEquals(LeaveEligibility.Owner, edit.eligibilityOf(2L))
        assertEquals(LeaveEligibility.Personal, edit.eligibilityOf(3L))
        // 나만의 지도는 물어볼 필요가 없다.
        assertEquals(listOf(1L, 2L, 4L), repository.outcomeChecks.sorted())
    }

    @Test
    fun `고를 수 없는 지도를 누르면 이유를 알리고 고르지 않는다`() = runTest(dispatcher) {
        val viewModel = editingViewModel()

        viewModel.toggleSelection(2L)
        assertEquals(OWNER_CANNOT_LEAVE_MESSAGE, viewModel.uiState.value.notice)

        viewModel.toggleSelection(3L)
        assertEquals(PERSONAL_CANNOT_LEAVE_MESSAGE, viewModel.uiState.value.notice)

        assertTrue(viewModel.uiState.value.edit!!.selected.isEmpty())
    }

    @Test
    fun `나갈 수 있는 지도는 눌러서 고르고 다시 누르면 푼다`() = runTest(dispatcher) {
        val viewModel = editingViewModel()

        viewModel.toggleSelection(1L)
        assertEquals(setOf(1L), viewModel.uiState.value.edit!!.selected)
        assertTrue(viewModel.uiState.value.edit!!.selectionBarVisible)

        viewModel.toggleSelection(1L)
        assertTrue(viewModel.uiState.value.edit!!.selected.isEmpty())
    }

    @Test
    fun `확인이 끝나지 않은 지도는 눌러도 아무 일이 없다`() = runTest(dispatcher) {
        repository.result = { listOf(myMap(1L)) }
        val viewModel = startedViewModel()

        viewModel.startEdit()
        viewModel.toggleSelection(1L)

        assertTrue(viewModel.uiState.value.edit!!.selected.isEmpty())
        assertNull(viewModel.uiState.value.notice)
    }

    @Test
    fun `확인에 실패한 지도는 고를 수 없다`() = runTest(dispatcher) {
        repository.result = { listOf(myMap(1L)) }
        repository.leaveOutcome = { throw IOException("boom") }
        val viewModel = startedViewModel()
        viewModel.startEdit()
        advanceUntilIdle()

        viewModel.toggleSelection(1L)

        assertEquals(LEAVE_CHECK_FAILED_MESSAGE, viewModel.uiState.value.notice)
        assertTrue(viewModel.uiState.value.edit!!.selected.isEmpty())
    }

    @Test
    fun `고른 지도에서 모두 나가면 편집을 끝내고 목록을 다시 읽는다`() = runTest(dispatcher) {
        val viewModel = editingViewModel()
        viewModel.toggleSelection(1L)
        viewModel.toggleSelection(4L)
        val loadsBefore = repository.calls.size

        viewModel.openLeaveConfirm()
        assertTrue(viewModel.uiState.value.edit!!.confirmVisible)
        viewModel.leaveSelected()
        advanceUntilIdle()

        assertEquals(listOf(1L, 4L), repository.leftMapIds)
        assertNull(viewModel.uiState.value.edit)
        assertNull(viewModel.uiState.value.notice)
        assertEquals(loadsBefore + 1, repository.calls.size)
    }

    @Test
    fun `일부만 나가지 못하면 몇 개인지 알린다`() = runTest(dispatcher) {
        val viewModel = editingViewModel()
        repository.leaveFailures = setOf(4L)
        viewModel.toggleSelection(1L)
        viewModel.toggleSelection(4L)

        viewModel.leaveSelected()
        advanceUntilIdle()

        assertEquals(listOf(1L), repository.leftMapIds)
        assertEquals(leaveFailedMessage(1), viewModel.uiState.value.notice)
    }

    @Test
    fun `고른 지도가 없으면 나가기 팝업을 열지 않는다`() = runTest(dispatcher) {
        val viewModel = editingViewModel()

        viewModel.openLeaveConfirm()

        assertFalse(viewModel.uiState.value.edit!!.confirmVisible)
    }

    @Test
    fun `탭을 옮기면 편집을 끝낸다`() = runTest(dispatcher) {
        val viewModel = editingViewModel()
        viewModel.toggleSelection(1L)

        viewModel.selectTab(MapType.Private)

        assertNull(viewModel.uiState.value.edit)
    }

    // ---------- 편집: 순서 바꾸기 ----------

    private fun CollectionViewModel.communityIds(): List<Long> =
        (uiState.value.community as MyMapsState.Success).maps.map { it.id }

    @Test
    fun `편집 중 지도를 이웃 자리로 옮긴다`() = runTest(dispatcher) {
        val viewModel = editingViewModel()

        viewModel.moveMap(mapId = 1L, targetId = 2L)
        assertEquals(listOf(2L, 1L, 3L, 4L), viewModel.communityIds())

        viewModel.moveMap(mapId = 4L, targetId = 3L)
        assertEquals(listOf(2L, 1L, 4L, 3L), viewModel.communityIds())
    }

    @Test
    fun `멀리 떨어진 지도 자리로도 옮긴다`() = runTest(dispatcher) {
        val viewModel = editingViewModel()

        viewModel.moveMap(mapId = 4L, targetId = 1L)

        assertEquals(listOf(4L, 1L, 2L, 3L), viewModel.communityIds())
    }

    @Test
    fun `편집 중이 아니면 순서를 바꾸지 않는다`() = runTest(dispatcher) {
        repository.result = { listOf(myMap(1L), myMap(2L)) }
        val viewModel = startedViewModel()

        viewModel.moveMap(mapId = 1L, targetId = 2L)

        assertEquals(listOf(1L, 2L), viewModel.communityIds())
    }

    @Test
    fun `편집을 마치면 바꾼 순서를 저장한다`() = runTest(dispatcher) {
        val viewModel = editingViewModel()
        viewModel.moveMap(mapId = 1L, targetId = 2L)

        viewModel.finishEdit()
        advanceUntilIdle()

        // 서버는 그 탭의 지도를 빠짐없이 받아야 한다. 나만의 지도(3)도 들어간다.
        assertEquals(listOf(MapType.Community to listOf(2L, 1L, 3L, 4L)), repository.orderUpdates)
        assertEquals(listOf(2L, 1L, 3L, 4L), viewModel.communityIds())
        assertNull(viewModel.uiState.value.notice)
    }

    @Test
    fun `순서가 처음과 같으면 저장하지 않는다`() = runTest(dispatcher) {
        val viewModel = editingViewModel()
        // 옮겼다가 제자리로 돌려놓았다.
        viewModel.moveMap(mapId = 1L, targetId = 2L)
        viewModel.moveMap(mapId = 1L, targetId = 2L)

        viewModel.finishEdit()
        advanceUntilIdle()

        assertTrue(repository.orderUpdates.isEmpty())
    }

    @Test
    fun `순서를 저장하지 못하면 알리고 서버 순서로 되돌린다`() = runTest(dispatcher) {
        val viewModel = editingViewModel()
        repository.orderFailure = IOException("boom")
        viewModel.moveMap(mapId = 1L, targetId = 2L)

        viewModel.finishEdit()
        advanceUntilIdle()

        assertEquals(ORDER_SAVE_FAILED_MESSAGE, viewModel.uiState.value.notice)
        assertEquals(listOf(1L, 2L, 3L, 4L), viewModel.communityIds())
    }

    @Test
    fun `화면에 돌아와 다시 읽을 때는 순서 저장이 끝난 뒤에 읽는다`() = runTest(dispatcher) {
        val viewModel = editingViewModel()
        repository.delayMillis = 1_000
        repository.events.clear()
        viewModel.moveMap(mapId = 1L, targetId = 2L)

        // 편집 중에 다른 화면에 갔다 돌아왔다. 편집이 끝나며 저장하고 목록을 다시 읽는다.
        viewModel.refresh()
        advanceUntilIdle()

        // 먼저 읽으면 저장 전 순서를 받아 되돌아간다.
        assertEquals(listOf("save", "load"), repository.events)
    }

    @Test
    fun `탭을 옮기면 떠난 탭의 바꾼 순서를 저장한다`() = runTest(dispatcher) {
        val viewModel = editingViewModel()
        viewModel.moveMap(mapId = 4L, targetId = 1L)

        viewModel.selectTab(MapType.Private)
        advanceUntilIdle()

        assertEquals(listOf(MapType.Community to listOf(4L, 1L, 2L, 3L)), repository.orderUpdates)
    }

    @Test
    fun `커뮤니티 탭의 공식지도는 공식지도 순서로 따로 저장한다`() = runTest(dispatcher) {
        repository.officialResult = { listOf(myMap(9L, official = true), myMap(8L, official = true)) }
        repository.result = { listOf(myMap(1L), myMap(2L)) }
        val viewModel = startedViewModel()
        viewModel.startEdit()
        advanceUntilIdle()

        viewModel.moveMap(mapId = 8L, targetId = 9L)
        viewModel.moveMap(mapId = 2L, targetId = 1L)
        viewModel.finishEdit()
        advanceUntilIdle()

        assertEquals(
            listOf(MapType.Official to listOf(8L, 9L), MapType.Community to listOf(2L, 1L)),
            repository.orderUpdates,
        )
    }

    @Test
    fun `순서가 그대로인 종류는 보내지 않는다`() = runTest(dispatcher) {
        repository.officialResult = { listOf(myMap(9L, official = true), myMap(8L, official = true)) }
        repository.result = { listOf(myMap(1L), myMap(2L)) }
        val viewModel = startedViewModel()
        viewModel.startEdit()
        advanceUntilIdle()

        viewModel.moveMap(mapId = 8L, targetId = 9L)
        viewModel.finishEdit()
        advanceUntilIdle()

        assertEquals(listOf(MapType.Official to listOf(8L, 9L)), repository.orderUpdates)
    }

    @Test
    fun `공식지도와 커뮤니티 지도는 서로의 자리로 옮기지 않는다`() = runTest(dispatcher) {
        repository.officialResult = { listOf(myMap(9L, official = true)) }
        repository.result = { listOf(myMap(1L), myMap(2L)) }
        val viewModel = startedViewModel()
        viewModel.startEdit()
        advanceUntilIdle()

        viewModel.moveMap(mapId = 1L, targetId = 9L)

        // 서버가 공식지도를 늘 위에 두므로, 섞어 두면 다시 읽을 때 순서가 바뀐다.
        assertEquals(listOf(9L, 1L, 2L), viewModel.communityIds())
    }

    @Test
    fun `프라이빗 탭은 나만의 지도까지 프라이빗 순서로 저장한다`() = runTest(dispatcher) {
        repository.result = { listOf(myMap(1L, personal = true), myMap(2L), myMap(3L)) }
        val viewModel = startedViewModel()
        viewModel.selectTab(MapType.Private)
        advanceUntilIdle()
        viewModel.startEdit()
        advanceUntilIdle()

        viewModel.moveMap(mapId = 3L, targetId = 2L)
        viewModel.finishEdit()
        advanceUntilIdle()

        assertEquals(listOf(MapType.Private to listOf(1L, 3L, 2L)), repository.orderUpdates)
    }

    @Test
    fun `순서를 바꾸고 나가면 나가기 전에 순서를 저장한다`() = runTest(dispatcher) {
        val viewModel = editingViewModel()
        repository.events.clear()
        viewModel.moveMap(mapId = 4L, targetId = 1L)
        viewModel.toggleSelection(1L)

        viewModel.leaveSelected()
        advanceUntilIdle()

        // 나가기 전이라 나갈 지도(1)까지 모두 보낸다. 나간 뒤에도 남은 지도의 순서는 그대로다.
        assertEquals(listOf(MapType.Community to listOf(4L, 1L, 2L, 3L)), repository.orderUpdates)
        assertEquals(listOf("save", "leave", "load"), repository.events)
    }

    @Test
    fun `순서 저장 실패 안내는 나가기가 성공해도 남는다`() = runTest(dispatcher) {
        val viewModel = editingViewModel()
        repository.orderFailure = IOException("boom")
        viewModel.moveMap(mapId = 4L, targetId = 1L)
        viewModel.toggleSelection(1L)

        viewModel.leaveSelected()
        advanceUntilIdle()

        assertEquals(listOf(1L), repository.leftMapIds)
        assertEquals(ORDER_SAVE_FAILED_MESSAGE, viewModel.uiState.value.notice)
    }

    @Test
    fun `편집 중 초대 코드로 합류하면 편집을 끝내고 바꾼 순서를 저장한다`() = runTest(dispatcher) {
        val viewModel = editingViewModel()
        viewModel.moveMap(mapId = 1L, targetId = 2L)
        viewModel.openJoinDialog()
        viewModel.updateInviteCode("A1B2C3")

        viewModel.join()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.edit)
        assertEquals(listOf(MapType.Community to listOf(2L, 1L, 3L, 4L)), repository.orderUpdates)
    }
}
