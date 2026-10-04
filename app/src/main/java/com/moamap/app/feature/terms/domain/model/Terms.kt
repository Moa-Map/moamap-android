package com.moamap.app.feature.terms.domain.model

import androidx.compose.runtime.Immutable

/**
 * 약관 한 종류의 현재 버전.
 *
 * 모양은 백엔드 약관 설계(백엔드 이슈 #118)와 맞춘다 - 종류 코드·제목·성격·버전·본문(마크다운).
 * 지금은 앱에 넣어 둔 약관을 쓰고, API 가 생기면 서버에서 받은 값으로 같은 모델을 채운다.
 */
@Immutable
data class Terms(
    val code: String,
    val title: String,
    val consentType: TermsConsentType,
    val version: String,
    /** 마크다운 본문. `## ` 줄이 소제목이다. */
    val content: String,
)

/** 약관의 성격. 백엔드 설계의 `consentType` 과 같다. */
enum class TermsConsentType {
    /** 동의해야 서비스를 쓸 수 있다. */
    REQUIRED,

    /** 동의하지 않아도 된다. */
    OPTIONAL,

    /** 동의를 받지 않고 보여 주기만 한다(개인정보처리방침). */
    NOTICE,
}

/** 화면에서 이름으로 여는 약관의 코드. */
object TermsCode {
    const val SERVICE = "SERVICE"
    const val PRIVACY_COLLECTION = "PRIVACY_COLLECTION"
    const val SENSITIVE_INFO = "SENSITIVE_INFO"
    const val THIRD_PARTY = "THIRD_PARTY"
    const val MARKETING = "MARKETING"
    const val PRIVACY_POLICY = "PRIVACY_POLICY"
}
