package com.moamap.app.feature.mapdetail

import com.moamap.app.feature.mapdetail.domain.model.LeaveOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapLeaveDialogTextTest {

    @Test
    fun `공개 지도는 시안 문구 그대로다`() {
        assertEquals("나가시면 모음 탭에서 지도가 사라집니다", leaveConfirmMessage(LeaveOutcome.Leave))
    }

    @Test
    fun `프라이빗 지도는 초대코드가 필요하다고 덧붙인다`() {
        assertTrue(leaveConfirmMessage(LeaveOutcome.LeaveNeedsInviteCode).contains("초대코드"))
    }

    /** 되돌릴 수 없는 삭제가 걸린 갈래라 "사라진다" 수준으로 뭉개면 안 된다. */
    @Test
    fun `지도가 삭제되는 경우는 삭제된다고 분명히 알린다`() {
        val message = leaveConfirmMessage(LeaveOutcome.DeleteMap)

        assertTrue(message.contains("삭제"))
        assertTrue(message.contains("되돌릴 수 없"))
    }
}
