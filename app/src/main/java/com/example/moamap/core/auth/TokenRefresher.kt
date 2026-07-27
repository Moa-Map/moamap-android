package com.example.moamap.core.auth

/**
 * 만료된 액세스 토큰을 새로 발급받는다.
 *
 * 갱신 API 는 `feature/onboarding` 의 `AuthService` 가 갖고 있지만, 네트워크 계층(core)이
 * feature 를 직접 참조하지 않도록 이 인터페이스를 경계로 둔다. 구현은 onboarding 쪽에 있다.
 */
interface TokenRefresher {

    /** 갱신에 성공하면 새 토큰, 실패하면 null 을 돌려준다. 저장은 호출부가 한다. */
    suspend fun refresh(refreshToken: String): AuthToken?
}
