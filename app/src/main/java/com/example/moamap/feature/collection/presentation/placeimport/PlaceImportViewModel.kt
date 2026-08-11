package com.example.moamap.feature.collection.presentation.placeimport

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moamap.core.navigation.MoaMapRoute
import com.example.moamap.core.network.ApiException
import com.example.moamap.core.network.ConnectionException
import com.example.moamap.feature.collection.domain.model.ImportedPlace
import com.example.moamap.feature.collection.domain.model.MapType
import com.example.moamap.feature.collection.domain.model.PlaceExtractionException
import com.example.moamap.feature.collection.domain.model.PlaceImportSource
import com.example.moamap.feature.collection.domain.model.PlaceSaveResult
import com.example.moamap.feature.collection.domain.repository.MapRepository
import com.example.moamap.feature.collection.domain.repository.PlaceImportRepository
import com.example.moamap.feature.collection.presentation.MyMapsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "PlaceImportViewModel"
private const val DEFAULT_EXTRACTION_ERROR = "장소를 가져오지 못했어요"
private const val MAPS_LOAD_FAILED_MESSAGE = "지도 목록을 불러오지 못했어요"
private const val DEFAULT_SAVE_ERROR = "장소를 저장하지 못했어요"
private const val NETWORK_ERROR_MESSAGE = "네트워크에 연결할 수 없어요"

/**
 * 장소 가져오기 4단계가 공유하는 ViewModel.
 *
 * 중첩 그래프의 back stack entry 에 스코프해서 4개 화면이 같은 인스턴스를 본다.
 * 흐름을 벗어나면 함께 정리되므로 다음에 다시 들어와도 이전 입력이 남지 않는다.
 */
