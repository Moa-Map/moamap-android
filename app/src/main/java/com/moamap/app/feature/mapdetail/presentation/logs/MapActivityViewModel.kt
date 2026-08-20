package com.moamap.app.feature.mapdetail.presentation.logs

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moamap.app.core.navigation.MoaMapRoute
import com.moamap.app.core.network.ApiException
import com.moamap.app.feature.mapdetail.domain.model.MapActivity
import com.moamap.app.feature.mapdetail.domain.repository.MapActivityRepository
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

private const val TAG = "MapActivityViewModel"

internal const val ACTIVITY_LOAD_FAILED_MESSAGE = "활동 내역을 불러오지 못했어요"

/** `[403] PLACE_002: 해당 지도의 멤버가 아닙니다.` */
private const val NOT_MAP_MEMBER_CODE = "PLACE_002"

internal const val ACTIVITY_NOT_MEMBER_MESSAGE = "지도에 참여해야 활동 내역을 볼 수 있어요"

/**
 * 로그 탭 상태.
 *
 * 목록을 아예 못 읽은 것([errorMessage])과 활동이 하나도 없는 것을 나눈다. 둘을 묶으면
 * 통신이 끊겼는데도 "아직 활동 내역이 없어요" 가 뜨고, 다시 시도할 자리가 사라진다.
 *
 * 조회를 시작하기 전에도 [loading] 이 참이다. 탭을 여는 순간부터 목록 자리가 채워져야
 * 하는데, 시작 전을 따로 두면 "없음" 이 한 프레임 스쳐 간다.
 */
@Immutable
data class MapActivityUiState(
    val loading: Boolean = true,
    val activities: List<MapActivity> = emptyList(),
    val errorMessage: String? = null,
)

/**
 * 지도 활동 내역.
 *
 * [com.moamap.app.feature.mapdetail.presentation.MapDetailViewModel] 에 얹지 않는다.
 * 로그 탭을 열 때만 필요한 값이라, 지도 상태에 섞으면 장소 탭만 보는 사용자도 화면에 들어올
 * 때마다 활동 내역을 함께 받게 된다.
 */
@HiltViewModel
class MapActivityViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MapActivityRepository,
) : ViewModel() {

    private val mapId: Long = checkNotNull(savedStateHandle[MoaMapRoute.MapDetail.ARG_MAP_ID]) {
        "지도 상세는 ${MoaMapRoute.MapDetail.ARG_MAP_ID} 없이 열 수 없다"
    }

    private val _uiState = MutableStateFlow(MapActivityUiState())
    val uiState: StateFlow<MapActivityUiState> = _uiState.asStateFlow()

    /** 진행 중인 조회. 재시도를 연달아 눌러도 마지막 것만 남게 한다. */
    private var loadJob: Job? = null

    /** 한 번이라도 읽기 시작했는가. 탭을 오갈 때마다 다시 받지 않으려고 본다. */
    private var started = false

    /**
     * 로그 탭이 처음 열렸다.
     *
     * `init` 에서 읽지 않는다. 이 ViewModel 은 화면이 그려질 때 함께 만들어져서, 거기서
     * 시작하면 장소 탭만 보고 나가는 사용자도 활동 내역을 받게 된다.
     */
    fun loadOnce() {
        if (started) return
        start()
    }

    fun retry() {
        _uiState.update { state -> state.copy(loading = true, errorMessage = null) }
        start()
    }

    private fun start() {
        started = true
        load()
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val activities = repository.getActivities(mapId)
                _uiState.value = MapActivityUiState(loading = false, activities = activities)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "활동 내역 조회 실패", e)
                _uiState.value = MapActivityUiState(
                    loading = false,
                    errorMessage = e.toActivityMessage(),
                )
            }
        }
    }
}

/**
 * 조회 실패 안내.
 *
 * 멤버가 아닌 경우만 따로 가른다. "불러오지 못했어요" 로 묶으면 프라이빗 지도를 기웃거리는
 * 사용자가 통신 문제로 오해하고 재시도만 반복한다.
 */
private fun Throwable.toActivityMessage(): String =
    if (this is ApiException && code == NOT_MAP_MEMBER_CODE) {
        ACTIVITY_NOT_MEMBER_MESSAGE
    } else {
        toUserMessage(ACTIVITY_LOAD_FAILED_MESSAGE)
    }
