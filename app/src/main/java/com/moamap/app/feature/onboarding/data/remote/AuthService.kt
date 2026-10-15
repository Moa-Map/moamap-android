package com.moamap.app.feature.onboarding.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST

interface AuthService {

    @POST("api/v1/auth/kakao/login")
    suspend fun kakaoLogin(@Body request: KakaoLoginRequestDto): TokenDto

    @POST("api/v1/auth/token/refresh")
    suspend fun refresh(@Body request: RefreshRequestDto): TokenDto

    // 본문 없는 성공 응답 → EnvelopeConverterFactory 가 Unit 으로 처리한다.
    @POST("api/v1/auth/logout")
    suspend fun logout(@Body request: LogoutRequestDto)

    /** 회원 탈퇴. 경로는 사용자 API 지만 세션을 끝내는 일이라 로그인·로그아웃과 함께 둔다. 로그인 토큰을 실어 보낸다. */
    @DELETE("api/v1/users/me")
    suspend fun withdraw()
}
