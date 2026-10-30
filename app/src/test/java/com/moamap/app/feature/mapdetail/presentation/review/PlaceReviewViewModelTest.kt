package com.moamap.app.feature.mapdetail.presentation.review

import android.net.Uri
import com.moamap.app.core.auth.FakeCurrentUserStore
import com.moamap.app.core.common.upload.ImageUploadException
import com.moamap.app.core.network.ApiException
import com.moamap.app.feature.mapdetail.domain.model.PlaceReview
import com.moamap.app.feature.mapdetail.domain.model.ReportReason
import com.moamap.app.feature.mapdetail.domain.repository.PlaceReviewRepository
import com.moamap.app.feature.mypage.domain.BlockedUserStore
import com.moamap.app.feature.mypage.domain.model.BlockedUser
import com.moamap.app.feature.mypage.domain.repository.BlockRepository
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

    override suspend fun deleteReview(placeId: Long, reviewId: Long) {
        calls += "deleteReview($placeId, $reviewId)"
        delay(responseDelayMillis)
    }

    var reportFailure: Throwable? = null

    override suspend fun reportReview(placeId: Long, reviewId: Long, reason: ReportReason, detail: String) {
        calls += "reportReview($placeId, $reviewId, $reason, $detail)"
        delay(responseDelayMillis)
        reportFailure?.let { throw it }
    }
}

/** 차단만 흉내 낸다. 처음 목록은 [initial] 이다. */
private class FakeBlockRepository(private val initial: List<Long> = emptyList()) : BlockRepository {
    val blocked = mutableListOf<Long>()
    var blockFailure: Throwable? = null

    override suspend fun getBlockedUsers() = initial.map { id -> BlockedUser(id, "차단$id", null) }

    override suspend fun block(userId: Long) {
        blockFailure?.let { throw it }
        blocked += userId
    }

    override suspend fun unblock(userId: Long) = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class PlaceReviewViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val blockRepository = FakeBlockRepository(initial = listOf(9L))

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
        val viewModel = PlaceReviewViewModel(repository, FakeCurrentUserStore(), BlockedUserStore(blockRepository))

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
        val viewModel = PlaceReviewViewModel(repository, FakeCurrentUserStore(), BlockedUserStore(blockRepository))

