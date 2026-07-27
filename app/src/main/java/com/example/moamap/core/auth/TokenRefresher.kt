package com.example.moamap.core.auth

/**
 * 갱신 시도의 결과.
 *
 * "실패"를 한 덩어리로 다루면 일시적인 네트워크 장애만으로도 세션을 지우게 된다.
 * 서버가 거부한 것([Rejected])과 닿지 못한 것([Failed])을 반드시 구분한다.
 */
sealed interface TokenRefreshResult {

    data class Success(val token: AuthToken) : TokenRefreshResult

    /** 서버가 리프레시 토큰을 거부했다. 재로그인 외에는 방법이 없다. */
    data object Rejected : TokenRefreshResult

    /** 서버에 닿지 못했거나 응답이 온전하지 않다. 세션은 그대로 두고 다음 기회에 다시 시도한다. */
    data object Failed : TokenRefreshResult
}

/**
 * 만료된 액세스 토큰을 새로 발급받는다.
 *
 * 갱신 API 는 `feature/onboarding` 의 `AuthService` 가 갖고 있지만, 네트워크 계층(core)이
 * feature 를 직접 참조하지 않도록 이 인터페이스를 경계로 둔다. 구현은 onboarding 쪽에 있다.
 */
interface TokenRefresher {

    /** 저장은 호출부가 한다. */
    suspend fun refresh(refreshToken: String): TokenRefreshResult
}
