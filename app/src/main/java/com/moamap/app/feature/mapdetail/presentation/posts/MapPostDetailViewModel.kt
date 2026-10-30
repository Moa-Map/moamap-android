package com.moamap.app.feature.mapdetail.presentation.posts

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moamap.app.core.auth.CurrentUserStore
import com.moamap.app.core.navigation.MoaMapRoute
import com.moamap.app.core.navigation.requireMapId
import com.moamap.app.feature.mapdetail.domain.model.MapPost
import com.moamap.app.feature.mapdetail.domain.model.MapPostComment
import com.moamap.app.feature.mapdetail.domain.model.ReportReason
import com.moamap.app.feature.mapdetail.domain.repository.MapPostCommentRepository
import com.moamap.app.feature.mapdetail.domain.repository.MapPostRepository
import com.moamap.app.core.common.toUserMessage
import com.moamap.app.feature.mapdetail.presentation.review.REPORTED_TOAST
import com.moamap.app.feature.mapdetail.presentation.review.REVIEW_DELETED_TOAST
import com.moamap.app.feature.mapdetail.presentation.review.blockedToast
import com.moamap.app.feature.mapdetail.presentation.review.toBlockFailureMessage
import com.moamap.app.feature.mapdetail.presentation.review.toReportFailureMessage
import com.moamap.app.feature.mypage.domain.BlockedUserStore
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
internal const val COMMENT_DELETE_FAILED_MESSAGE = "댓글을 삭제하지 못했어요"

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
    /** 삭제·신고·차단 중 하나가 서버로 가 있다. 그동안 다른 것을 받지 않는다. */
    val moderating: Boolean = false,
    /** 삭제·신고·차단 결과. 한 번 띄우고 지운다. 입력칸 바로 위에 뜬다. */
    val toast: String? = null,
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
 *
 * 알림으로 들어와 경로에 게시물(`postId`)이 있을 때만 그 게시물을 읽어 바로 연다.
 */
@HiltViewModel
class MapPostDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: MapPostCommentRepository,
    private val currentUserStore: CurrentUserStore,
    private val postRepository: MapPostRepository,
    private val blockedUsers: BlockedUserStore,
) : ViewModel() {

    private val mapId: Long = savedStateHandle.requireMapId()

    private val _uiState = MutableStateFlow(MapPostDetailUiState())
    val uiState: StateFlow<MapPostDetailUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var sendJob: Job? = null

    init {
        openFromRoute()
    }

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

    /** 내 댓글을 지운다. 확인은 화면이 먼저 받는다. 다시 읽지 않고 목록에서 뺀다 - 남긴 댓글을 붙일 때와 같다. */
    fun delete(commentId: Long) {
        val state = _uiState.value
        val post = state.post ?: return
        val comment = state.comments.firstOrNull { item -> item.id == commentId } ?: return
        if (state.moderating || !comment.isMine(state.myId)) return

        moderate(failureMessage = { e -> e.toUserMessage(COMMENT_DELETE_FAILED_MESSAGE) }) {
            repository.deleteComment(mapId = mapId, postId = post.id, commentId = commentId)
            _uiState.update { current ->
                current.copy(
                    comments = current.comments.filterNot { item -> item.id == commentId },
                    toast = REVIEW_DELETED_TOAST,
                )
            }
        }
    }

    /** 남의 댓글을 신고한다. 「기타」는 내용이 있어야 한다 - 서버가 꼭 받는다. */
    fun report(commentId: Long, reason: ReportReason, detail: String) {
        val state = _uiState.value
        val post = state.post ?: return
        val comment = state.comments.firstOrNull { item -> item.id == commentId } ?: return
        if (state.moderating || comment.isMine(state.myId)) return
        if (reason == ReportReason.Other && detail.isBlank()) return

        moderate(failureMessage = Throwable::toReportFailureMessage) {
            repository.reportComment(mapId = mapId, postId = post.id, commentId = commentId, reason = reason, detail = detail)
            _uiState.update { current -> current.copy(toast = REPORTED_TOAST) }
        }
    }

    /** 댓글을 쓴 사람을 차단한다. 지도 상세가 차단 목록으로 바로 걸러 그 사람의 댓글이 사라진다. */
    fun block(commentId: Long) {
        val state = _uiState.value
        val comment = state.comments.firstOrNull { item -> item.id == commentId } ?: return
        if (state.moderating || comment.isMine(state.myId)) return

        moderate(failureMessage = Throwable::toBlockFailureMessage) {
            blockedUsers.block(comment.authorId)
            _uiState.update { current -> current.copy(toast = blockedToast(comment.authorName)) }
        }
    }

    fun consumeToast() {
        _uiState.update { state -> state.copy(toast = null) }
    }

    /** 삭제·신고·차단을 하나씩만 보낸다. 실패하면 [failureMessage] 를 토스트로 띄운다. */
    private fun moderate(failureMessage: (Throwable) -> String, action: suspend () -> Unit) {
        _uiState.update { current -> current.copy(moderating = true) }
        viewModelScope.launch {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "게시물 댓글 삭제·신고·차단 실패 (mapId=$mapId)", e)
                _uiState.update { current -> current.copy(toast = failureMessage(e)) }
            } finally {
                _uiState.update { current -> current.copy(moderating = false) }
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

    /**
     * 경로에 실려 온 게시물을 연다. 한 번 열면 경로에서 지운다 - 닫은 뒤 화면을 돌리면 다시 열리면 안 된다.
     *
     * 지워진 게시물이면 열지 않고 지도만 보인다.
     */
    private fun openFromRoute() {
        val postId = savedStateHandle.get<Long>(MoaMapRoute.MapDetail.ARG_POST_ID)
            ?.takeIf { id -> id != MoaMapRoute.MapDetail.NO_POST_ID }
            ?: return
        savedStateHandle[MoaMapRoute.MapDetail.ARG_POST_ID] = MoaMapRoute.MapDetail.NO_POST_ID
        viewModelScope.launch {
            try {
                open(postRepository.getPost(mapId = mapId, postId = postId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "알림의 게시물 조회 실패 (mapId=$mapId, postId=$postId)", e)
            }
        }
    }

    private fun cancelJobs() {
        loadJob?.cancel()
        sendJob?.cancel()
    }
}

/** 내가 쓴 댓글인가. 누군지 모르면 아무것도 내 것이 아니다 - 서버가 거절할 메뉴를 띄우지 않는다. */
internal fun MapPostComment.isMine(myId: Long?): Boolean = myId != null && authorId == myId
