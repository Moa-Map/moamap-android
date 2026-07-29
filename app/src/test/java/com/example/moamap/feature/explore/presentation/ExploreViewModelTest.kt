package com.example.moamap.feature.explore.presentation

import com.example.moamap.feature.explore.domain.model.CommunityMap
import com.example.moamap.feature.explore.domain.model.CommunityMapSort
import com.example.moamap.feature.explore.domain.repository.CommunityMapRepository
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
        joined = false,
    )

    /**
     * 호출될 때마다 (tag, sort) 를 기록하고 [result] 가 만든 값을 돌려준다.
     *
     * [responseDelayMillis] 를 두면 요청이 진행 중인 상태를 만들 수 있다. 취소를 검증하려면
     * 앞선 요청이 실제로 시작해 매달려 있어야 한다.
     */
    private class FakeRepository(
        val responseDelayMillis: Long = 0L,
        var result: () -> List<CommunityMap> = { emptyList() },
    ) : CommunityMapRepository {
        val calls = mutableListOf<Pair<String?, CommunityMapSort>>()

        override suspend fun getCommunityMaps(
            tag: String?,
            sort: CommunityMapSort,
        ): List<CommunityMap> {
            calls += tag to sort
            delay(responseDelayMillis)
            return result()
        }
    }

    @Test
    fun `첫 로드는 전체 태그와 인기순으로 조회한다`() = runTest {
        val repository = FakeRepository { listOf(sampleMap(1L)) }

        val viewModel = ExploreViewModel(repository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(null to CommunityMapSort.POPULAR), repository.calls)
        val state = viewModel.uiState.value.communityMaps
        assertTrue(state is CommunityMapsState.Success)
        assertEquals(1, (state as CommunityMapsState.Success).maps.size)
    }

    @Test
    fun `만들어지기만 하면 조회하지 않는다`() = runTest {
        val repository = FakeRepository()

        ExploreViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        // 화면이 보일 때 refresh 가 첫 조회를 겸한다. init 에서도 읽으면 요청이 두 번 나간다.
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun `돌아와서 다시 읽는 동안에는 보던 목록이 남는다`() = runTest {
        val repository = FakeRepository(responseDelayMillis = 100L) { listOf(sampleMap(1L)) }
        val viewModel = ExploreViewModel(repository).apply { refresh() }
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
        val viewModel = ExploreViewModel(repository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()

        fail = true
        viewModel.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.communityMaps is CommunityMapsState.Success)
    }

    @Test
    fun `전체가 아닌 카테고리는 태그로 넘어간다`() = runTest {
        val repository = FakeRepository()
        val viewModel = ExploreViewModel(repository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.selectCategory("카페")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("카페", viewModel.uiState.value.selectedCategory)
        assertEquals("카페" to CommunityMapSort.POPULAR, repository.calls.last())
    }

    @Test
    fun `전체를 고르면 태그 없이 조회한다`() = runTest {
        val repository = FakeRepository()
        val viewModel = ExploreViewModel(repository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.selectCategory("카페")
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.selectCategory(ALL_CATEGORY)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(null to CommunityMapSort.POPULAR, repository.calls.last())
    }

    @Test
    fun `같은 선택을 다시 누르면 재조회하지 않는다`() = runTest {
        val repository = FakeRepository()
        val viewModel = ExploreViewModel(repository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.selectCategory(ALL_CATEGORY)
        viewModel.selectSort(CommunityMapSort.POPULAR)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.calls.size)
    }

    @Test
    fun `정렬을 바꾸면 해당 정렬로 재조회한다`() = runTest {
        val repository = FakeRepository()
        val viewModel = ExploreViewModel(repository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.selectSort(CommunityMapSort.LATEST)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(CommunityMapSort.LATEST, viewModel.uiState.value.sort)
        assertEquals(null to CommunityMapSort.LATEST, repository.calls.last())
    }

    @Test
    fun `실패하면 Error 상태가 되고 retry로 복구한다`() = runTest {
        var fail = true
        val repository = FakeRepository {
            if (fail) throw RuntimeException("boom") else listOf(sampleMap(1L))
        }
        val viewModel = ExploreViewModel(repository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.communityMaps is CommunityMapsState.Error)

        fail = false
        viewModel.retry()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.communityMaps is CommunityMapsState.Success)
    }

    @Test
    fun `진행 중인 요청이 취소돼도 오류로 새지 않고 마지막 선택 결과만 남는다`() = runTest {
        val repository = FakeRepository(responseDelayMillis = 100L) { listOf(sampleMap(1L)) }
        val viewModel = ExploreViewModel(repository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.selectCategory("카페")
        // 응답을 기다리는 지점까지만 진행시켜 "카페" 요청을 실제로 매달아 둔다.
        dispatcher.scheduler.advanceTimeBy(50L)
        assertEquals("카페" to CommunityMapSort.POPULAR, repository.calls.last())

        viewModel.selectCategory("데이트")
        // 가상 시간을 넘기지 않고 지금 큐에 있는 것만 실행한다. 취소된 "카페" 는 여기서 깨어나고,
        // "데이트" 는 아직 응답을 기다리는 중이다. 취소가 Error 로 새면 이 시점에 드러난다.
        dispatcher.scheduler.runCurrent()
        assertEquals(CommunityMapsState.Loading, viewModel.uiState.value.communityMaps)

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("데이트", viewModel.uiState.value.selectedCategory)
        assertTrue(viewModel.uiState.value.communityMaps is CommunityMapsState.Success)
        assertEquals(
            listOf(
                null to CommunityMapSort.POPULAR,
                "카페" to CommunityMapSort.POPULAR,
                "데이트" to CommunityMapSort.POPULAR,
            ),
            repository.calls,
        )
    }
}
