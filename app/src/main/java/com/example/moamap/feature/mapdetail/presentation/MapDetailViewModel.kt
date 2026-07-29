package com.example.moamap.feature.mapdetail.presentation

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moamap.core.navigation.MoaMapRoute
import com.example.moamap.feature.mapdetail.domain.model.MapDetailAction
import com.example.moamap.feature.mapdetail.domain.model.leavingDeletesMap
import com.example.moamap.feature.mapdetail.domain.model.roleBadge
import com.example.moamap.feature.mapdetail.domain.model.topBarAction
import com.example.moamap.feature.mapdetail.domain.repository.MapDetailRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MapDetailViewModel"

/**
 * 지도 상세 화면 상태.
 *
 * 장소 목록·마커·로그는 아직 목데이터라 여기에 담기지 않는다. 서버에서 받아오는 건
 * 지도 한 건([map])과 참여·나가기 진행 상황뿐이다.
 */
@Immutable
data class MapDetailScreenState(
    val map: MapLoadState = MapLoadState.Loading,
    /** 참여·나가기 요청이 진행 중. 버튼을 두 번 누르지 못하게 막는다. */
    val actionInProgress: Boolean = false,
    /** 한 번 보여주고 지우는 실패 안내. */
    val errorMessage: String? = null,
    /** 나가기가 끝났다. 화면이 이 신호를 보고 이전 화면으로 돌아간다. */
    val left: Boolean = false,
) {
    /** 서버 이름. 아직 응답이 없으면 null 이고, 화면은 라우트로 받은 초기값을 쓴다. */
    val title: String? get() = map.mapOrNull?.title

    val roleBadge: String? get() = map.mapOrNull?.roleBadge

    val action: MapDetailAction get() = map.mapOrNull?.topBarAction ?: MapDetailAction.None

    /** 참여 중인 지도에만 장소를 더할 수 있다. */
    val canAddPlace: Boolean get() = map.mapOrNull?.joined == true

    val placeCount: Int? get() = map.mapOrNull?.placeCount
}

@HiltViewModel
class MapDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MapDetailRepository,
) : ViewModel() {

    private val mapId: Long = checkNotNull(savedStateHandle[MoaMapRoute.MapDetail.ARG_MAP_ID]) {
        "지도 상세는 ${MoaMapRoute.MapDetail.ARG_MAP_ID} 없이 열 수 없다"
    }

    private val _uiState = MutableStateFlow(MapDetailScreenState())
    val uiState: StateFlow<MapDetailScreenState> = _uiState.asStateFlow()

    /** 참여·나가기가 겹쳐 돌지 않게 잡아 두는 자리. */
    private var actionJob: Job? = null

    init {
        load()
    }

    fun retry() = load()

    fun consumeErrorMessage() {
        _uiState.update { state -> state.copy(errorMessage = null) }
    }

    /**
     * 공개 지도에 참여한다.
     *
     * 성공해도 화면을 떠나지 않는다. 상단 라벨이 나가기로 바뀌고 장소 추가가 열린다.
     *
     * 응답을 그대로 쓰지 않고 상세를 다시 읽는다. `myRole` 과 `memberCount` 가 함께 바뀌어
     * 배지와 나가기 가능 여부가 달라지는데, 참여 응답이 그 값들을 최신으로 준다는 보장이 없다.
     */
    fun join() = runAction(JOIN_FAILED_MESSAGE) {
        repository.joinMap(mapId)
        loadInto(_uiState.value.copy(actionInProgress = false))
    }

    /**
     * 지도에서 나간다.
     *
     * 프라이빗 지도를 만든 사람은 서버가 탈퇴를 거절한다. 혼자 남았을 때만 나갈 수 있고,
     * 그때는 나가기가 곧 지도를 없애는 것과 같아 삭제로 대신한다.
     */
    fun leave() {
        val map = _uiState.value.map.mapOrNull ?: return

        runAction(LEAVE_FAILED_MESSAGE) {
            if (map.leavingDeletesMap) {
                repository.deleteMap(mapId)
            } else {
                repository.leaveMap(mapId)
            }
            _uiState.update { state -> state.copy(actionInProgress = false, left = true) }
        }
    }

    private fun load() {
        _uiState.update { state -> state.copy(map = MapLoadState.Loading) }
        viewModelScope.launch { loadInto(_uiState.value) }
    }

    /**
     * 상세를 읽어 [base] 위에 얹는다.
     *
     * 참여 직후처럼 다른 필드를 함께 바꿔야 할 때가 있어 바탕이 될 상태를 받는다.
     */
    private suspend fun loadInto(base: MapDetailScreenState) {
        val next = try {
            base.copy(map = MapLoadState.Success(repository.getMapDetail(mapId)))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "지도 상세 조회 실패 (mapId=$mapId)", e)
            base.copy(map = MapLoadState.Error(e.toUserMessage(MAP_LOAD_FAILED_MESSAGE)))
        }
        _uiState.update { next }
    }

    /** 진행 중이면 무시하고, 아니면 잠근 채 [block] 을 돌린다. */
    private fun runAction(failureMessage: String, block: suspend () -> Unit) {
        if (_uiState.value.actionInProgress) return

        _uiState.update { state -> state.copy(actionInProgress = true, errorMessage = null) }
        actionJob?.cancel()
        actionJob = viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "지도 상세 액션 실패 (mapId=$mapId)", e)
                _uiState.update { state ->
                    state.copy(
                        actionInProgress = false,
                        errorMessage = e.toUserMessage(failureMessage),
                    )
                }
            }
        }
    }
}
