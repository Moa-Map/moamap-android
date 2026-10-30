package com.moamap.app.feature.mapdetail.domain.model

/**
 * 신고 사유. 값은 서버(`ReportReason`)와 같고, 글자는 로그 댓글 시안·iOS 와 같다.
 *
 * 장소 댓글 시안은 옛 네 가지였지만 서버·iOS 에 맞춰 여섯 가지로 통일했다(10-30 사용자 결정).
 */
enum class ReportReason(val serverValue: String, val label: String) {
    Spam("SPAM", "스팸·광고·도배"),
    Abuse("ABUSE", "욕설·비하·혐오 표현"),
    Sexual("SEXUAL", "음란·선정적 내용·부적절한 사진"),
    Privacy("PRIVACY", "개인정보 노출"),
    FalseInfo("FALSE_INFO", "사실과 다른 정보"),

    /** 서버가 내용을 꼭 받는다. */
    Other("OTHER", "기타"),
}
