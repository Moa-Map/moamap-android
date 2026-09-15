package com.moamap.app.feature.mapdetail.presentation.posts

import androidx.compose.runtime.Immutable
import com.moamap.app.feature.mapdetail.domain.model.MapPost
import java.util.Calendar
import java.util.TimeZone

/*
 * 달력 형식에 쓰는 날짜 계산.
 *
 * `java.time` 을 쓰지 않는다. minSdk 가 24 인데 desugaring 을 켜지 않아서, Android 8 미만에서는
 * `LocalDate` 가 없어 앱이 죽는다. 서버 시각을 읽는 코드도 같은 이유로 `Calendar` 를 쓴다.
 */

/** 달력의 한 날. [month] 는 1~12 다. */
@Immutable
data class CalendarDay(val year: Int, val month: Int, val dayOfMonth: Int) {
    val calendarMonth: CalendarMonth get() = CalendarMonth(year, month)
}

/** 달력의 한 달. [month] 는 1~12 다. */
@Immutable
data class CalendarMonth(val year: Int, val month: Int) {

    val daysInMonth: Int get() = firstDay().getActualMaximum(Calendar.DAY_OF_MONTH)

    /**
     * 월요일부터 시작하는 달력에서 1일 앞에 비워 둘 칸 수.
     *
     * `Calendar.DAY_OF_WEEK` 는 일요일이 1, 월요일이 2 다. 월요일을 0 으로 옮기면 일요일이 6 이 된다.
     */
    val leadingBlankDays: Int get() = (firstDay().get(Calendar.DAY_OF_WEEK) + 5) % 7

    val label: String get() = "${year}년 ${month}월"

    fun previous(): CalendarMonth = if (month == 1) CalendarMonth(year - 1, 12) else CalendarMonth(year, month - 1)

    fun next(): CalendarMonth = if (month == 12) CalendarMonth(year + 1, 1) else CalendarMonth(year, month + 1)

    fun day(dayOfMonth: Int): CalendarDay = CalendarDay(year, month, dayOfMonth)

    /** 이 달 1일 0시. 이보다 이른 글이 나오면 이 달 게시물은 다 받은 것이다. */
    fun startMillis(timeZone: TimeZone = TimeZone.getDefault()): Long = firstDay(timeZone).timeInMillis

    private fun firstDay(timeZone: TimeZone = TimeZone.getDefault()): Calendar =
        Calendar.getInstance(timeZone).apply {
            clear()
            set(year, month - 1, 1)
        }
}

/** 시각이 [timeZone] 에서 며칠인지. 날짜 경계는 기기 시간대를 따른다. */
internal fun calendarDayOf(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): CalendarDay {
    val calendar = Calendar.getInstance(timeZone).apply { timeInMillis = millis }
    return CalendarDay(
        year = calendar.get(Calendar.YEAR),
        month = calendar.get(Calendar.MONTH) + 1,
        dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH),
    )
}

internal fun todayCalendarDay(): CalendarDay = calendarDayOf(System.currentTimeMillis())

/**
 * 이 달 날짜별로 칸을 채울 사진. 키는 날짜(1~31)다.
 *
 * 게시물이 최신순으로 들어오므로 날짜마다 처음 만나는 사진 게시물이 그날 가장 최신 글이다.
 * 사진이 없는 글만 있는 날은 넣지 않는다 - 그 칸은 사진 없는 날과 같이 회색이다.
 */
internal fun List<MapPost>.coverPhotosIn(
    month: CalendarMonth,
    timeZone: TimeZone = TimeZone.getDefault(),
): Map<Int, String> {
    val covers = mutableMapOf<Int, String>()
    for (post in this) {
        val millis = post.createdAtMillis ?: continue
        val photo = post.imageUrls.firstOrNull() ?: continue
        val day = calendarDayOf(millis, timeZone)
        if (day.calendarMonth != month) continue
        covers.getOrPut(day.dayOfMonth) { photo }
    }
    return covers
}

/** 그날 올라온 게시물. 받은 순서(최신순)를 그대로 둔다. */
internal fun List<MapPost>.postsOn(
    day: CalendarDay,
    timeZone: TimeZone = TimeZone.getDefault(),
): List<MapPost> = filter { post ->
    post.createdAtMillis?.let { millis -> calendarDayOf(millis, timeZone) == day } == true
}