        viewModel.open(placeId = 7L)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.open(placeId = 7L)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("getReviews(7)"), repository.calls)
    }

    @Test
    fun `닫았다 열면 서버에서 다시 읽는다`() = runTest {
        val repository = FakePlaceReviewRepository()
        val viewModel = PlaceReviewViewModel(repository, FakeCurrentUserStore(), BlockedUserStore(blockRepository))

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
        val viewModel = PlaceReviewViewModel(repository, FakeCurrentUserStore(), BlockedUserStore(blockRepository))

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
        val viewModel = PlaceReviewViewModel(repository, FakeCurrentUserStore(), BlockedUserStore(blockRepository))

        viewModel.open(placeId = 7L)
        viewModel.open(placeId = 8L)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(8L, viewModel.uiState.value.placeId)
        assertEquals(listOf(8L), viewModel.uiState.value.reviews.map { review -> review.id })
    }

    @Test
    fun `글도 사진도 없으면 보내지 않는다`() = runTest {
        val repository = FakePlaceReviewRepository()
        val viewModel = PlaceReviewViewModel(repository, FakeCurrentUserStore(), BlockedUserStore(blockRepository))
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
        val viewModel = PlaceReviewViewModel(repository, FakeCurrentUserStore(), BlockedUserStore(blockRepository))
        viewModel.open(placeId = 7L)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.submit(content = "좋았어요", photo = null)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("사진 크기는 5MB 이하여야 해요", viewModel.uiState.value.submitErrorMessage)
    }

    @Test
    fun `보내고 나면 목록을 다시 읽고 작성 수를 올린다`() = runTest {
        val repository = FakePlaceReviewRepository()
        val viewModel = PlaceReviewViewModel(repository, FakeCurrentUserStore(), BlockedUserStore(blockRepository))
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
        val viewModel = PlaceReviewViewModel(repository, FakeCurrentUserStore(), BlockedUserStore(blockRepository))
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
        val viewModel = PlaceReviewViewModel(repository, FakeCurrentUserStore(), BlockedUserStore(blockRepository))
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
        val viewModel = PlaceReviewViewModel(repository, FakeCurrentUserStore(), BlockedUserStore(blockRepository))
        viewModel.open(placeId = 7L)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.submit(content = "좋았어요", photo = null)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(REVIEW_SUBMIT_FAILED_MESSAGE, viewModel.uiState.value.submitErrorMessage)
        assertEquals(listOf(1L), viewModel.uiState.value.reviews.map { review -> review.id })
    }

    // ---------- 내 후기 삭제, 남의 후기 신고·차단 ----------

    /** 1번은 내 후기(작성자 1), 2번은 남의 후기다. */
    private fun openedAsAuthor(
        repository: FakePlaceReviewRepository = FakePlaceReviewRepository(
            reviews = { listOf(testReview(1L), testReview(2L)) },
        ),
    ): PlaceReviewViewModel {
        val viewModel = PlaceReviewViewModel(repository, FakeCurrentUserStore(initial = 1L), BlockedUserStore(blockRepository))
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
    fun `남의 후기는 지우지 않는다`() = runTest {
        val repository = FakePlaceReviewRepository(reviews = { listOf(testReview(1L), testReview(2L)) })
        val viewModel = openedAsAuthor(repository)

        viewModel.delete(2L)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(repository.calls.none { call -> call.startsWith("deleteReview") })
    }

    @Test
    fun `지운 뒤 토스트를 띄운다`() = runTest {
        val viewModel = openedAsAuthor()

        viewModel.delete(1L)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(REVIEW_DELETED_TOAST, viewModel.uiState.value.toast)
        viewModel.consumeToast()
        assertNull(viewModel.uiState.value.toast)
    }

    @Test
    fun `남의 후기를 신고하면 사유를 보내고 접수 토스트를 띄운다`() = runTest {
        val repository = FakePlaceReviewRepository(reviews = { listOf(testReview(1L), testReview(2L)) })
        val viewModel = openedAsAuthor(repository)

        viewModel.report(2L, ReportReason.Abuse, "")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue("reportReview(7, 2, Abuse, )" in repository.calls)
        assertEquals(REPORTED_TOAST, viewModel.uiState.value.toast)
    }

    @Test
    fun `내 후기와 내용 없는 기타 신고는 보내지 않는다`() = runTest {
        val repository = FakePlaceReviewRepository(reviews = { listOf(testReview(1L), testReview(2L)) })
        val viewModel = openedAsAuthor(repository)

        viewModel.report(1L, ReportReason.Spam, "")
        viewModel.report(2L, ReportReason.Other, "  ")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(repository.calls.none { call -> call.startsWith("reportReview") })
    }

    @Test
    fun `이미 신고했으면 서버 원문 대신 정한 문구를 띄운다`() = runTest {
        val repository = FakePlaceReviewRepository(reviews = { listOf(testReview(1L), testReview(2L)) })
            .apply { reportFailure = ApiException(code = "PLACE_019", status = 409, serverMessage = "이미 신고한 댓글입니다.") }
        val viewModel = openedAsAuthor(repository)

        viewModel.report(2L, ReportReason.Spam, "")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("이미 신고했어요", viewModel.uiState.value.toast)
        assertFalse(viewModel.uiState.value.moderating)
    }

    @Test
    fun `차단하면 작성자를 차단 목록에 바로 넣고 토스트를 띄운다`() = runTest {
        val viewModel = openedAsAuthor()

        viewModel.block(2L)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(2L), blockRepository.blocked)
        // 다시 읽지 않아도 지도 상세가 바로 거른다.
        assertEquals(setOf(9L, 2L), viewModel.blockedUserIds.value)
        assertEquals("작성자2님을 차단했어요", viewModel.uiState.value.toast)
    }

    @Test
    fun `내 후기 작성자는 차단하지 않고 실패하면 정한 문구를 띄운다`() = runTest {
        val viewModel = openedAsAuthor()

        viewModel.block(1L)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(blockRepository.blocked.isEmpty())

        blockRepository.blockFailure = ApiException(code = "USER_026", status = 400, serverMessage = "")
        viewModel.block(2L)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("나 자신은 차단할 수 없어요", viewModel.uiState.value.toast)
        assertFalse(2L in viewModel.blockedUserIds.value)
    }

    @Test
    fun `처리 중에는 다른 삭제·신고·차단을 받지 않는다`() = runTest {
        val repository = FakePlaceReviewRepository(
            responseDelayMillis = 100L,
            reviews = { listOf(testReview(1L), testReview(2L)) },
        )
        val viewModel = openedAsAuthor(repository)

        viewModel.report(2L, ReportReason.Spam, "")
        viewModel.delete(1L)
        viewModel.block(2L)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.calls.count { call -> call.startsWith("reportReview") })
        assertTrue(repository.calls.none { call -> call.startsWith("deleteReview") })
        assertTrue(blockRepository.blocked.isEmpty())
    }

    @Test
    fun `열면 차단 목록을 서버에서 받아 둔다`() = runTest {
        val viewModel = openedAsAuthor()

        assertEquals(setOf(9L), viewModel.blockedUserIds.value)
    }
}
