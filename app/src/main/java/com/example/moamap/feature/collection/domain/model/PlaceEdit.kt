package com.example.moamap.feature.collection.domain.model

import android.net.Uri

/**
 * 편집 화면에서 장소 하나에 붙인 값.
 *
 * 사진은 아직 올리지 않은 [Uri] 다. 업로드는 등록 직전 한 번에 하므로, 편집을 오갈 때마다
 * 올리지 않는다 - 되돌리거나 흐름을 떠나면 올린 사진이 스토리지에 고아로 남는다.
 *
 * [ImportedPlace] 에 합치지 않고 나눠 둔다. 그쪽은 "링크에서 뽑아낸 결과"이고 이쪽은
 * "사용자가 덧붙인 값"이라, 합치면 다시 추출했을 때 무엇을 지우고 무엇을 남길지 흐려진다.
 */
data class PlaceEdit(
    val tags: List<String> = emptyList(),
    val memo: String = "",
    val photos: List<Uri> = emptyList(),
) {
    /** 등록 요청에 실을 것이 있는지. 편집 목록 카드가 편집 여부를 이 값으로 표시한다. */
    val isEmpty: Boolean
        get() = tags.isEmpty() && memo.isBlank() && photos.isEmpty()
}

/**
 * 등록 직전의 장소 한 건. 추출 결과와 편집값을 함께 들고 다닌다.
 *
 * 저장소가 둘을 따로 받으면 짝이 어긋날 수 있어 한 덩어리로 넘긴다.
 */
data class EditedPlace(
    val place: ImportedPlace,
    val edit: PlaceEdit,
)
