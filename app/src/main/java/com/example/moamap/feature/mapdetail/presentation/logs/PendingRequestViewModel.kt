package com.example.moamap.feature.mapdetail.presentation.logs

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moamap.core.navigation.MoaMapRoute
import com.example.moamap.feature.mapdetail.domain.model.PendingPlace
import com.example.moamap.feature.mapdetail.domain.repository.PendingPlaceRepository
import com.example.moamap.feature.mapdetail.presentation.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "PendingRequestViewModel"

internal const val PENDING_LOAD_FAILED_MESSAGE = "장소 등록 요청을 불러오지 못했어요"

internal const val PENDING_ACTION_FAILED_MESSAGE = "요청을 처리하지 못했어요"

/**
 * 장소 등록 요청 상태.
 *
 * 목록을 못 읽은 것([errorMessage])과 한 건을 처리하지 못한 것([actionErrorMessage])을
 * 나눈다. 앞은 요청 자리에 남고, 뒤는 목록을 그대로 둔 채 스낵바로 스쳐 간다.
 */
@Immutable
data class PendingRequestUiState(
    val loading: Boolean = true,
    val requests: List<PendingPlace> = emptyList(),
    val errorMessage: String? = null,
    /** 수락·거절이 오가는 중. 버튼을 연달아 눌러도 서버에는 한 번만 간다. */
    val processing: Boolean = false,
    val actionErrorMessage: String? = null,
    /**
     * 지금까지 수락한 건수.
     *
     * 화면이 이 값이 오르는 것을 보고 장소 목록을 다시 읽는다. 수락한 장소가 지도에 새로
     * 떠야 하기 때문이다. 불리언으로 두면 두 번째 수락에서 값이 그대로라 신호가 되지 않는다.
     */
    val approvedCount: Int = 0,
)

/**
 * 장소 등록 요청 수락·거절.
 *
 * [MapActivityViewModel] 에 얹지 않는다. 같은 탭에 있지만 활동 내역은 멤버 누구나 보고
 * 요청 목록은 방장·관리자만 본다. 한 상태로 묶으면 일반 멤버도 요청을 받게 된다.
 */
@HiltViewModel
class PendingRequestViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PendingPlaceRepository,
) : ViewModel() {

    private val mapId: Long = checkNotNull(savedStateHandle[MoaMapRoute.MapDetail.ARG_MAP_ID]) {
        "지도 상세는 ${MoaMapRoute.MapDetail.ARG_MAP_ID} 없이 열 수 없다"
    }

    private val _uiState = MutableStateFlow(PendingRequestUiState())
    val uiState: StateFlow<PendingRequestUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var actionJob: Job? = null

    /** 한 번이라도 읽기 시작했는가. 탭을 오갈 때마다 다시 받지 않으려고 본다. */
    private var started = false

    /** 로그 탭이 처음 열렸다. `init` 에서 읽으면 장소 탭만 보는 사용자도 요청을 받게 된다. */
    fun loadOnce() {
        if (started) return
        started = true
        load()
    }

    fun retry() {
        _uiState.update { state -> state.copy(loading = true, errorMessage = null) }
        load()
    }

    /** 수락한 장소는 지도에 올라간다. 그래서 갱신 신호를 함께 올린다. */
    fun approve(placeId: Long) = process(placeId, approved = true) {
        repository.approve(placeId)
    }

    /** 거절한 장소는 지도에 올라가지 않아 갱신 신호를 올리지 않는다. */
    fun reject(placeId: Long) = process(placeId, approved = false) {
        repository.reject(placeId)
    }

    /** 스낵바가 한 번 뜨고 나면 지운다. 화면을 되돌아올 때 다시 뜨지 않게 한다. */
    fun consumeActionError() {
        _uiState.update { state -> state.copy(actionErrorMessage = null) }
    }

    /**
     * 처리가 끝나면 목록을 다시 받지 않고 그 한 건만 뺀다.
     *
     * 다시 그리려고 통신을 한 번 더 하면, 그 조회가 실패했을 때 이미 처리된 요청이 없던
     * 일처럼 되살아난다.
     */
    private fun process(placeId: Long, approved: Boolean, block: suspend () -> Unit) {
        if (_uiState.value.processing) return

        _uiState.update { state -> state.copy(processing = true, actionErrorMessage = null) }
        actionJob?.cancel()
        actionJob = viewModelScope.launch {
            try {
                block()
                _uiState.update { state ->
                    state.copy(
                        processing = false,
                        requests = state.requests.filterNot { request -> request.id == placeId },
                        approvedCount = state.approvedCount + if (approved) 1 else 0,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "장소 등록 요청 처리 실패 (mapId=$mapId)", e)
                _uiState.update { state ->
                    state.copy(
                        processing = false,
                        actionErrorMessage = e.toUserMessage(PENDING_ACTION_FAILED_MESSAGE),
                    )
                }
            }
        }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val pending = repository.getPendingPlaces(mapId)
                _uiState.update { state ->
                    state.copy(loading = false, requests = pending, errorMessage = null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "장소 등록 요청 조회 실패 (mapId=$mapId)", e)
                _uiState.update { state ->
                    state.copy(
                        loading = false,
                        errorMessage = e.toUserMessage(PENDING_LOAD_FAILED_MESSAGE),
                    )
                }
            }
        }
    }
}
