package com.example.moamap.feature.onboarding.data.remote

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
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val tokenType: String? = null,
    val expiresIn: Long = 0,
    val refreshTokenExpiresIn: Long = 0,
    val isNewUser: Boolean = false,
)
