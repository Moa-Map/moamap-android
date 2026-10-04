package com.moamap.app.core.auth

/**
 * 지금 세션이 동의한 약관 버전을 기기에 보관한다.
 *
 * 로그인할 때 지우고 약관에 동의하면 남긴다. 앱을 켤 때 이 값이 앱의 약관 버전과 다르면 로그아웃해
 * 다시 동의를 받는다 - 이 기능 전부터 로그인해 있던 사람, 약관이 바뀐 뒤 처음 켠 사람, 동의 화면에서
 * 앱을 끈 사람이다.
 *
 * 동의 기록 자체는 아니다. 동의 화면은 로그인할 때마다 뜬다(10-05 사용자 결정).
 */
interface AgreedTermsVersionStore {

    /** 동의한 약관 버전. 없으면 null 이다. */
    suspend fun load(): String?

    suspend fun save(version: String)

    suspend fun clear()
}
