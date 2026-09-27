package com.moamap.app.feature.mapdetail.presentation.review

import android.net.Uri
import com.moamap.app.core.auth.FakeCurrentUserStore
import com.moamap.app.core.common.upload.ImageUploadException
import com.moamap.app.core.network.ApiException
import com.moamap.app.feature.mapdetail.domain.model.PlaceReview
import com.moamap.app.feature.mapdetail.domain.repository.PlaceReviewRepository
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

private fun testReview(id: Long, imageUrls: List<String> = emptyList()) = PlaceReview(
    id = id,
    authorId = id,
    authorName = "작성자$id",
    content = "후기$id",
    imageUrls = imageUrls,
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

    override suspend fun createReview(placeId: Long, content: String, photo: Uri?) {
        calls += "createReview($placeId, $content, $photo)"
        delay(responseDelayMillis)
        onCreate()
    }

    /** 수정을 실패시켜야 하는 테스트가 있다. */
    var updateFailure: Throwable? = null

    override suspend fun updateReview(placeId: Long, reviewId: Long, content: String) {
        calls += "updateReview($placeId, $reviewId, $content)"
        delay(responseDelayMillis)
        updateFailure?.let { throw it }
    }

    override suspend fun deleteReview(placeId: Long, reviewId: Long) {
        calls += "deleteReview($placeId, $reviewId)"
        delay(responseDelayMillis)
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
        val viewModel = PlaceReviewViewModel(repository, FakeCurrentUserStore())

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
        val viewModel = PlaceReviewViewModel(repository, FakeCurrentUserStore())

        viewModel.open(placeId = 7L)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.open(placeId = 7L)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("getReviews(7)"), repository.calls)
    }

    @Test
    fun `닫았다 열면 서버에서 다시 읽는다`() = runTest {
        val repository = FakePlaceReviewRepository()
        val viewModel = PlaceReviewViewModel(repository, FakeCurrentUserStore())

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
        val viewModel = PlaceReviewViewModel(repository, FakeCurrentUserStore())

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
        val viewModel = PlaceReviewViewModel(repository, FakeCurrentUserStore())

        viewModel.open(placeId = 7L)
        viewModel.open(placeId = 8L)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(8L, viewModel.uiState.value.placeId)
        assertEquals(listOf(8L), viewModel.uiState.value.reviews.map { review -> review.id })
    }

    @Test
    fun `글도 사진도 없으면 보내지 않는다`() = runTest {
        val repository = FakePlaceReviewRepository()
        val viewModel = PlaceReviewViewModel(repository, FakeCurrentUserStore())
        viewModel.open(placeId = 7L)
        dispatcher.scheduler.advanceUntilIdle()
        repository.calls.clear()

        assertFalse(viewModel.submit(content = "   ", photo = null))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(emptyList<String>(), repository.calls)
        assertEquals(REVIEW_EMPTY_MESSAGE, viewModel.uiState.value.submitErrorMessage)
    }

    @Test
    fun `사진 형식·크기 안내는 그대로 보여준다`() = runTest {
        val repository = FakePlaceReviewRepository(
            onCreate = { throw ImageUploadException.TooLarge(5L * 1024 * 1024) },
        )
        val viewModel = PlaceReviewViewModel(repository, FakeCurrentUserStore())
        viewModel.open(placeId = 7L)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.submit(content = "좋았어요", photo = null)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("사진 크기는 5MB 이하여야 해요", viewModel.uiState.value.submitErrorMessage)
    }

    @Test
    fun `보내고 나면 목록을 다시 읽고 작성 수를 올린다`() = runTest {
        val repository = FakePlaceReviewRepository()
        val viewModel = PlaceReviewViewModel(repository, FakeCurrentUserStore())
        viewModel.open(placeId = 7L)
        dispatcher.scheduler.advanceUntilIdle()
        repository.calls.clear()

        assertTrue(viewModel.submit(content = " 좋았어요 ", photo = null))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("createReview(7, 좋았어요, null)", "getReviews(7)"), repository.calls)
        assertEquals(1, viewModel.uiState.value.submittedCount)
        assertFalse(viewModel.uiState.value.submitting)
        assertNull(viewModel.uiState.value.submitErrorMessage)
    }

    @Test
    fun `보내는 중에는 두 번째 전송을 받지 않는다`() = runTest {
        val repository = FakePlaceReviewRepository(responseDelayMillis = 100L)
        val viewModel = PlaceReviewViewModel(repository, FakeCurrentUserStore())
        viewModel.open(placeId = 7L)
        dispatcher.scheduler.advanceUntilIdle()
        repository.calls.clear()

        assertTrue(viewModel.submit(content = "좋았어요", photo = null))
        assertFalse(viewModel.submit(content = "또 왔어요", photo = null))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.calls.count { call -> call.startsWith("createReview") })
        assertEquals(1, viewModel.uiState.value.submittedCount)
    }

    @Test
    fun `멤버가 아니면 왜 안 되는지 알려준다`() = runTest {
        val repository = FakePlaceReviewRepository(
            onCreate = { throw ApiException(code = "PLACE_002", status = 403, serverMessage = "not a member") },
        )
        val viewModel = PlaceReviewViewModel(repository, FakeCurrentUserStore())
        viewModel.open(placeId = 7L)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.submit(content = "좋았어요", photo = null)
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
        val viewModel = PlaceReviewViewModel(repository, FakeCurrentUserStore())
        viewModel.open(placeId = 7L)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.submit(content = "좋았어요", photo = null)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(REVIEW_SUBMIT_FAILED_MESSAGE, viewModel.uiState.value.submitErrorMessage)
        assertEquals(listOf(1L), viewModel.uiState.value.reviews.map { review -> review.id })
    }

    // ---------- 내 후기 수정·삭제 ----------

    /** 1번은 내 후기(작성자 1), 2번은 남의 후기다. */
    private fun openedAsAuthor(
        repository: FakePlaceReviewRepository = FakePlaceReviewRepository(
            reviews = { listOf(testReview(1L), testReview(2L)) },
        ),
    ): PlaceReviewViewModel {
        val viewModel = PlaceReviewViewModel(repository, FakeCurrentUserStore(initial = 1L))
        viewModel.open(7L)
        dispatcher.scheduler.advanceUntilIdle()
        return viewModel
    }

    @Test
    fun `로그인한 사람이 쓴 후기만 내 것으로 본다`() = runTest {
        val viewModel = openedAsAuthor()

        assertEquals(1L, viewModel.uiState.value.myUserId)
        assertTrue(testReview(1L).isMine(1L))
        assertFalse(testReview(2L).isMine(1L))
        // 누군지 모르면 아무것도 내 것이 아니다. 서버가 거절할 버튼을 띄우지 않는다.
        assertFalse(testReview(1L).isMine(null))
    }

    @Test
    fun `내 후기를 고쳐 보내면 글만 수정하고 목록을 다시 읽는다`() = runTest {
        val repository = FakePlaceReviewRepository(reviews = { listOf(testReview(1L), testReview(2L)) })
        val viewModel = openedAsAuthor(repository)

        viewModel.startEdit(1L)
        assertEquals(1L, viewModel.uiState.value.editingReviewId)

        assertTrue(viewModel.submit(" 고친 글 ", photo = null))
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue("updateReview(7, 1, 고친 글)" in repository.calls)
        assertTrue(repository.calls.none { call -> call.startsWith("createReview") })
        assertNull(viewModel.uiState.value.editingReviewId)
        assertEquals(1, viewModel.uiState.value.submittedCount)
        assertEquals(2, repository.calls.count { call -> call.startsWith("getReviews") })
    }

    @Test
    fun `남의 후기는 고칠 수 없다`() = runTest {
        val viewModel = openedAsAuthor()

        viewModel.startEdit(2L)

        assertNull(viewModel.uiState.value.editingReviewId)
    }

    @Test
    fun `사진 없는 후기를 빈 글로 고치면 막는다`() = runTest {
        val repository = FakePlaceReviewRepository(reviews = { listOf(testReview(1L)) })
        val viewModel = openedAsAuthor(repository)
        viewModel.startEdit(1L)

        assertFalse(viewModel.submit("   ", photo = null))

        assertEquals(REVIEW_EMPTY_MESSAGE, viewModel.uiState.value.submitErrorMessage)
        assertTrue(repository.calls.none { call -> call.startsWith("updateReview") })
    }

    @Test
    fun `사진 있는 후기는 글을 비워 고칠 수 있다`() = runTest {
        val repository = FakePlaceReviewRepository(
            reviews = { listOf(testReview(1L, imageUrls = listOf("https://cdn/1.jpg"))) },
        )
        val viewModel = openedAsAuthor(repository)
        viewModel.startEdit(1L)

        assertTrue(viewModel.submit("", photo = null))
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue("updateReview(7, 1, )" in repository.calls)
    }

    @Test
    fun `고치기를 취소하면 새 글 쓰기로 돌아간다`() = runTest {
        val repository = FakePlaceReviewRepository(reviews = { listOf(testReview(1L)) })
        val viewModel = openedAsAuthor(repository)
        viewModel.startEdit(1L)

        viewModel.cancelEdit()
        viewModel.submit("새 글", photo = null)
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.editingReviewId)
        assertTrue(repository.calls.any { call -> call.startsWith("createReview(7, 새 글") })
    }

    @Test
    fun `수정에 실패하면 안내하고 고치기를 이어 간다`() = runTest {
        val repository = FakePlaceReviewRepository(reviews = { listOf(testReview(1L)) })
            .apply { updateFailure = RuntimeException("boom") }
        val viewModel = openedAsAuthor(repository)
        viewModel.startEdit(1L)

        viewModel.submit("고친 글", photo = null)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(REVIEW_UPDATE_FAILED_MESSAGE, viewModel.uiState.value.submitErrorMessage)
        assertEquals(1L, viewModel.uiState.value.editingReviewId)
    }

    @Test
    fun `내 후기를 지우면 목록을 다시 읽고 지운 수를 알린다`() = runTest {
        val repository = FakePlaceReviewRepository(reviews = { listOf(testReview(1L), testReview(2L)) })
        val viewModel = openedAsAuthor(repository)

        viewModel.delete(1L)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue("deleteReview(7, 1)" in repository.calls)
        assertEquals(1, viewModel.uiState.value.deletedCount)
        // 지우기는 적고 있던 글을 비우지 않는다.
        assertEquals(0, viewModel.uiState.value.submittedCount)
        assertEquals(2, repository.calls.count { call -> call.startsWith("getReviews") })
    }

    @Test
    fun `고치던 후기를 지우면 고치기도 끝난다`() = runTest {
        val viewModel = openedAsAuthor()
        viewModel.startEdit(1L)

        viewModel.delete(1L)
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.editingReviewId)
    }

    @Test
    fun `남의 후기는 지우지 않는다`() = runTest {
        val repository = FakePlaceReviewRepository(reviews = { listOf(testReview(1L), testReview(2L)) })
        val viewModel = openedAsAuthor(repository)

        viewModel.delete(2L)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(repository.calls.none { call -> call.startsWith("deleteReview") })
    }
}
