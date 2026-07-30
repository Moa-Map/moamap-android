package com.example.moamap.feature.mapdetail.presentation.review

import com.example.moamap.core.network.ApiException
import com.example.moamap.feature.mapdetail.domain.model.PlaceReview
import com.example.moamap.feature.mapdetail.domain.repository.PlaceReviewRepository
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

private fun testReview(id: Long) = PlaceReview(
    id = id,
    authorId = id,
    authorName = "작성자$id",
    rating = 5,
    content = "후기$id",
    createdAtMillis = null,
)

/**
 * 호출을 기록하는 가짜 저장소.
 *
 * [responseDelayMillis] 를 두면 요청이 진행 중인 상태를 만들 수 있다. 이중 전송 차단을
 * 검증하려면 앞선 요청이 실제로 매달려 있어야 한다.
 */
private class FakePlaceReviewRepository(
    private val responseDelayMillis: Long = 0L,
    var reviews: (Long) -> List<PlaceReview> = { emptyList() },
    var onCreate: () -> Unit = {},
) : PlaceReviewRepository {

    val calls = mutableListOf<String>()

    override suspend fun getReviews(placeId: Long): List<PlaceReview> {
        calls += "getReviews($placeId)"
        delay(responseDelayMillis)
        return reviews(placeId)
    }

    override suspend fun createReview(placeId: Long, rating: Int, content: String) {
        calls += "createReview($placeId, $rating, $content)"
        delay(responseDelayMillis)
        onCreate()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PlaceReviewViewModelTest {

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
    fun `열면 그 장소의 후기를 읽는다`() = runTest {
        val repository = FakePlaceReviewRepository(reviews = { listOf(testReview(1L), testReview(2L)) })
        val viewModel = PlaceReviewViewModel(repository)

        viewModel.open(placeId = 7L)
        assertTrue(viewModel.uiState.value.loading)

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("getReviews(7)"), repository.calls)
        assertEquals(7L, viewModel.uiState.value.placeId)
        assertFalse(viewModel.uiState.value.loading)
        assertEquals(listOf(1L, 2L), viewModel.uiState.value.reviews.map { review -> review.id })
    }

    @Test
    fun `같은 장소를 다시 열면 받아 둔 목록을 그대로 쓴다`() = runTest {
        val repository = FakePlaceReviewRepository()
        val viewModel = PlaceReviewViewModel(repository)

        viewModel.open(placeId = 7L)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.open(placeId = 7L)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("getReviews(7)"), repository.calls)
    }

    @Test
    fun `닫았다 열면 서버에서 다시 읽는다`() = runTest {
        val repository = FakePlaceReviewRepository()
        val viewModel = PlaceReviewViewModel(repository)

        viewModel.open(placeId = 7L)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.close()
        assertNull(viewModel.uiState.value.placeId)

        viewModel.open(placeId = 7L)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("getReviews(7)", "getReviews(7)"), repository.calls)
    }

    @Test
    fun `조회에 실패하면 안내를 남기고 retry 로 복구한다`() = runTest {
        var fail = true
        val repository = FakePlaceReviewRepository(
            reviews = { if (fail) throw RuntimeException("boom") else listOf(testReview(1L)) },
        )
        val viewModel = PlaceReviewViewModel(repository)

        viewModel.open(placeId = 7L)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(REVIEW_LOAD_FAILED_MESSAGE, viewModel.uiState.value.loadErrorMessage)

        fail = false
        viewModel.retry()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.loadErrorMessage)
        assertEquals(listOf(1L), viewModel.uiState.value.reviews.map { review -> review.id })
    }

    @Test
    fun `늦게 도착한 응답은 다른 장소의 시트를 덮지 않는다`() = runTest {
        val repository = FakePlaceReviewRepository(
            responseDelayMillis = 100L,
            reviews = { placeId -> listOf(testReview(placeId)) },
        )
        val viewModel = PlaceReviewViewModel(repository)

        viewModel.open(placeId = 7L)
        viewModel.open(placeId = 8L)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(8L, viewModel.uiState.value.placeId)
        assertEquals(listOf(8L), viewModel.uiState.value.reviews.map { review -> review.id })
    }

    @Test
    fun `별점을 안 고르면 보내지 않는다`() = runTest {
        val repository = FakePlaceReviewRepository()
        val viewModel = PlaceReviewViewModel(repository)
        viewModel.open(placeId = 7L)
        dispatcher.scheduler.advanceUntilIdle()
        repository.calls.clear()

        assertFalse(viewModel.submit(rating = 0, content = "좋았어요"))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(emptyList<String>(), repository.calls)
        assertEquals(RATING_REQUIRED_MESSAGE, viewModel.uiState.value.submitErrorMessage)
    }

    @Test
    fun `보내고 나면 목록을 다시 읽고 작성 수를 올린다`() = runTest {
        val repository = FakePlaceReviewRepository()
        val viewModel = PlaceReviewViewModel(repository)
        viewModel.open(placeId = 7L)
        dispatcher.scheduler.advanceUntilIdle()
        repository.calls.clear()

        assertTrue(viewModel.submit(rating = 4, content = " 좋았어요 "))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("createReview(7, 4, 좋았어요)", "getReviews(7)"), repository.calls)
        assertEquals(1, viewModel.uiState.value.submittedCount)
        assertFalse(viewModel.uiState.value.submitting)
        assertNull(viewModel.uiState.value.submitErrorMessage)
    }

    @Test
    fun `보내는 중에는 두 번째 전송을 받지 않는다`() = runTest {
        val repository = FakePlaceReviewRepository(responseDelayMillis = 100L)
        val viewModel = PlaceReviewViewModel(repository)
        viewModel.open(placeId = 7L)
        dispatcher.scheduler.advanceUntilIdle()
        repository.calls.clear()

        assertTrue(viewModel.submit(rating = 4, content = "좋았어요"))
        assertFalse(viewModel.submit(rating = 5, content = "또 왔어요"))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.calls.count { call -> call.startsWith("createReview") })
        assertEquals(1, viewModel.uiState.value.submittedCount)
    }

    @Test
    fun `멤버가 아니면 왜 안 되는지 알려준다`() = runTest {
        val repository = FakePlaceReviewRepository(
            onCreate = { throw ApiException(code = "PLACE_002", status = 403, serverMessage = "not a member") },
        )
        val viewModel = PlaceReviewViewModel(repository)
        viewModel.open(placeId = 7L)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.submit(rating = 4, content = "좋았어요")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(NOT_MAP_MEMBER_MESSAGE, viewModel.uiState.value.submitErrorMessage)
        assertEquals(0, viewModel.uiState.value.submittedCount)
    }

    @Test
    fun `작성에 실패해도 목록은 그대로 둔다`() = runTest {
        val repository = FakePlaceReviewRepository(
            reviews = { listOf(testReview(1L)) },
            onCreate = { throw RuntimeException("boom") },
        )
        val viewModel = PlaceReviewViewModel(repository)
        viewModel.open(placeId = 7L)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.submit(rating = 4, content = "좋았어요")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(REVIEW_SUBMIT_FAILED_MESSAGE, viewModel.uiState.value.submitErrorMessage)
        assertEquals(listOf(1L), viewModel.uiState.value.reviews.map { review -> review.id })
    }
}
