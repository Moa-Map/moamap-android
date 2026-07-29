package com.example.moamap.feature.collection.presentation.placeimport

import androidx.compose.runtime.Immutable
import com.example.moamap.feature.collection.CollectionMapUiModel
import com.example.moamap.feature.collection.domain.model.ImportedPlace
import com.example.moamap.feature.collection.domain.model.PlaceImportSource

/**
 * 장소 추출 단계. 로딩 화면과 장소 선택 화면이 이 값으로 갈린다.
 *
 * 실패는 여기 담지 않는다. 담아버리면 재시도가 실패했을 때 보고 있던 목록과 선택이
 * 통째로 날아간다. 실패는 [PlaceImportUiState.errorMessage] 로 따로 다룬다.
 */
internal sealed interface ExtractionState {

    data object Idle : ExtractionState

    data object Loading : ExtractionState

    data class Success(val places: List<ImportedPlace>) : ExtractionState
}

/**
 * 장소 가져오기 4단계가 함께 쓰는 상태.
 *
 * 단계마다 상태를 따로 들면 뒤로 오갈 때 값이 끊기므로 한 곳에 모은다.
 */
@Immutable
internal data class PlaceImportUiState(
    /** 어느 카드로 들어왔는지. 화면 문구와 버튼 구성이 이 값으로 갈린다. */
    val source: PlaceImportSource = PlaceImportSource.Instagram,
    val url: String = "",
    val extraction: ExtractionState = ExtractionState.Idle,
    val selectedPlaceIds: Set<String> = emptySet(),
    val targetMaps: List<CollectionMapUiModel> = emptyList(),
    val selectedMapIds: Set<Long> = emptySet(),
    /** 한 번 보여주고 소비하는 실패 안내. 추출 결과와 독립적이다. */
    val errorMessage: String? = null,
) {
    val places: List<ImportedPlace>
        get() = (extraction as? ExtractionState.Success)?.places.orEmpty()

    /**
     * 고른 장소들. 고른 순서가 아니라 목록에 나온 순서로 준다.
     *
     * 지도 선택 화면이 이 목록을 그대로 나열하므로, 체크한 순서대로 섞이면
     * 앞 화면에서 보던 배열과 어긋난다. 목록에 없는 id 는 자연히 걸러진다.
     */
    val selectedPlaces: List<ImportedPlace>
        get() = places.filter { place -> place.id in selectedPlaceIds }

    /** URL 이 비어 있으면 검색할 것이 없다. */
    val canSearch: Boolean get() = url.isNotBlank()

    /** 장소를 하나 이상 골라야 다음 단계로 넘어갈 수 있다. */
    val canProceed: Boolean get() = selectedPlaces.isNotEmpty()

    /** 지도를 하나도 고르지 않으면 저장할 곳이 없다. */
    val canSave: Boolean get() = selectedMapIds.isNotEmpty()
}
