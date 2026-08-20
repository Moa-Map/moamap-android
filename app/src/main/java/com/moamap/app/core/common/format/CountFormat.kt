package com.moamap.app.core.common.format

import java.util.Locale

/**
 * 지도 카드 메타 줄에 들어가는 숫자 표기.
 *
 * 탐색·모음·공식지도 세 탭이 같은 카드 문법을 쓴다. feature 하나에 두면 나머지 둘이 그쪽을
 * import 하게 되고, 지금처럼 방향이 서로 엇갈린다.
 */

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

/**
 * 등록 장소 수를 카드 메타에 들어갈 문자열로 만든다. (예: 5416 -> "5,416곳")
 *
 * 천 단위 구분을 넣는다. 공식지도는 공공데이터라 장소가 수천 건씩 들어 있어
 * 구분자가 없으면 자릿수를 읽기 어렵다.
 *
 * 로케일을 고정한다. 기기 로케일에 맡기면 독일어처럼 마침표로 묶는 곳에서 `"5.416곳"` 이
 * 되는데, 문구가 한국어라 자릿수 구분이 아니라 소수점으로 읽힌다.
 */
fun formatPlaceCount(count: Int): String = "%,d곳".format(Locale.KOREA, count)

/** 내림해서 실제 인원보다 많아 보이지 않게 한다. (9990명이 "1만명" 이 되면 안 된다.) */
private fun trimTrailingZero(value: Double): String {
    val truncated = (value * 10).toInt()
    val whole = truncated / 10
    val decimal = truncated % 10
    return if (decimal == 0) "$whole" else "$whole.$decimal"
}

private const val THOUSAND = 1_000
private const val TEN_THOUSAND = 10_000
