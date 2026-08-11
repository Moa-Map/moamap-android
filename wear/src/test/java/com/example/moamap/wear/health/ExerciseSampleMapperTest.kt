package com.example.moamap.wear.health

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExerciseSampleMapperTest {

    private val bootEpochMillis = 1_700_000_000_000L

    @Test
    fun `부팅 기준 경과 시간을 실제 시각으로 바꾼다`() {
        val samples = toWalkSamples(
            bootEpochMillis = bootEpochMillis,
            locations = listOf(RawLocationReading(5_000, 37.5665, 126.9780, 12.0)),
            heartRates = emptyList(),
        )

        assertEquals(bootEpochMillis + 5_000, samples.single().tsEpochMillis)
    }

    @Test
    fun `위치 샘플과 심박 샘플이 시간 순으로 병합된다`() {
        val samples = toWalkSamples(
            bootEpochMillis = bootEpochMillis,
            locations = listOf(
                RawLocationReading(3_000, 37.5, 127.0, null),
                RawLocationReading(1_000, 37.4, 126.9, null),
            ),
            heartRates = listOf(RawHeartRateReading(2_000, 80.0)),
        )

        assertEquals(3, samples.size)
        assertEquals(bootEpochMillis + 1_000, samples[0].tsEpochMillis)
        assertEquals(bootEpochMillis + 2_000, samples[1].tsEpochMillis)
        assertEquals(bootEpochMillis + 3_000, samples[2].tsEpochMillis)
    }

    @Test
    fun `위치 샘플에는 심박이 들어가지 않는다`() {
        val samples = toWalkSamples(
            bootEpochMillis = bootEpochMillis,
            locations = listOf(RawLocationReading(1_000, 37.5, 127.0, 8.5)),
            heartRates = listOf(RawHeartRateReading(1_000, 80.0)),
        )

        val locationSample = samples.first { it.lat != null }
        assertNull(locationSample.hr)
        assertEquals(8.5, locationSample.accuracyMeters!!, 0.001)

        val heartRateSample = samples.first { it.hr != null }
        assertNull(heartRateSample.lat)
        assertNull(heartRateSample.lng)
    }

    @Test
    fun `읽은 값이 없으면 빈 목록을 돌려준다`() {
        val samples = toWalkSamples(bootEpochMillis, emptyList(), emptyList())

        assertEquals(emptyList<Any>(), samples)
    }
}
