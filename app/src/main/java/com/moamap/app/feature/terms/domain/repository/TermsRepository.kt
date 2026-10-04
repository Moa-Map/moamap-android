package com.moamap.app.feature.terms.domain.repository

import com.moamap.app.feature.terms.domain.model.Terms

/**
 * 약관을 읽는다. 지금은 앱에 넣어 둔 약관(`BundledTermsRepository`)이고, 약관 API 가 생기면
 * 서버에서 받는 구현으로 바꾼다.
 */
interface TermsRepository {
    /** 동의 화면에 나오는 약관(필수·선택), 보여 줄 순서대로. */
    suspend fun getAgreementTerms(): List<Terms>

    /** 약관 하나. 동의를 받지 않는 개인정보처리방침도 여기서 읽는다. */
    suspend fun getTerms(code: String): Terms

    /** 지금 약관 버전. 이 세션이 동의한 버전과 다르면 다시 동의를 받는다. */
    suspend fun getCurrentVersion(): String
}
