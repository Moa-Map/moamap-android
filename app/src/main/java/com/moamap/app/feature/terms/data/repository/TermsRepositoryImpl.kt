package com.moamap.app.feature.terms.data.repository

import com.moamap.app.feature.terms.data.remote.TermsAgreeRequestDto
import com.moamap.app.feature.terms.data.remote.TermsAgreementDto
import com.moamap.app.feature.terms.data.remote.TermsDetailDto
import com.moamap.app.feature.terms.data.remote.TermsDto
import com.moamap.app.feature.terms.data.remote.TermsService
import com.moamap.app.feature.terms.domain.model.Terms
import com.moamap.app.feature.terms.domain.model.TermsAgreementStatus
import com.moamap.app.feature.terms.domain.model.TermsDetail
import com.moamap.app.feature.terms.domain.model.TermsKind
import com.moamap.app.feature.terms.domain.repository.TermsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class TermsRepositoryImpl @Inject constructor(
    private val termsService: TermsService,
) : TermsRepository {

    override suspend fun getTermsList(): List<Terms> = termsService.getTerms().map { dto -> dto.toTerms() }

    override suspend fun getTermsDetail(termId: Long): TermsDetail =
        termsService.getTermsDetail(termId).toTermsDetail()

    override suspend fun getAgreementStatus(): TermsAgreementStatus =
        termsService.getMyAgreement().toAgreementStatus()

    override suspend fun agree(termIds: List<Long>): TermsAgreementStatus =
        termsService.agree(TermsAgreeRequestDto(termIds)).toAgreementStatus()
}

internal fun TermsDto.toTerms(): Terms = Terms(
    termId = termId,
    code = code.orEmpty(),
    title = title.orEmpty(),
    version = version.orEmpty(),
    kind = kind.toTermsKind(),
)

internal fun TermsDetailDto.toTermsDetail(): TermsDetail = TermsDetail(
    termId = termId,
    code = code.orEmpty(),
    title = title.orEmpty(),
    version = version.orEmpty(),
    kind = kind.toTermsKind(),
    active = active,
    content = content.orEmpty(),
)

internal fun TermsAgreementDto.toAgreementStatus(): TermsAgreementStatus = TermsAgreementStatus(
    needsAgreement = needsAgreement,
    pendingTerms = pendingTerms.map { dto -> dto.toTerms() },
)

/** 모르는 값은 보기만 하는 약관으로 둔다. 필수로 잘못 읽으면 동의할 수 없는 약관에 체크를 요구하게 된다. */
private fun String?.toTermsKind(): TermsKind =
    if (this == TermsKind.REQUIRED.name) TermsKind.REQUIRED else TermsKind.VIEW_ONLY
