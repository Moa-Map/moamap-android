package com.example.moamap.feature.onboarding.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {

    @POST("api/v1/auth/kakao/login")
    suspend fun kakaoLogin(@Body request: KakaoLoginRequestDto): TokenDto

    @POST("api/v1/auth/token/refresh")
    suspend fun refresh(@Body request: RefreshRequestDto): TokenDto

    // 본문 없는 성공 응답 → EnvelopeConverterFactory 가 Unit 으로 처리한다.
    @POST("api/v1/auth/logout")
    suspend fun logout(@Body request: LogoutRequestDto)
}
