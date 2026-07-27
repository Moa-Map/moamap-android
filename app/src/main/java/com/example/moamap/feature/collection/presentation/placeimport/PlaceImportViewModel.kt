package com.example.moamap.feature.collection.presentation.placeimport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moamap.feature.collection.CollectionMapUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 실제 추출이 붙기 전까지 로딩 화면을 보여주기 위한 지연. */
private const val EXTRACTION_DELAY_MILLIS = 2_000L

// TODO: API 연동 시 InstagramCaptionExtractor + POST /api/v1/places/instagram-extractions 로 교체한다.
private val SamplePlaces = listOf(
    ImportedPlaceUiModel(id = 1L, name = "커피나무", address = "서울시 동작구 369"),
    ImportedPlaceUiModel(id = 2L, name = "블루보틀 성수", address = "서울시 성동구 아차산로 7"),
    ImportedPlaceUiModel(id = 3L, name = "노티드 도넛", address = "서울시 강남구 압구정로 42길"),
)

// TODO: API 연동 시 GET /api/v1/maps/me 로 교체한다.
private val SampleTargetMaps = listOf(
    CollectionMapUiModel(id = 11L, title = "내 지도", placeCount = "128곳"),
    CollectionMapUiModel(id = 12L, title = "성수 카페 투어", placeCount = "24곳"),
    CollectionMapUiModel(id = 13L, title = "주말 데이트", placeCount = "8곳"),
)

/**
 * 장소 가져오기 4단계가 공유하는 ViewModel.
 *
 * 중첩 그래프의 back stack entry 에 스코프해서 4개 화면이 같은 인스턴스를 본다.
 * 흐름을 벗어나면 함께 정리되므로 다음에 다시 들어와도 이전 입력이 남지 않는다.
 */
@HiltViewModel
internal class PlaceImportViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(PlaceImportUiState(targetMaps = SampleTargetMaps))
    val uiState: StateFlow<PlaceImportUiState> = _uiState.asStateFlow()

    private var extractionJob: Job? = null

    /**
     * 재시도를 취소했을 때 되돌아갈 직전 결과.
     *
     * 재시도는 이미 목록을 보고 있는 상태에서 시작하므로, 취소하면 보던 목록으로 돌아가야 한다.
     * 그냥 비워버리면 취소한 사용자가 결과를 잃고 URL 입력부터 다시 해야 한다.
     */
    private var previousResult: Pair<ExtractionState.Success, Long?>? = null

    fun updateUrl(url: String) {
        _uiState.update { state -> state.copy(url = url) }
    }

    /** 검색하기와 재시도가 함께 쓴다. */
    fun startExtraction() {
        val current = _uiState.value
        if (!current.canSearch) return

        extractionJob?.cancel()
        previousResult = (current.extraction as? ExtractionState.Success)
            ?.let { success -> success to current.selectedPlaceId }

        _uiState.update { state ->
            state.copy(extraction = ExtractionState.Loading, selectedPlaceId = null)
        }

        extractionJob = viewModelScope.launch {
            delay(EXTRACTION_DELAY_MILLIS)
            // 새 결과가 나왔으니 되돌릴 대상도 사라진다.
            previousResult = null
            _uiState.update { state ->
                state.copy(extraction = ExtractionState.Success(SamplePlaces))
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
                state.copy(extraction = restored.first, selectedPlaceId = restored.second)
            }
        }
    }

    /** 장소는 하나만 고른다. */
    fun selectPlace(placeId: Long) {
        _uiState.update { state -> state.copy(selectedPlaceId = placeId) }
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
