package com.moamap.app.feature.terms.domain.repository

import com.moamap.app.feature.terms.domain.model.Terms
import com.moamap.app.feature.terms.domain.model.TermsAgreementStatus
import com.moamap.app.feature.terms.domain.model.TermsDetail

/** 서버 약관 API. 목록·전문은 로그인 없이, 동의 상태·동의는 로그인한 세션으로 부른다. */
interface TermsRepository {
    /** 지금 보여 줄 약관, 화면 순서대로. */
    suspend fun getTermsList(): List<Terms>

    /** 약관 하나의 전문. 없는 약관이면 404 `USER_020`. */
    suspend fun getTermsDetail(termId: Long): TermsDetail

    /** 이 세션이 동의해야 할 약관이 남았는지. */
    suspend fun getAgreementStatus(): TermsAgreementStatus

    /**
     * [termIds] 에 동의한다. 응답은 동의한 뒤의 상태다.
     *
     * 그사이 약관이 개정됐으면 409 `USER_023` 이다([TERMS_CHANGED_CODE]).
     */
    suspend fun agree(termIds: List<Long>): TermsAgreementStatus
}

/** 동의 화면을 연 뒤 약관이 개정됐다. 동의 상태를 다시 불러와 새 약관에 동의를 받는다. */
const val TERMS_CHANGED_CODE = "USER_023"
