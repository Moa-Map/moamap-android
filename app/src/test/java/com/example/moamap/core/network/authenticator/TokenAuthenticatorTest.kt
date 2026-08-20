package com.example.moamap.core.network.authenticator

import com.example.moamap.core.auth.AuthToken
import com.example.moamap.core.auth.FakeAuthTokenStore
import com.example.moamap.core.auth.FakeCurrentUserStore
import com.example.moamap.core.auth.FakeTokenRefresher
import com.example.moamap.core.auth.TokenRefreshResult
import com.example.moamap.core.network.interceptor.AuthInterceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class TokenAuthenticatorTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun clientFor(
        store: FakeAuthTokenStore,
        refresher: FakeTokenRefresher,
        userStore: FakeCurrentUserStore = FakeCurrentUserStore(),
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(store))
        .authenticator(TokenAuthenticator(store, userStore) { refresher })
        .build()

    private fun OkHttpClient.get() =
        newCall(Request.Builder().url(server.url("/api/v1/users/me")).build()).execute()

    @Test
    fun `401을 받으면 토큰을 갱신하고 원래 요청을 재시도한다`() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200))

        val store = FakeAuthTokenStore(AuthToken("old-access", "old-refresh"))
        val refresher = FakeTokenRefresher(
            TokenRefreshResult.Success(AuthToken("new-access", "new-refresh"))
        )

        val response = clientFor(store, refresher).get()

        assertEquals(200, response.code)
        response.close()

        assertEquals("Bearer old-access", server.takeRequest().getHeader("Authorization"))
        assertEquals("Bearer new-access", server.takeRequest().getHeader("Authorization"))
        assertEquals(AuthToken("new-access", "new-refresh"), store.token)
    }

    /**
     * 갱신은 토큰만 바꾼다. 신원은 그대로다.
     *
     * 토큰과 신원을 한 저장소에 묶지 않은 이유가 이것이다. 갱신 경로가 신원까지 다시 실어
     * 보내야 하는 구조였다면, 한 번만 빠뜨려도 갱신 직후 내 글이 남의 글로 보인다.
     */
    @Test
    fun `토큰을 갱신해도 사용자 식별자는 그대로다`() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200))

        val store = FakeAuthTokenStore(AuthToken("old-access", "old-refresh"))
        val userStore = FakeCurrentUserStore(42L)
        val refresher = FakeTokenRefresher(
            TokenRefreshResult.Success(AuthToken("new-access", "new-refresh")),
        )

        clientFor(store, refresher, userStore).get().close()

        assertEquals(AuthToken("new-access", "new-refresh"), store.token)
        assertEquals(42L, userStore.userId)
        assertEquals(0, userStore.clearCount)
    }

    /**
     * 세션이 끝났으므로 신원도 함께 지운다. 남겨두면 다음 사람이 이 기기에 로그인했을 때
     * 남의 글이 자기 것으로 보인다.
     */
    @Test
    fun `서버가 리프레시 토큰을 거부하면 사용자 식별자도 지운다`() {
        server.enqueue(MockResponse().setResponseCode(401))

        val store = FakeAuthTokenStore(AuthToken("old-access", "old-refresh"))
        val userStore = FakeCurrentUserStore(42L)
        val refresher = FakeTokenRefresher(TokenRefreshResult.Rejected)

        clientFor(store, refresher, userStore).get().close()

        assertNull(userStore.userId)
    }

    /** 통신이 잠깐 끊긴 것뿐이라면 세션을 유지한다. 신원도 그대로 둔다. */
    @Test
    fun `일시적 실패에는 사용자 식별자를 지우지 않는다`() {
        server.enqueue(MockResponse().setResponseCode(401))

        val store = FakeAuthTokenStore(AuthToken("old-access", "old-refresh"))
        val userStore = FakeCurrentUserStore(42L)
        val refresher = FakeTokenRefresher(TokenRefreshResult.Failed)

        clientFor(store, refresher, userStore).get().close()

        assertEquals(42L, userStore.userId)
    }

    @Test
    fun `서버가 리프레시 토큰을 거부하면 토큰을 지우고 포기한다`() {
        server.enqueue(MockResponse().setResponseCode(401))

        val store = FakeAuthTokenStore(AuthToken("old-access", "old-refresh"))
        val refresher = FakeTokenRefresher(TokenRefreshResult.Rejected)

        val response = clientFor(store, refresher).get()

        assertEquals(401, response.code)
        response.close()

        assertNull(store.token)
        assertEquals(1, store.clearCount)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `일시적인 갱신 실패에는 세션을 지우지 않는다`() {
        // 통신이 잠깐 끊겼다는 이유로 재로그인을 강요하면 안 된다.
        server.enqueue(MockResponse().setResponseCode(401))

        val store = FakeAuthTokenStore(AuthToken("old-access", "old-refresh"))
        val refresher = FakeTokenRefresher(TokenRefreshResult.Failed)

        val response = clientFor(store, refresher).get()

        assertEquals(401, response.code)
        response.close()

        assertEquals(AuthToken("old-access", "old-refresh"), store.token)
        assertEquals(0, store.clearCount)
    }

    @Test
    fun `갱신한 토큰으로도 401이면 무한 재시도하지 않는다`() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401))

        val store = FakeAuthTokenStore(AuthToken("old-access", "old-refresh"))
        val refresher = FakeTokenRefresher(
            TokenRefreshResult.Success(AuthToken("new-access", "new-refresh"))
        )

        val response = clientFor(store, refresher).get()

        assertEquals(401, response.code)
        response.close()

        // 최초 요청 1회 + 갱신 후 재시도 1회에서 멈춰야 한다.
        assertEquals(2, server.requestCount)
        assertEquals(1, refresher.callCount)
    }

    @Test
    fun `애초에 토큰을 싣지 않은 요청은 갱신하지 않는다`() {
        server.enqueue(MockResponse().setResponseCode(401))

        val store = FakeAuthTokenStore()
        val refresher = FakeTokenRefresher(
            TokenRefreshResult.Success(AuthToken("new-access", "new-refresh"))
        )

        val response = clientFor(store, refresher).get()

        assertEquals(401, response.code)
        response.close()

        assertEquals(0, refresher.callCount)
        assertEquals(1, server.requestCount)
    }
}
