package com.example.moamap.feature.mapdetail.presentation.intro

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moamap.core.navigation.MoaMapRoute
import com.example.moamap.feature.mapdetail.domain.model.MapPlacePreview
import com.example.moamap.feature.mapdetail.domain.repository.MapDetailRepository
import com.example.moamap.feature.mapdetail.presentation.JOIN_FAILED_MESSAGE
import com.example.moamap.feature.mapdetail.presentation.MAP_LOAD_FAILED_MESSAGE
import com.example.moamap.feature.mapdetail.presentation.MapLoadState
import com.example.moamap.feature.mapdetail.presentation.mapOrNull
import com.example.moamap.feature.mapdetail.presentation.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MapIntroViewModel"

/** 설명 화면 장소 목록에 보여줄 개수. 피그마가 4개 + `더보기` 다. */
const val INTRO_PLACE_COUNT = 4

@Immutable
data class MapIntroUiState(
    val map: MapLoadState = MapLoadState.Loading,
    val places: MapPlacePreview = MapPlacePreview(),
    /** 참여 요청 진행 중. 버튼을 두 번 누르지 못하게 막는다. */
    val joining: Boolean = false,
    /** 한 번 보여주고 지우는 실패 안내. */
    val errorMessage: String? = null,
    /** 참여가 끝났다. 화면이 이 신호를 보고 상세로 넘어간다. */
    val joined: Boolean = false,
) {
    val title: String get() = map.mapOrNull?.title.orEmpty()
}

@HiltViewModel
class MapIntroViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MapDetailRepository,
) : ViewModel() {

    private val mapId: Long = checkNotNull(savedStateHandle[MoaMapRoute.MapIntro.ARG_MAP_ID]) {
        "지도 설명은 ${MoaMapRoute.MapIntro.ARG_MAP_ID} 없이 열 수 없다"
    }

    private val _uiState = MutableStateFlow(MapIntroUiState())
    val uiState: StateFlow<MapIntroUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var joinJob: Job? = null

    /**
     * 화면이 보일 때마다 다시 읽는다. 첫 조회도 이 경로가 겸한다.
     *
     * 미리보기로 상세에 들어가 거기서 참여하고 돌아오는 경로가 여기다. 다시 읽지 않으면
     * 하단 버튼이 "참여하기" 인 채로 남는다.
     *
     * 이미 받아 둔 지도가 있으면 지우지 않는다. 돌아올 때마다 화면이 깜빡이면 곤란하다.
     */
    fun refresh() {
        loadJob?.cancel()

        val keepCurrent = _uiState.value.map is MapLoadState.Success
        if (!keepCurrent) {
            _uiState.update { state -> state.copy(map = MapLoadState.Loading) }
        }

        loadJob = viewModelScope.launch { load(keepCurrent) }
    }

    fun retry() {
        _uiState.update { state -> state.copy(map = MapLoadState.Loading) }
        loadJob?.cancel()
        loadJob = viewModelScope.launch { load(keepCurrent = false) }
    }

    fun consumeErrorMessage() {
        _uiState.update { state -> state.copy(errorMessage = null) }
    }

    /** 공개 지도에 참여한다. 성공하면 화면이 상세로 넘어간다. */
    fun join() {
        if (_uiState.value.joining) return

        _uiState.update { state -> state.copy(joining = true, errorMessage = null) }
        joinJob?.cancel()
        joinJob = viewModelScope.launch {
            try {
                repository.joinMap(mapId)
                _uiState.update { state -> state.copy(joining = false, joined = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "지도 참여 실패 (mapId=$mapId)", e)
                _uiState.update { state ->
                    state.copy(
                        joining = false,
                        errorMessage = e.toUserMessage(JOIN_FAILED_MESSAGE),
                    )
                }
            }
        }
    }

    /**
     * 지도와 장소를 함께 읽는다.
     *
     * 장소만 실패해도 화면은 뜬다. 그 섹션만 비우고 지도 소개는 그대로 보여준다.
     */
    private suspend fun load(keepCurrent: Boolean) = coroutineScope {
        val placesDeferred = async { runCatchingPlaces() }

        val mapState = try {
            MapLoadState.Success(repository.getMapDetail(mapId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "지도 상세 조회 실패 (mapId=$mapId)", e)
            // 새로고침이 실패했는데 이미 보여줄 지도가 있으면 지우지 않는다.
            if (keepCurrent) null
            else MapLoadState.Error(e.toUserMessage(MAP_LOAD_FAILED_MESSAGE))
        }

        val places = placesDeferred.await()
        _uiState.update { state ->
            state.copy(
                map = mapState ?: state.map,
                places = places ?: state.places,
            )
        }
    }

    private suspend fun runCatchingPlaces(): MapPlacePreview? = try {
        repository.getPlacePreview(mapId, visibleCount = INTRO_PLACE_COUNT)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "지도 장소 목록 조회 실패 (mapId=$mapId)", e)
        null
    }
}
