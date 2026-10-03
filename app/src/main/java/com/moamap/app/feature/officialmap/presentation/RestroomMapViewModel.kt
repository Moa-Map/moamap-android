package com.moamap.app.feature.officialmap.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moamap.app.feature.mapdetail.ViewportBounds
import com.moamap.app.feature.officialmap.domain.model.RestroomDetail
import com.moamap.app.feature.officialmap.domain.model.RestroomMarker
import com.moamap.app.feature.officialmap.domain.repository.RestroomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "RestroomMapViewModel"

data class RestroomMapUiState(
    val restrooms: List<RestroomMarker> = emptyList(),
    /** 화면 안 화장실이 서버 한도를 넘어 일부만 왔는지. */
    val truncated: Boolean = false,
    /** 마지막 조회가 실패했는지. 지도를 움직이면 다시 읽는다. */
    val loadFailed: Boolean = false,
    /** 누른 화장실. 지도를 옮겨 목록에서 빠져도 카드는 남는다. */
    val selected: RestroomMarker? = null,
    /** [selected] 의 자세한 정보. 고른 화장실이 없으면 null. */
    val detail: RestroomDetailState? = null,
)

sealed interface RestroomDetailState {
    data object Loading : RestroomDetailState
    data class Loaded(val detail: RestroomDetail) : RestroomDetailState
    data object Failed : RestroomDetailState
}

/**
 * 공중화장실 지도. 카메라가 멈출 때마다 보이는 범위의 화장실을 다시 읽는다.
 *
 * 화장실은 전국 수만 곳이라 한 번에 받지 않는다. 서버가 범위당 최대 500곳을 주고, 넘으면
 * 화면이 확대를 안내한다.
 */
@HiltViewModel
class RestroomMapViewModel @Inject constructor(
    private val repository: RestroomRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RestroomMapUiState())
    val uiState: StateFlow<RestroomMapUiState> = _uiState.asStateFlow()

    /** 마지막으로 읽은 범위. 카메라가 그대로인데 지도가 다시 멈춘 경우(다시 그리기 등)를 거른다. */
    private var loadedBounds: ViewportBounds? = null
    private var loadJob: Job? = null
    private var detailJob: Job? = null

    /** 카메라가 멈췄다. 새 범위면 앞선 조회를 버리고 다시 읽는다. */
    internal fun onCameraIdle(bounds: ViewportBounds) {
        if (bounds == loadedBounds) return
        loadedBounds = bounds

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val result = repository.getRestrooms(
                    south = bounds.south,
                    west = bounds.west,
                    north = bounds.north,
                    east = bounds.east,
                )
                _uiState.update { state ->
                    state.copy(
                        restrooms = result.restrooms,
                        truncated = result.truncated,
                        loadFailed = false,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "화장실 목록을 읽지 못했다", e)
                // 같은 자리에서 다시 멈춰도 재시도되게 비운다.
                loadedBounds = null
                _uiState.update { state -> state.copy(loadFailed = true) }
            }
        }
    }

    /** 마커를 눌렀다. 같은 화장실을 다시 누르면 실패했던 상세만 다시 읽는다. */
    fun selectRestroom(id: Long) {
        val state = _uiState.value
        if (state.selected?.id == id && state.detail != RestroomDetailState.Failed) return
        val marker = state.restrooms.firstOrNull { it.id == id } ?: state.selected?.takeIf { it.id == id } ?: return

        _uiState.update { it.copy(selected = marker, detail = RestroomDetailState.Loading) }
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            val detail = try {
                RestroomDetailState.Loaded(repository.getRestroom(id))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "화장실 상세를 읽지 못했다", e)
                RestroomDetailState.Failed
            }
            _uiState.update { it.copy(detail = detail) }
        }
    }

    /** 빈 곳을 눌렀다. 카드를 닫는다. */
    fun clearSelection() {
        detailJob?.cancel()
        _uiState.update { it.copy(selected = null, detail = null) }
    }
}
