package com.moamap.app.feature.mypage.presentation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InquiryFormTest {

    @Test
    fun `유형·이메일·내용을 다 채우면 문의하기가 켜진다`() {
        assertTrue(isInquiryComplete(InquiryType.OTHER, "moamap@example.com", "내용"))
        // 앞뒤 공백은 이메일 모양을 판단할 때 무시한다.
        assertTrue(isInquiryComplete(InquiryType.OTHER, " moamap@example.co.kr ", "내용"))
    }

    @Test
    fun `하나라도 비면 꺼진다`() {
        assertFalse(isInquiryComplete(null, "moamap@example.com", "내용"))
        assertFalse(isInquiryComplete(InquiryType.OTHER, "", "내용"))
        assertFalse(isInquiryComplete(InquiryType.OTHER, "moamap@example.com", "  \n "))
    }

    @Test
    fun `이메일 모양이 아니면 꺼진다`() {
        listOf("moamap", "moamap@example", "@example.com", "moamap@.com", "moamap@example.", "moa map@example.com")
            .forEach { email ->
                assertFalse(email, isInquiryComplete(InquiryType.OTHER, email, "내용"))
            }
    }
}
