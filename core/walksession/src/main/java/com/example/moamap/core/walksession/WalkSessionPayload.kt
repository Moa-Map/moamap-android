package com.example.moamap.core.walksession

import kotlinx.serialization.Serializable

/**
 * 워치가 기록한 한 번의 외출 세션.
 *
 * 워치와 폰이 공유하는 스키마다. 필드를 바꾸면 양쪽이 함께 바뀐다.
 */
@Serializable
data class WalkSessionPayload(
    /** 워치가 생성하는 세션 식별자. */
    val clientSessionId: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
    /** 시간 오름차순으로 정렬된 샘플. 위치와 심박은 수집 주기가 달라 별개의 행으로 들어온다. */
    val samples: List<WalkSample>,
    /** 사용자가 워치에서 직접 표시한 시점(스트레치 기능용). 지금은 항상 비어 있다. */
    val watchMarks: List<Long> = emptyList(),
)

/**
 * 한 시점의 관측값.
 *
 * 위치 샘플은 [lat]/[lng]만, 심박 샘플은 [hr]만 채워진다.
 * 한 행에 둘 다 채워 넣지 않는다 — 센서 타임스탬프를 임의로 맞추면 없는 정밀도를 지어내는 것이다.
 */
@Serializable
data class WalkSample(
    val tsEpochMillis: Long,
    val lat: Double? = null,
    val lng: Double? = null,
    val accuracyMeters: Double? = null,
    val hr: Double? = null,
)
