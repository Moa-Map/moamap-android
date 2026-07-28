package com.example.moamap.feature.explore.presentation

/**
 * 참여 인원을 카드 메타에 들어갈 짧은 문자열로 줄인다. (예: 2312 -> "2.3천명")
 *
 * 소수 첫째 자리까지만 남기고, 그 자리가 0이면 떼어낸다. ("2.0천명" 이 아니라 "2천명")
 */
fun formatMemberCount(count: Int): String = when {
    count < THOUSAND -> "${count}명"
    count < TEN_THOUSAND -> "${trimTrailingZero(count / THOUSAND.toDouble())}천명"
    else -> "${trimTrailingZero(count / TEN_THOUSAND.toDouble())}만명"
}

/** 내림해서 실제 인원보다 많아 보이지 않게 한다. (9990명이 "1만명" 이 되면 안 된다.) */
private fun trimTrailingZero(value: Double): String {
    val truncated = (value * 10).toInt()
    val whole = truncated / 10
    val decimal = truncated % 10
    return if (decimal == 0) "$whole" else "$whole.$decimal"
}

private const val THOUSAND = 1_000
private const val TEN_THOUSAND = 10_000
