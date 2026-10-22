package com.moamap.app.feature.terms.domain

import com.moamap.app.core.network.ApiException
import com.moamap.app.feature.onboarding.domain.repository.AuthRepository
import com.moamap.app.feature.terms.domain.repository.TermsRepository
import kotlinx.coroutines.CancellationException
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED

/** 스플래시와 로그인 직후에 이 세션의 약관 동의 상태(`/agreements/me`)를 본 결과. */
enum class TermsAgreementCheck {
    /** 동의할 약관이 남았다. 동의 화면으로 간다. */
    NeedsAgreement,

    /** 다 동의했다. 홈으로 간다. */
    Agreed,

    /** 서버가 토큰 갱신을 거부해 세션이 끝났다([isSessionExpired]). 로그인 화면으로 간다. */
    SessionExpired,

    /** 네트워크 끊김·5xx·일시적인 토큰 갱신 실패 등. 홈으로 그냥 보내지도, 로그아웃하지도 않고 다시 시도하게 한다. */
    Failed,
}

/** 확인에 실패했을 때 스플래시·로그인 화면에 띄우는 안내. */
internal const val TERMS_CHECK_FAILED_MESSAGE = "약관 동의 여부를 확인하지 못했어요"

/** 동의 상태를 확인한다. 실패해도 던지지 않고 결과로 돌려준다. */
suspend fun TermsRepository.checkAgreement(authRepository: AuthRepository): TermsAgreementCheck = try {
    if (getAgreementStatus().needsAgreement) TermsAgreementCheck.NeedsAgreement else TermsAgreementCheck.Agreed
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    if (e.isSessionExpired(authRepository)) TermsAgreementCheck.SessionExpired else TermsAgreementCheck.Failed
}

/**
 * 이 실패가 세션이 끝났다는 뜻인지.
 *
 * 401 만으로는 가를 수 없다. 토큰 갱신이 네트워크 오류·5xx 로 잠깐 실패해도(`TokenRefreshResult.Failed`) 원래 요청의
 * 401 이 그대로 올라온다. 서버가 갱신을 거부했을 때만 `TokenAuthenticator` 가 토큰을 지우므로, 401 뒤 세션이 비었을
 * 때만 끝난 것으로 본다. 토큰이 남았으면 다시 시도하면 되는 실패라 로그아웃하면 안 된다 - 서버 리프레시 토큰까지 버린다.
 */
internal suspend fun Throwable.isSessionExpired(authRepository: AuthRepository): Boolean =
    this is ApiException && status == HTTP_UNAUTHORIZED && !authRepository.hasSession()
