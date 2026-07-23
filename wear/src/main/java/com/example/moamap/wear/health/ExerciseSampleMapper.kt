package com.example.moamap.wear.health

import com.example.moamap.core.walksession.WalkSample

/**
 * Health Services 에서 읽은 값을 프레임워크 타입 없이 표현한 것.
 * 이렇게 두면 변환 로직을 기기 없이 단위 테스트할 수 있다.
 */
data class RawLocationReading(
    val elapsedFromBootMillis: Long,
    val lat: Double,
    val lng: Double,
    val accuracyMeters: Double?,
)

data class RawHeartRateReading(
    val elapsedFromBootMillis: Long,
    val bpm: Double,
)

/**
 * 센서 판독값을 세션 샘플로 바꾼다.
 *
 * Health Services 는 부팅 기준 경과 시간을 주므로 [bootEpochMillis] 를 더해 실제 시각으로 만든다.
 * 위치와 심박은 수집 주기가 달라 각각 별개의 행이 되며, 시간 오름차순으로 정렬한다.
 */
fun toWalkSamples(
    bootEpochMillis: Long,
    locations: List<RawLocationReading>,
    heartRates: List<RawHeartRateReading>,
): List<WalkSample> {
    val locationSamples = locations.map { reading ->
        WalkSample(
            tsEpochMillis = bootEpochMillis + reading.elapsedFromBootMillis,
            lat = reading.lat,
            lng = reading.lng,
            accuracyMeters = reading.accuracyMeters,
        )
    }
    val heartRateSamples = heartRates.map { reading ->
        WalkSample(
            tsEpochMillis = bootEpochMillis + reading.elapsedFromBootMillis,
            hr = reading.bpm,
        )
    }
    return (locationSamples + heartRateSamples).sortedBy { it.tsEpochMillis }
}
