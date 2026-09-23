package com.moamap.app.feature.mapdetail.presentation.review

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moamap.app.core.common.upload.ImageUploadException
import com.moamap.app.core.network.ApiException
import com.moamap.app.feature.mapdetail.domain.model.PlaceReview
import com.moamap.app.feature.mapdetail.domain.repository.PlaceReviewRepository
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

private const val TAG = "PlaceReviewViewModel"

internal const val REVIEW_LOAD_FAILED_MESSAGE = "댓글을 불러오지 못했어요"
internal const val REVIEW_SUBMIT_FAILED_MESSAGE = "댓글을 남기지 못했어요"
internal const val NOT_MAP_MEMBER_MESSAGE = "지도에 참여해야 댓글을 남길 수 있어요"
internal const val REVIEW_EMPTY_MESSAGE = "내용이나 사진을 남겨주세요"

/** `[403] PLACE_002: 해당 지도의 멤버가 아닙니다.` */
private const val NOT_MAP_MEMBER_CODE = "PLACE_002"

/**
 * 후기 작성 실패 안내.
 *
 * 멤버가 아닌 경우만 따로 가른다. "남기지 못했어요" 하나로 묶으면 왜 안 되는지 알 수 없어
 * 같은 글을 계속 다시 보내게 된다. 화면이 참여 여부를 보고 미리 막지만, 다른 기기에서
 * 나간 뒤라면 여기까지 온다.
 */
private fun Throwable.toSubmitMessage(): String = when {
    this is ApiException && code == NOT_MAP_MEMBER_CODE -> NOT_MAP_MEMBER_MESSAGE
    // 형식·크기 안내는 그대로 보여준다. "남기지 못했어요" 로는 사진을 바꿔야 한다는 걸 알 수 없다.
    this is ImageUploadException -> message ?: REVIEW_SUBMIT_FAILED_MESSAGE
    else -> toUserMessage(REVIEW_SUBMIT_FAILED_MESSAGE)
}

/**
 * 장소 상세 시트에 띄우는 후기 상태.
 *
 * [placeId] 는 이 상태가 **어느 장소의 것인지** 알려 준다. 시트는 열리자마자 그려지고
 * 조회는 그다음에 시작해, 이 값을 안 보면 직전 장소의 후기가 한 프레임 스쳐 간다.
 */
@Immutable
data class PlaceReviewUiState(
    val placeId: Long? = null,
    val loading: Boolean = false,
    val reviews: List<PlaceReview> = emptyList(),
    /** 목록을 아예 못 읽었다. 후기가 하나도 없는 것과 구분해 다시 시도할 자리를 준다. */
    val loadErrorMessage: String? = null,
    val submitting: Boolean = false,
    val submitErrorMessage: String? = null,
    /**
     * 서버가 받아들인 후기 수.
     *
     * 값이 늘어난 것만 신호로 쓴다. 입력창은 이때 비우고, 화면은 장소의 후기 수를
     * 다시 읽는다. 보내자마자 지우면 실패했을 때 적어 둔 게 날아간다.
     */
    val submittedCount: Int = 0,
)

@HiltViewModel
class PlaceReviewViewModel @Inject constructor(
    private val repository: PlaceReviewRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaceReviewUiState())
    val uiState: StateFlow<PlaceReviewUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var submitJob: Job? = null

    /**
     * 장소 하나의 후기를 읽는다.
     *
     * 이미 그 장소를 보고 있으면 아무것도 하지 않는다. 재구성마다 다시 부르면 같은 내용을
     * 위해 왕복만 늘고, 그 사이 목록이 비었다 채워지며 화면이 깜빡인다.
     */
    fun open(placeId: Long) {
        if (_uiState.value.placeId == placeId) return

        submitJob?.cancel()
        _uiState.value = PlaceReviewUiState(placeId = placeId, loading = true)
        load(placeId)
    }

    /** 시트가 닫혔다. 다음에 열 때 서버에서 다시 읽도록 비운다. */
    fun close() {
        loadJob?.cancel()
        submitJob?.cancel()
        _uiState.value = PlaceReviewUiState()
    }

    fun retry() {
        val placeId = _uiState.value.placeId ?: return

        _uiState.update { state -> state.copy(loading = true, loadErrorMessage = null) }
        load(placeId)
    }

    /**
     * 후기를 남긴다.
     *
     * 보냈는지가 아니라 **보내기 시작했는지**를 돌려준다. 서버 응답을 기다리지 않으므로
     * 입력창은 이 값으로 비우지 않는다 - [PlaceReviewUiState.submittedCount] 가 그 신호다.
     *
     * 글과 사진 중 하나는 있어야 한다. 둘 다 없는 후기는 목록에 빈 줄로만 남는다.
     */
    fun submit(content: String, photo: Uri?): Boolean {
        val placeId = _uiState.value.placeId ?: return false
        if (_uiState.value.submitting) return false

        if (content.isBlank() && photo == null) {
            _uiState.update { state -> state.copy(submitErrorMessage = REVIEW_EMPTY_MESSAGE) }
            return false
        }

        _uiState.update { state -> state.copy(submitting = true, submitErrorMessage = null) }
        submitJob?.cancel()
        submitJob = viewModelScope.launch {
            try {
                repository.createReview(placeId, content.trim(), photo)
                _uiState.update { state ->
                    state.copy(submitting = false, submittedCount = state.submittedCount + 1)
                }
                // 방금 쓴 글이 목록에 보여야 한다. 서버가 매긴 순서와 닉네임까지 그대로 받는다.
                load(placeId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "후기 작성 실패 (placeId=$placeId)", e)
                _uiState.update { state ->
                    state.copy(submitting = false, submitErrorMessage = e.toSubmitMessage())
                }
            }
        }
        return true
    }

    /**
     * 조회 결과를 얹는다.
     *
     * 늦게 도착한 응답이 다른 장소의 시트를 덮지 않도록 [placeId] 를 다시 확인한다.
     * 취소가 항상 제때 닿는 건 아니다 - 작성 직후의 재조회처럼 취소 없이 이어지는 길도 있다.
     */
    private fun load(placeId: Long) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val reviews = repository.getReviews(placeId)
                _uiState.update { state ->
                    if (state.placeId != placeId) state
                    else state.copy(loading = false, reviews = reviews, loadErrorMessage = null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "후기 조회 실패 (placeId=$placeId)", e)
                _uiState.update { state ->
                    if (state.placeId != placeId) state
                    else state.copy(
                        loading = false,
                        loadErrorMessage = e.toUserMessage(REVIEW_LOAD_FAILED_MESSAGE),
                    )
                }
            }
        }
    }
}
