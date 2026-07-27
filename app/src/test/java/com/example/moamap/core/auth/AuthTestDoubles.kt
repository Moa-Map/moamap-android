package com.example.moamap.core.auth

/** 메모리에만 토큰을 들고 있는 테스트용 저장소. */
class FakeAuthTokenStore(initial: AuthToken? = null) : AuthTokenStore {

    var token: AuthToken? = initial
        private set

    var clearCount: Int = 0
        private set

    override fun blockingAccessToken(): String? = token?.accessToken

    override suspend fun load(): AuthToken? = token

    override suspend fun save(token: AuthToken) {
        this.token = token
    }

    override suspend fun clear() {
        token = null
        clearCount++
    }
}

/** 정해진 결과만 돌려주고 호출 횟수를 세는 테스트용 갱신기. */
class FakeTokenRefresher(
    private val result: AuthToken? = null,
) : TokenRefresher {

    var callCount: Int = 0
        private set

    override suspend fun refresh(refreshToken: String): AuthToken? {
        callCount++
        return result
    }
}
