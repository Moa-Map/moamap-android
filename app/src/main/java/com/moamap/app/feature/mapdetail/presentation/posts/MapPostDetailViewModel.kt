package com.moamap.app.feature.mapdetail.presentation.posts

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moamap.app.core.auth.CurrentUserStore
import com.moamap.app.core.navigation.MoaMapRoute
import com.moamap.app.feature.mapdetail.domain.model.MapPost
import com.moamap.app.feature.mapdetail.domain.model.MapPostComment
import com.moamap.app.feature.mapdetail.domain.repository.MapPostCommentRepository
import com.moamap.app.feature.mapdetail.presentation.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MapPostDetailViewModel"

internal const val COMMENT_LOAD_FAILED_MESSAGE = "댓글을 불러오지 못했어요"
internal const val COMMENT_SEND_FAILED_MESSAGE = "댓글을 남기지 못했어요"

/** 댓글 한 건의 서버 한도. */
internal const val COMMENT_MAX_LENGTH = 500

/**
 * 게시물 상세 상태. [post] 가 null 이면 상세가 닫혀 있다.
 *
 * 댓글을 못 읽은 것([errorMessage])과 못 남긴 것([sendErrorMessage])을 나눈다. 앞은 댓글 자리를 안내로
 * 바꾸고, 뒤는 이미 보이는 댓글을 둔 채 입력창 아래에만 알린다.
 */
@Immutable
data class MapPostDetailUiState(
    val post: MapPost? = null,
    val loading: Boolean = true,
    val comments: List<MapPostComment> = emptyList(),
    val errorMessage: String? = null,
    /** 로그인한 내 id. 내 댓글을 오른쪽에 둔다. 모르면(옛 세션) null 이고 모두 남의 댓글처럼 그린다. */
    val myId: Long? = null,
    val sending: Boolean = false,
    val sendErrorMessage: String? = null,
    /** 남긴 댓글 수. 늘면 화면이 입력칸을 비우고 맨 아래로 내린다. */
    val sentCount: Int = 0,
) {
    /**
     * 댓글을 다 읽었다. 그 전에는 보내지 않는다 - 먼저 보낸 댓글을 늦게 온 목록이 덮어 사라지게 한다.
     */
    val commentsReady: Boolean get() = !loading && errorMessage == null
}

/**
 * 로그 탭 게시물 상세와 댓글.
 *
 * 게시물 내용은 목록에서 받은 것을 그대로 쓴다. 따로 다시 읽지 않는다 - 목록이 이미 같은 응답을 들고 있다.
 */
@HiltViewModel
class MapPostDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MapPostCommentRepository,
    private val currentUserStore: CurrentUserStore,
) : ViewModel() {

    private val mapId: Long = checkNotNull(savedStateHandle[MoaMapRoute.MapDetail.ARG_MAP_ID]) {
        "지도 상세는 ${MoaMapRoute.MapDetail.ARG_MAP_ID} 없이 열 수 없다"
    }

    private val _uiState = MutableStateFlow(MapPostDetailUiState())
    val uiState: StateFlow<MapPostDetailUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var sendJob: Job? = null

    /** 상세를 열고 댓글을 읽는다. 다른 게시물을 보던 중이면 그 요청은 버린다. */
    fun open(post: MapPost) {
        cancelJobs()
        _uiState.value = MapPostDetailUiState(post = post)
        load(post)
    }

    fun close() {
        cancelJobs()
        _uiState.value = MapPostDetailUiState()
    }

    fun retry() {
        val post = _uiState.value.post ?: return
        _uiState.update { state -> state.copy(loading = true, errorMessage = null) }
        load(post)
    }

    /**
     * 댓글을 남긴다. 앞뒤 공백을 지우고, 비었으면 보내지 않는다.
     *
     * 성공하면 목록을 다시 읽지 않고 받은 댓글을 맨 아래에 붙인다. 다시 읽다 실패하면 이미 남긴 댓글이
     * 없던 일처럼 보인다.
     */
    fun send(content: String) {
        val state = _uiState.value
        val post = state.post ?: return
        val text = content.trim().take(COMMENT_MAX_LENGTH)
        if (text.isEmpty() || state.sending || !state.commentsReady) return

        _uiState.update { current -> current.copy(sending = true, sendErrorMessage = null) }
        sendJob = viewModelScope.launch {
            try {
                val comment = repository.createComment(mapId = mapId, postId = post.id, content = text)
                _uiState.update { current ->
                    current.copy(
                        comments = current.comments + comment,
                        sending = false,
                        sentCount = current.sentCount + 1,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "게시물 댓글 작성 실패 (mapId=$mapId)", e)
                _uiState.update { current ->
                    current.copy(sending = false, sendErrorMessage = e.toUserMessage(COMMENT_SEND_FAILED_MESSAGE))
                }
            }
        }
    }

    private fun load(post: MapPost) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val comments = repository.getComments(mapId = mapId, postId = post.id)
                val myId = currentUserStore.load()
                _uiState.update { state ->
                    state.copy(loading = false, comments = comments, myId = myId, errorMessage = null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "게시물 댓글 조회 실패 (mapId=$mapId)", e)
                _uiState.update { state ->
                    state.copy(loading = false, errorMessage = e.toUserMessage(COMMENT_LOAD_FAILED_MESSAGE))
                }
            }
        }
    }

    private fun cancelJobs() {
        loadJob?.cancel()
        sendJob?.cancel()
    }
}
