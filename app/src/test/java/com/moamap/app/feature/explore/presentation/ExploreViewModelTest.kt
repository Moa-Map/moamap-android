package com.moamap.app.feature.explore.presentation

import com.moamap.app.feature.explore.domain.model.CommunityMap
import com.moamap.app.feature.explore.domain.model.CommunityMapPage
import com.moamap.app.feature.explore.domain.model.CommunityMapSort
import com.moamap.app.feature.explore.domain.repository.CommunityMapRepository
import com.moamap.app.feature.mypage.domain.model.MyProfile
import com.moamap.app.feature.mypage.domain.repository.UserRepository
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

private fun profileWith(nickname: String) = MyProfile(
    id = 1L,
    nickname = nickname,
    email = "moamap@example.com",
    profileImageUrl = null,
    introduction = "",
)

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

    /** 목록 조회 한 번. 탐색 탭은 늘 전체·인기순 첫 페이지 5개를 부른다. */
    private data class Call(val tag: String?, val sort: CommunityMapSort, val page: Int, val size: Int)

    /**
     * 호출될 때마다 조건을 기록하고 [result] 가 만든 값을 돌려준다.
     *
     * [responseDelayMillis] 를 두면 요청이 진행 중인 상태를 만들 수 있다. 취소를 검증하려면
     * 앞선 요청이 실제로 시작해 매달려 있어야 한다.
     */
    private class FakeRepository(
        val responseDelayMillis: Long = 0L,
        var recommendations: () -> List<CommunityMap> = { emptyList() },
        // trailing lambda 가 목록을 뜻하도록 맨 뒤에 둔다. 대부분의 테스트가 그 형태로 쓴다.
        var result: () -> List<CommunityMap> = { emptyList() },
    ) : CommunityMapRepository {
        val calls = mutableListOf<Call>()
        var recommendationCalls = 0

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

        override suspend fun getRecommendedMaps(): List<CommunityMap> {
            recommendationCalls++
            delay(responseDelayMillis)
            return recommendations()
        }
    }

    /** 이름 조회만 대신한다. 편집은 이 화면이 부르지 않으므로 불리면 그 자체가 실패다. */
    private class FakeUserRepository(
        var profile: () -> MyProfile = { profileWith("모아맵") },
    ) : UserRepository {
        var calls = 0

        override suspend fun getMyProfile(): MyProfile {
            calls++
            return profile()
        }

        override suspend fun uploadProfileImage(imageUri: String): String =
            throw UnsupportedOperationException("탐색 화면은 프로필을 고치지 않는다")

        override suspend fun updateMyProfile(
            nickname: String,
            introduction: String,
            profileImageUrl: String?,
        ): MyProfile = throw UnsupportedOperationException("탐색 화면은 프로필을 고치지 않는다")
    }

    private fun viewModel(
        repository: CommunityMapRepository,
        userRepository: UserRepository = FakeUserRepository(),
    ) = ExploreViewModel(repository, userRepository)

    @Test
    fun `인기순 앞 5개만 태그 없이 조회한다`() = runTest {
        val repository = FakeRepository { listOf(sampleMap(1L)) }

        val viewModel = viewModel(repository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()

        // 나머지와 칩·정렬은 전체보기에서 본다. 첫 화면 순서가 전체보기와 이어지도록 인기순이다.
        assertEquals(
            listOf(Call(tag = null, sort = CommunityMapSort.POPULAR, page = 0, size = EXPLORE_COMMUNITY_MAP_COUNT)),
            repository.calls,
        )
        val state = viewModel.uiState.value.communityMaps
        assertTrue(state is CommunityMapsState.Success)
        assertEquals(1, (state as CommunityMapsState.Success).maps.size)
    }

    @Test
    fun `만들어지기만 하면 조회하지 않는다`() = runTest {
        val repository = FakeRepository()

        viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        // 화면이 보일 때 refresh 가 첫 조회를 겸한다. init 에서도 읽으면 요청이 두 번 나간다.
        assertTrue(repository.calls.isEmpty())
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
    fun `정렬을 바꾸면 그 정렬로 5개를 다시 읽는다`() = runTest {
        val repository = FakeRepository { listOf(sampleMap(1L)) }
        val viewModel = viewModel(repository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.selectSort(CommunityMapSort.LATEST)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(CommunityMapSort.LATEST, viewModel.uiState.value.sort)
        assertEquals(
            Call(tag = null, sort = CommunityMapSort.LATEST, page = 0, size = EXPLORE_COMMUNITY_MAP_COUNT),
            repository.calls.last(),
        )
    }

    @Test
    fun `같은 정렬을 다시 누르면 다시 읽지 않는다`() = runTest {
        val repository = FakeRepository()
        val viewModel = viewModel(repository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.selectSort(CommunityMapSort.POPULAR)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.calls.size)
    }

    @Test
    fun `돌아와서 다시 읽을 때도 고른 정렬을 쓴다`() = runTest {
        val repository = FakeRepository { listOf(sampleMap(1L)) }
        val viewModel = viewModel(repository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.selectSort(CommunityMapSort.LATEST)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(CommunityMapSort.LATEST, repository.calls.last().sort)
    }

    @Test
    fun `실패하면 Error 상태가 되고 retry로 복구한다`() = runTest {
        var fail = true
        val repository = FakeRepository {
            if (fail) throw RuntimeException("boom") else listOf(sampleMap(1L))
        }
        val viewModel = viewModel(repository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.communityMaps is CommunityMapsState.Error)

        fail = false
        viewModel.retry()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.communityMaps is CommunityMapsState.Success)
    }

    @Test
    fun `이름은 만들어질 때 한 번만 읽는다`() = runTest {
        val repository = FakeRepository()
        val userRepository = FakeUserRepository()

        val viewModel = viewModel(repository, userRepository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("모아맵", viewModel.uiState.value.nickname)

        // 화면을 다시 봐도 이름은 다시 읽지 않는다. 목록·추천만 새로 읽는다.
        viewModel.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, userRepository.calls)
    }

    @Test
    fun `이름을 못 읽어도 화면은 그대로 뜬다`() = runTest {
        val repository = FakeRepository(recommendations = { listOf(sampleMap(1L)) })
        val userRepository = FakeUserRepository { throw RuntimeException("boom") }

        val viewModel = viewModel(repository, userRepository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()

        // 빈 이름은 화면이 대체 말로 메운다. 추천은 이름과 무관하게 그려진다.
        assertEquals("", viewModel.uiState.value.nickname)
        assertEquals(1, viewModel.uiState.value.recommendedMaps.size)
    }

    @Test
    fun `추천은 화면을 다시 볼 때 읽고 정렬이나 목록 재시도에는 반응하지 않는다`() = runTest {
        val repository = FakeRepository(recommendations = { listOf(sampleMap(1L)) })
        val viewModel = viewModel(repository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, repository.recommendationCalls)

        viewModel.selectSort(CommunityMapSort.LATEST)
        viewModel.retry()
        dispatcher.scheduler.advanceUntilIdle()

        // 목록만 다시 읽는다. 추천은 정렬과 무관해서 멀쩡한 추천까지 다시 부르면 낭비다.
        assertEquals(1, repository.recommendationCalls)

        viewModel.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, repository.recommendationCalls)
    }

    @Test
    fun `추천 조회가 실패하면 보던 카드를 지우지 않는다`() = runTest {
        var fail = false
        val repository = FakeRepository(
            recommendations = {
                if (fail) throw RuntimeException("boom") else listOf(sampleMap(1L))
            },
        )
        val viewModel = viewModel(repository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.recommendedMaps.size)

        fail = true
        viewModel.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        // 돌아올 때마다 섹션이 사라지면 안 된다. 목록 실패를 다루는 방식과 같다.
        assertEquals(1, viewModel.uiState.value.recommendedMaps.size)
    }

    @Test
    fun `추천이 처음부터 실패하면 섹션을 그릴 것이 없다`() = runTest {
        val repository = FakeRepository(recommendations = { throw RuntimeException("boom") })

        val viewModel = viewModel(repository).apply { refresh() }
        dispatcher.scheduler.advanceUntilIdle()

        // 빈 목록이 곧 "섹션을 숨긴다" 는 신호다. 오류 상태를 따로 두지 않는다.
        assertTrue(viewModel.uiState.value.recommendedMaps.isEmpty())
    }

    @Test
    fun `진행 중인 요청이 취소돼도 오류로 새지 않고 마지막 요청 결과만 남는다`() = runTest {
        val repository = FakeRepository(responseDelayMillis = 100L) { listOf(sampleMap(1L)) }
        val viewModel = viewModel(repository).apply { refresh() }
        // 응답을 기다리는 지점까지만 진행시켜 첫 요청을 실제로 매달아 둔다.
        dispatcher.scheduler.advanceTimeBy(50L)

        viewModel.retry()
        // 가상 시간을 넘기지 않고 지금 큐에 있는 것만 실행한다. 취소된 첫 요청은 여기서 깨어나고,
        // 다시 보낸 요청은 아직 응답을 기다리는 중이다. 취소가 Error 로 새면 이 시점에 드러난다.
        dispatcher.scheduler.runCurrent()
        assertEquals(CommunityMapsState.Loading, viewModel.uiState.value.communityMaps)

        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.communityMaps is CommunityMapsState.Success)
        assertEquals(2, repository.calls.size)
    }
}
