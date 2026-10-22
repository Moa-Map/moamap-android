package com.moamap.app.feature.terms.domain

import com.moamap.app.core.network.ApiException
import com.moamap.app.feature.terms.domain.repository.TermsRepository
import kotlinx.coroutines.CancellationException
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED

/** 스플래시와 로그인 직후에 이 세션의 약관 동의 상태(`/agreements/me`)를 본 결과. */
enum class TermsAgreementCheck {
    /** 동의할 약관이 남았다. 동의 화면으로 간다. */
    NeedsAgreement,

    /** 다 동의했다. 홈으로 간다. */
    Agreed,

    /** 401. 세션이 끝났다. 로그인 화면으로 간다. */
    SessionExpired,

    /** 네트워크 끊김·5xx 등. 홈으로 그냥 보내지도, 로그아웃하지도 않고 다시 시도하게 한다. */
    Failed,
}

/** 확인에 실패했을 때 스플래시·로그인 화면에 띄우는 안내. */
internal const val TERMS_CHECK_FAILED_MESSAGE = "약관 동의 여부를 확인하지 못했어요"

/** 동의 상태를 확인한다. 실패해도 던지지 않고 결과로 돌려준다. */
suspend fun TermsRepository.checkAgreement(): TermsAgreementCheck = try {
    if (getAgreementStatus().needsAgreement) TermsAgreementCheck.NeedsAgreement else TermsAgreementCheck.Agreed
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    if (e.isUnauthorized()) TermsAgreementCheck.SessionExpired else TermsAgreementCheck.Failed
}

/** 토큰 갱신까지 실패해 끝내 401 을 받았다. */
internal fun Throwable.isUnauthorized(): Boolean = this is ApiException && status == HTTP_UNAUTHORIZED
