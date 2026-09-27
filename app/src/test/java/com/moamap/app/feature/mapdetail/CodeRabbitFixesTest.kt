package com.moamap.app.feature.mapdetail

import com.moamap.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeRabbitFixesTest {
    @Test
    fun `like icon follows the place liked state`() {
        assertEquals(R.drawable.ic_favorite_filled, likeIconRes(liked = true))
        assertEquals(R.drawable.ic_favorite_outline, likeIconRes(liked = false))
    }

    @Test
    fun `review is not submitted when no submission callback is connected`() {
        assertFalse(
            trySubmitReview(
                reviewText = "좋았어요",
                photo = null,
                onSubmitReview = null,
            ),
        )
    }

    @Test
    fun `review text can be cleared only after a successful submission`() {
        var submittedText = ""

        val succeeded = trySubmitReview(
            reviewText = "다시 가고 싶어요",
            photo = null,
            onSubmitReview = { text, _ ->
                submittedText = text
                true
            },
        )

        assertTrue(succeeded)
        assertEquals("다시 가고 싶어요", submittedText)
    }
}
