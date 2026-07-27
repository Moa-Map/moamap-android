package com.example.moamap.feature.onboarding.domain.model

/**
 * 사용자가 카카오 로그인을 직접 취소했다.
 *
 * 실패가 아니라 사용자의 선택이므로 화면에 에러 메시지를 띄우지 않는다.
 */
class KakaoLoginCancelledException : Exception("사용자가 카카오 로그인을 취소했습니다.")
