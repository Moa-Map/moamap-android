package com.moamap.app.feature.mapdetail.data.repository

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** 초 단위까지의 날짜·시각. 소수점 이하 초와 지역 표시는 떼어 놓고 따로 읽는다. */
private const val DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss"

/**
 * `2026-07-30T02:54:12`, `...12.123456`, `...12Z`, `...12+09:00` 을 모두 받는다.
 *
 * 서버는 지금 지역 표시 없이 내려주지만, 나중에 붙더라도 이 자리에서 조용히 받아 낸다.
 */
private val SERVER_DATE_TIME = Regex(
    """^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(?:\.\d+)?(Z|[+-]\d{2}:?\d{2})?$""",
)

/**
 * 서버가 준 시각을 epoch millis 로 옮긴다. 형식이 어긋나면 null 이다.
 *
 * 서버 시각은 `LocalDateTime` 이라 지역 표시 없이 온다. 표시가 없으면 **기기 시간대**로
 * 읽는다 - 서버와 사용자가 같은 지역에 있다는 전제다. 여기서 틀려도 "N시간 전" 이 어긋날
 * 뿐이라, 어느 한쪽으로 단정해 UTC 로 읽다가 아홉 시간씩 밀리는 것보다 낫다.
 *
 * 소수점 이하 초는 버린다. 자릿수가 마이크로초까지 와서 패턴 하나로 맞추기 어렵고,
 * 상대 시각 표시에는 초 미만이 쓰이지 않는다.
 */
internal fun parseServerDateTime(raw: String?): Long? {
    val match = SERVER_DATE_TIME.find(raw?.trim().orEmpty()) ?: return null
    val (dateTime, zone) = match.destructured

    val format = SimpleDateFormat(DATE_TIME_PATTERN, Locale.US).apply {
        // 관대하게 읽으면 13월·32일 같은 값이 다음 달로 넘어가 조용히 통과한다.
        isLenient = false
        timeZone = timeZoneOf(zone)
    }

    return runCatching { format.parse(dateTime)?.time }.getOrNull()
}

private fun timeZoneOf(designator: String): TimeZone = when {
    designator.isEmpty() -> TimeZone.getDefault()
    designator == "Z" -> TimeZone.getTimeZone("UTC")
    else -> TimeZone.getTimeZone("GMT$designator")
}
