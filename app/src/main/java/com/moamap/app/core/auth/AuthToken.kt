package com.moamap.app.core.auth

/**
 * 서버가 발급한 세션 토큰.
 *
 * 서버가 함께 주는 `expiresIn`(액세스 토큰 만료까지 남은 초)은 저장하지 않는다. 만료는 액세스 토큰(JWT) 안의
 * `exp` 로 보내기 전에 보고(`AuthInterceptor`), 그래도 401 이 오면 `TokenAuthenticator` 가 다시 받는다.
 */
data class AuthToken(
    val accessToken: String,
    val refreshToken: String,
)
