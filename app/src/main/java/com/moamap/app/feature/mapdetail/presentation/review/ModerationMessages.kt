package com.moamap.app.feature.mapdetail.presentation.review

import com.moamap.app.core.common.NETWORK_ERROR_MESSAGE
import com.moamap.app.core.network.ApiException
import com.moamap.app.core.network.ConnectionException

internal const val REPORTED_TOAST = "신고가 접수됐어요"
internal const val REVIEW_DELETED_TOAST = "댓글을 삭제했어요"

private const val REPORT_FAILED_MESSAGE = "신고하지 못했어요. 잠시 후 다시 시도해주세요"
private const val BLOCK_FAILED_MESSAGE = "차단하지 못했어요. 잠시 후 다시 시도해주세요"

/** 시안 「OOO님을 차단했어요」. 이름을 모르면 「사용자를」로 채운다. */
internal fun blockedToast(nickname: String?): String =
    if (nickname.isNullOrBlank()) "사용자를 차단했어요" else "${nickname}님을 차단했어요"

/** 신고 실패 안내. 서버 원문 대신 오류 코드로 고른다 - iOS `ModerationMessage` 와 같은 문구다. */
internal fun Throwable.toReportFailureMessage(): String = when {
    this is ConnectionException -> NETWORK_ERROR_MESSAGE
    this !is ApiException -> REPORT_FAILED_MESSAGE
    code in setOf("PLACE_019", "PLACE_022", "MAP_026", "USER_025") -> "이미 신고했어요"
    code in setOf("PLACE_020", "PLACE_021", "MAP_025") -> "내 콘텐츠는 신고할 수 없어요"
    code == "USER_024" -> "나 자신은 신고할 수 없어요"
    code in setOf("PLACE_002", "MAP_002") -> "볼 수 없는 콘텐츠예요"
    code == "MAP_024" -> "공식 지도는 신고할 수 없어요"
    status == 404 -> "이미 삭제된 콘텐츠예요"
    else -> REPORT_FAILED_MESSAGE
}

internal fun Throwable.toBlockFailureMessage(): String = when {
    this is ConnectionException -> NETWORK_ERROR_MESSAGE
    this is ApiException && code == "USER_026" -> "나 자신은 차단할 수 없어요"
    this is ApiException && status == 404 -> "탈퇴했거나 없는 사용자예요"
    else -> BLOCK_FAILED_MESSAGE
}
