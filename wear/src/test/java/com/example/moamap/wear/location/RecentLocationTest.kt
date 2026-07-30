package com.example.moamap.wear.location

import com.example.moamap.core.walksession.WalkSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecentLocationTest {

    private val now = 1_700_000_100_000L
    private val maxAge = 30_000L

    @Test
    fun `최근 위치 샘플이 있으면 그것을 쓴다`() {
        val samples = listOf(
            WalkSample(tsEpochMillis = now - 60_000, lat = 37.1, lng = 127.1),
            WalkSample(tsEpochMillis = now - 5_000, lat = 37.2, lng = 127.2),
        )

        val picked = latestFreshLocation(samples, now, maxAge)

        assertEquals(37.2, picked?.lat)
    }

    @Test
    fun `심박만 있는 샘플은 위치로 쓰지 않는다`() {
        // 심박과 위치는 별개의 행으로 들어온다. 마지막 샘플이 심박이라고 해서
        // 위치가 없는 것은 아니다.
        val samples = listOf(
            WalkSample(tsEpochMillis = now - 5_000, lat = 37.2, lng = 127.2),
            WalkSample(tsEpochMillis = now - 1_000, hr = 82.0),
        )

        val picked = latestFreshLocation(samples, now, maxAge)

        assertEquals(37.2, picked?.lat)
    }

    @Test
    fun `오래된 위치는 쓰지 않는다`() {
        // 30초 전 좌표를 "현재 위치"라고 보내면 사용자가 이미 지나온 곳이 기록된다.
        val samples = listOf(WalkSample(tsEpochMillis = now - 31_000, lat = 37.2, lng = 127.2))

        assertNull(latestFreshLocation(samples, now, maxAge))
    }

    @Test
    fun `미래 시각 샘플은 쓰지 않는다`() {
        // 부팅 시각 보정이 어긋나면 미래 타임스탬프가 만들어질 수 있다.
        val samples = listOf(WalkSample(tsEpochMillis = now + 5_000, lat = 37.2, lng = 127.2))

        assertNull(latestFreshLocation(samples, now, maxAge))
    }

    @Test
    fun `샘플이 없으면 null 이다`() {
        assertNull(latestFreshLocation(emptyList(), now, maxAge))
    }
}
