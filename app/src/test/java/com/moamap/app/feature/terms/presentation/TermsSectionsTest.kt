package com.moamap.app.feature.terms.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TermsSectionsTest {

    @Test
    fun `소제목마다 나누고 목록·번호 줄은 기호와 글을 나눈다`() {
        val sections = parseTermsSections(
            """
            ## 제1조 (목적)
            이 약관은 목적을 정합니다.

            ## 제2조 (정의)
            - "회원": 가입한 사람
            1. 첫째 내용
            12. 열두째 내용
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                TermsSection("제1조 (목적)", listOf(TermsLine(null, "이 약관은 목적을 정합니다."))),
                TermsSection(
                    "제2조 (정의)",
                    listOf(
                        TermsLine("·", "\"회원\": 가입한 사람"),
                        TermsLine("1.", "첫째 내용"),
                        TermsLine("12.", "열두째 내용"),
                    ),
                ),
            ),
            sections,
        )
    }

    @Test
    fun `첫 소제목 앞의 글은 소제목 없는 덩어리가 되고 빈 줄은 버린다`() {
        assertEquals(
            listOf(TermsSection(null, listOf(TermsLine(null, "머리말"))), TermsSection("본문", emptyList())),
            parseTermsSections("\n머리말\n## 본문\n"),
        )
        assertEquals(
            listOf(TermsSection("본문", listOf(TermsLine(null, "내용")))),
            parseTermsSections("\n\n## 본문\n\n내용\n"),
        )
    }

    @Test
    fun `앱에 넣은 약관 파일 형식 그대로 읽히고 글꼴에 없는 기호를 쓰지 않는다`() {
        val sections = parseTermsSections(java.io.File("src/main/res/raw/terms_service.md").readText())

        // 제1조~제12조 + 문의 + 부칙
        assertEquals(14, sections.size)
        assertEquals("제1조 (목적)", sections.first().heading)
        assertEquals(TermsLine(null, "이 약관은 2026년 10월 9일부터 시행합니다."), sections.last().lines.single())
        assertTrue(sections.flatMap { it.lines }.none { line -> "•" in line.text || line.marker == "•" })
    }
}
