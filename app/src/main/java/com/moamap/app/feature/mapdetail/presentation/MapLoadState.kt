package com.moamap.app.feature.mapdetail.presentation

import com.moamap.app.feature.mapdetail.domain.model.MapDetail

/** 지도 한 건을 받아오는 동안의 상태. 설명 화면과 상세 화면이 함께 쓴다. */
sealed interface MapLoadState {
    data object Loading : MapLoadState
    data class Success(val map: MapDetail) : MapLoadState
    data class Error(val message: String) : MapLoadState
}

val MapLoadState.mapOrNull: MapDetail?
    get() = (this as? MapLoadState.Success)?.map

internal const val MAP_LOAD_FAILED_MESSAGE = "지도를 불러오지 못했어요"
internal const val JOIN_FAILED_MESSAGE = "지도에 참여하지 못했어요"
internal const val LEAVE_FAILED_MESSAGE = "지도에서 나가지 못했어요"
internal const val LIKE_FAILED_MESSAGE = "하트를 반영하지 못했어요"
internal const val LIKE_NEEDS_JOIN_MESSAGE = "지도에 참여하면 하트를 누를 수 있어요"

/** `[409] PLACE_010: 해당 지도에 이미 등록된 장소입니다.` 장소 추가·나만의 지도 추가가 가른다. */
internal const val DUPLICATE_PLACE_CODE = "PLACE_010"

/** `[403] PLACE_002: 해당 지도의 멤버가 아닙니다.` 활동 기록·장소 댓글이 가른다. */
internal const val NOT_MAP_MEMBER_CODE = "PLACE_002"
