package com.moamap.app.feature.mypage.domain.repository

import com.moamap.app.feature.mypage.domain.model.InquiryType

interface InquiryRepository {

    /** 문의를 보낸다. 실패하면 던진다 - 하루 한도를 넘기면 429 `USER_013`. */
    suspend fun sendInquiry(type: InquiryType, replyEmail: String, content: String)
}
