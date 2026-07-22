package com.example.moamap.wear.record

/** 전송 진행 상태. 종료 화면에서만 의미가 있다. */
enum class TransferState { SENDING, SUCCESS, FAILED }

/**
 * 워치 화면 상태. 화면은 3개뿐이다 — 시작 대기 / 기록 중 / 종료 확인.
 * 권한 거부는 시작 자체가 불가능한 상태라 별도로 둔다.
 */
sealed interface RecordUiState {

    data object Idle : RecordUiState

    data class Recording(
        val elapsedMillis: Long = 0,
        val latestHeartRate: Double? = null,
        val sampleCount: Int = 0,
    ) : RecordUiState

    data class Finished(
        val sampleCount: Int,
        val transferState: TransferState,
    ) : RecordUiState

    data class PermissionDenied(val message: String) : RecordUiState
}

fun RecordUiState.onStartRequested(): RecordUiState = when (this) {
    is RecordUiState.Recording -> this
    else -> RecordUiState.Recording()
}

/** 심박이 null인 샘플(위치만 들어온 경우)은 직전 심박 표시를 지우지 않는다. */
fun RecordUiState.onSampleObserved(elapsedMillis: Long, heartRate: Double?): RecordUiState =
    when (this) {
        is RecordUiState.Recording -> copy(
            elapsedMillis = elapsedMillis,
            latestHeartRate = heartRate ?: latestHeartRate,
            sampleCount = sampleCount + 1,
        )
        else -> this
    }

fun RecordUiState.onStopRequested(): RecordUiState = when (this) {
    is RecordUiState.Recording -> RecordUiState.Finished(
        sampleCount = sampleCount,
        transferState = TransferState.SENDING,
    )
    else -> this
}

/** 이미 SUCCESS/FAILED로 결착된 전송 결과는 종단 상태이므로 다시 덮어쓰지 않는다. */
fun RecordUiState.onTransferResult(success: Boolean): RecordUiState = when {
    this is RecordUiState.Finished && transferState == TransferState.SENDING ->
        copy(transferState = if (success) TransferState.SUCCESS else TransferState.FAILED)
    else -> this
}

fun RecordUiState.onRetryRequested(): RecordUiState = when {
    this is RecordUiState.Finished && transferState == TransferState.FAILED ->
        copy(transferState = TransferState.SENDING)
    else -> this
}
