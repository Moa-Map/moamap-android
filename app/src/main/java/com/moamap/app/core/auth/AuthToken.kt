package com.moamap.app.core.auth

/**
 * 서버가 발급한 세션 토큰.
 *
 * 서버는 만료 정보([expiresIn] 등)도 함께 내려주지만 단위(초/밀리초)와 의미(잔여 시간/절대 시각)가
 * 확인되지 않아 저장하지 않는다. 만료는 401 응답으로만 판단한다.
 */
data class AuthToken(
    val accessToken: String,
    val refreshToken: String,
)
