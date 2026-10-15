package com.moamap.app.feature.onboarding.domain.repository

import android.content.Context

interface AuthRepository {

    /**
     * 카카오 로그인부터 서버 토큰 교환·저장까지 한 번에 처리한다.
     *
     * [context] 는 카카오 SDK 가 로그인 화면을 띄우는 데 쓰는 **Activity 컨텍스트**여야 한다.
     * 어디에도 보관하지 않고 호출 동안에만 사용한다.
     *
     * @throws com.moamap.app.feature.onboarding.domain.model.KakaoLoginCancelledException 사용자가 취소한 경우
     */
    suspend fun loginWithKakao(context: Context)

    /** 서버·카카오 로그아웃을 시도하고, 성공 여부와 무관하게 로컬 세션을 지운다. */
    suspend fun logout()

    /**
     * 카카오 연결을 끊고 서버에서 탈퇴한 뒤 로컬 세션을 지운다. 되돌릴 수 없다.
     *
     * 실패하면 던지고 로컬 세션은 그대로 둔다 - 다시 시도할 수 있어야 한다.
     */
    suspend fun withdraw()

    suspend fun hasSession(): Boolean
}
