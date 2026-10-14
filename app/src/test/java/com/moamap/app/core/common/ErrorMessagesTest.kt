package com.moamap.app.core.common

import com.moamap.app.core.network.ApiException
import com.moamap.app.core.network.model.UNKNOWN_ERROR_CODE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ErrorMessagesTest {

    @Test
    fun `각 서비스의 4xx 안내는 그대로 보여 준다`() {
        val error = ApiException(code = "PLACE_015", status = 400, serverMessage = "지원하지 않는 링크입니다.")

        assertEquals("지원하지 않는 링크입니다.", error.actionableMessage())
    }

    @Test
    fun `서버 오류는 보여 주지 않는다`() {
        val error = ApiException(code = "COMMON_006", status = 500, serverMessage = "서버 내부 오류가 발생했습니다.")

        assertNull(error.actionableMessage())
    }

    @Test
    fun `공통 오류는 4xx 라도 보여 주지 않는다`() {
        // 요청 형식 오류는 입력 칸 이름이 섞인 개발자용 문구가 올 수 있다.
        val error = ApiException(code = "COMMON_001", status = 400, serverMessage = "url: 공백일 수 없습니다")

        assertNull(error.actionableMessage())
    }

    @Test
    fun `본문을 읽지 못한 응답의 HTTP 상태 문구는 보여 주지 않는다`() {
        val error = ApiException(code = UNKNOWN_ERROR_CODE, status = 404, serverMessage = "Not Found")

        assertNull(error.actionableMessage())
    }

    @Test
    fun `빈 문구는 보여 주지 않는다`() {
        assertNull(ApiException(code = "USER_001", status = 401, serverMessage = " ").actionableMessage())
    }
}
