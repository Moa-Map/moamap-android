package com.moamap.app.feature.terms.presentation

/** 약관 전문 화면의 한 덩어리. 시안 「서비스 이용약관」: 소제목 + 본문. */
data class TermsSection(
    val heading: String?,
    val lines: List<TermsLine>,
)

/**
 * 본문 한 줄. [marker] 가 있으면(목록 「·」, 번호 「1.」) 화면이 기호와 글을 나눠 그려,
 * 글이 길어 다음 줄로 넘어가도 글 시작 위치에 맞춰 이어진다.
 */
data class TermsLine(
    val marker: String?,
    val text: String,
)

/** 목록 줄 기호. 앱 글꼴(나눔스퀘어)에 「•」 모양이 비어 있어 빈칸으로 나와서 「·」 를 쓴다. */
private const val BULLET = "·"

private val NumberedLine = Regex("""^(\d+\.)\s+(.*)$""")

/**
 * 마크다운 본문을 소제목 단위로 나눈다. 약관에 쓰는 만큼만 읽는다.
 *
 * - `## ` 로 시작하는 줄이 소제목이고, 다음 소제목까지가 그 본문이다.
 * - `- ` 로 시작하는 줄은 목록, `1. ` 처럼 숫자와 점으로 시작하는 줄은 번호 줄이다.
 * - 빈 줄은 버리고, 첫 소제목 앞의 글은 소제목 없는 덩어리가 된다.
 *
 * ponytail: 표·굵게·링크는 글자 그대로 나온다. 서버 약관이 그런 문법을 쓰면 마크다운 라이브러리로 바꾼다.
 */
internal fun parseTermsSections(markdown: String): List<TermsSection> {
    val sections = mutableListOf<TermsSection>()
    var heading: String? = null
    val lines = mutableListOf<TermsLine>()

    fun flush() {
        if (heading != null || lines.isNotEmpty()) {
            sections += TermsSection(heading = heading, lines = lines.toList())
        }
        lines.clear()
    }

    markdown.lineSequence().map { it.trim() }.forEach { line ->
        val numbered = NumberedLine.find(line)
        when {
            line.isEmpty() -> Unit
            line.startsWith("## ") -> {
                flush()
                heading = line.removePrefix("## ").trim()
            }
            line.startsWith("- ") -> lines += TermsLine(BULLET, line.removePrefix("- ").trim())
            numbered != null -> lines += TermsLine(numbered.groupValues[1], numbered.groupValues[2])
            else -> lines += TermsLine(null, line)
        }
    }
    flush()
    return sections
}
