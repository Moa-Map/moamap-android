package com.moamap.app.core.common

// 완료 토스트 문구. 시안 릴리즈 v1 토스트 예시 그대로다(장소 추가는 두 시안 중 「장소를 추가했어요」로 통일, 10-26 사용자 결정).

internal const val MAP_LIST_UPDATED_TOAST = "지도 목록이 업데이트 됐어요"
internal const val MAP_CREATED_TOAST = "새 지도를 만들었어요"
internal const val PLACE_ADDED_TOAST = "장소를 추가했어요"
internal const val PLACE_REQUESTED_TOAST = "추가 요청을 보냈어요"
internal const val MAP_JOINED_TOAST = "지도에 참여했어요"
internal const val MAP_INFO_UPDATED_TOAST = "지도 정보가 수정됐어요"
internal const val PLACE_UPDATED_TOAST = "장소 정보가 수정됐어요"

/** 장소 삭제는 시안이 없다(이슈 #376 에서 정함). */
internal const val PLACE_DELETED_TOAST = "장소를 삭제했어요"

/** 차단 해제도 시안이 없다(이슈 #392 에서 정함). */
internal const val UNBLOCKED_TOAST = "차단을 해제했어요"

/** 장소를 넣은 뒤의 토스트. 한 곳이라도 승인을 기다려야 하면 요청을 보냈다고 알린다(10-26 사용자 결정). */
internal fun placeAddedToast(needsApproval: Boolean): String =
    if (needsApproval) PLACE_REQUESTED_TOAST else PLACE_ADDED_TOAST
