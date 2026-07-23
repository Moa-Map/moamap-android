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

/**
 * 심박이 null인 샘플(위치만 들어온 경우)은 직전 심박 표시를 지우지 않는다.
 *
 * [sampleCount] 는 이번 배치까지 누적된 전체 샘플 수(권위값)이며, 배치 수가 아니다 —
 * ExerciseRecorder.samples 는 매번 "지금까지 전체 목록"을 emit 하므로 호출 1회당
 * +1 하면 배치 수를 세게 되어 실제 샘플 수를 과소 집계한다. 그래서 여기서는 절대값을
 * 그대로 대입한다.
 *
 * 주의(shadowing): 아래 `copy(...)` 안의 `sampleCount = sampleCount`에서 우변의
 * `sampleCount`는 Recording.sampleCount 프로퍼티가 아니라 이 함수의 파라미터를
 * 가리킨다 — Kotlin은 로컬 파라미터를 암시적 리시버 멤버보다 우선 해석하기 때문이다.
 * 즉 "이전 값 + 1"이 아니라 "이번에 전달된 전체 개수로 교체"가 의도된 동작이다.
 */
fun RecordUiState.onSampleObserved(
    elapsedMillis: Long,
    heartRate: Double?,
    sampleCount: Int,
): RecordUiState =
    when (this) {
        is RecordUiState.Recording -> copy(
            elapsedMillis = elapsedMillis,
            latestHeartRate = heartRate ?: latestHeartRate,
            sampleCount = sampleCount,
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
