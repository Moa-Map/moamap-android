package com.moamap.app.feature.mapdetail

import androidx.compose.ui.unit.dp
import com.moamap.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeRabbitFixesTest {
    @Test
    fun `favorite icon follows the place favorite state`() {
        assertEquals(R.drawable.ic_favorite_filled, favoriteIconRes(favorite = true))
        assertEquals(R.drawable.ic_favorite_outline, favoriteIconRes(favorite = false))
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

    @Test
    fun `place list text area keeps a 64 dp minimum height`() {
        assertEquals(64.dp, PlaceListTextMinHeight)
    }
}
