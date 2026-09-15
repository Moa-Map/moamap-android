package com.moamap.app.feature.mapdetail.presentation.posts

import com.moamap.app.feature.mapdetail.domain.model.MapPost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

private val UTC: TimeZone = TimeZone.getTimeZone("UTC")

internal fun utcMillis(year: Int, month: Int, day: Int, hour: Int = 12): Long =
    Calendar.getInstance(UTC).apply {
        clear()
        set(year, month - 1, day, hour, 0)
    }.timeInMillis

internal fun calendarPost(id: Long, millis: Long?, photo: String? = null) = MapPost(
    id = id,
    authorId = 2,
    content = "글$id",
    imageUrls = listOfNotNull(photo),
    placeNames = emptyList(),
    createdAtMillis = millis,
)

class PostCalendarTest {

    /** 2026년 8월 1일은 토요일이다. 월요일부터 세면 앞에 다섯 칸이 빈다. */
    @Test
    fun `1일 앞 빈칸은 월요일부터 센다`() {
        assertEquals(5, CalendarMonth(2026, 8).leadingBlankDays)
        // 2026년 9월 1일은 화요일, 2026년 6월 1일은 월요일, 2026년 11월 1일은 일요일이다.
        assertEquals(1, CalendarMonth(2026, 9).leadingBlankDays)
        assertEquals(0, CalendarMonth(2026, 6).leadingBlankDays)
        assertEquals(6, CalendarMonth(2026, 11).leadingBlankDays)
    }

    @Test
    fun `달마다 날 수가 다르고 윤년 2월은 29일이다`() {
        assertEquals(31, CalendarMonth(2026, 8).daysInMonth)
        assertEquals(30, CalendarMonth(2026, 9).daysInMonth)
        assertEquals(28, CalendarMonth(2026, 2).daysInMonth)
        assertEquals(29, CalendarMonth(2028, 2).daysInMonth)
    }

    @Test
    fun `해가 바뀌는 달 이동`() {
        assertEquals(CalendarMonth(2025, 12), CalendarMonth(2026, 1).previous())
        assertEquals(CalendarMonth(2027, 1), CalendarMonth(2026, 12).next())
        assertEquals("2026년 8월", CalendarMonth(2026, 8).label)
    }

    /** 날짜 경계는 시간대를 따른다. 같은 순간이 UTC 로는 전날일 수 있다. */
    @Test
    fun `시각을 시간대 기준 날짜로 바꾼다`() {
        val millis = utcMillis(2026, 8, 12, hour = 20)

        assertEquals(CalendarDay(2026, 8, 12), calendarDayOf(millis, UTC))
        assertEquals(CalendarDay(2026, 8, 13), calendarDayOf(millis, TimeZone.getTimeZone("Asia/Seoul")))
    }

    /** 최신순이라 날마다 처음 만나는 사진 게시물이 그날 가장 최신 글이다. */
    @Test
    fun `날짜 칸에는 그날 가장 최신 사진 게시물의 사진을 쓴다`() {
        val posts = listOf(
            calendarPost(4, utcMillis(2026, 8, 12, hour = 18)),
            calendarPost(3, utcMillis(2026, 8, 12, hour = 15), photo = "https://cdn/new.jpg"),
            calendarPost(2, utcMillis(2026, 8, 12, hour = 9), photo = "https://cdn/old.jpg"),
            calendarPost(1, utcMillis(2026, 8, 3), photo = "https://cdn/3rd.jpg"),
        )

        val covers = posts.coverPhotosIn(CalendarMonth(2026, 8), UTC)

        assertEquals(mapOf(12 to "https://cdn/new.jpg", 3 to "https://cdn/3rd.jpg"), covers)
    }

    @Test
    fun `다른 달 게시물과 사진 없는 게시물과 시각 모르는 게시물은 칸을 채우지 않는다`() {
        val posts = listOf(
            calendarPost(3, utcMillis(2026, 9, 1), photo = "https://cdn/sep.jpg"),
            calendarPost(2, utcMillis(2026, 8, 20)),
            calendarPost(1, null, photo = "https://cdn/unknown.jpg"),
        )

        assertTrue(posts.coverPhotosIn(CalendarMonth(2026, 8), UTC).isEmpty())
    }

    @Test
    fun `고른 날의 게시물을 받은 순서대로 모은다`() {
        val posts = listOf(
            calendarPost(3, utcMillis(2026, 8, 12, hour = 18)),
            calendarPost(2, utcMillis(2026, 8, 11)),
            calendarPost(1, utcMillis(2026, 8, 12, hour = 9)),
        )

        assertEquals(listOf(3L, 1L), posts.postsOn(CalendarDay(2026, 8, 12), UTC).map { it.id })
    }
}
