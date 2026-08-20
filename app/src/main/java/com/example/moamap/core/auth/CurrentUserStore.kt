package com.example.moamap.core.auth

/**
 * 로그인한 사람이 누구인지 기기에 보관한다.
 *
 * 토큰([AuthTokenStore])과 따로 둔다. 토큰은 갱신될 때마다 새로 저장되지만 신원은 그대로다.
 */
interface CurrentUserStore {

    /** 저장된 식별자. 없으면 null 이다. */
    suspend fun load(): Long?

    /** 0 이하는 서버가 식별자를 주지 않았다는 뜻이라 저장하지 않는다. */
    suspend fun save(userId: Long)

    suspend fun clear()
}
