package com.moamap.app.feature.mypage.domain.model

/**
 * 문의 유형. 시안 「문의하기」 유형 목록 순서대로.
 *
 * 이름이 곧 서버 값이다(`POST /inquiries` 의 `type`). 바꾸면 서버가 400 으로 거절한다.
 */
enum class InquiryType(val label: String) {
    SERVICE("서비스 이용 문제"),
    TECHNICAL("기술적 문제"),
    ACCOUNT("계정 관련"),
    REPORT("신고 및 제재"),
    PARTNERSHIP("제휴 문의"),
    OTHER("기타"),
}
