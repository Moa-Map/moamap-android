package com.example.moamap.wear.record

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordUiStateTest {

    @Test
    fun `대기 상태에서 시작하면 기록 중으로 바뀐다`() {
        val state = RecordUiState.Idle.onStartRequested()

        assertTrue(state is RecordUiState.Recording)
        assertEquals(0, (state as RecordUiState.Recording).sampleCount)
    }

    @Test
    fun `샘플이 관측되면 경과 시간과 심박과 샘플 수가 갱신된다`() {
        val state = RecordUiState.Idle
            .onStartRequested()
            .onSampleObserved(elapsedMillis = 5_000, heartRate = 82.0)
            .onSampleObserved(elapsedMillis = 10_000, heartRate = null)

        val recording = state as RecordUiState.Recording
        assertEquals(10_000, recording.elapsedMillis)
        assertEquals(2, recording.sampleCount)
    }

    @Test
    fun `심박이 없는 샘플은 직전 심박 값을 유지한다`() {
        val state = RecordUiState.Idle
            .onStartRequested()
            .onSampleObserved(elapsedMillis = 1_000, heartRate = 75.0)
            .onSampleObserved(elapsedMillis = 2_000, heartRate = null)

        assertEquals(75.0, (state as RecordUiState.Recording).latestHeartRate!!, 0.001)
    }

    @Test
    fun `기록 중이 아닐 때 관측된 샘플은 무시한다`() {
        val state = RecordUiState.Idle.onSampleObserved(elapsedMillis = 1_000, heartRate = 80.0)

        assertEquals(RecordUiState.Idle, state)
    }

    @Test
    fun `종료하면 전송 중 상태의 완료 화면으로 간다`() {
        val state = RecordUiState.Idle
            .onStartRequested()
            .onSampleObserved(elapsedMillis = 1_000, heartRate = 80.0)
            .onStopRequested()

        val finished = state as RecordUiState.Finished
        assertEquals(1, finished.sampleCount)
        assertEquals(TransferState.SENDING, finished.transferState)
    }

    @Test
    fun `전송 성공과 실패가 완료 화면에 반영된다`() {
        val finished = RecordUiState.Idle.onStartRequested().onStopRequested()

        assertEquals(
            TransferState.SUCCESS,
            (finished.onTransferResult(success = true) as RecordUiState.Finished).transferState,
        )
        assertEquals(
            TransferState.FAILED,
            (finished.onTransferResult(success = false) as RecordUiState.Finished).transferState,
        )
    }

    @Test
    fun `전송 실패 후 재시도하면 다시 전송 중이 된다`() {
        val failed = RecordUiState.Idle
            .onStartRequested()
            .onStopRequested()
            .onTransferResult(success = false)

        val retried = failed.onRetryRequested() as RecordUiState.Finished

        assertEquals(TransferState.SENDING, retried.transferState)
    }

    @Test
    fun `전송에 성공한 뒤에는 재시도해도 상태가 바뀌지 않는다`() {
        val succeeded = RecordUiState.Idle
            .onStartRequested()
            .onStopRequested()
            .onTransferResult(success = true)

        assertEquals(succeeded, succeeded.onRetryRequested())
    }

    @Test
    fun `전송 성공 후에는 전송 결과가 다시 들어와도 상태가 바뀌지 않는다`() {
        val succeeded = RecordUiState.Idle
            .onStartRequested()
            .onStopRequested()
            .onTransferResult(success = true)

        assertEquals(succeeded, succeeded.onTransferResult(success = false))
    }

    @Test
    fun `전송 실패 후에는 전송 결과가 다시 들어와도 상태가 바뀌지 않는다`() {
        val failed = RecordUiState.Idle
            .onStartRequested()
            .onStopRequested()
            .onTransferResult(success = false)

        assertEquals(failed, failed.onTransferResult(success = true))
    }
}
