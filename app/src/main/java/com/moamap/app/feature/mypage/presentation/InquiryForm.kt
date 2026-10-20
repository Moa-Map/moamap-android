package com.moamap.app.feature.mypage.presentation

import com.moamap.app.feature.mypage.domain.model.InquiryType

/** 서버가 받는 길이. 넘으면 400 이라 입력에서 자른다. */
internal const val INQUIRY_EMAIL_MAX_LENGTH = 254
internal const val INQUIRY_CONTENT_MAX_LENGTH = 2000

/** 이메일 모양. 공백 없이 `@` 앞뒤에 글자가 있고, `@` 뒤에 `.` 이 있다. 서버 정규식과 같다. */
private val EmailShape = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")

/**
 * 「문의하기」 버튼을 켤지. 유형을 골랐고, 이메일이 이메일 모양이고(@ 와 . 확인), 내용을 적었을 때다
 * (10-05 사용자 결정).
 */
internal fun isInquiryComplete(type: InquiryType?, email: String, content: String): Boolean =
    type != null && EmailShape.matches(email.trim()) && content.isNotBlank()
