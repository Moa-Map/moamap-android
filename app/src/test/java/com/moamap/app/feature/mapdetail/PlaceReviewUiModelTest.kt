package com.moamap.app.feature.mapdetail

import com.moamap.app.feature.mapdetail.domain.model.PlaceReview
import org.junit.Assert.assertEquals
import org.junit.Test

private const val NOW = 1_800_000_000_000L

private const val MINUTE = 60_000L
private const val HOUR = 60 * MINUTE
private const val DAY = 24 * HOUR

class PlaceReviewUiModelTest {

    @Test
    fun `지난 만큼에 맞는 단위로 줄여 쓴다`() {
        assertEquals("방금 전", label(30 * 1000L))
        assertEquals("5분 전", label(5 * MINUTE))
        assertEquals("2시간 전", label(2 * HOUR))
        assertEquals("3일 전", label(3 * DAY))
        assertEquals("1주일 전", label(8 * DAY))
        assertEquals("2개월 전", label(70 * DAY))
        assertEquals("1년 전", label(400 * DAY))
    }

    @Test
    fun `단위가 바뀌는 경계에서 넘어간다`() {
        assertEquals("방금 전", label(MINUTE - 1))
        assertEquals("1분 전", label(MINUTE))
        assertEquals("59분 전", label(HOUR - 1))
        assertEquals("1시간 전", label(HOUR))
        assertEquals("23시간 전", label(DAY - 1))
        assertEquals("1일 전", label(DAY))
    }

    @Test
    fun `앞으로 온 시각은 방금 전으로 접는다`() {
        // 서버와 기기의 시계가 어긋나도 "-3분 전" 같은 게 보이면 안 된다.
        assertEquals("방금 전", label(-3 * HOUR))
    }

    @Test
    fun `시각을 못 읽었으면 자리를 비운다`() {
        assertEquals("", relativeTimeLabel(createdAtMillis = null, nowMillis = NOW))
    }

    @Test
    fun `닉네임이 없으면 이름 자리를 대신 채운다`() {
        assertEquals("이름 없는 사용자", review(authorName = null).toPlaceReviewUiModel(NOW).userName)
        assertEquals("민지", review(authorName = "민지").toPlaceReviewUiModel(NOW).userName)
    }

    private fun label(elapsedMillis: Long): String =
        relativeTimeLabel(createdAtMillis = NOW - elapsedMillis, nowMillis = NOW)

    private fun review(authorName: String?) = PlaceReview(
        id = 1L,
        authorId = 1L,
        authorName = authorName,
        rating = 5,
        content = "좋았어요",
        createdAtMillis = NOW,
    )
}
