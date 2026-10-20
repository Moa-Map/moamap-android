package com.moamap.app.feature.mypage.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 문의하기. 로그인해야 보낼 수 있다.
 *
 * 최근 24시간 동안 10건까지 받고, 넘으면 429 `USER_013`. 답변은 관리자가 [InquiryRequestDto.replyEmail] 로 직접 보낸다.
 */
interface InquiryService {

    @POST("api/v1/inquiries")
    suspend fun createInquiry(@Body request: InquiryRequestDto): InquiryCreatedDto
}

/**
 * POST api/v1/inquiries 요청.
 *
 * 이메일은 앞뒤 공백을 떼고 보낸다. 서버는 공백을 떼기 전에 형식을 검사해서, 공백이 붙어 있으면 400 이다.
 */
@Serializable
data class InquiryRequestDto(
    val type: String,
    val replyEmail: String,
    val content: String,
)

/** 등록 결과. 보낸 이메일·내용은 돌려주지 않는다. */
@Serializable
data class InquiryCreatedDto(
    val inquiryId: Long = 0,
)
