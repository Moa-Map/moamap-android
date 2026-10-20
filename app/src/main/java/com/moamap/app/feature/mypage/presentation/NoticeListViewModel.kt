package com.moamap.app.feature.mypage.presentation

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moamap.app.core.common.toUserMessage
import com.moamap.app.feature.mypage.domain.model.Notice
import com.moamap.app.feature.mypage.domain.repository.NoticeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "NoticeListVM"

/** 한 번에 받는 수. 서버 기본값과 같다. */
internal const val NOTICE_PAGE_SIZE = 20

internal const val NOTICE_LOAD_FAILED_MESSAGE = "공지사항을 불러오지 못했어요"

/**
 * 공지사항 목록 상태.
 *
 * 첫 페이지 실패([errorMessage])와 다음 페이지 실패([loadMoreFailed])를 나눈다. 커뮤니티 지도 전체보기와 같다.
 */
@Immutable
data class NoticeListUiState(
    val loading: Boolean = true,
    val notices: List<Notice> = emptyList(),
    val errorMessage: String? = null,
    val loadingMore: Boolean = false,
    val loadMoreFailed: Boolean = false,
    /** 마지막 페이지까지 받았다. */
    val endReached: Boolean = false,
)

@HiltViewModel
class NoticeListViewModel @Inject constructor(
    private val repository: NoticeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoticeListUiState())
    val uiState: StateFlow<NoticeListUiState> = _uiState.asStateFlow()

    /** 다음에 받을 페이지 번호. */
    private var nextPage = 0

    init {
        loadFirstPage()
    }

    fun retry() = loadFirstPage()

    /**
     * 스크롤이 끝에 가까워졌다.
     *
     * 첫 페이지를 받는 중이거나 실패했으면 부르지 않는다. 그때 다음 페이지를 받으면 첫 페이지 없이 둘째 페이지만 붙는다.
     */
    fun loadMore() {
        val state = _uiState.value
        if (state.loading || state.loadingMore || state.endReached || state.errorMessage != null) return

        _uiState.update { current -> current.copy(loadingMore = true, loadMoreFailed = false) }
        val page = nextPage
        viewModelScope.launch {
            try {
                val result = repository.getNotices(page = page, size = NOTICE_PAGE_SIZE)
                nextPage = page + 1
                _uiState.update { current ->
                    // 받는 사이 새 공지가 올라오면 페이지 경계가 밀려 앞 페이지의 공지가 한 번 더 온다.
                    val known = current.notices.mapTo(HashSet()) { notice -> notice.id }
                    current.copy(
                        notices = current.notices + result.notices.filterNot { notice -> notice.id in known },
                        loadingMore = false,
                        endReached = result.isLast,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "공지사항 다음 페이지 조회 실패 (page=$page)", e)
                _uiState.update { current -> current.copy(loadingMore = false, loadMoreFailed = true) }
            }
        }
    }

    private fun loadFirstPage() {
        _uiState.value = NoticeListUiState()
        nextPage = 0
        viewModelScope.launch {
            try {
                val result = repository.getNotices(page = 0, size = NOTICE_PAGE_SIZE)
                nextPage = 1
                _uiState.update { current ->
                    current.copy(loading = false, notices = result.notices, endReached = result.isLast)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "공지사항 목록 조회 실패", e)
                _uiState.update { current ->
                    current.copy(loading = false, errorMessage = e.toUserMessage(NOTICE_LOAD_FAILED_MESSAGE))
                }
            }
        }
    }
}
