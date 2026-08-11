package com.example.moamap.wear.record

import com.example.moamap.core.walksession.SessionKind
import com.example.moamap.core.walksession.WalkSample
import com.example.moamap.core.walksession.WalkSessionPayload

/**
 * 현재 위치 한 점을 폰으로 보내는 동안의 상태.
 *
 * [RecordUiState] 와 일부러 분리한다. 위치 보내기는 기록 중이든 아니든 똑같이 동작해야
 * 하는데, 기록 상태에 섞으면 기록의 모든 분기가 이 상태를 중복해서 들고 다녀야 한다.
 */
sealed interface PointSendState {

    data object Idle : PointSendState

    data object Sending : PointSendState

    data object Sent : PointSendState

    data class Failed(val message: String) : PointSendState
}

/** 전송이 도는 동안에는 다른 버튼도 함께 잠근다. */
val PointSendState.isBusy: Boolean
    get() = this == PointSendState.Sending

/** 이미 도는 전송이 있으면 그대로 둔다 - 취소하지 않는다. */
fun PointSendState.onSendRequested(): PointSendState = when (this) {
    PointSendState.Sending -> this
    else -> PointSendState.Sending
}

/**
 * 전송 중일 때만 결과를 반영한다.
 *
 * 사용자가 결과를 보고 넘어간 뒤 늦게 도착한 응답이 화면을 되돌리면 안 된다.
 */
fun PointSendState.onSendResult(success: Boolean, failureMessage: String): PointSendState =
    when {
        this != PointSendState.Sending -> this
        success -> PointSendState.Sent
        else -> PointSendState.Failed(failureMessage)
    }

/**
 * 성공 안내를 다 보여준 뒤 대기로 되돌린다.
 *
 * 실패는 되돌리지 않는다. 저절로 사라지면 사용자가 못 보고 지나쳐 보냈다고 믿게 된다.
 */
fun PointSendState.onSentShown(): PointSendState = when (this) {
    PointSendState.Sent -> PointSendState.Idle
    else -> this
}

/**
 * 좌표 한 점을 세션 페이로드로 감싼다.
 *
 * 시작과 종료가 같은 시각이다. 길이가 없는 기록이라 그 사이에 채울 것이 없다.
 */
internal fun singlePointPayload(
    clientSessionId: String,
    sample: WalkSample,
): WalkSessionPayload = WalkSessionPayload(
    clientSessionId = clientSessionId,
    kind = SessionKind.SINGLE_POINT,
    startedAtEpochMillis = sample.tsEpochMillis,
    endedAtEpochMillis = sample.tsEpochMillis,
    samples = listOf(sample),
)
