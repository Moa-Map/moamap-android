package com.moamap.app.feature.terms.domain.model

import androidx.compose.runtime.Immutable

/**
 * 지금 보여 줄 약관 하나. 서버 `GET /api/v1/terms` 목록과 `/agreements/me` 의 `pendingTerms` 가 같은 모양이다.
 *
 * 약관 목록·제목·개수는 앱에 두지 않는다. 늘 서버가 준 대로 그린다.
 */
@Immutable
data class Terms(
    val termId: Long,
    val code: String,
    val title: String,
    val version: String,
    val kind: TermsKind,
)

/** 약관 전문. 본문은 마크다운이다. */
@Immutable
data class TermsDetail(
    val termId: Long,
    val code: String,
    val title: String,
    val version: String,
    val kind: TermsKind,
    /** 지금 보여 주는 약관인지. 개정돼 내려간 옛 약관이면 false 다. */
    val active: Boolean,
    val content: String,
)

/** 약관의 성격. 서버의 `kind` 와 같다. */
enum class TermsKind {
    /** 동의해야 서비스를 쓸 수 있다. */
    REQUIRED,

    /** 동의를 받지 않고 보여 주기만 한다(개인정보 처리방침). */
    VIEW_ONLY,
}

/**
 * 이 세션의 약관 동의 상태.
 *
 * @param pendingTerms 아직 동의하지 않은 필수 약관. 첫 가입이면 필수 약관 전체, 약관이 개정된 뒤면 바뀐 약관만 들어 있다.
 */
@Immutable
data class TermsAgreementStatus(
    val needsAgreement: Boolean,
    val pendingTerms: List<Terms>,
)

/** 설정 화면이 이름으로 찾아 여는 약관의 코드. */
object TermsCode {
    const val SERVICE = "SERVICE"
    const val POLICY = "POLICY"
}
