package com.moamap.app.feature.mypage.data.repository

import com.moamap.app.feature.mypage.data.remote.InquiryRequestDto
import com.moamap.app.feature.mypage.data.remote.InquiryService
import com.moamap.app.feature.mypage.domain.model.InquiryType
import com.moamap.app.feature.mypage.domain.repository.InquiryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class InquiryRepositoryImpl @Inject constructor(
    private val inquiryService: InquiryService,
) : InquiryRepository {

    override suspend fun sendInquiry(type: InquiryType, replyEmail: String, content: String) {
        inquiryService.createInquiry(
            InquiryRequestDto(type = type.name, replyEmail = replyEmail.trim(), content = content.trim()),
        )
    }
}
