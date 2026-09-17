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
    fun `빈 사진 주소는 걸러 낸다`() {
        assertEquals(
            listOf("https://img/1.jpg"),
            PlaceReviewDto(imageUrls = listOf(" ", "https://img/1.jpg")).toPlaceReview(null).imageUrls,
        )
    }
}
