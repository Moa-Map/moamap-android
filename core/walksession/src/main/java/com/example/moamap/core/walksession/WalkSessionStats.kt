package com.example.moamap.core.walksession

import kotlin.math.max

/** 수집된 세션이 쓸만한지 눈으로 판단하기 위한 요약. */
data class WalkSessionStats(
    val sampleCount: Int,
    val locationSampleCount: Int,
    val heartRateSampleCount: Int,
    val durationMillis: Long,
    /**
     * 심박이 관측된 1분 구간의 비율(0.0~1.0).
     *
     * 샘플 개수 비율이 아니라 시간 구간 비율이다. 심박이 한 구간에 몰려 있으면
     * 샘플이 많아도 커버리지는 낮게 나오고, 그게 실제 데이터 품질에 가깝다.
     *
     * 전체 구간 수는 세션 길이를 1분 단위로 올림(ceiling)해서 센다 — 끝에 남는
     * 1분 미만 구간도 하나의 구간으로 취급한다. 세션 시작~종료 시각 범위를 벗어난
     * (예: 클럭 스큐로 시작 시각 이전에 찍힌) 심박 샘플은 커버리지 계산에서 제외한다.
     */
    val heartRateCoverageRatio: Double,
)

private const val BUCKET_MILLIS = 60_000L

fun WalkSessionPayload.computeStats(): WalkSessionStats {
    val durationMillis = max(0L, endedAtEpochMillis - startedAtEpochMillis)

    val totalBuckets = (durationMillis + BUCKET_MILLIS - 1) / BUCKET_MILLIS

    val heartRateBuckets = samples
        .filter { it.hr != null && it.tsEpochMillis in startedAtEpochMillis..endedAtEpochMillis }
        .map { sample ->
            val offset = sample.tsEpochMillis - startedAtEpochMillis
            (offset / BUCKET_MILLIS).coerceAtMost(totalBuckets - 1)
        }
        .toSet()

    return WalkSessionStats(
        sampleCount = samples.size,
        locationSampleCount = samples.count { it.lat != null && it.lng != null },
        heartRateSampleCount = samples.count { it.hr != null },
        durationMillis = durationMillis,
        heartRateCoverageRatio = if (totalBuckets <= 0L) {
            0.0
        } else {
            heartRateBuckets.size.toDouble() / totalBuckets.toDouble()
        },
    )
}
