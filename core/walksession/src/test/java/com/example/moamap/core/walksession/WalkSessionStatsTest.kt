package com.example.moamap.core.walksession

import org.junit.Assert.assertEquals
import org.junit.Test

class WalkSessionStatsTest {

    private val start = 1_700_000_000_000L
    private val minute = 60_000L

    private fun session(samples: List<WalkSample>, durationMinutes: Long) =
        sessionMillis(samples, durationMillis = durationMinutes * minute)

    private fun sessionMillis(samples: List<WalkSample>, durationMillis: Long) = WalkSessionPayload(
        clientSessionId = "s",
        startedAtEpochMillis = start,
        endedAtEpochMillis = start + durationMillis,
        samples = samples,
    )

    @Test
    fun `위치 샘플과 심박 샘플을 따로 센다`() {
        val stats = session(
            samples = listOf(
                WalkSample(tsEpochMillis = start, lat = 37.5, lng = 127.0),
                WalkSample(tsEpochMillis = start + 1_000, hr = 80.0),
                WalkSample(tsEpochMillis = start + 2_000, hr = 82.0),
            ),
            durationMinutes = 1,
        ).computeStats()

        assertEquals(3, stats.sampleCount)
        assertEquals(1, stats.locationSampleCount)
        assertEquals(2, stats.heartRateSampleCount)
    }

    @Test
    fun `세션 길이는 종료 시각에서 시작 시각을 뺀 값이다`() {
        val stats = session(samples = emptyList(), durationMinutes = 10).computeStats()

        assertEquals(10 * minute, stats.durationMillis)
    }

    @Test
    fun `심박 커버리지는 심박이 관측된 1분 구간의 비율이다`() {
        // 10분 세션에서 0분, 1분, 2분 구간에만 심박이 있으면 커버리지 0.3
        val samples = listOf(0L, 1L, 2L).map { m ->
            WalkSample(tsEpochMillis = start + m * minute, hr = 80.0)
        }

        val stats = session(samples = samples, durationMinutes = 10).computeStats()

        assertEquals(0.3, stats.heartRateCoverageRatio, 0.001)
    }

    @Test
    fun `같은 1분 구간에 심박이 여러 개 있어도 한 구간으로 센다`() {
        val samples = listOf(0L, 10_000L, 20_000L, 30_000L).map { offset ->
            WalkSample(tsEpochMillis = start + offset, hr = 80.0)
        }

        val stats = session(samples = samples, durationMinutes = 10).computeStats()

        assertEquals(0.1, stats.heartRateCoverageRatio, 0.001)
    }

    @Test
    fun `세션 길이가 0이면 커버리지는 0이다`() {
        val stats = session(samples = emptyList(), durationMinutes = 0).computeStats()

        assertEquals(0.0, stats.heartRateCoverageRatio, 0.001)
    }

    @Test
    fun `5분30초 세션은 끝의 30초도 하나의 구간으로 세어 커버리지가 1을 넘지 않는다`() {
        // 5분30초 세션은 6개 구간(0~5분)을 갖는다. 0,1,2,3,4,5분 지점에 심박이 있으면
        // 6개 구간을 모두 채우므로 커버리지는 6/6 = 1.0이다.
        val samples = listOf(0L, 60_000L, 120_000L, 180_000L, 240_000L, 315_000L).map { offset ->
            WalkSample(tsEpochMillis = start + offset, hr = 80.0)
        }

        val stats = sessionMillis(samples = samples, durationMillis = 5 * minute + 30_000L).computeStats()

        assertEquals(1.0, stats.heartRateCoverageRatio, 0.001)
        assertEquals(true, stats.heartRateCoverageRatio <= 1.0)
    }

    @Test
    fun `1분 단위가 아닌 세션에서 일부 구간만 채워지면 정확한 비율을 반환한다`() {
        // 2분30초 세션은 3개 구간(0,1,2분)을 갖는다. 0분 구간에만 심박이 있으면 1/3.
        val samples = listOf(
            WalkSample(tsEpochMillis = start, hr = 80.0),
        )

        val stats = sessionMillis(samples = samples, durationMillis = 2 * minute + 30_000L).computeStats()

        assertEquals(1.0 / 3.0, stats.heartRateCoverageRatio, 0.001)
    }

    @Test
    fun `시작 시각 이전에 찍힌 심박 샘플은 커버리지에 포함되지 않는다`() {
        val samples = listOf(
            WalkSample(tsEpochMillis = start - 1_000L, hr = 80.0),
        )

        val stats = session(samples = samples, durationMinutes = 10).computeStats()

        assertEquals(0.0, stats.heartRateCoverageRatio, 0.001)
    }

    @Test
    fun `1분 미만 세션에 심박 샘플이 있으면 커버리지는 1이다`() {
        val samples = listOf(
            WalkSample(tsEpochMillis = start, hr = 80.0),
        )

        val stats = sessionMillis(samples = samples, durationMillis = 30_000L).computeStats()

        assertEquals(1.0, stats.heartRateCoverageRatio, 0.001)
    }
}
