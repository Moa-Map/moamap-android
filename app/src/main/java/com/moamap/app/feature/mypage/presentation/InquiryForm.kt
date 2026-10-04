package com.moamap.app.feature.mypage.presentation

/** 문의 유형. 시안 「문의하기」 유형 목록 순서대로. */
enum class InquiryType(val label: String) {
    SERVICE("서비스 이용 문제"),
    TECHNICAL("기술적 문제"),
    ACCOUNT("계정 관련"),
    REPORT("신고 및 제재"),
    PARTNERSHIP("제휴 문의"),
    OTHER("기타"),
}

/** 이메일 모양. 공백 없이 `@` 앞뒤에 글자가 있고, `@` 뒤에 `.` 이 있다. */
private val EmailShape = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")

/**
 * 「문의하기」 버튼을 켤지. 유형을 골랐고, 이메일이 이메일 모양이고(@ 와 . 확인), 내용을 적었을 때다
 * (10-05 사용자 결정).
 */
internal fun isInquiryComplete(type: InquiryType?, email: String, content: String): Boolean =
    type != null && EmailShape.matches(email.trim()) && content.isNotBlank()
