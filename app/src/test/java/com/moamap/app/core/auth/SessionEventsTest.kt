package com.moamap.app.core.auth

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionEventsTest {

    /** 화면이 수집을 시작하기 전에 세션이 끝날 수 있다. */
    @Test
    fun `받는 쪽이 없을 때 보낸 신호도 나중에 받는다`() = runTest {
        val events = SessionEvents()

        events.notifySessionExpired()

        assertEquals(Unit, withTimeoutOrNull(1_000) { events.sessionExpired.first() })
    }

    /**
     * 동시에 여러 요청이 거부돼도 로그인 화면으로는 한 번만 보낸다.
     *
     * 받은 신호가 남아 있으면 다시 로그인한 뒤에도 로그인 화면으로 튕긴다.
     */
    @Test
    fun `연달아 보낸 신호는 하나로 합치고 받은 뒤에는 남기지 않는다`() = runTest {
        val events = SessionEvents()

        events.notifySessionExpired()
        events.notifySessionExpired()
        events.sessionExpired.first()

        assertNull(withTimeoutOrNull(1_000) { events.sessionExpired.first() })
    }
}
