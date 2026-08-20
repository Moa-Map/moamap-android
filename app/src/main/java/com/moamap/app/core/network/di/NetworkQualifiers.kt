package com.moamap.app.core.network.di

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

/**
 * 카카오 로컬 API 전용 클라이언트.
 *
 * 우리 서버용과 셋이 다르다.
 * - 주소가 다르다 (`https://dapi.kakao.com/`)
 * - 인증이 다르다. 우리 `AuthInterceptor` 가 붙으면 우리 토큰이 카카오로 나간다
 * - 응답 봉투가 없다. `EnvelopeConverterFactory` 는 `{success, data, error}` 를 벗기는데
 *   카카오는 `{documents, meta}` 를 그대로 준다
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class KakaoLocalClient

/**
 * presigned URL 업로드 전용 클라이언트.
 *
 * 인증 헤더를 붙이지 않는다. presigned URL 은 주소 자체에 서명이 들어 있어서, `Authorization`
 * 헤더가 함께 가면 스토리지가 인증 방식이 겹쳤다고 보고 요청을 거절한다.
 *
 * 이미지 몇 MB 를 올리므로 쓰기 제한 시간도 기본보다 길다.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PresignedUploadClient
