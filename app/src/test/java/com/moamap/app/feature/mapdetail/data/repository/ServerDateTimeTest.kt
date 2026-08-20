package com.moamap.app.feature.mapdetail.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

class ServerDateTimeTest {

    @Test
    fun `지역 표시가 없으면 기기 시간대로 읽는다`() {
        val parsed = parseServerDateTime("2026-07-30T02:54:12")

        assertEquals(millisOf(TimeZone.getDefault()), parsed)
    }

    @Test
    fun `소수점 이하 초는 버린다`() {
        // 마이크로초까지 와도 초 단위 값과 같아야 한다.
        assertEquals(
            parseServerDateTime("2026-07-30T02:54:12"),
            parseServerDateTime("2026-07-30T02:54:12.123456"),
        )
    }

    @Test
    fun `지역 표시가 붙어 오면 그대로 따른다`() {
        assertEquals(millisOf(TimeZone.getTimeZone("UTC")), parseServerDateTime("2026-07-30T02:54:12Z"))
        assertEquals(
            millisOf(TimeZone.getTimeZone("GMT+09:00")),
            parseServerDateTime("2026-07-30T02:54:12+09:00"),
        )
        assertEquals(
            millisOf(TimeZone.getTimeZone("GMT+09:00")),
            parseServerDateTime("2026-07-30T02:54:12.500+0900"),
        )
    }

    @Test
    fun `읽을 수 없는 시각은 비운다`() {
        assertNull(parseServerDateTime(null))
        assertNull(parseServerDateTime(""))
        assertNull(parseServerDateTime("2026-07-30"))
        assertNull(parseServerDateTime("어제"))
        // 관대하게 읽으면 13월이 다음 해 1월로 넘어가 조용히 통과한다.
        assertNull(parseServerDateTime("2026-13-30T02:54:12"))
    }

    /** 2026-07-30 02:54:12 을 주어진 시간대에서 읽었을 때의 epoch millis. */
    private fun millisOf(zone: TimeZone): Long =
        GregorianCalendar(zone).apply {
            clear()
            set(2026, Calendar.JULY, 30, 2, 54, 12)
        }.timeInMillis
}
