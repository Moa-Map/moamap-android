package com.example.moamap.wear.record

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PointSendStateTest {

    @Test
    fun `대기에서 보내기를 요청하면 전송 중이 된다`() {
        assertEquals(PointSendState.Sending, PointSendState.Idle.onSendRequested())
    }

    @Test
    fun `실패 상태에서 다시 보내면 전송 중이 된다`() {
        val state = PointSendState.Failed("위치를 못 찾았어요").onSendRequested()

        assertEquals(PointSendState.Sending, state)
    }

    @Test
    fun `이미 전송 중이면 요청을 무시한다`() {
        // 연속 탭으로 전송이 두 개 뜨면 같은 좌표가 폰에 두 번 쌓인다.
        assertEquals(PointSendState.Sending, PointSendState.Sending.onSendRequested())
    }

    @Test
    fun `전송에 성공하면 보냄으로 바뀐다`() {
        val state = PointSendState.Sending.onSendResult(success = true, failureMessage = "무시됨")

        assertEquals(PointSendState.Sent, state)
    }

    @Test
    fun `전송에 실패하면 사유를 담은 실패가 된다`() {
        val state = PointSendState.Sending.onSendResult(
            success = false,
            failureMessage = "폰에 보내지 못했어요",
        )

        assertEquals(PointSendState.Failed("폰에 보내지 못했어요"), state)
    }

    @Test
    fun `전송 중이 아닐 때 도착한 결과는 화면을 덮어쓰지 않는다`() {
        // 사용자가 이미 결과를 보고 다음 동작으로 넘어간 뒤 늦게 도착한 응답이
        // 화면을 되돌리면 안 된다.
        val state = PointSendState.Idle.onSendResult(success = true, failureMessage = "무시됨")

        assertEquals(PointSendState.Idle, state)
    }

    @Test
    fun `보냄을 다 보여주면 대기로 돌아간다`() {
        assertEquals(PointSendState.Idle, PointSendState.Sent.onSentShown())
    }

    @Test
    fun `실패는 저절로 사라지지 않는다`() {
        // 실패를 못 보고 지나치면 사용자는 보냈다고 믿는다.
        val failed = PointSendState.Failed("위치를 못 찾았어요")

        assertEquals(failed, failed.onSentShown())
    }

    @Test
    fun `전송 중일 때만 바쁜 상태다`() {
        assertTrue(PointSendState.Sending.isBusy)
        assertFalse(PointSendState.Idle.isBusy)
        assertFalse(PointSendState.Sent.isBusy)
        assertFalse(PointSendState.Failed("x").isBusy)
    }
}
