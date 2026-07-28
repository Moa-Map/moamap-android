package com.example.moamap.feature.explore.presentation

import com.example.moamap.feature.explore.domain.model.CommunityMap
import com.example.moamap.feature.explore.domain.model.CommunityMapSort
import com.example.moamap.feature.explore.domain.repository.CommunityMapRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    /** 호출될 때마다 (tag, sort) 를 기록하고 [result] 가 만든 값을 돌려준다. */
    private class FakeRepository(
        var result: () -> List<CommunityMap> = { emptyList() },
    ) : CommunityMapRepository {
        val calls = mutableListOf<Pair<String?, CommunityMapSort>>()

        override suspend fun getCommunityMaps(
            tag: String?,
            sort: CommunityMapSort,
        ): List<CommunityMap> {
            calls += tag to sort
            return result()
        }
    }

    @Test
    fun `첫 로드는 전체 태그와 인기순으로 조회한다`() = runTest {
        val repository = FakeRepository { listOf(sampleMap(1L)) }

        val viewModel = ExploreViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(null to CommunityMapSort.POPULAR), repository.calls)
        val state = viewModel.uiState.value.communityMaps
        assertTrue(state is CommunityMapsState.Success)
        assertEquals(1, (state as CommunityMapsState.Success).maps.size)
    }

    @Test
    fun `전체가 아닌 카테고리는 태그로 넘어간다`() = runTest {
        val repository = FakeRepository()
        val viewModel = ExploreViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.selectCategory("카페")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("카페", viewModel.uiState.value.selectedCategory)
        assertEquals("카페" to CommunityMapSort.POPULAR, repository.calls.last())
    }

    @Test
    fun `전체를 고르면 태그 없이 조회한다`() = runTest {
        val repository = FakeRepository()
        val viewModel = ExploreViewModel(repository)
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
        val viewModel = ExploreViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.selectCategory(ALL_CATEGORY)
        viewModel.selectSort(CommunityMapSort.POPULAR)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.calls.size)
    }

    @Test
    fun `정렬을 바꾸면 해당 정렬로 재조회한다`() = runTest {
        val repository = FakeRepository()
        val viewModel = ExploreViewModel(repository)
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
        val viewModel = ExploreViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.communityMaps is CommunityMapsState.Error)

        fail = false
        viewModel.retry()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.communityMaps is CommunityMapsState.Success)
    }

    @Test
    fun `칩을 연달아 누르면 마지막 선택 결과만 남는다`() = runTest {
        val repository = FakeRepository { listOf(sampleMap(1L)) }
        val viewModel = ExploreViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        // 앞선 요청이 취소되며 CancellationException 이 나도 Error 로 새지 않아야 한다.
        viewModel.selectCategory("카페")
        viewModel.selectCategory("데이트")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("데이트", viewModel.uiState.value.selectedCategory)
        assertTrue(viewModel.uiState.value.communityMaps is CommunityMapsState.Success)
        assertEquals("데이트" to CommunityMapSort.POPULAR, repository.calls.last())
    }
}
