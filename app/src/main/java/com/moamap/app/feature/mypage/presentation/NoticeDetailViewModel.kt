package com.moamap.app.feature.mypage.presentation

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moamap.app.core.common.toUserMessage
import com.moamap.app.core.navigation.MoaMapRoute
import com.moamap.app.core.network.ApiException
import com.moamap.app.feature.mypage.domain.model.Notice
import com.moamap.app.feature.mypage.domain.repository.NoticeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import javax.inject.Inject

private const val TAG = "NoticeDetailVM"

/** 목록에서 누른 사이 지워진 공지. 다시 불러와도 같다. */
internal const val NOTICE_DELETED_MESSAGE = "삭제된 공지사항이에요"

@Immutable
data class NoticeDetailUiState(
    val loading: Boolean = true,
    val notice: Notice? = null,
    val errorMessage: String? = null,
)

/** 공지 상세. 목록이 들고 있는 값을 넘기지 않고 다시 읽는다 - 목록을 본 뒤 고쳐졌거나 지워졌을 수 있다. */
@HiltViewModel
class NoticeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: NoticeRepository,
) : ViewModel() {

    private val noticeId: Long = checkNotNull(savedStateHandle[MoaMapRoute.NoticeDetail.ARG_NOTICE_ID])

    private val _uiState = MutableStateFlow(NoticeDetailUiState())
    val uiState: StateFlow<NoticeDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        _uiState.value = NoticeDetailUiState()
        viewModelScope.launch {
            _uiState.value = try {
                NoticeDetailUiState(loading = false, notice = repository.getNotice(noticeId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "공지사항 상세 조회 실패 (id=$noticeId)", e)
                val deleted = e is ApiException && e.status == HTTP_NOT_FOUND
                NoticeDetailUiState(
                    loading = false,
                    errorMessage = if (deleted) NOTICE_DELETED_MESSAGE else e.toUserMessage(NOTICE_LOAD_FAILED_MESSAGE),
                )
            }
        }
    }
}
