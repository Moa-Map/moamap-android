package com.example.moamap.feature.collection.presentation.placeimport

import androidx.compose.runtime.Immutable
import com.example.moamap.feature.collection.CollectionMapUiModel

/** 인스타그램 URL 에서 뽑아낸 장소 후보. */
@Immutable
internal data class ImportedPlaceUiModel(
    val id: Long,
    val name: String,
    val address: String,
)

/** 장소 추출 단계. 로딩 화면과 장소 선택 화면이 이 값으로 갈린다. */
internal sealed interface ExtractionState {

    data object Idle : ExtractionState

    data object Loading : ExtractionState

    data class Success(val places: List<ImportedPlaceUiModel>) : ExtractionState

    data class Error(val message: String) : ExtractionState
}

/**
 * 장소 가져오기 4단계가 함께 쓰는 상태.
 *
 * 단계마다 상태를 따로 들면 뒤로 오갈 때 값이 끊기므로 한 곳에 모은다.
 */
@Immutable
internal data class PlaceImportUiState(
    val url: String = "",
    val extraction: ExtractionState = ExtractionState.Idle,
    val selectedPlaceId: Long? = null,
    val targetMaps: List<CollectionMapUiModel> = emptyList(),
    val selectedMapIds: Set<Long> = emptySet(),
) {
    val places: List<ImportedPlaceUiModel>
        get() = (extraction as? ExtractionState.Success)?.places.orEmpty()

    val selectedPlace: ImportedPlaceUiModel?
        get() = places.firstOrNull { place -> place.id == selectedPlaceId }

    /** URL 이 비어 있으면 검색할 것이 없다. */
    val canSearch: Boolean get() = url.isNotBlank()

    /** 장소를 골라야 다음 단계로 넘어갈 수 있다. */
    val canProceed: Boolean get() = selectedPlace != null

    /** 지도를 하나도 고르지 않으면 저장할 곳이 없다. */
    val canSave: Boolean get() = selectedMapIds.isNotEmpty()
}
