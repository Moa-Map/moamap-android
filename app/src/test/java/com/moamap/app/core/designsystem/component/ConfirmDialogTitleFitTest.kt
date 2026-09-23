package com.moamap.app.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 폭은 글자 수 × 10 으로 흉내 낸다. */
class ConfirmDialogTitleFitTest {

    private val suffix = "에서 나가시겠습니까?"
    private val minScale = 0.8f

    private fun fit(title: String, availableWidth: Float) = fitTitleToOneLine(
        title = title,
        suffix = suffix,
        availableWidth = availableWidth,
        minScale = minScale,
    ) { text -> text.length * 10f }

    @Test
    fun `들어가면 그대로 둔다`() {
        val fitted = fit("카공족", availableWidth = 500f)

        assertEquals(FittedTitle("카공족에서 나가시겠습니까?", 1f), fitted)
    }

    @Test
    fun `조금 넘치면 글자만 줄여 한 줄에 넣는다`() {
        // 17자 = 170 → 150 에 맞추려면 0.88 배. 최소 배율보다 크다.
        val fitted = fit("카공족 모여", availableWidth = 150f)

        assertEquals("카공족 모여에서 나가시겠습니까?", fitted.text)
        assertEquals(150f / 170f, fitted.scale, 0.0001f)
    }

    @Test
    fun `가장 작은 글자로도 넘치면 이름 끝을 줄이고 질문은 남긴다`() {
        val fitted = fit("서울 성수동 주말에 가기 좋은 카페 모음 지도", availableWidth = 150f)

        assertEquals(minScale, fitted.scale, 0.0001f)
        assertTrue(fitted.text.startsWith("서울"))
        assertTrue(fitted.text.endsWith("…$suffix"))
        // 가장 작은 글자에서 폭 안에 들어간다.
        assertTrue(fitted.text.length * 10f * minScale <= 150f)
    }

    @Test
    fun `줄인 이름 끝에 공백을 남기지 않는다`() {
        val fitted = fit("서울 성수동 주말에 가기 좋은 카페 모음 지도", availableWidth = 150f)

        assertFalse(fitted.text.contains(" …"))
    }

    @Test
    fun `이모지를 반으로 자르지 않는다`() {
        val fitted = fit("🍰".repeat(20), availableWidth = 150f)

        val head = fitted.text.substringBefore("…")
        assertTrue(head.isEmpty() || !head.last().isHighSurrogate())
    }

    @Test
    fun `이름이 한 글자도 안 들어가도 질문은 남긴다`() {
        val fitted = fit("카공족 모여라", availableWidth = 50f)

        assertEquals("…$suffix", fitted.text)
    }
}
