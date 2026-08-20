package com.moamap.app.feature.onboarding.data.remote

import kotlinx.serialization.Serializable

/** POST api/v1/auth/kakao/login 요청 */
@Serializable
data class KakaoLoginRequestDto(
    val kakaoAccessToken: String,
)

/** POST api/v1/auth/token/refresh 요청 */
@Serializable
data class RefreshRequestDto(
    val refreshToken: String,
)

/** POST api/v1/auth/logout 요청 */
@Serializable
data class LogoutRequestDto(
    val refreshToken: String,
)

/** 로그인·토큰 갱신 응답 */
@Serializable
data class TokenDto(
    /**
     * 로그인한 사용자 식별자.
     *
     * 갱신 응답도 같은 스키마를 쓰지만 거기서는 읽지 않는다. 신원은 토큰을 갱신해도 그대로다.
     *
     * 빠져 있으면 0 이다. 로그인 경로가 이 값을 검사해 세션 자체를 막는다.
     */
    val userId: Long = 0,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val tokenType: String? = null,
    val expiresIn: Long = 0,
    val refreshTokenExpiresIn: Long = 0,
    val isNewUser: Boolean = false,
)