@HiltViewModel
internal class PlaceImportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val placeImportRepository: PlaceImportRepository,
    private val mapRepository: MapRepository,
) : ViewModel() {

    private val source: PlaceImportSource = PlaceImportSource.valueOf(
        requireNotNull(savedStateHandle[MoaMapRoute.PlaceImport.ARG_SOURCE]),
    )

    /** 다른 앱에서 공유로 들어온 링크. 모음 탭으로 들어오면 비어 있다. */
    private val sharedUrl: String = savedStateHandle[MoaMapRoute.PlaceImport.ARG_URL] ?: ""

    private val _uiState = MutableStateFlow(PlaceImportUiState(source = source, url = sharedUrl))
    val uiState: StateFlow<PlaceImportUiState> = _uiState.asStateFlow()

    private var extractionJob: Job? = null

    private var mapsJob: Job? = null

    init {
        loadTargetMaps()
    }

    /**
     * 재시도를 취소했을 때 되돌아갈 직전 결과.
     *
     * 재시도는 이미 목록을 보고 있는 상태에서 시작하므로, 취소하면 보던 목록으로 돌아가야 한다.
     * 그냥 비워버리면 취소한 사용자가 결과를 잃고 URL 입력부터 다시 해야 한다.
     */
    private var previousResult: Pair<ExtractionState.Success, Set<String>>? = null

    fun updateUrl(url: String) {
        _uiState.update { state -> state.copy(url = url) }
    }

    /** 검색하기와 재시도가 함께 쓴다. */
    fun startExtraction() {
        val current = _uiState.value
        if (!current.canSearch) return

        extractionJob?.cancel()
        previousResult = (current.extraction as? ExtractionState.Success)
            ?.let { success -> success to current.selectedPlaceIds }

        _uiState.update { state ->
            state.copy(
                extraction = ExtractionState.Loading,
                selectedPlaceIds = emptySet(),
                errorMessage = null,
            )
        }

        extractionJob = viewModelScope.launch {
            val places = try {
                when (source) {
                    PlaceImportSource.Instagram ->
                        placeImportRepository.extractPlaces(current.url)

                    PlaceImportSource.MapShare ->
                        placeImportRepository.extractMapSharePlaces(current.url)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Throwable) {
                Log.e(TAG, "장소 추출 실패", throwable)
                failExtraction(throwable.toUserMessage())
                return@launch
            }

            // 새 결과가 나왔으니 되돌릴 대상도 사라진다.
            previousResult = null
            _uiState.update { state ->
                state.copy(
                    extraction = ExtractionState.Success(places),
                    selectedPlaceIds = initialSelection(places),
                )
            }
        }
    }

    /**
     * 로딩 중 뒤로가기. 진행 중이던 작업을 취소하고 직전 상태로 되돌린다.
     *
     * 진행 중이 아닐 때는 아무것도 하지 않는다. 이미 나온 결과를 지워버리면 안 된다.
     */
    fun cancelExtraction() {
        if (_uiState.value.extraction !is ExtractionState.Loading) return

        extractionJob?.cancel()
        extractionJob = null

        val restored = previousResult
        previousResult = null

        _uiState.update { state ->
            if (restored == null) {
                state.copy(extraction = ExtractionState.Idle)
            } else {
                state.copy(extraction = restored.first, selectedPlaceIds = restored.second)
            }
        }
    }

    /**
     * 추출에 실패해도 보고 있던 목록은 유지하고 안내만 띄운다.
     *
     * 결과를 지워버리면 재시도가 한 번 실패했다는 이유로 사용자가 처음부터 다시 해야 한다.
     */
    private fun failExtraction(message: String) {
        val restored = previousResult
        previousResult = null

        _uiState.update { state ->
            state.copy(
                extraction = restored?.first ?: ExtractionState.Idle,
                selectedPlaceIds = restored?.second.orEmpty(),
                errorMessage = message,
            )
        }
    }

    /**
     * 목록이 막 나왔을 때의 선택 상태.
     *
     * 외부 지도는 리스트를 통째로 가져오는 것이라 전부 고른 채로 시작하고 뺄 것만 해제하게 한다.
     * 인스타그램은 영상에서 찾은 후보라 사용자가 맞는 곳을 직접 고른다.
     */
    private fun initialSelection(places: List<ImportedPlace>): Set<String> = when (source) {
        PlaceImportSource.Instagram -> emptySet()

        PlaceImportSource.MapShare -> places
            .filter { place -> place.savable }
            .mapTo(mutableSetOf()) { place -> place.id }
    }

    /** 안내를 보여준 뒤 호출한다. 같은 메시지가 다시 뜨지 않게 한다. */
    fun consumeError() {
        _uiState.update { state -> state.copy(errorMessage = null) }
    }

    /** 한 링크에서 나온 장소를 여러 개 가져갈 수 있으므로 장소도 토글이다. */
    fun togglePlace(placeId: String) {
        _uiState.update { state ->
            // 등록 키가 없는 후보는 골라도 서버가 거절한다. 아예 선택되지 않게 둔다.
            if (state.places.none { place -> place.id == placeId && place.savable }) {
                return@update state
            }

            val selected = if (placeId in state.selectedPlaceIds) {
                state.selectedPlaceIds - placeId
            } else {
                state.selectedPlaceIds + placeId
            }
            state.copy(selectedPlaceIds = selected)
        }
    }

    /** 지도 목록을 읽지 못했을 때 다시 읽는다. */
    fun retryLoadMaps() = loadTargetMaps()

    private fun loadTargetMaps() {
        mapsJob?.cancel()
        _uiState.update { state -> state.copy(targetMaps = MyMapsState.Loading) }

        mapsJob = viewModelScope.launch {
            val maps = try {
                mapRepository.getMyMaps(MapType.Private)
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Throwable) {
                Log.w(TAG, "저장할 지도 목록 조회 실패", throwable)
                _uiState.update { state ->
                    state.copy(targetMaps = MyMapsState.Error(MAPS_LOAD_FAILED_MESSAGE))
                }
                return@launch
            }

            _uiState.update { state -> state.copy(targetMaps = MyMapsState.Success(maps)) }
        }
    }

    /**
     * 고른 장소를 고른 지도에 등록한다.
     *
     * 성공하면 [PlaceImportUiState.saveResult] 가 채워지고, 그 값을 보고 흐름을 빠져나간다.
     * 실패하면 고른 것을 그대로 둔 채 안내만 띄워 같은 자리에서 다시 누를 수 있게 한다.
     */
    fun savePlaces() {
        val current = _uiState.value
        if (!current.canSave || current.selectedPlaces.isEmpty()) return

        _uiState.update { state -> state.copy(saving = true, errorMessage = null) }

        viewModelScope.launch {
            val result = try {
                placeImportRepository.savePlaces(current.selectedMapIds, current.selectedPlaces)
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Throwable) {
                Log.e(TAG, "장소 저장 실패", throwable)
                _uiState.update { state ->
                    state.copy(saving = false, errorMessage = throwable.toSaveMessage())
                }
                return@launch
            }

            _uiState.update { state ->
                // 한 곳도 들어가지 않았는데 저장된 것처럼 흐름을 닫으면 안 된다.
                if (result.created == 0) {
                    state.copy(saving = false, errorMessage = result.toEmptyMessage())
                } else {
                    state.copy(saving = false, saveResult = result)
                }
            }
        }
    }

    /** 한 장소를 여러 지도에 넣을 수 있으므로 지도는 토글이다. */
    fun toggleMap(mapId: Long) {
        _uiState.update { state ->
            val selected = if (mapId in state.selectedMapIds) {
                state.selectedMapIds - mapId
            } else {
                state.selectedMapIds + mapId
            }
            state.copy(selectedMapIds = selected)
        }
    }
}

/** 등록된 것이 하나도 없을 때의 안내. 전부 중복인 경우와 진짜 실패를 구분한다. */
private fun PlaceSaveResult.toEmptyMessage(): String =
    if (failed == 0) "이미 저장되어 있는 장소예요" else DEFAULT_SAVE_ERROR

private fun Throwable.toSaveMessage(): String = when (this) {
    // 권한이나 정원 같은 등록 거절 사유는 사용자가 조치할 수 있어 그대로 노출한다.
    is ApiException -> serverMessage.ifBlank { DEFAULT_SAVE_ERROR }
    is ConnectionException -> NETWORK_ERROR_MESSAGE
    else -> DEFAULT_SAVE_ERROR
}

private fun Throwable.toUserMessage(): String = when (this) {
    // 캡션을 못 읽은 이유는 사용자가 조치할 수 있는 내용이라 그대로 노출한다.
    is PlaceExtractionException -> message ?: DEFAULT_EXTRACTION_ERROR
    is ApiException -> serverMessage.ifBlank { DEFAULT_EXTRACTION_ERROR }
    is ConnectionException -> NETWORK_ERROR_MESSAGE
    else -> DEFAULT_EXTRACTION_ERROR
}
