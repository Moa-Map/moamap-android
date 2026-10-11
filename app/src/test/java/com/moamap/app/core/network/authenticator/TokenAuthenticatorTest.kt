package com.moamap.app.core.network.authenticator

import com.moamap.app.core.auth.AuthToken
import com.moamap.app.core.auth.FakeAuthTokenStore
import com.moamap.app.core.auth.FakeCurrentUserStore
import com.moamap.app.core.auth.FakeTokenRefresher
import com.moamap.app.core.auth.SessionEvents
import com.moamap.app.core.auth.TokenRefreshResult
import com.moamap.app.core.network.interceptor.AuthInterceptor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.ByteString.Companion.encodeUtf8
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
        sessionEvents: SessionEvents = SessionEvents(),
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(store))
        .authenticator(TokenAuthenticator(store, userStore, sessionEvents) { refresher })
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

    /** 세션을 지우기만 하면 보던 화면에 남아 이후 요청이 전부 401 로 실패한다. */
    @Test
    fun `서버가 리프레시 토큰을 거부하면 세션 만료를 알린다`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))

        val store = FakeAuthTokenStore(AuthToken("old-access", "old-refresh"))
        val sessionEvents = SessionEvents()

        clientFor(store, FakeTokenRefresher(TokenRefreshResult.Rejected), sessionEvents = sessionEvents)
            .get()
            .close()

        assertNotNull(withTimeoutOrNull(1_000) { sessionEvents.sessionExpired.first() })
    }

    /** 통신이 잠깐 끊긴 것뿐이라 세션이 살아 있다. 로그인 화면으로 보내면 안 된다. */
    @Test
    fun `일시적인 갱신 실패에는 세션 만료를 알리지 않는다`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))

        val store = FakeAuthTokenStore(AuthToken("old-access", "old-refresh"))
        val sessionEvents = SessionEvents()

        clientFor(store, FakeTokenRefresher(TokenRefreshResult.Failed), sessionEvents = sessionEvents)
            .get()
            .close()

        assertNull(withTimeoutOrNull(1_000) { sessionEvents.sessionExpired.first() })
    }

    @Test
    fun `갱신에 성공하면 세션 만료를 알리지 않는다`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200))

        val store = FakeAuthTokenStore(AuthToken("old-access", "old-refresh"))
        val refresher = FakeTokenRefresher(
            TokenRefreshResult.Success(AuthToken("new-access", "new-refresh")),
        )
        val sessionEvents = SessionEvents()

        clientFor(store, refresher, sessionEvents = sessionEvents).get().close()

        assertNull(withTimeoutOrNull(1_000) { sessionEvents.sessionExpired.first() })
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

    // ---- 보내기 전에 미리 새로 받기 (이슈 #302): 401 때와 같은 갱신을 쓴다 ----

    private val now = 1_800_000_000L

    private fun jwt(exp: Long) = "e30." + "{\"exp\":$exp}".encodeUtf8().base64Url().trimEnd('=') + ".sig"

    private fun refreshAheadClientFor(
        store: FakeAuthTokenStore,
        refresher: FakeTokenRefresher,
        sessionEvents: SessionEvents = SessionEvents(),
    ): OkHttpClient {
        val authenticator = TokenAuthenticator(store, FakeCurrentUserStore(), sessionEvents) { refresher }
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(store, authenticator::refresh, nowSeconds = { now }))
            .authenticator(authenticator)
            .build()
    }

    /** 공개 경로는 만료 토큰에 401 을 주지 않는다. 보내기 전에 받아 두면 401 왕복 없이 한 번에 간다. */
    @Test
    fun `만료된 토큰은 보내기 전에 한 번 새로 받아 401 없이 보낸다`() {
        server.enqueue(MockResponse().setResponseCode(200))

        val fresh = AuthToken(jwt(now + 1800), "new-refresh")
        val store = FakeAuthTokenStore(AuthToken(jwt(now - 10), "old-refresh"))
        val refresher = FakeTokenRefresher(TokenRefreshResult.Success(fresh))

        refreshAheadClientFor(store, refresher).get().close()

        assertEquals(1, refresher.callCount)
        assertEquals(1, server.requestCount)
        assertEquals("Bearer ${fresh.accessToken}", server.takeRequest().getHeader("Authorization"))
        assertEquals(fresh, store.token)
    }

    /** 리프레시 토큰까지 만료된 세션이다. 공개 화면만 보고 있어도 로그인 화면으로 보낸다(10-11 사용자 결정). */
    @Test
    fun `보내기 전에 새로 받기를 서버가 거부하면 세션을 지우고 만료를 알린다`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))

        val store = FakeAuthTokenStore(AuthToken(jwt(now - 10), "old-refresh"))
        val sessionEvents = SessionEvents()

        refreshAheadClientFor(store, FakeTokenRefresher(TokenRefreshResult.Rejected), sessionEvents)
            .get()
            .close()

        assertNull(store.token)
        assertNotNull(withTimeoutOrNull(1_000) { sessionEvents.sessionExpired.first() })
    }
}
