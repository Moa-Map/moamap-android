package com.moamap.app.core.network.interceptor

import com.moamap.app.core.auth.AuthToken
import com.moamap.app.core.auth.FakeAuthTokenStore
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {

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

    private fun call(path: String, store: FakeAuthTokenStore) {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(store))
            .build()
        server.enqueue(MockResponse().setResponseCode(200))
        client.newCall(Request.Builder().url(server.url(path)).build()).execute().close()
    }

    @Test
    fun `토큰이 있으면 Bearer 헤더를 붙인다`() {
        val store = FakeAuthTokenStore(AuthToken("access-1", "refresh-1"))

        call("/api/v1/users/me", store)

        assertEquals("Bearer access-1", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `토큰이 없으면 헤더 없이 그대로 보낸다`() {
        call("/api/v1/users/me", FakeAuthTokenStore())

        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `카카오 로그인 요청에는 헤더를 붙이지 않는다`() {
        val store = FakeAuthTokenStore(AuthToken("access-1", "refresh-1"))

        call("/api/v1/auth/kakao/login", store)

        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `토큰 갱신 요청에는 헤더를 붙이지 않는다`() {
        // 만료된 액세스 토큰을 실으면 갱신 자체가 401 로 막힌다.
        val store = FakeAuthTokenStore(AuthToken("expired", "refresh-1"))

        call("/api/v1/auth/token/refresh", store)

        assertNull(server.takeRequest().getHeader("Authorization"))
    }
}
