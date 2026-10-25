package com.moamap.app.feature.notification.presentation

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moamap.app.core.common.toUserMessage
import com.moamap.app.feature.notification.domain.model.UserNotification
import com.moamap.app.feature.notification.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "NotificationListVM"

/** 한 번에 받는 수. 서버 기본값과 같다. */
internal const val NOTIFICATION_PAGE_SIZE = 20

internal const val NOTIFICATION_LOAD_FAILED_MESSAGE = "알림을 불러오지 못했어요"

/**
 * 알림 목록 상태. 첫 페이지 실패([errorMessage])와 다음 페이지 실패([loadMoreFailed])를 나눈다. 공지사항과 같다.
 */
@Immutable
data class NotificationListUiState(
    val loading: Boolean = true,
    val notifications: List<UserNotification> = emptyList(),
    val errorMessage: String? = null,
    val loadingMore: Boolean = false,
    val loadMoreFailed: Boolean = false,
    /** 마지막 페이지까지 받았다. */
    val endReached: Boolean = false,
)

@HiltViewModel
class NotificationListViewModel @Inject constructor(
    private val repository: NotificationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationListUiState())
    val uiState: StateFlow<NotificationListUiState> = _uiState.asStateFlow()

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
                val result = repository.getNotifications(page = page, size = NOTIFICATION_PAGE_SIZE)
                nextPage = page + 1
                _uiState.update { current ->
                    // 받는 사이 새 알림이 생기면 페이지 경계가 밀려 앞 페이지의 알림이 한 번 더 온다.
                    val known = current.notifications.mapTo(HashSet()) { notification -> notification.id }
                    current.copy(
                        notifications = current.notifications +
                            result.notifications.filterNot { notification -> notification.id in known },
                        loadingMore = false,
                        endReached = result.isLast,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "알림 다음 페이지 조회 실패 (page=$page)", e)
                _uiState.update { current -> current.copy(loadingMore = false, loadMoreFailed = true) }
            }
        }
    }

    /**
     * 알림을 눌렀다. 화면을 바로 읽음으로 바꾸고 서버에 알린다.
     *
     * 실패해도 되돌리지 않는다 - 이미 다른 화면으로 넘어간 뒤라 보이지 않고, 다음에 열면 서버 상태로 돌아온다.
     *
     * @return 갈 곳. null 이면 읽음으로만 바꾼다.
     */
    fun open(notification: UserNotification): NotificationDestination? {
        if (!notification.read) {
            setRead { item -> item.id == notification.id }
            viewModelScope.launch {
                try {
                    repository.markRead(notification.id)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "알림 읽음 실패 (id=${notification.id})", e)
                }
            }
        }
        return notification.destination()
    }

    /** 모두 읽음. 화면을 먼저 바꾸고, 실패하면 보던 목록으로 되돌린다. */
    fun markAllRead() {
        val before = _uiState.value.notifications
        if (before.all { notification -> notification.read }) return
        setRead { true }
        viewModelScope.launch {
            try {
                repository.markAllRead()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "모두 읽음 실패", e)
                _uiState.update { current -> current.copy(notifications = before) }
            }
        }
    }

    private fun setRead(predicate: (UserNotification) -> Boolean) {
        _uiState.update { current ->
            current.copy(
                notifications = current.notifications.map { item ->
                    if (!item.read && predicate(item)) item.copy(read = true) else item
                },
            )
        }
    }

    private fun loadFirstPage() {
        _uiState.value = NotificationListUiState()
        nextPage = 0
        viewModelScope.launch {
            try {
                val result = repository.getNotifications(page = 0, size = NOTIFICATION_PAGE_SIZE)
                nextPage = 1
                _uiState.update { current ->
                    current.copy(loading = false, notifications = result.notifications, endReached = result.isLast)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "알림 목록 조회 실패", e)
                _uiState.update { current ->
                    current.copy(loading = false, errorMessage = e.toUserMessage(NOTIFICATION_LOAD_FAILED_MESSAGE))
                }
            }
        }
    }
}
