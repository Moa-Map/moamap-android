package com.example.moamap.core.common.format

import org.junit.Assert.assertEquals
import org.junit.Test

class MemberCountFormatTest {

    @Test
    fun `천 미만은 그대로 명으로 쓴다`() {
        assertEquals("0명", formatMemberCount(0))
        assertEquals("1명", formatMemberCount(1))
        assertEquals("999명", formatMemberCount(999))
    }

    @Test
    fun `천 단위는 소수 첫째 자리까지만 남긴다`() {
        assertEquals("1천명", formatMemberCount(1_000))
        assertEquals("2.3천명", formatMemberCount(2_312))
        assertEquals("9.9천명", formatMemberCount(9_999))
    }

    @Test
    fun `만 단위로 넘어가면 만명으로 쓴다`() {
        assertEquals("1만명", formatMemberCount(10_000))
        assertEquals("1.2만명", formatMemberCount(12_500))
    }

    @Test
    fun `내림하므로 실제 인원보다 많아 보이지 않는다`() {
        // 9990명이 "1만명" 으로 부풀지 않아야 한다.
        assertEquals("9.9천명", formatMemberCount(9_990))
        assertEquals("2천명", formatMemberCount(2_099))
    }
}
