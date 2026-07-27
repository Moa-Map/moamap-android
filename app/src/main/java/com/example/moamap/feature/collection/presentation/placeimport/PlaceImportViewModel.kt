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

    fun updateUrl(url: String) {
        _uiState.update { state -> state.copy(url = url) }
    }

    /** 검색하기와 재시도가 함께 쓴다. 이전 결과와 선택은 버리고 처음부터 다시 한다. */
    fun startExtraction() {
        if (!_uiState.value.canSearch) return

        extractionJob?.cancel()
        _uiState.update { state ->
            state.copy(extraction = ExtractionState.Loading, selectedPlaceId = null)
        }

        extractionJob = viewModelScope.launch {
            delay(EXTRACTION_DELAY_MILLIS)
            _uiState.update { state ->
                state.copy(extraction = ExtractionState.Success(SamplePlaces))
            }
        }
    }

    /** 로딩 중 뒤로가기. 진행 중이던 작업을 취소하고 입력 화면 상태로 되돌린다. */
    fun cancelExtraction() {
        extractionJob?.cancel()
        extractionJob = null
        _uiState.update { state -> state.copy(extraction = ExtractionState.Idle) }
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
