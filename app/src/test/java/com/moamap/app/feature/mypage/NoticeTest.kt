package com.moamap.app.feature.mypage

import androidx.lifecycle.SavedStateHandle
import com.moamap.app.core.navigation.MoaMapRoute
import com.moamap.app.core.network.ApiException
import com.moamap.app.feature.mypage.data.remote.NoticeDto
import com.moamap.app.feature.mypage.data.repository.toNotice
import com.moamap.app.feature.mypage.domain.model.Notice
import com.moamap.app.feature.mypage.domain.model.NoticePage
import com.moamap.app.feature.mypage.domain.repository.NoticeRepository
import com.moamap.app.feature.mypage.presentation.NOTICE_DELETED_MESSAGE
import com.moamap.app.feature.mypage.presentation.NOTICE_LOAD_FAILED_MESSAGE
import com.moamap.app.feature.mypage.presentation.NoticeDetailViewModel
import com.moamap.app.feature.mypage.presentation.NoticeListViewModel
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
import java.io.IOException
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class NoticeTest {

    private val dispatcher = StandardTestDispatcher()
    private val defaultTimeZone = TimeZone.getDefault()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        TimeZone.setDefault(defaultTimeZone)
    }

    private fun notice(id: Long) = Notice(id = id, title = "공지$id", content = "본문$id", createdAtMillis = null)

    private fun page(ids: LongRange, isLast: Boolean) = NoticePage(notices = ids.map(::notice), isLast = isLast)

    private class FakeRepository(
        var pages: (page: Int) -> NoticePage,
        var detail: (id: Long) -> Notice = { error("상세는 부르지 않는다") },
    ) : NoticeRepository {
        val requestedPages = mutableListOf<Int>()

        override suspend fun getNotices(page: Int, size: Int): NoticePage {
            requestedPages += page
            return pages(page)
        }

        override suspend fun getNotice(id: Long): Notice = detail(id)
    }

    @Test
    fun `다음 페이지를 이어 붙이고 겹친 공지는 한 번만 둔다`() = runTest(dispatcher) {
        val repository = FakeRepository(pages = { page ->
            // 받는 사이 새 공지가 올라와 둘째 페이지가 첫 페이지 끝(20번)부터 다시 온 경우다.
            if (page == 0) page(1L..20L, isLast = false) else page(20L..25L, isLast = true)
        })
        val viewModel = NoticeListViewModel(repository)
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()
        // 끝까지 받았으면 더 부르지 않는다.
        viewModel.loadMore()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals((1L..25L).toList(), state.notices.map { notice -> notice.id })
        assertTrue(state.endReached)
        assertEquals(listOf(0, 1), repository.requestedPages)
    }

    @Test
    fun `첫 페이지가 실패하면 안내하고 다음 페이지는 부르지 않는다`() = runTest(dispatcher) {
        val repository = FakeRepository(pages = { throw IOException("서버 오류") })
        val viewModel = NoticeListViewModel(repository)
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(NOTICE_LOAD_FAILED_MESSAGE, viewModel.uiState.value.errorMessage)
        assertEquals(listOf(0), repository.requestedPages)

        repository.pages = { page(1L..3L, isLast = true) }
        viewModel.retry()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(3, viewModel.uiState.value.notices.size)
    }

    @Test
    fun `다음 페이지가 실패하면 받은 목록은 두고 다시 시도를 띄운다`() = runTest(dispatcher) {
        val repository = FakeRepository(pages = { page ->
            if (page == 0) page(1L..20L, isLast = false) else throw IOException("서버 오류")
        })
        val viewModel = NoticeListViewModel(repository)
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(20, state.notices.size)
        assertTrue(state.loadMoreFailed)
        assertFalse(state.loadingMore)
    }

    @Test
    fun `지워진 공지를 열면 삭제됐다고 안내한다`() = runTest(dispatcher) {
        val repository = FakeRepository(
            pages = { page(1L..1L, isLast = true) },
            detail = { throw ApiException(code = "USER_011", status = 404, serverMessage = "공지를 찾을 수 없습니다.") },
        )
        val viewModel = NoticeDetailViewModel(
            SavedStateHandle(mapOf(MoaMapRoute.NoticeDetail.ARG_NOTICE_ID to 7L)),
            repository,
        )
        advanceUntilIdle()

        assertEquals(NOTICE_DELETED_MESSAGE, viewModel.uiState.value.errorMessage)
    }

    /** 서버는 `Z` 를 붙인 UTC 로 준다. 한국 시간 자정을 넘긴 공지는 다음 날로 보여야 한다. */
    @Test
    fun `등록일은 기기 시간대 날짜로 2026_10_20 처럼 적는다`() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))

        val notice = NoticeDto(noticeId = 1L, title = "점검", content = "본문", createdAt = "2026-10-19T15:30:00.123456Z")
            .toNotice()

        assertEquals("2026.10.20", formatNoticeDate(checkNotNull(notice.createdAtMillis)))
    }
}
