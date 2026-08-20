package com.moamap.app.feature.mapdetail.data.repository

import com.moamap.app.feature.explore.data.remote.PlaceReviewDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaceReviewMapperTest {

    @Test
    fun `공백뿐인 닉네임은 이름 없음으로 남긴다`() {
        assertNull(PlaceReviewDto().toPlaceReview(authorName = "  ").authorName)
        assertNull(PlaceReviewDto().toPlaceReview(authorName = null).authorName)
        assertEquals("민지", PlaceReviewDto().toPlaceReview(authorName = "민지").authorName)
    }

    @Test
    fun `본문이 없거나 공백뿐이면 빈 문자열로 옮긴다`() {
        assertEquals("", PlaceReviewDto(content = null).toPlaceReview(null).content)
        assertEquals("", PlaceReviewDto(content = "   ").toPlaceReview(null).content)
        assertEquals("좋았어요", PlaceReviewDto(content = " 좋았어요 ").toPlaceReview(null).content)
    }

    @Test
    fun `별점은 별 다섯 칸을 벗어나지 않는다`() {
        assertEquals(5, PlaceReviewDto(rating = 9).toPlaceReview(null).rating)
        assertEquals(0, PlaceReviewDto(rating = -1).toPlaceReview(null).rating)
    }
}
