package com.moamap.app.feature.mapdetail.presentation.addplace

import android.net.Uri
import androidx.compose.runtime.Immutable
import com.moamap.app.feature.collection.domain.model.MapType
import com.moamap.app.feature.mapdetail.domain.model.MapDetail
import com.moamap.app.feature.mapdetail.domain.model.MapRole
import com.moamap.app.feature.mapdetail.domain.model.PlaceCandidate

/** 서버가 한 장소에 최대 5장까지 받는다. */
const val MAX_PLACE_PHOTOS = 5

/** 서버가 태그 하나를 30자로 제한한다. */
const val MAX_TAG_LENGTH = 30

sealed interface PlaceSearchState {
    /** 아직 검색어를 넣지 않았다. */
    data object Idle : PlaceSearchState
    data object Loading : PlaceSearchState
    data class Success(val candidates: List<PlaceCandidate>) : PlaceSearchState
    data class Error(val message: String) : PlaceSearchState
}

/**
 * 장소 추가 시트 상태.
 *
 * 단계를 [selected] 하나로 가른다 - null 이면 검색, 값이 있으면 등록 폼이다. 별도 step 값을
 * 두면 둘이 어긋날 수 있다.
 */
@Immutable
data class AddPlaceUiState(
    val query: String = "",
    val search: PlaceSearchState = PlaceSearchState.Idle,
    val selected: PlaceCandidate? = null,
    val photos: List<Uri> = emptyList(),
    val tags: List<String> = emptyList(),
    val tagInput: String = "",
    val memo: String = "",
    /**
     * 이미 올려 둔 사진 주소.
     *
     * 등록이 실패해도 올린 사진은 스토리지에 남는다(지울 API 가 없다). 다시 시도할 때
     * 또 올리면 고아 파일이 시도할 때마다 쌓이므로, 한 번 올린 건 여기 두고 재사용한다.
     */
    val uploadedPhotoUrls: List<String> = emptyList(),
    val submitting: Boolean = false,
    /** 한 번 보여주고 지우는 실패 안내. */
    val errorMessage: String? = null,
    /** 등록이 끝났다. 화면이 이 신호로 시트를 닫는다. 안내 문구가 함께 담긴다. */
    val addedMessage: String? = null,
) {
    val isFormStep: Boolean get() = selected != null

    val canAddPhoto: Boolean get() = photos.size < MAX_PLACE_PHOTOS
}

/**
 * 이 지도에 장소를 바로 넣을 수 있는지.
 *
 * 프라이빗은 역할을 보지 않는다. 권한 분류가 없는 지도다.
 *
 * 실제 등록 상태(APPROVED/PENDING)는 **서버가 정한다.** 이 값은 버튼 글씨와 완료 안내를
 * 고르는 데만 쓴다.
 */
val MapDetail.addsPlaceDirectly: Boolean
    get() = type == MapType.Private || role == MapRole.Owner || role == MapRole.Admin

/** 등록 버튼 글씨. */
fun addPlaceButtonLabel(map: MapDetail): String =
    if (map.addsPlaceDirectly) "추가하기" else "추가 요청 보내기"

/**
 * 등록 완료 안내.
 *
 * 승인 대기는 목록에 나타나지 않는다(`GET /places` 가 APPROVED 만 준다). 안내가 없으면
 * 사라진 것처럼 보인다.
 */
fun addPlaceDoneMessage(map: MapDetail): String =
    if (map.addsPlaceDirectly) "장소를 추가했어요" else "추가 요청을 보냈어요"
