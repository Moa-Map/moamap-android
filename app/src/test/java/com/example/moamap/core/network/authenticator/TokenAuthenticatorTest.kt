package com.example.moamap.core.network.authenticator

import com.example.moamap.core.auth.AuthToken
import com.example.moamap.core.auth.FakeAuthTokenStore
import com.example.moamap.core.auth.FakeTokenRefresher
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
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(store))
        .authenticator(TokenAuthenticator(store) { refresher })
        .build()

    private fun OkHttpClient.get() =
        newCall(Request.Builder().url(server.url("/api/v1/users/me")).build()).execute()

    @Test
    fun `401을 받으면 토큰을 갱신하고 원래 요청을 재시도한다`() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200))

        val store = FakeAuthTokenStore(AuthToken("old-access", "old-refresh"))
        val refresher = FakeTokenRefresher(AuthToken("new-access", "new-refresh"))

        val response = clientFor(store, refresher).get()

        assertEquals(200, response.code)
        response.close()

        assertEquals("Bearer old-access", server.takeRequest().getHeader("Authorization"))
        assertEquals("Bearer new-access", server.takeRequest().getHeader("Authorization"))
        assertEquals(AuthToken("new-access", "new-refresh"), store.token)
    }

    @Test
    fun `갱신에 실패하면 토큰을 지우고 포기한다`() {
        server.enqueue(MockResponse().setResponseCode(401))

        val store = FakeAuthTokenStore(AuthToken("old-access", "old-refresh"))
        val refresher = FakeTokenRefresher(result = null)

        val response = clientFor(store, refresher).get()

        assertEquals(401, response.code)
        response.close()

        assertNull(store.token)
        assertEquals(1, store.clearCount)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `갱신한 토큰으로도 401이면 무한 재시도하지 않는다`() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401))

        val store = FakeAuthTokenStore(AuthToken("old-access", "old-refresh"))
        val refresher = FakeTokenRefresher(AuthToken("new-access", "new-refresh"))

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
        val refresher = FakeTokenRefresher(AuthToken("new-access", "new-refresh"))

        val response = clientFor(store, refresher).get()

        assertEquals(401, response.code)
        response.close()

        assertEquals(0, refresher.callCount)
        assertEquals(1, server.requestCount)
    }
}
