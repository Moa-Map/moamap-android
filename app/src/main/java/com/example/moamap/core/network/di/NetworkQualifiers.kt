package com.example.moamap.core.network.di

import javax.inject.Qualifier

/**
 * 토큰 갱신 전용 클라이언트.
 *
 * 인증 인터셉터와 Authenticator 가 붙지 않은 최소 구성이다. 갱신 요청까지 인증 경로를 타면
 * 401 -> 갱신 -> 401 -> 갱신 으로 순환하기 때문에 반드시 분리해야 한다.
 *
 * 그 외 모든 API 는 한정자 없는 기본 `Retrofit` 을 쓴다.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TokenRefreshClient
