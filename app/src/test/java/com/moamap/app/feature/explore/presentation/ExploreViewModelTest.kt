package com.moamap.app.feature.explore.presentation

import com.moamap.app.feature.explore.domain.model.CommunityMap
import com.moamap.app.feature.explore.domain.model.CommunityMapPage
import com.moamap.app.feature.explore.domain.model.CommunityMapSort
import com.moamap.app.feature.explore.domain.repository.CommunityMapRepository
import com.moamap.app.feature.officialmap.domain.model.OfficialMap
import com.moamap.app.feature.officialmap.domain.repository.OfficialMapRepository
import com.moamap.app.feature.officialmap.presentation.OfficialMapsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExploreViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sampleMap(id: Long) = CommunityMap(
        id = id,
        title = "지도$id",
        imageUrl = null,
        hashtags = emptyList(),
        memberCount = 0,
        placeCount = 0,
        joined = false,
    )

    private fun sampleOfficialMap(id: Long) = OfficialMap(
        id = id,
        title = "공식지도$id",
        description = "설명",
        imageUrl = null,
        memberCount = 0,
        placeCount = 0,
        joined = false,
    )

    /** 목록 조회 한 번. 탐색 탭은 늘 전체·인기순 첫 페이지 3개를 부른다. */
    private data class Call(val tag: String?, val sort: CommunityMapSort, val page: Int, val size: Int)

    /**
     * 호출될 때마다 조건을 기록하고 [result] 가 만든 값을 돌려준다.
     *
     * [responseDelayMillis] 를 두면 요청이 진행 중인 상태를 만들 수 있다. 취소를 검증하려면
     * 앞선 요청이 실제로 시작해 매달려 있어야 한다.
     */
    private class FakeRepository(
        val responseDelayMillis: Long = 0L,
        // trailing lambda 가 목록을 뜻하도록 맨 뒤에 둔다. 대부분의 테스트가 그 형태로 쓴다.
        var result: () -> List<CommunityMap> = { emptyList() },
    ) : CommunityMapRepository {
        val calls = mutableListOf<Call>()

        override suspend fun getCommunityMaps(
            tag: String?,
            sort: CommunityMapSort,
            page: Int,
            size: Int,
        ): CommunityMapPage {
            calls += Call(tag, sort, page, size)
            delay(responseDelayMillis)
            return CommunityMapPage(maps = result(), isLast = true)
        }

        override suspend fun getRecommendedMaps(): List<CommunityMap> =
            throw UnsupportedOperationException("홈은 맞춤 추천을 부르지 않는다")
    }

    private class FakeOfficialMapRepository(
        var result: () -> List<OfficialMap> = { emptyList() },
    ) : OfficialMapRepository {
        var calls = 0

        override suspend fun getOfficialMaps(): List<OfficialMap> {
            calls++
            return result()
        }
    }

    private fun viewModel(
        repository: CommunityMapRepository,
        officialMapRepository: OfficialMapRepository = FakeOfficialMapRepository(),
    ) = ExploreViewModel(repository, officialMapRepository)

    @Test
    fun `인기순 앞 3개만 태그 없이 조회한다`() = runTest {
        val repository = FakeRepository { listOf(sampleMap(1L)) }

        val viewModel = viewModel(repository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()

        // 나머지와 칩·정렬은 전체보기에서 본다.
        assertEquals(
            listOf(Call(tag = null, sort = CommunityMapSort.POPULAR, page = 0, size = EXPLORE_COMMUNITY_MAP_COUNT)),
            repository.calls,
        )
        assertEquals(3, EXPLORE_COMMUNITY_MAP_COUNT)
        val state = viewModel.uiState.value.communityMaps
        assertTrue(state is CommunityMapsState.Success)
        assertEquals(1, (state as CommunityMapsState.Success).maps.size)
    }

    @Test
    fun `만들어지기만 하면 조회하지 않는다`() = runTest {
        val repository = FakeRepository()
        val officialMapRepository = FakeOfficialMapRepository()

        viewModel(repository, officialMapRepository)
        dispatcher.scheduler.advanceUntilIdle()

        // 화면이 보일 때 refresh 가 첫 조회를 겸한다. init 에서도 읽으면 요청이 두 번 나간다.
        assertTrue(repository.calls.isEmpty())
        assertEquals(0, officialMapRepository.calls)
    }

    @Test
    fun `돌아와서 다시 읽는 동안에는 보던 목록이 남는다`() = runTest {
        val repository = FakeRepository(responseDelayMillis = 100L) { listOf(sampleMap(1L)) }
        val viewModel = viewModel(repository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.refresh()
        dispatcher.scheduler.runCurrent()

        // Loading 으로 되돌리면 돌아올 때마다 목록이 사라졌다 나타난다.
        assertTrue(viewModel.uiState.value.communityMaps is CommunityMapsState.Success)
    }

    @Test
    fun `새로고침이 실패해도 보던 목록을 지우지 않는다`() = runTest {
        var fail = false
        val repository = FakeRepository {
            if (fail) throw RuntimeException("boom") else listOf(sampleMap(1L))
        }
        val viewModel = viewModel(repository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()

        fail = true
        viewModel.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.communityMaps is CommunityMapsState.Success)
    }

    @Test
    fun `실패하면 Error 상태가 되고 재시도로 복구한다`() = runTest {
        var fail = true
        val repository = FakeRepository {
            if (fail) throw RuntimeException("boom") else listOf(sampleMap(1L))
        }
        val viewModel = viewModel(repository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.communityMaps is CommunityMapsState.Error)

        fail = false
        viewModel.retryCommunityMaps()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.communityMaps is CommunityMapsState.Success)
    }

    @Test
    fun `진행 중인 요청이 취소돼도 오류로 새지 않고 마지막 요청 결과만 남는다`() = runTest {
        val repository = FakeRepository(responseDelayMillis = 100L) { listOf(sampleMap(1L)) }
        val viewModel = viewModel(repository).apply { refresh() }
        // 응답을 기다리는 지점까지만 진행시켜 첫 요청을 실제로 매달아 둔다.
        dispatcher.scheduler.advanceTimeBy(50L)

        viewModel.retryCommunityMaps()
        // 가상 시간을 넘기지 않고 지금 큐에 있는 것만 실행한다. 취소된 첫 요청은 여기서 깨어나고,
        // 다시 보낸 요청은 아직 응답을 기다리는 중이다. 취소가 Error 로 새면 이 시점에 드러난다.
        dispatcher.scheduler.runCurrent()
        assertEquals(CommunityMapsState.Loading, viewModel.uiState.value.communityMaps)

        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.communityMaps is CommunityMapsState.Success)
        assertEquals(2, repository.calls.size)
    }

    @Test
    fun `공식지도는 앞 5개만 쓴다`() = runTest {
        val officialMapRepository = FakeOfficialMapRepository { (1L..7L).map(::sampleOfficialMap) }

        val viewModel = viewModel(FakeRepository(), officialMapRepository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value.officialMaps as OfficialMapsState.Success
        assertEquals((1L..5L).toList(), state.maps.map { it.id })
    }

    @Test
    fun `공식지도만 실패하면 커뮤니티 목록은 그대로 뜨고 공식지도 재시도로 복구한다`() = runTest {
        var fail = true
        val officialMapRepository = FakeOfficialMapRepository {
            if (fail) throw RuntimeException("boom") else listOf(sampleOfficialMap(1L))
        }
        val viewModel = viewModel(FakeRepository { listOf(sampleMap(1L)) }, officialMapRepository)
            .apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.communityMaps is CommunityMapsState.Success)
        assertTrue(viewModel.uiState.value.officialMaps is OfficialMapsState.Error)

        fail = false
        viewModel.retryOfficialMaps()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.officialMaps is OfficialMapsState.Success)
    }

    @Test
    fun `커뮤니티 재시도는 공식지도를 다시 읽지 않는다`() = runTest {
        val officialMapRepository = FakeOfficialMapRepository { listOf(sampleOfficialMap(1L)) }
        val viewModel = viewModel(FakeRepository(), officialMapRepository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.retryCommunityMaps()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, officialMapRepository.calls)
    }

    @Test
    fun `공식지도 새로고침이 실패해도 보던 카드를 지우지 않는다`() = runTest {
        var fail = false
        val officialMapRepository = FakeOfficialMapRepository {
            if (fail) throw RuntimeException("boom") else listOf(sampleOfficialMap(1L))
        }
        val viewModel = viewModel(FakeRepository(), officialMapRepository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()

        fail = true
        viewModel.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.officialMaps is OfficialMapsState.Success)
    }
}
