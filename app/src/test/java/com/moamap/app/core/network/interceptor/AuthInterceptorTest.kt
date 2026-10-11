package com.moamap.app.core.network.interceptor

import com.moamap.app.core.auth.AuthToken
import com.moamap.app.core.auth.FakeAuthTokenStore
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.ByteString.Companion.encodeUtf8
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

    private fun call(path: String, store: FakeAuthTokenStore, anonymous: Boolean = false) {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(store))
            .build()
        server.enqueue(MockResponse().setResponseCode(200))
        val request = Request.Builder().url(server.url(path))
            .apply { if (anonymous) header(ANONYMOUS_REQUEST_HEADER, "true") }
            .build()
        client.newCall(request).execute().close()
    }

    @Test
    fun `로그인 없이 보내라는 표시가 있으면 토큰도 표시도 싣지 않는다`() {
        val store = FakeAuthTokenStore(AuthToken("access-1", "refresh-1"))

        call("/api/v1/maps", store, anonymous = true)

        val recorded = server.takeRequest()
        assertNull(recorded.getHeader("Authorization"))
        assertNull(recorded.getHeader(ANONYMOUS_REQUEST_HEADER))
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

    // ---- 보내기 전에 미리 새로 받기 (이슈 #302) ----

    private val now = 1_800_000_000L

    /** 서명은 보지 않으니 머리·서명은 아무 값이다. 서버처럼 패딩 없는 URL-safe Base64 로 싣는다. */
    private fun jwt(exp: Long) = "e30." + "{\"exp\":$exp}".encodeUtf8().base64Url().trimEnd('=') + ".sig"

    private fun send(interceptor: AuthInterceptor, path: String = "/api/v1/maps/1", anonymous: Boolean = false) {
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        server.enqueue(MockResponse().setResponseCode(200))
        val request = Request.Builder().url(server.url(path))
            .apply { if (anonymous) header(ANONYMOUS_REQUEST_HEADER, "true") }
            .build()
        client.newCall(request).execute().close()
    }

    @Test
    fun `JWT 에서 만료 시각을 읽는다`() {
        assertEquals(now, jwtExpiresAtSeconds(jwt(now)))
        assertNull(jwtExpiresAtSeconds("access-1"))
        assertNull(jwtExpiresAtSeconds("a.!!!.c"))
        assertNull(jwtExpiresAtSeconds("e30." + "{\"sub\":\"1\"}".encodeUtf8().base64Url() + ".sig"))
    }

    @Test
    fun `곧 만료될 토큰은 보내기 전에 새로 받아 싣는다`() {
        val stale = jwt(now + 30)
        val fresh = jwt(now + 1800)
        val asked = mutableListOf<String>()
        val interceptor = AuthInterceptor(
            FakeAuthTokenStore(AuthToken(stale, "refresh-1")),
            refreshToken = { token -> asked += token; fresh },
            nowSeconds = { now },
        )

        send(interceptor)

        assertEquals(listOf(stale), asked)
        assertEquals("Bearer $fresh", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `만료까지 넉넉하면 새로 받지 않는다`() {
        val token = jwt(now + 1800)
        var asked = 0
        val interceptor = AuthInterceptor(
            FakeAuthTokenStore(AuthToken(token, "refresh-1")),
            refreshToken = { asked++; null },
            nowSeconds = { now },
        )

        send(interceptor)

        assertEquals(0, asked)
        assertEquals("Bearer $token", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `새로 받지 못하면 원래 토큰으로 보낸다`() {
        val stale = jwt(now - 10)
        val interceptor = AuthInterceptor(
            FakeAuthTokenStore(AuthToken(stale, "refresh-1")),
            refreshToken = { null },
            nowSeconds = { now },
        )

        send(interceptor)

        assertEquals("Bearer $stale", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `막 받은 토큰도 만료로 보이면 시계가 틀린 것이라 다음부터는 미리 받지 않는다`() {
        var asked = 0
        // 기기 시계가 서버보다 한참 앞서 있다. 서버가 새로 준 토큰도 이 시계로는 이미 지났다.
        val interceptor = AuthInterceptor(
            FakeAuthTokenStore(AuthToken(jwt(now - 7200), "refresh-1")),
            refreshToken = { asked++; jwt(now - 5400) },
            nowSeconds = { now },
        )

        send(interceptor)
        send(interceptor)

        assertEquals(1, asked)
    }

    @Test
    fun `로그인 없이 보내는 요청은 토큰이 만료돼도 새로 받지 않는다`() {
        var asked = 0
        val interceptor = AuthInterceptor(
            FakeAuthTokenStore(AuthToken(jwt(now - 10), "refresh-1")),
            refreshToken = { asked++; null },
            nowSeconds = { now },
        )

        send(interceptor, path = "/api/v1/maps", anonymous = true)

        assertEquals(0, asked)
        assertNull(server.takeRequest().getHeader("Authorization"))
    }
}
