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

/** 메모리에만 식별자를 들고 있는 테스트용 저장소. */
class FakeCurrentUserStore(initial: Long? = null) : CurrentUserStore {

    var userId: Long? = initial
        private set

    var clearCount: Int = 0
        private set

    /** 디스크 쓰기가 실패하는 상황을 만들 때 채운다. */
    var saveError: Exception? = null

    override suspend fun load(): Long? = userId

    override suspend fun save(userId: Long) {
        saveError?.let { throw it }
        if (userId > 0) this.userId = userId
    }

    override suspend fun clear() {
        userId = null
        clearCount++
    }
}

/** 정해진 결과만 돌려주고 호출 횟수를 세는 테스트용 갱신기. */
class FakeTokenRefresher(
    private val result: TokenRefreshResult = TokenRefreshResult.Rejected,
) : TokenRefresher {

    var callCount: Int = 0
        private set

    override suspend fun refresh(refreshToken: String): TokenRefreshResult {
        callCount++
        return result
    }
}
