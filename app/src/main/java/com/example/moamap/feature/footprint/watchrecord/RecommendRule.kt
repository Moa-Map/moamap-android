package com.example.moamap.feature.footprint.watchrecord

import com.example.moamap.feature.footprint.domain.model.ReceivedWalkSession

/**
 * 추천 장소를 여러 곳 줄지 가르는 기록 시간.
 *
 * 짧게 걸었으면 들른 곳이랄 게 없어 한 곳만 짚어 주고, 오래 걸었으면 주변을 여럿 준다.
 */
internal const val RECOMMEND_MULTI_THRESHOLD_MILLIS = 10 * 60 * 1_000L

/**
 * 이 기록에 추천 장소를 여러 곳 띄울 것인가.
 *
 * [RECOMMEND_MULTI_THRESHOLD_MILLIS] 와 같은 시간은 여러 곳 쪽이다. "10분 이상" 이 기준이다.
 *
 * 화면이 아니라 여기서 정한다. 전에는 버튼의 왼쪽 절반과 오른쪽 절반이 각각 다른 결과를
 * 내는 임시 장치였다 - 두 결과를 눈으로 견주려고 둔 것이지 규칙이 아니었다.
 */
internal fun ReceivedWalkSession.recommendsMultiplePlaces(): Boolean =
    stats.durationMillis >= RECOMMEND_MULTI_THRESHOLD_MILLIS
